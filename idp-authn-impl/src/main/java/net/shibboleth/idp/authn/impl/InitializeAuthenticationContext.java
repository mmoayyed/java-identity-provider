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

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.authn.AuthenticationRequestContext;
import net.shibboleth.idp.authn.AuthenticationWorkflowDescriptor;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.EventIds;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.IdPSessionContext;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Function;

/**
 * An action that creates a {@link AuthenticationRequestContext} and sets it as a child of the current
 * {@link ProfileRequestContext}.
 */
@Events({@Event(id = EventIds.PROCEED_EVENT_ID)})
public class InitializeAuthenticationContext extends AbstractProfileAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(InitializeAuthenticationContext.class);

    /** Strategy used to look up the current IdP session context if one exists. */
    private Function<ProfileRequestContext, IdPSessionContext> sessionCtxLookupStrategy;

    /**
     * Constructor.
     * <p>
     * Sets {@link #sessionCtxLookupStrategy} to an instance of {@link ChildContextLookup}
     * </p>
     */
    public InitializeAuthenticationContext() {
        super();

        sessionCtxLookupStrategy =
                new ChildContextLookup<ProfileRequestContext, IdPSessionContext>(IdPSessionContext.class, false);
    }

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event doExecute(@Nullable final HttpServletRequest httpRequest,
            @Nullable final HttpServletResponse httpResponse, @Nullable final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {

        final IdPSessionContext sessionCtx = sessionCtxLookupStrategy.apply(profileRequestContext);

        final IdPSession session;
        if (sessionCtx == null) {
            log.debug("Action {}: no user session currently exists", getId());
            session = null;
        } else {
            session = sessionCtx.getIdPSession();
            log.debug("Action {}: user session {} currently exists", getId(), session.getId());
        }

        // TODO get configured authentication mechanisms
        Collection<AuthenticationWorkflowDescriptor> availableFlows = null;

        AuthenticationRequestContext authnCtx = new AuthenticationRequestContext(session, availableFlows);
        profileRequestContext.addSubcontext(authnCtx);

        return ActionSupport.buildProceedEvent(this);
    }
}