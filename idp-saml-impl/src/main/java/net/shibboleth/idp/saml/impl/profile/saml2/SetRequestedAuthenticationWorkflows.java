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

package net.shibboleth.idp.saml.impl.profile.saml2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.ActionSupport;

import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextDeclRef;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

/**
 * An authentication action that populates the requested authentication workflows based on the incoming SAML 2
 * {@link AuthnRequest}.
 */
@Events({@Event(id = EventIds.PROCEED_EVENT_ID),
        @Event(id = EventIds.INVALID_MSG_CTX, description = "inbound message was missing or not a SAML 2 AuthnRequest")})
public class SetRequestedAuthenticationWorkflows extends AbstractAuthenticationAction {

    /** Class logger. */
    private Logger log = LoggerFactory.getLogger(SetRequestedAuthenticationWorkflows.class);

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event doExecute(@Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {

        final AuthnRequest authnRequest = getAuthnRequest(profileRequestContext);
        if (authnRequest == null) {
            log.debug("Action {}: inbound message was not an AuthnRequest, unable to proceed", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_MSG_CTX);
        }

        final List<String> requestedWorkflowIds = getRequestedWorkflows(authnRequest);
        log.debug("Action {}: inbound AuthnRequest requested the following workflow IDs: {}", getId(),
                requestedWorkflowIds);

        final Map<String, AuthenticationFlowDescriptor> availableDescriptors =
                authenticationContext.getPotentialWorkflows();

        final HashMap<String, AuthenticationFlowDescriptor> requestedWorkflows =
                new HashMap<String, AuthenticationFlowDescriptor>();
        AuthenticationFlowDescriptor descriptor;
        for (String workflowId : requestedWorkflowIds) {
            descriptor = availableDescriptors.get(workflowId);
            if (descriptor != null) {
                requestedWorkflows.put(workflowId, descriptor);
            } else {
                log.debug("Action {}: ignoring requested workflow {}, it is not an available workflow", getId(),
                        workflowId);
            }
        }

        log.debug(
                "Action {}: inbound AuthnRequest requested, and workflows were configured for, the following workflows {}",
                getId(), requestedWorkflows.keySet());

        return ActionSupport.buildProceedEvent(this);
    }

    /**
     * Gets the {@link AuthnRequest} from the current profile request context.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return the {@link AuthnRequest} or null if there was no inbound message context, no inbound message, or the
     *         inbound message was not an {@link AuthnRequest}
     */
    @Nullable protected AuthnRequest getAuthnRequest(@Nonnull final ProfileRequestContext profileRequestContext) {
        final MessageContext msgCtx = profileRequestContext.getInboundMessageContext();
        if (msgCtx == null) {
            log.debug("Action {}: no inbound message context, no AuthnRequest available", getId());
            return null;
        }

        final Object message = msgCtx.getMessage();
        if (message == null) {
            log.debug("Action {}: no inbound message, no AuthnRequest available", getId());
            return null;
        }

        if (!AuthnRequest.class.isInstance(message)) {
            log.debug("Action {}: inbound message was not a SAML 2 AuthnRequest", getId());
            return null;
        }

        return (AuthnRequest) message;
    }

    /**
     * Extracts the requested authentication workflows from the SAML 2 AuthnRequest.
     * 
     * @param authnRequest the authentication request
     * 
     * @return the list of requested workflows or an empty list if nothing specific was requested
     */
    @Nonnull protected List<String> getRequestedWorkflows(@Nonnull final AuthnRequest authnRequest) {
        final ArrayList<String> requestedRefs = new ArrayList<String>();

        // TODO: break connection between workflow IDs and SAML bits
        final RequestedAuthnContext requestedCtx = authnRequest.getRequestedAuthnContext();
        if (requestedCtx == null) {
            log.debug("Action {}: inbound AuthnRequest did not contain a RequestedAuthnContext, nothing to do",
                    getId());
            return requestedRefs;
        }

        if (requestedCtx.getAuthnContextClassRefs() != null) {
            for (AuthnContextClassRef ref : requestedCtx.getAuthnContextClassRefs()) {
                requestedRefs.add(ref.getAuthnContextClassRef());
            }
        }
        if (requestedCtx.getAuthnContextDeclRefs() != null) {
            for (AuthnContextDeclRef ref : requestedCtx.getAuthnContextDeclRefs()) {
                requestedRefs.add(ref.getAuthnContextDeclRef());
            }
        }

        return requestedRefs;
    }
}