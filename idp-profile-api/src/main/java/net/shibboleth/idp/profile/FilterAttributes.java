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

package net.shibboleth.idp.profile;

import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringEngine;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/** A stage which invokes the {@link AttributeFilteringEngine} for the current request. */
public class FilterAttributes extends AbstractIdentityProviderAction {

    /** Resolver used to fetch attributes. */
    private AttributeFilteringEngine filterEngine;

    /** Constructor. The ID of this component is set to the name of this class. */
    public FilterAttributes() {
        super(FilterAttributes.class.getName());
    }

    /**
     * Constructor.
     * 
     * @param componentId unique ID for this component
     */
    public FilterAttributes(String componentId) {
        super(componentId);
    }

    /** {@inheritDoc} */
    public Event doExecute(RequestContext springRequestContext, ProfileRequestContext profileRequestContext) {

        // Get the resolution context from the profile request
        // this may already exist but if not, auto-create it
        AttributeFilterContext filterContext = profileRequestContext.getSubcontext(AttributeFilterContext.class, true);

        // If the filter context doesn't have a set of attributes to filter already
        // then look for them in the profile request context
        if (filterContext.getPrefilteredAttributes().isEmpty()) {
            AttributeResolutionContext resolutionContext =
                    profileRequestContext.getSubcontext(AttributeResolutionContext.class, false);
            if (resolutionContext == null) {
                // TODO error
                return ActionSupport.buildEvent(this, ActionSupport.ERROR_EVENT_ID, null);
            }

            filterContext.setPrefilteredAttributes(resolutionContext.getResolvedAttributes().values());
        }

        try {
            filterEngine.filterAttributes(filterContext);
        } catch (AttributeFilteringException e) {
            // TODO error
            return ActionSupport.buildEvent(this, ActionSupport.ERROR_EVENT_ID, null);
        }

        return ActionSupport.buildEvent(this, ActionSupport.PROCEED_EVENT_ID, null);
    }
}