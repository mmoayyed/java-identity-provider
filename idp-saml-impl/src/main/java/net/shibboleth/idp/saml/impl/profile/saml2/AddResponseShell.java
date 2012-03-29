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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.InvalidOutboundMessageException;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.relyingparty.RelyingPartySubcontext;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.core.xml.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.BasicMessageMetadataContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * A profile action that creates a {@link Response}, adds a {@link StatusCode#SUCCESS_URI} status to it, and sets it as
 * the message for the {@link ProfileRequestContext#getOutboundMessageContext()}.
 */
public class AddResponseShell extends AbstractIdentityProviderAction<Object, Response> {

    /** Class logger. */
    private Logger log = LoggerFactory.getLogger(AddResponseShell.class);

    /** {@inheritDoc} */
    protected Event doExecute(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final RequestContext springRequestContext,
            final ProfileRequestContext<Object, Response> profileRequestContext) throws ProfileException {

        final MessageContext<Response> outboundMessageCtx =
                ActionSupport.getRequiredOutboundMessageContext(this, profileRequestContext);
        if (outboundMessageCtx.getMessage() != null) {
            log.error("Action {}: Outbound message context already contains a Response", getId());
            throw new InvalidOutboundMessageException("Outbound message context already contains a Response");
        }

        final RelyingPartySubcontext relyingPartyCtx =
                ActionSupport.getRequiredRelyingPartyContext(this, profileRequestContext);

        final SAMLObjectBuilder<StatusCode> statusCodeBuilder =
                (SAMLObjectBuilder<StatusCode>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(StatusCode.TYPE_NAME);
        final SAMLObjectBuilder<Status> statusBuilder =
                (SAMLObjectBuilder<Status>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(Status.TYPE_NAME);
        final SAMLObjectBuilder<Response> responseBuilder =
                (SAMLObjectBuilder<Response>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                        Response.DEFAULT_ELEMENT_NAME);

        final StatusCode statusCode = statusCodeBuilder.buildObject();
        statusCode.setValue(StatusCode.SUCCESS_URI);

        final Status status = statusBuilder.buildObject();
        status.setStatusCode(statusCode);

        final Response response = responseBuilder.buildObject();
        // TODO check for nulls
        response.setID(relyingPartyCtx.getProfileConfig().getSecurityConfiguration().getIdGenerator()
                .generateIdentifier());
        response.setIssueInstant(new DateTime(ISOChronology.getInstanceUTC()));
        response.setStatus(status);
        response.setVersion(SAMLVersion.VERSION_11);

        outboundMessageCtx.setMessage(response);

        BasicMessageMetadataContext messageMetadata = new BasicMessageMetadataContext();
        messageMetadata.setMessageId(response.getID());
        messageMetadata.setMessageIssueInstant(response.getIssueInstant().getMillis());

        outboundMessageCtx.addSubcontext(messageMetadata);

        return ActionSupport.buildProceedEvent(this);
    }
}