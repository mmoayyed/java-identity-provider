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

package net.shibboleth.idp.authn.impl;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.webflow.execution.RequestContext;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.models.errors.Error;
import com.github.jasminb.jsonapi.models.errors.Errors;
import com.google.common.base.Strings;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.shibboleth.idp.authn.AccountLockoutManager;
import net.shibboleth.idp.authn.context.LockoutManagerContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.SpringRequestContext;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Action that implements a JSON REST API for the {@link AccountLockoutManager} interface.
 * 
 * <p>The API supports GET, POST, and DELETE at the moment, using jsonapi.org conventions.</p>
 * 
 * <dl>
 *  <dt>GET</dt>
 *  <dd>Check for lockout.</dd>
 *  
 *  <dt>POST</dt>
 *  <dd>Increment lockout.</dd>
 *  
 *  <dt>DELETE</dt>
 *  <dd>Clear lockout count.</dd>
 * </dl>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#IO_ERROR}
 */
public class DoLockoutManagerOperation extends AbstractProfileAction {
    
    /** Flow variable indicating ID of manager bean to access. */
    @Nonnull @NotEmpty public static final String MANAGER_ID = "lockoutManagerId";

    /** Flow variable indicating ID of account key. */
    @Nonnull @NotEmpty public static final String KEY = "key";

    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(DoLockoutManagerOperation.class);
    
    /** JSON object mapper. */
    @NonnullAfterInit private ObjectMapper objectMapper;

    /** Manager ID to operate on. */
    @Nullable @NotEmpty private String managerId;

    /** Account key to operate on. */
    @NonnullBeforeExec @NotEmpty private String key;
    
    /** {@link AccountLockoutManager} to operate on. */
    @NonnullBeforeExec private AccountLockoutManager lockoutManager;

    /**
     * Set the JSON {@link ObjectMapper} to use for serialization.
     * 
     * @param mapper object mapper
     */
    public void setObjectMapper(@Nonnull final ObjectMapper mapper) {
        checkSetterPreconditions();
        objectMapper = Constraint.isNotNull(mapper, "ObjectMapper cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (objectMapper == null) {
            throw new ComponentInitializationException("ObjectMapper cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(final @Nonnull ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        } else if (getHttpServletRequest() == null || getHttpServletResponse() == null) {
            log.warn("{} No HttpServletRequest or HttpServletResponse available", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        try {
            final SpringRequestContext springRequestContext =
                    profileRequestContext.getSubcontext(SpringRequestContext.class);
            if (springRequestContext == null) {
                log.warn("{} Spring request context not found in profile request context", getLogPrefix());
                sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Internal Server Error", "System misconfiguration.");
                return false;
            }
    
            final RequestContext requestContext = springRequestContext.getRequestContext();
            if (requestContext == null) {
                log.warn("{} Web Flow request context not found in Spring request context", getLogPrefix());
                sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Internal Server Error", "System misconfiguration.");
                return false;
            }
            
            lockoutManager = setupLockoutManager(requestContext);
            if (lockoutManager == null) {
                sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Invalid Lockout Manager", "Invalid lockout manager identifier in path.");
                return false;
            }
            
            key = (String) requestContext.getFlowScope().get(KEY);
            if (Strings.isNullOrEmpty(key)) {
                sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Missing Account Key", "No account key specified.");
                return false;
            }

        } catch (final IOException e) {
            log.error("{} I/O error issuing API response", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(final @Nonnull ProfileRequestContext profileRequestContext) {

        profileRequestContext.ensureSubcontext(LockoutManagerContext.class).setKey(key);
        
        try {
            final HttpServletRequest request = getHttpServletRequest();
            final HttpServletResponse response = getHttpServletResponse();
            assert response != null && request != null;
            response.setContentType("application/json");
            response.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
            
            if ("GET".equals(request.getMethod())) {
                try {
                    final boolean lockout = getLockoutManager().check(profileRequestContext);
                    response.setStatus(HttpServletResponse.SC_OK);
                    final JsonFactory jsonFactory = new JsonFactory();
                    try (final JsonGenerator g = jsonFactory.createGenerator(
                            response.getOutputStream()).useDefaultPrettyPrinter()) {
                        g.setCodec(objectMapper);
                        g.writeStartObject();
                        g.writeObjectFieldStart("data");
                        g.writeStringField("type", "lockout-statuses");
                        g.writeStringField("id", managerId + '/' + key);
                        g.writeObjectFieldStart("attributes");
                        g.writeBooleanField("lockout", lockout);
                    }
                } catch (final IOException e) {
                    sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error",
                            "Lockout manager error.");
                }
                
            } else if ("POST".equals(request.getMethod())) {
                try {
                    if (getLockoutManager().increment(profileRequestContext)) {
                        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    } else {
                        throw new IOException();
                    }
                } catch (final IOException e) {
                    sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error",
                            "Lockout manager error.");
                }
                
            } else if ("DELETE".equals(request.getMethod())) {
                try {
                    if (getLockoutManager().clear(profileRequestContext)) {
                        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    } else {
                        throw new IOException();
                    }
                } catch (final IOException e) {
                    sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error",
                            "Lockout manager error.");
                }
                
            } else {
                log.warn("{} Invalid method: {}", getLogPrefix(), request.getMethod());
                sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                        "Unknown Operation", "Only GET, POST, and DELETE are supported.");
            }
            
        } catch (final IOException e) {
            log.error("{} I/O error responding to request", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
        }
    }

    /**
     * Helper method to get the manager bean to operate on.
     * 
     * @param requestContext current SWF request context
     * 
     * @return lockout manager or null
     */
    @Nullable private AccountLockoutManager setupLockoutManager(@Nonnull final RequestContext requestContext) {
        
        final String mgrId = this.managerId = (String) requestContext.getFlowScope().get(MANAGER_ID);
        if (mgrId == null) {
            log.warn("{} No {} flow variable found in request", getLogPrefix(), MANAGER_ID);
            return null;
        }
        
        try {
            assert mgrId != null;
            final Object bean = requestContext.getActiveFlow().getApplicationContext().getBean(mgrId);
            if (bean != null && bean instanceof AccountLockoutManager) {
                return (AccountLockoutManager) bean;
            }
        } catch (final BeansException e) {
            
        }
        
        log.warn("{} No bean of the correct type found named {}", getLogPrefix(), mgrId);
        return null;
    }

    /** Null safe getter.
     * @return Returns the lockoutManager.
     */
    @Nonnull private AccountLockoutManager getLockoutManager() {
        assert isPreExecuteCalled();
        return lockoutManager;
    }

    /**
     * Output an error object.
     * 
     * @param status HTTP status
     * @param title fixed error description
     * @param detail human-readable error description
     * 
     * @throws IOException if unable to output the error
     */
    private void sendError(final int status, @Nonnull @NotEmpty final String title,
            @Nonnull @NotEmpty final String detail) throws IOException {
        
        final HttpServletResponse response = getHttpServletResponse();
        assert response != null;
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
        response.setStatus(status);
        
        final Error e = new Error();
        final Errors errors = new Errors();
        errors.setErrors(CollectionSupport.singletonList(e));
        e.setStatus(Integer.toString(status));
        e.setTitle(title);
        e.setDetail(detail);
        
        objectMapper.writer().withDefaultPrettyPrinter().writeValue(response.getOutputStream(), errors);
    }
    
}
