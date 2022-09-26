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

package net.shibboleth.idp.profile.impl;

import java.io.IOException;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.webflow.execution.RequestContext;

import jakarta.servlet.http.HttpServletResponse;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.SpringRequestContext;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.IdentifiedComponent;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.shared.service.ServiceException;

/**
 * Action that refreshes a {@link ReloadableService} manually.
 * 
 * <p>With the default strategy, the service to reload is indicated by supplying {@link #SERVICE_ID}
 * as a flow variable.</p>
 * 
 * <p>On success, a 200 HTTP status with a simple response body is returned. On failure, a non-successful
 * HTTP status is returned.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_MESSAGE}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#IO_ERROR}
 */
public class ReloadServiceConfiguration extends AbstractProfileAction {
    
    /** Flow variable indicating ID of service bean to reload. */
    @Nonnull @NotEmpty public static final String SERVICE_ID = "serviceId";
    
    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(ReloadServiceConfiguration.class);
    
    /** Lookup function to locate service bean to operate on. */
    @Nonnull private Function<ProfileRequestContext,ReloadableService<?>> serviceLookupStrategy;
    
    /** The service to reload. */
    @Nullable private ReloadableService<?> service;
    
    /** Constructor. */
    public ReloadServiceConfiguration() {
        serviceLookupStrategy = new WebFlowApplicationContextLookupStrategy();
    }

    /**
     * Set the lookup strategy for the service object to reload.
     * 
     * @param strategy  lookup strategy
     */
    public void setServiceLookupStrategy(@Nonnull final Function<ProfileRequestContext,ReloadableService<?>> strategy) {
        checkSetterPreconditions();
        serviceLookupStrategy = Constraint.isNotNull(strategy, "ReloadableService lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(final ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        } else if (getHttpServletResponse() == null) {
            log.debug("{} No HttpServletResponse available", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        service = serviceLookupStrategy.apply(profileRequestContext);
        if (service == null) {
            log.warn("{} Unable to locate service to reload", getLogPrefix());
            try {
                getHttpServletResponse().sendError(HttpServletResponse.SC_NOT_FOUND, "Service not found.");
            } catch (final IOException e) {
                ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
            }
            return false;
        }
        
        return true;
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(final ProfileRequestContext profileRequestContext) {
        
        final String id;
        if (service instanceof IdentifiedComponent) {
            id = ((IdentifiedComponent) service).getId();
        } else {
            id = "(unnamed)";
        }
        
        log.debug("{} Reloading configuration for '{}'", getLogPrefix(), id);
        
        try {
            service.reload();
            log.debug("{} Reloaded configuration for '{}'", getLogPrefix(), id);
            getHttpServletResponse().setStatus(HttpServletResponse.SC_OK);
            getHttpServletResponse().getWriter().println("Configuration reloaded for '" + id + "'");
        } catch (final ServiceException e) {
            log.error("{} Error reloading service configuration for '{}'", getLogPrefix(), id);
            try {
                getHttpServletResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (final IOException e2) {
                log.error("{} I/O error responding to request", getLogPrefix(), e2);
                ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
            }
        } catch (final IOException e) {
            log.error("{} I/O error responding to request", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
        }
    }
    
    /**
     * Default strategy locates a bean identified with a flow-scope parameter in the web flow application context.
     */
    private class WebFlowApplicationContextLookupStrategy
            implements Function<ProfileRequestContext,ReloadableService<?>> {

        /** {@inheritDoc} */
        @Override
        @Nullable public ReloadableService<?> apply(@Nullable final ProfileRequestContext input) {
            
            final SpringRequestContext springRequestContext = input.getSubcontext(SpringRequestContext.class);
            if (springRequestContext == null) {
                log.warn("{} Spring request context not found in profile request context", getLogPrefix());
                return null;
            }

            final RequestContext requestContext = springRequestContext.getRequestContext();
            if (requestContext == null) {
                log.warn("{} Web Flow request context not found in Spring request context", getLogPrefix());
                return null;
            }

            final String id = (String) requestContext.getFlowScope().get(SERVICE_ID);
            if (id == null) {
                log.warn("{} No {} flow variable found in request", getLogPrefix(), SERVICE_ID);
                return null;
            }
            
            try {
                final Object bean = requestContext.getActiveFlow().getApplicationContext().getBean(id);
                if (bean != null && bean instanceof ReloadableService) {
                    return (ReloadableService<?>) bean;
                }
            } catch (final BeansException e) {
                
            }
            
            log.warn("{} No bean of the correct type found named {}", getLogPrefix(), id);
            return null;
        }
        
    }

}