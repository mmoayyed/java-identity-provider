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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeSubcontext;
import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileRequestContext;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml1.core.Assertion;
import org.opensaml.saml1.core.AttributeStatement;
import org.opensaml.saml1.core.Response;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

//TODO need attributes to be encoded

/**
 * Builds an {@link AttributeStatement} and adds it to the {@link Response} set as the message of the
 * {@link ProfileRequestContext#getOutboundMessageContext()}. The {@link Attribute} set to be encoded is drawn from the
 * ...
 */
public class AddAttributeStatementToAssertion extends AbstractIdentityProviderAction<Object, Response> {

    /** {@inheritDoc} */
    public Event doExecute(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final RequestContext springRequestContext,
            final ProfileRequestContext<Object, Response> profileRequestContext) {

        final AttributeSubcontext attributeCtx = null; // TODO get this from relying party context
        if (attributeCtx == null) {
            return ActionSupport.buildProceedEvent(this);
        }

        final MessageContext<Response> outboundMessageCtx = profileRequestContext.getOutboundMessageContext();
        // TODO check for null

        final Response samlResponse = outboundMessageCtx.getMessage();
        // TODO check for null

        final List<Assertion> assertions = samlResponse.getAssertions();
        if (assertions.isEmpty()) {
            // TODO generate assertion
        }

        final Assertion assertion = assertions.get(0);

        // TODO generate attribute statement and add to assertion

        for (Attribute<?> attributes : attributeCtx.getAttributes().values()) {
            // TODO encode attributes and add to statement
        }

        return null;
    }

}