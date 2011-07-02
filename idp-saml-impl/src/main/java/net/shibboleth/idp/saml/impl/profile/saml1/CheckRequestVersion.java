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

import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileRequestContext;

import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.RequestAbstractType;
import org.opensaml.saml2.core.Response;
import org.opensaml.util.ObjectSupport;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/** Checks whether the inbound SAML request has the appropriate version. */
public class CheckRequestVersion extends AbstractIdentityProviderAction<RequestAbstractType, Response> {

    /** Constructor. The ID of this component is set to the name of this class. */
    public CheckRequestVersion() {
        super(CheckRequestVersion.class.getName());
    }

    /**
     * Constructor.
     * 
     * @param componentId unique ID for this component
     */
    public CheckRequestVersion(String componentId) {
        super(componentId);
    }

    /** {@inheritDoc} */
    public Event doExecute(RequestContext springRequestContext,
            ProfileRequestContext<RequestAbstractType, Response> profileRequestContext) {

        RequestAbstractType request = profileRequestContext.getInboundMessageContext().getMessage();
        if (ObjectSupport.equals(SAMLVersion.VERSION_10, request.getVersion())
                || ObjectSupport.equals(SAMLVersion.VERSION_11, request.getVersion())) {
            return ActionSupport.buildEvent(this, ActionSupport.PROCEED_EVENT_ID, null);
        }

        // TODO Error
        return ActionSupport.buildEvent(this, ActionSupport.ERROR_EVENT_ID, null);
    }

}