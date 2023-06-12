/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.admin.impl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.springframework.core.annotation.AnnotationUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.annotation.ConfigurationSetting;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.NullableElements;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.component.IdentifiableComponent;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Action that outputs the settings from the effective {@link ProfileConfiguration} and so on.
 * 
 * <p>On success, a 200 HTTP status is returned. On failure, a non-successful HTTP status is returned.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link IdPEventIds#INVALID_RELYING_PARTY_CTX}
 * @event {@link IdPEventIds#INVALID_RELYING_PARTY_CONFIG}
 * @event {@link IdPEventIds#INVALID_PROFILE_CONFIG}
 * @event {@link EventIds#IO_ERROR}
 * 
 * @since 5.0.0
 */
public class OutputConfig extends AbstractProfileAction {
    
    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(OutputConfig.class);
    
    /** Value for Access-Control-Allow-Origin header, if any. */
    @Nullable private String allowedOrigin;
    
    /** Name of JSONP callback function, if any. */
    @Nullable private String jsonpCallbackName;
    
    /** Lookup strategy for {@link RelyingPartyContext}. */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;
    
    /** Relying party context. */
    @NonnullBeforeExec private RelyingPartyContext relyingPartyContext;
    
    /** Constructor. */
    public OutputConfig() {
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
    }
    
    /**
     * Set the value of the Access-Control-Allow-Origin CORS header, if any.
     * 
     * @param origin header value
     */
    public void setAllowedOrigin(@Nullable final String origin) {
        checkSetterPreconditions();
        
        allowedOrigin = StringSupport.trimOrNull(origin);
    }
    
    /**
     * Set a JSONP callback function to wrap the result in, if any.
     * 
     * @param callbackName callback function name.
     */
    public void setJSONPCallbackName(@Nullable final String callbackName) {
        checkSetterPreconditions();
        
        jsonpCallbackName = StringSupport.trimOrNull(callbackName);
    }
    
    /**
     * Set the lookup strategy to locate the {@link RelyingPartyContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        checkSetterPreconditions();
        
        relyingPartyContextLookupStrategy = Constraint.isNotNull(strategy,
                "RelyingPartyContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        relyingPartyContext = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (relyingPartyContext == null) {
            log.warn("{} No RelyingPartyContext available", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return false;
        } else if (relyingPartyContext.getConfiguration() == null) {
            log.warn("{} No RelyingPartyConfiguration available", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CONFIG);
            return false;
        } else if (relyingPartyContext.getProfileConfig() == null) {
            log.warn("{} No ProfileConfiguration available", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_PROFILE_CONFIG);
            return false;
        }
        
        final HttpServletResponse response = getHttpServletResponse();
        if (response == null) {
            log.warn("{} No HttpServletResponse available", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        return true;
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
                
        try {
            final HttpServletResponse response = getHttpServletResponse();
            assert response != null;
            response.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
            response.setStatus(HttpServletResponse.SC_OK);
            if (allowedOrigin != null) {
                response.setHeader("Access-Control-Allow-Origin", allowedOrigin);
            }
            
            final ObjectMapper mapper = new ObjectMapper();
            
            mapper.registerModule(new JavaTimeModule());
            // These don't do much of anything, except the first one I think.
            mapper.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
            mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            mapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
            
            if (jsonpCallbackName != null) {
                response.setContentType("application/javascript");
                mapper.writer().writeValue(response.getOutputStream(), getConfig(profileRequestContext));
            } else {
                response.setContentType("application/json");
                mapper.writer().writeValue(response.getOutputStream(), getConfig(profileRequestContext));
            }
        } catch (final IOException e) {
            log.error("{} I/O error responding to request", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
        }
    }
    
    /**
     * Extract configuration settings into a map to output.
     * 
     * @param profileRequestContext profile request context
     * 
     * @return configuration settings map
     */
    @Nonnull @NullableElements private Map<String,Object> getConfig(
            @Nonnull final ProfileRequestContext profileRequestContext) {
        
        final Map<String,Object> settings = new HashMap<>();
        
        settings.put("RelyingPartyConfiguration",
                getSettings(profileRequestContext, relyingPartyContext.ensureConfiguration()));

        settings.put("ProfileConfiguration",
                getSettings(profileRequestContext, relyingPartyContext.ensureProfileConfig()));

        return settings;
    }
    
    /**
     * Interrogate a target object for configuration settings, extract them, and return in a map.
     * 
     * @param profileRequestContext profile request context
     * @param target target object
     * 
     * @return map of settings
     */
// Checkstyle: CyclomaticComplexity OFF
    @Nonnull @NullableElements @Unmodifiable @NotLive
    private Map<String,Object> getSettings(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final Object target) {
        final Map<String,Object> settings = new HashMap<>();

        // Specially handle ID property.
        if (target instanceof IdentifiableComponent comp) {
            settings.put("id", comp.getId());
        }
        
        final Method[] methods = target.getClass().getMethods();
        for (final Method m : methods) {
            assert m != null;
            // The Spring method deals with the lack of inheritance on method-level annotations.
            final ConfigurationSetting annotation = AnnotationUtils.findAnnotation(m, ConfigurationSetting.class);
            if (annotation == null || annotation.name() == null || annotation.name().isEmpty()) {
                continue;
            }
            final Class<?>[] paramTypes = m.getParameterTypes();
            if (paramTypes.length == 1 && paramTypes[0].isAssignableFrom(ProfileRequestContext.class)) {
                try {
                    final Object ret = m.invoke(target, profileRequestContext);
                    if (ret == null) {
                        continue;
                    }
                    if (ret instanceof IdentifiableComponent comp) {
                        settings.put(annotation.name(), comp.getId());
                    } else if (ret instanceof Collection<?> c) {
                        if (!c.isEmpty()) {
                            settings.put(annotation.name(), ret);
                        }
                    } else if (isPrimitive(ret)) {
                        settings.put(annotation.name(), ret);
                    } else {
                        settings.put(annotation.name(), ret.getClass().getName());
                    }
                } catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    log.error("{} Error introspecting configuration setting '{}'", getLogPrefix(), annotation.name(),
                            e);
                }
            }
        }
        
        return settings;
    }
// Checkstyle: CyclomaticComplexity ON    

    private boolean isPrimitive(@Nullable final Object o) {
        return o instanceof Boolean
                || o instanceof Integer
                || o instanceof Long
                || o instanceof Double
                || o instanceof String
                || o instanceof Duration
                || o instanceof Instant;
    }
    
}