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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.SpringRequestContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceException;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

/**
 * Action that refreshes a {@link ReloadableService} manually.
 * 
 * <p>The service to reload is indicated by supplying {@link #SERVICE_ID} as a query parameter.</p>
 * 
 * <p>On success, a 200 HTTP status with a simple response body is returned. On failure, a non-successful
 * HTTP status is returned.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#UNABLE_TO_DECODE}
 */
public class ReloadServiceConfiguration extends AbstractProfileAction {
    
    /** Query parameter indicating ID of service bean to reload. */
    @Nonnull @NotEmpty public static final String SERVICE_ID = "id";
    
    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(ReloadServiceConfiguration.class);
    
    /** Service ID. */
    @Nullable private String id;
    
    /** The service to reload. */
    @Nullable private ReloadableService service;

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        } else if (getHttpServletRequest() == null || getHttpServletResponse() == null) {
            log.debug("{} No HttpServletRequest or HttpServletResponse available", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        id = StringSupport.trimOrNull(getHttpServletRequest().getParameter(SERVICE_ID));
        if (id == null) {
            log.debug("{} No 'id' parameter found in request", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.UNABLE_TO_DECODE);
            return false;
        }

        final SpringRequestContext springRequestContext =
                profileRequestContext.getSubcontext(SpringRequestContext.class);
        if (springRequestContext == null) {
            log.debug("{} Spring request context not found in profile request context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }

        final RequestContext requestContext = springRequestContext.getRequestContext();
        if (requestContext == null) {
            log.debug("{} Web Flow request context not found in Spring request context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
                
        final Object bean = requestContext.getActiveFlow().getApplicationContext().getBean(id);
        if (bean == null || !(bean instanceof ReloadableService)) {
            log.debug("{} No bean of the correct type found named {}", getLogPrefix(), id);
            getHttpServletResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }
        
        return true;
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(ProfileRequestContext profileRequestContext) {
        
        log.debug("{} Reloading configuration for {}", getLogPrefix(), id);
        
        try {
            service.reload();
            log.debug("{} Reloaded configuration for {}", getLogPrefix(), id);
        } catch (final ServiceException e) {
            log.error("{} Error reloading service configuration for {}", getLogPrefix(), id, e);
        }
    }

}