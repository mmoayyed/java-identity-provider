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

package net.shibboleth.idp.saml.impl.profile.saml1;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;

import org.opensaml.common.SAMLVersion;
import org.opensaml.saml1.core.RequestAbstractType;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Objects;

/** Checks whether the inbound SAML request has the appropriate version. */
public class CheckRequestVersion extends AbstractIdentityProviderAction<RequestAbstractType, Object> {

    /** Constructor. The ID of this component is set to the name of this class. */
    public CheckRequestVersion() {
        setId(CheckRequestVersion.class.getName());
    }

    /** {@inheritDoc} */
    protected Event doExecute(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final RequestContext springRequestContext,
            final ProfileRequestContext<RequestAbstractType, Object> profileRequestContext) throws ProfileException {

        final RequestAbstractType request = ActionSupport.getRequiredInboundMessage(this, profileRequestContext);

        if (Objects.equal(SAMLVersion.VERSION_10, request.getVersion())
                || Objects.equal(SAMLVersion.VERSION_11, request.getVersion())) {
            return ActionSupport.buildProceedEvent(this);
        }else{
            throw new InvalidMessageVersionException(request.getVersion());
        }
    }

    /** Exception thrown when the incoming message was not an expected SAML version. */
    public static class InvalidMessageVersionException extends ProfileException {

        /** Serial version UID. */
        private static final long serialVersionUID = -872917446217307755L;

        /**
         * Constructor.
         * 
         * @param message exception message
         */
        public InvalidMessageVersionException(String message) {
            super(message);
        }

        /**
         * Constructor.
         * 
         * @param messageVersion SAML version of the message
         */
        public InvalidMessageVersionException(SAMLVersion messageVersion) {
            super("SAML message version was " + messageVersion.toString() + ", expected 1.0 or 1.1");
        }
    }
}