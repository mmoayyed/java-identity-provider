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

package net.shibboleth.idp.session.impl;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.SessionResolver;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.idp.session.criterion.HttpServletRequestCriterion;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.AbstractProfileAction;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A profile action that populates a {@link SessionContext} with an active, valid
 * {@link IdPSession} as a direct child of the {@link ProfileRequestContext}.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @post As above, and the session will be bound to the client's address if the underlying
 *  {@link SessionManager} is configured to do so.
 */
public class PopulateSessionContext extends AbstractProfileAction {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateSessionContext.class);
    
    /** Session resolver. */
    @NonnullAfterInit private SessionResolver sessionResolver;

    /**
     * Set the {@link SessionResolver} to use.
     * 
     * @param resolver  session resolver to use
     */
    public void setSessionResolver(@Nonnull final SessionResolver resolver) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        sessionResolver = Constraint.isNotNull(resolver, "SessionResolver cannot be null");
    }
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (sessionResolver == null) {
            throw new ComponentInitializationException("SessionResolver cannot be null");
        }
    }

    /** {@inheritDoc} */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {
        IdPSession session = null;
        try {
            session = sessionResolver.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
            if (session == null) {
                log.debug("{} No session found for client", getLogPrefix());
                return;
            } else if (!session.checkTimeout()) {
                log.info("{} Session {} no longer valid due to inactivity", getLogPrefix(), session.getId());
                return;
            }
            
            HttpServletRequest request = getHttpServletRequest();
            if (request != null && request.getRemoteAddr() != null) {
                if (!session.checkAddress(request.getRemoteAddr())) {
                    return;
                }
            } else {
                log.info("{} No servlet request or client address available, skipping address check for session {}",
                        getLogPrefix(), session.getId());
            }
            
            profileRequestContext.getSubcontext(SessionContext.class, true).setIdPSession(session);
            
        } catch (ResolverException e) {
            log.error(getLogPrefix() + " Error resolving a session for the active client", e);
        } catch (SessionException e) {
            log.error(getLogPrefix() + " Error during timeout or address checking for session " + session.getId(), e);
        }
    }

}