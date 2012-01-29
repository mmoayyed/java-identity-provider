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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.attribute.AttributeContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.relyingparty.RelyingPartySubcontext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/** A stage which invokes the {@link AttributeResolver} for the current request. */
public class ResolveAttributes extends AbstractIdentityProviderAction {

    /** Class logger. */
    private Logger log = LoggerFactory.getLogger(ResolveAttributes.class);

    /** Resolver used to fetch attributes. */
    private AttributeResolver attributeResolver;

    /** Constructor. The ID of this component is set to the name of this class. */
    public ResolveAttributes() {
        setId(ResolveAttributes.class.getName());
    }

    /** {@inheritDoc} */
    protected Class<RelyingPartySubcontext> getSubcontextType() {
        return RelyingPartySubcontext.class;
    }

    /** {@inheritDoc} */
    protected Event doExecute(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            RequestContext springRequestContext, ProfileRequestContext profileRequestContext) throws ProfileException {

        final RelyingPartySubcontext relyingPartyCtx =
                ActionSupport.getRequiredRelyingPartyContext(this, profileRequestContext);

        // Get the resolution context from the profile request
        // this may already exist but if not, auto-create it
        final AttributeResolutionContext resolutionContext =
                profileRequestContext.getSubcontext(AttributeResolutionContext.class, true);

        try {
            attributeResolver.resolveAttributes(resolutionContext);
            profileRequestContext.removeSubcontext(resolutionContext);

            final AttributeContext attributeCtx = new AttributeContext();
            attributeCtx.setAttributes(resolutionContext.getResolvedAttributes().values());

            relyingPartyCtx.addSubcontext(attributeCtx);
        } catch (AttributeResolutionException e) {
            log.error("Action {}: Error resolving attributes", getId(), e);
            throw new UnableToResolveAttributeException(e);
        }

        return ActionSupport.buildProceedEvent(this);
    }

    /** Exception thrown when there is a problem resolving attributes. */
    public static class UnableToResolveAttributeException extends ProfileException {

        /** Serial version UID. */
        private static final long serialVersionUID = 6696449800131215343L;

        /** Constructor. */
        public UnableToResolveAttributeException() {
            super();
        }

        /**
         * Constructor.
         * 
         * @param message exception message
         */
        public UnableToResolveAttributeException(String message) {
            super(message);
        }

        /**
         * Constructor.
         * 
         * @param wrappedException exception to be wrapped by this one
         */
        public UnableToResolveAttributeException(Exception wrappedException) {
            super(wrappedException);
        }

        /**
         * Constructor.
         * 
         * @param message exception message
         * @param wrappedException exception to be wrapped by this one
         */
        public UnableToResolveAttributeException(String message, Exception wrappedException) {
            super(message, wrappedException);
        }
    }
}