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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.messaging.context.Subcontext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * A base class for actions which check or use the information from a {@link Subcontext} of the
 * {@link ProfileRequestContext}.
 * 
 * @param <SubcontextType> the subcontext type upon which this action operates
 */
public abstract class AbstractProfileRequestSubcontextAction<SubcontextType extends Subcontext> extends
        AbstractIdentityProviderAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractProfileRequestSubcontextAction.class);

    /**
     * Retrieves the profile request subcontext specified by {@link #getSubcontextType()}. If no subcontext is available
     * an error is returned.
     * 
     * {@inheritDoc}
     */
    public Event doExecute(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final RequestContext springRequestContext, final ProfileRequestContext profileRequestContext) {

        final Class<SubcontextType> subcontxtType = getSubcontextType();
        final SubcontextType subcontext = profileRequestContext.getSubcontext(subcontxtType, false);
        if (subcontext == null) {
            log.error("Action {}: ProfileRequestContext does contain a subcontext of type {}", getId(),
                    subcontxtType.getName());
            return ActionSupport.buildErrorEvent(this, new InvalidProfileRequestContextStateException(
                    "ProfileRequestContext does contain a subcontext of type " + subcontxtType.getName()));
        }

        return doExecute(httpRequest, httpResponse, springRequestContext, profileRequestContext, subcontext);
    }

    /**
     * Gets the type of subcontext upon which this action operates.
     * 
     * @return type of subcontext upon which this action operates
     */
    protected abstract Class<SubcontextType> getSubcontextType();

    /**
     * Performs the action operation.
     * 
     * @param httpRequest current HTTP request
     * @param httpResponse current HTTP response
     * @param springRequestContext current WebFlow request context, never null
     * @param profileRequestContext current identity provider profile request context, never null
     * @param subcontext subcontext upon which this action operates, never null
     * 
     * @return the result of this action
     */
    protected abstract Event doExecute(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final RequestContext springRequestContext, final ProfileRequestContext profileRequestContext,
            final SubcontextType subcontext);
}