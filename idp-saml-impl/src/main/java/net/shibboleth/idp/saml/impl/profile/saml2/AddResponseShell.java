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

import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileRequestContext;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.xml.Configuration;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * A profile action that creates a {@link Response}, adds a {@link StatusCode#SUCCESS} status to it, and sets it as the
 * message for the {@link ProfileRequestContext#getOutboundMessageContext()}.
 */
public class AddResponseShell extends AbstractIdentityProviderAction<Object, Response> {

    /** {@inheritDoc} */
    public Event doExecute(RequestContext springRequestContext,
            ProfileRequestContext<Object, Response> profileRequestContext) {
        final SAMLObjectBuilder<StatusCode> statusCodeBuilder =
                (SAMLObjectBuilder<StatusCode>) Configuration.getBuilderFactory().getBuilder(StatusCode.TYPE_NAME);
        final SAMLObjectBuilder<Status> statusBuilder =
                (SAMLObjectBuilder<Status>) Configuration.getBuilderFactory().getBuilder(Status.TYPE_NAME);
        final SAMLObjectBuilder<Response> responseBuilder =
                (SAMLObjectBuilder<Response>) Configuration.getBuilderFactory().getBuilder(Response.TYPE_NAME);

        final StatusCode statusCode = statusCodeBuilder.buildObject();
        statusCode.setValue(StatusCode.SUCCESS_URI);

        final Status status = statusBuilder.buildObject();
        status.setStatusCode(statusCode);

        final Response response = responseBuilder.buildObject();
        response.setIssueInstant(new DateTime(ISOChronology.getInstanceUTC()));
        response.setStatus(status);

        profileRequestContext.getOutboundMessageContext().setMessage(response);

        return ActionSupport.buildEvent(this, ActionSupport.PROCEED_EVENT_ID, null);
    }
}