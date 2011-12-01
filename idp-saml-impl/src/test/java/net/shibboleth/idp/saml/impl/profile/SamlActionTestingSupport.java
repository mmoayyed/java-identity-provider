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

package net.shibboleth.idp.saml.impl.profile;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.relyingparty.ProfileConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartySubcontext;

import org.opensaml.messaging.context.BasicMessageContext;
import org.opensaml.messaging.context.BasicMessageMetadataSubcontext;
import org.opensaml.messaging.context.SubcontextContainer;
import org.opensaml.util.collections.CollectionSupport;
import org.opensaml.util.constraint.documented.NotEmpty;
import org.opensaml.util.constraint.documented.NotNull;
import org.opensaml.util.constraint.documented.Null;
import org.opensaml.util.criteria.StaticResponseEvaluableCriterion;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.execution.RequestContext;

/**
 * Helper methods for creating/testing objects within profile action tests. When methods herein refer to mock objects
 * they are always objects that have been created via Mockito unless otherwise noted.
 */
public final class SamlActionTestingSupport {

    /** ID of the inbound message. */
    public final static String INBOUND_MSG_ID = "inbound";

    /** Issuer of the inbound message. */
    public final static String INBOUND_MSG_ISSUER = "http://sp.example.org";

    /** ID of the outbound message. */
    public final static String OUTBOUND_MSG_ID = "outbound";

    /** Issuer of the outbound message. */
    public final static String OUTBOUND_MSG_ISSUER = "http://idp.example.org";

    /**
     * Builds a {@link BasicMessageContext} with a {@link BasicMessageMetadataSubcontext}. The subcontext has a message
     * ID of {@link #INBOUND_MSG_ID}, an issue instant of 1970-01-01T00:00:00Z, and an issuer of
     * {@link #INBOUND_MSG_ISSUER}.
     * 
     * @return the constructed message context
     */
    public static BasicMessageContext buildInboundMessageContext() {
        BasicMessageContext context = new BasicMessageContext();

        BasicMessageMetadataSubcontext metadataCtx = new BasicMessageMetadataSubcontext(context);
        metadataCtx.setMessageId(INBOUND_MSG_ID);
        metadataCtx.setMessageIssueInstant(0);
        metadataCtx.setMessageIssuer(INBOUND_MSG_ISSUER);

        return context;
    }

    /**
     * Builds a {@link BasicMessageContext} with a {@link BasicMessageMetadataSubcontext}. The subcontext has a message
     * ID of {@link #OUTBOUND_MSG_ID}, an issue instant of 1970-01-01T00:00:00Z, and an issuer of
     * {@link #OUTBOUND_MSG_ISSUER}.
     * 
     * @return the constructed message context
     */
    public static BasicMessageContext buildOutboundMessageContext() {
        BasicMessageContext context = new BasicMessageContext();

        BasicMessageMetadataSubcontext metadataCtx = new BasicMessageMetadataSubcontext(context);
        metadataCtx.setMessageId(OUTBOUND_MSG_ID);
        metadataCtx.setMessageIssueInstant(0);
        metadataCtx.setMessageIssuer(OUTBOUND_MSG_ISSUER);

        return context;
    }

    /**
     * Creates a {@link ProfileRequestContext} with an inbound message context created by
     * {@link #buildInboundMessageContext()} and an outbound message context created by
     * {@link #buildOutboundMessageContext()}.
     * 
     * @return the profile context that was built
     */
    public static <In, Out> ProfileRequestContext<In, Out> buildProfileRequestContext() {
        final ProfileRequestContext<In, Out> profileContext = new ProfileRequestContext<In, Out>();
        profileContext.setInboundMessageContext(buildInboundMessageContext());
        profileContext.setOutboundMessageContext(buildOutboundMessageContext());

        return profileContext;
    }

    /**
     * Builds a mock Spring {@link RequestContext} with the following properties:
     * <ul>
     * <li>provides a mock {@link ServletExternalContext} via {@link RequestContext#getExternalContext()}</li>
     * <li>{@link ServletExternalContext} will return {@link MockHttpServletRequest} and {@link MockHttpServletResponse}
     * from {@link ServletExternalContext#getNativeRequest()} and {@link ServletExternalContext#getNativeResponse()},
     * respectively</li>
     * <li>{@link RequestContext#getConversationScope()} will return a {@link LocalAttributeMap}</li>
     * <li>if given, the {@link ProfileRequestContext} is bound to the {@link RequestContext} in the expected place</li>
     * </ul>
     * 
     * @param profileRequestContext profile request context to add to the spring request context, may be null
     * 
     * @return the mock {@link RequestContext}
     */
    public static RequestContext buildMockSpringRequestContext(final ProfileRequestContext<?, ?> profileRequestContext) {
        final RequestContext context = mock(RequestContext.class);

        final ServletExternalContext externalContext = mock(ServletExternalContext.class);
        when(externalContext.getNativeRequest()).thenReturn(new MockHttpServletRequest());
        when(externalContext.getNativeResponse()).thenReturn(new MockHttpServletResponse());
        when(context.getExternalContext()).thenReturn(externalContext);

        final LocalAttributeMap scope = new LocalAttributeMap();
        if (profileRequestContext != null) {
            scope.put(ProfileRequestContext.BINDING_KEY, profileRequestContext);
        }
        when(context.getConversationScope()).thenReturn(scope);

        return context;
    }

    /**
     * Builds a {@link RelyingPartySubcontext} that is a child of the given parent context. The build subcontext
     * contains:
     * <ul>
     * <li>a {@link RelyingPartyConfiguration} whose ID is the given relying party ID and contains the given active
     * profile configuration</li>
     * <li>a {@link ProfileConfiguration} set to the given profile configuration</li>
     * </ul>
     * 
     * @param parent the parent of the created subcontext
     * @param relyingPartyId the ID of the relying party
     * @param activeProfileConfig the active profile configuration
     * 
     * @return the constructed subcontext
     */
    public static RelyingPartySubcontext buildRelyingPartySubcontext(@NotNull SubcontextContainer parent,
            @NotNull @NotEmpty String relyingPartyId, @Null ProfileConfiguration activeProfileConfig) {
        RelyingPartyConfiguration rpConfig =
                new RelyingPartyConfiguration(relyingPartyId, OUTBOUND_MSG_ISSUER,
                        StaticResponseEvaluableCriterion.TRUE_RESPONSE, CollectionSupport.toList(activeProfileConfig));

        RelyingPartySubcontext subcontext = new RelyingPartySubcontext(parent, relyingPartyId);
        subcontext.setProfileConfiguration(activeProfileConfig);
        subcontext.setRelyingPartyConfiguration(rpConfig);

        return subcontext;
    }
}