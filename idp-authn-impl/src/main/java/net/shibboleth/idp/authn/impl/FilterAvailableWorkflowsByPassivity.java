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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.ActionSupport;

import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

/**
 * An authentication action that filters out potential authentication workflows if the current request is a passive
 * request and the workflow does not support passive authentication.
 */
@Events({@Event(id = EventIds.PROCEED_EVENT_ID),
        @Event(id = AuthnEventIds.NO_POTENTIAL_WORKFLOW, description = "No potential workflows left after filtering")})
public class FilterAvailableWorkflowsByPassivity extends AbstractAuthenticationAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(FilterAvailableWorkflowsByPassivity.class);

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event doExecute(@Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {

        if (!profileRequestContext.isPassiveProfile()) {
            log.debug("Action {}: current profile request is not a passive request, nothing to do", getId());
            return ActionSupport.buildProceedEvent(this);
        }

        final Map<String, AuthenticationFlowDescriptor> potentialWorkflows =
                authenticationContext.getPotentialFlows();

        final Iterator<Entry<String, AuthenticationFlowDescriptor>> descriptorItr =
                potentialWorkflows.entrySet().iterator();
        AuthenticationFlowDescriptor descriptor;
        while (descriptorItr.hasNext()) {
            descriptor = descriptorItr.next().getValue();
            if (descriptor == null) {
                log.debug("Action {}: null workflow descriptor detected, removing it", getId());
                descriptorItr.remove();
                continue;
            }

            if (descriptor.isPassiveAuthenticationSupported()) {
                log.debug("Action {}: retaining workflow {}, it supports passive authentication", getId(),
                        descriptor.getId());
            } else {
                log.debug("Action {}: removing workflow {}, it does not support passive authentication", getId(),
                        descriptor.getId());
                descriptorItr.remove();
            }
        }

        log.debug("Action {}: potential authentication workflows left after filtering: {}", getId(),
                potentialWorkflows);

        if (potentialWorkflows.size() == 0) {
            return ActionSupport.buildEvent(this, AuthnEventIds.NO_POTENTIAL_WORKFLOW);
        }
        return ActionSupport.buildProceedEvent(this);
    }
}