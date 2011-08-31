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

import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileRequestContext;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/** A stage which invokes the {@link AttributeResolver} for the current request. */
public class ResolveAttributes extends AbstractIdentityProviderAction {

    /** Resolver used to fetch attributes. */
    private AttributeResolver attributeResolver;

    /** Constructor. The ID of this component is set to the name of this class. */
    public ResolveAttributes() {
        setId(ResolveAttributes.class.getName());
    }

    /** {@inheritDoc} */
    public Event doExecute(RequestContext springRequestContext, ProfileRequestContext profileRequestContext) {

        // Get the resolution context from the profile request
        // this may already exist but if not, auto-create it
        AttributeResolutionContext resolutionContext =
                profileRequestContext.getSubcontext(AttributeResolutionContext.class, true);

        try {
            attributeResolver.resolveAttributes(resolutionContext);
        } catch (AttributeResolutionException e) {
            // TODO error
            return ActionSupport.buildEvent(this, ActionSupport.ERROR_EVENT_ID, null);
        }

        return ActionSupport.buildEvent(this, ActionSupport.PROCEED_EVENT_ID, null);
    }
}