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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.relyingparty.MockProfileConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;

import org.opensaml.messaging.context.BasicMessageMetadataContext;
import org.opensaml.messaging.context.MessageContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.test.MockRequestContext;

import com.google.common.base.Objects;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Builder used to construct {@link RequestContext} used in {@link org.springframework.webflow.execution.Action}
 * executions.
 */
public class RequestContextBuilder {

    /** Value used to represent a string value that has not be set. */
    private final String NO_VAL = "novalue";

    /** The {@link ServletContext} used when building the request context. */
    private ServletContext servletContext;

    /** The {@link HttpServletRequest} used when building the request context. */
    private HttpServletRequest httpRequest;

    /** The {@link HttpServletResponse} used when building the request context. */
    private HttpServletResponse httpResponse;

    /** The ID of the inbound message. */
    private String inboundMessageId = NO_VAL;

    /** The issue instant of the inbound message in milliseconds. */
    private long inboundMessageIssueInstant;

    /** The issuer of the inbound message. */
    private String inboundMessageIssuer = NO_VAL;

    /** The inbound message. */
    private Object inboundMessage;

    /** The ID of the outbound message. */
    private String outboundMessageId = NO_VAL;

    /** The issue instant of the outbound message in milliseconds. */
    private long outboundMessageIssueInstant;

    /** The issuer of the outbound message. */
    private String outboundMessageIssuer = NO_VAL;

    /** The outbound message. */
    private Object outboundMessage;

    /** The profile configurations associated with the relying party. */
    private Collection<ProfileConfiguration> relyingPartyProfileConfigurations;

    /** Constructor. */
    public RequestContextBuilder() {

    }

    /**
     * Constructor.
     * 
     * @param prototype prototype whose properties are copied onto this builder
     */
    public RequestContextBuilder(RequestContextBuilder prototype) {
        servletContext = prototype.servletContext;
        httpRequest = prototype.httpRequest;
        httpResponse = prototype.httpResponse;
        inboundMessageId = prototype.inboundMessageId;
        inboundMessageIssueInstant = prototype.inboundMessageIssueInstant;
        inboundMessageIssuer = prototype.inboundMessageIssuer;
        inboundMessage = prototype.inboundMessage;
        outboundMessageId = prototype.outboundMessageId;
        outboundMessageIssueInstant = prototype.outboundMessageIssueInstant;
        outboundMessageIssuer = prototype.outboundMessageIssuer;
        outboundMessage = prototype.outboundMessage;

        if (prototype.relyingPartyProfileConfigurations != null) {
            relyingPartyProfileConfigurations =
                    new ArrayList<ProfileConfiguration>(prototype.relyingPartyProfileConfigurations);
        }
    }

    /**
     * Sets the {@link ServletContext} used when building the request context.
     * 
     * @param context the {@link ServletContext} used when building the request context
     * 
     * @return this builder
     */
    @Nonnull public RequestContextBuilder setServletContext(@Nullable final ServletContext context) {
        servletContext = context;
        return this;
    }

    /**
     * Sets the {@link HttpServletRequest} used when building the request context.
     * 
     * @param request the {@link HttpServletRequest} used when building the request context
     * 
     * @return this builder
     */
    @Nonnull public RequestContextBuilder setHttpRequest(@Nullable final HttpServletRequest request) {
        httpRequest = request;
        return this;
    }

    /**
     * Sets the {@link HttpServletResponse} used when building the request context.
     * 
     * @param response the {@link HttpServletResponse} used when building the request context
     * 
     * @return this builder
     */
    @Nonnull public RequestContextBuilder setHttpResponse(@Nullable final HttpServletResponse response) {
        httpResponse = response;
        return this;
    }

    /**
     * Sets the ID of the inbound message.
     * 
     * @param id ID of the inbound message
     * 
     * @return this builder
     */
    @Nonnull public RequestContextBuilder setInboundMessageId(@Nullable final String id) {
        inboundMessageId = id;
        return this;
    }

    /**
     * Sets the issue instant of the inbound message in milliseconds.
     * 
     * @param instant issue instant of the inbound message in milliseconds
     * 
     * @return this builder
     */
    @Nonnull public RequestContextBuilder setInboundMessageIssueInstant(final long instant) {
        inboundMessageIssueInstant = instant;
        return this;
    }

    /**
     * Sets the issuer of the inbound message.
     * 
     * @param issuer issuer of the inbound message
     * 
     * @return this builder
     */
    @Nonnull public RequestContextBuilder setInboundMessageIssuer(@Nullable final String issuer) {
        inboundMessageIssuer = issuer;
        return this;
    }

    /**
     * Sets the inbound message.
     * 
     * @param message the inbound message
     * 
     * @return this builder
     */
    @Nonnull public RequestContextBuilder setInboundMessage(@Nullable final Object message) {
        inboundMessage = message;
        return this;
    }

    /**
     * Sets the ID of the outbound message.
     * 
     * @param id ID of the outbound message
     * 
     * @return this builder
     */
    @Nonnull public RequestContextBuilder setOutboundMessageId(@Nullable final String id) {
        outboundMessageId = id;
        return this;
    }

    /**
     * Sets the issue instant of the outbound message in milliseconds.
     * 
     * @param instant issue instant of the outbound message in milliseconds
     * 
     * @return this builder
     */
    @Nonnull public RequestContextBuilder setOutboundMessageIssueInstant(final long instant) {
        outboundMessageIssueInstant = instant;
        return this;
    }

    /**
     * Sets the issuer of the outbound message.
     * 
     * @param issuer issuer of the outbound message
     * 
     * @return this builder
     */
    public RequestContextBuilder setOutboundMessageIssuer(@Nullable final String issuer) {
        outboundMessageIssuer = issuer;
        return this;
    }

    /**
     * Sets the outbound message.
     * 
     * @param message the outbound message
     * 
     * @return this builder
     */
    @Nonnull public RequestContextBuilder setOutboundMessage(@Nullable final Object message) {
        outboundMessage = message;
        return this;
    }

    /**
     * Sets the profile configurations associated with the relying party.
     * 
     * @param configs profile configurations associated with the relying party
     * 
     * @return this builder
     */
    @Nonnull public RequestContextBuilder setRelyingPartyProfileConfigurations(
            @Nullable final Collection<ProfileConfiguration> configs) {
        relyingPartyProfileConfigurations = configs;
        return this;
    }

    /**
     * Builds a {@link MockRequestContext}.
     * 
     * The default implementation builds a {@link MockRequestContext} that contains a:
     * <ul>
     * <li>{@link ServletExternalContext}, via
     * {@link org.springframework.webflow.execution.RequestContext#getExternalContext()}, created by
     * {@link #buildServletExternalContext()}</li>
     * <li>{@link ProfileRequestContext} created by {@link #buildProfileRequestContext()}</li>
     * </ul>
     * 
     * @return the constructed {@link MockRequestContext}
     */
    @Nonnull public RequestContext buildRequestContext() {
        final MockRequestContext context = new MockRequestContext();
        context.setExternalContext(buildServletExternalContext());

        final MutableAttributeMap scope = context.getConversationScope();
        scope.put(ProfileRequestContext.BINDING_KEY, buildProfileRequestContext());

        return context;
    }

    /**
     * Builds a {@link ServletExternalContext}.
     * 
     * The default implementations builds a {@link ServletExternalContext} that contains a:
     * <ul>
     * <li>the {@link ServletContext} provided by {@link #setServletContext(ServletContext)} or a
     * {@link MockServletContext} if none was provided</li>
     * <li>the {@link HttpServletRequest} provided by {@link #setHttpRequest(HttpServletRequest)} or a
     * {@link MockHttpServletRequest} if none was provided</li>
     * <li>the {@link HttpServletResponse} provided by {@link #setHttpResponse(HttpServletResponse)} or a
     * {@link MockHttpServletResponse} if none was provided</li>
     * </ul>
     * 
     * @return the constructed {@link ServletContext}
     */
    @Nonnull public ServletExternalContext buildServletExternalContext() {
        if (servletContext == null) {
            servletContext = new MockServletContext();
        }

        if (httpRequest == null) {
            httpRequest = new MockHttpServletRequest();
        }

        if (httpResponse == null) {
            httpResponse = new MockHttpServletResponse();
        }

        return new ServletExternalContext(servletContext, httpRequest, httpResponse);
    }

    /**
     * Builds a {@link ProfileRequestContext}.
     * 
     * The default implementation builds a {@link ProfileRequestContext} that contains a:
     * <ul>
     * <li>inbound message context created by {@link #buildInboundMessageContext()}</li>
     * <li>outbound message context created by {@link #buildOutboundMessageContext()}</li>
     * <li>{@link RelyingPartyContext} created by {@link #buildRelyingPartyContext()}</li>
     * </ul>
     * 
     * @return the constructed {@link ProfileRequestContext

     */
    @Nonnull public ProfileRequestContext buildProfileRequestContext() {
        final ProfileRequestContext profileContext = new ProfileRequestContext();
        profileContext.setInboundMessageContext(buildInboundMessageContext());
        profileContext.setOutboundMessageContext(buildOutboundMessageContext());
        buildRelyingPartyContext(profileContext);
        return profileContext;
    }

    /**
     * Builds a inbound {@link MessageContext}.
     * 
     * The default implementation builds a {@link MessageContext} that contains:
     * <ul>
     * <li>the message provided by {@link #setInboundMessage(Object)}</li>
     * <li>a {@link BasicMessageMetadataContext} created by {@link #buildInboudMessageMetadataContext(MessageContext)}</li>
     * </ul>
     * 
     * @return the constructed {@link MessageContext}
     */
    @Nonnull protected MessageContext buildInboundMessageContext() {
        final MessageContext context = new MessageContext();
        context.setMessage(inboundMessage);
        buildInboudMessageMetadataContext(context);
        return context;
    }

    /**
     * Builds a {@link BasicMessageMetadataContext} and adds it to the given inbound {@link MessageContext}.
     * 
     * The default implementation builds a {@link BasicMessageMetadataContext} that contains:
     * <ul>
     * <li>a message ID provided by {@link #setInboundMessageId(String)} or {@link ActionTestingSupport#INBOUND_MSG_ID}
     * if none is given</li>
     * <li>a message issue instant provided by {@link #setInboundMessageIssueInstant(long)} or 0 (1970-01-01T00:00:00Z)
     * if none is given</li>
     * <li>a message issuer provided by {@link #setInboundMessageIssuer(String)} or
     * {@link ActionTestingSupport#INBOUND_MSG_ISSUER} if none is given
     * </ul>
     * 
     * @param inboundMsgCtx the inbound message context to which the constructed {@link BasicMessageMetadataContext} is
     *            added
     * 
     * @return the constructed {@link BasicMessageMetadataContext}
     */
    @Nullable protected BasicMessageMetadataContext buildInboudMessageMetadataContext(
            @Nonnull final MessageContext inboundMsgCtx) {
        if (Objects.equal(NO_VAL, inboundMessageId) && Objects.equal(NO_VAL, inboundMessageIssuer)) {
            return null;
        }

        final BasicMessageMetadataContext metadataCtx = new BasicMessageMetadataContext();
        inboundMsgCtx.addSubcontext(metadataCtx);

        if (Objects.equal(NO_VAL, inboundMessageId)) {
            metadataCtx.setMessageId(ActionTestingSupport.OUTBOUND_MSG_ID);
        } else {
            metadataCtx.setMessageId(inboundMessageId);
        }

        metadataCtx.setMessageIssueInstant(inboundMessageIssueInstant);

        if (Objects.equal(NO_VAL, inboundMessageIssuer)) {
            metadataCtx.setMessageIssuer(ActionTestingSupport.OUTBOUND_MSG_ISSUER);
        } else {
            metadataCtx.setMessageIssuer(inboundMessageIssuer);
        }

        return metadataCtx;
    }

    /**
     * Builds a outbound {@link MessageContext}.
     * 
     * The default implementation builds a {@link MessageContext} that contains:
     * <ul>
     * <li>the message provided by {@link #setOutboundMessage(Object)}</li>
     * <li>a {@link BasicMessageMetadataContext} created by {@link #buildOutboudMessageMetadataContext(MessageContext)}</li>
     * </ul>
     * 
     * @return the constructed {@link MessageContext}
     */
    @Nonnull protected MessageContext buildOutboundMessageContext() {
        final MessageContext context = new MessageContext();
        context.setMessage(outboundMessage);
        buildInboudMessageMetadataContext(context);
        return context;

    }

    /**
     * Builds a {@link BasicMessageMetadataContext} and adds it to the given outbound {@link MessageContext}.
     * 
     * The default implementation builds a {@link BasicMessageMetadataContext} that contains:
     * <ul>
     * <li>a message ID provided by {@link #setOutboundMessageId(String)} or {@link ActionTestingSupport#INBOUND_MSG_ID}
     * if none is given</li>
     * <li>a message issue instant provided by {@link #setOutboundMessageIssueInstant(long)} or 0 (1970-01-01T00:00:00Z)
     * if none is given</li>
     * <li>a message issuer provided by {@link #setOutboundMessageIssuer(String)} or
     * {@link ActionTestingSupport#INBOUND_MSG_ISSUER} if none is given
     * </ul>
     * 
     * @param outboundMsgCtx the outbound message context to which the constructed {@link BasicMessageMetadataContext}
     *            is added
     * 
     * @return the constructed {@link BasicMessageMetadataContext}
     */
    @Nonnull protected BasicMessageMetadataContext buildOutboundMessageMetadataContext(
            @Nonnull final MessageContext outboundMsgCtx) {
        if (Objects.equal(NO_VAL, outboundMessageId) && Objects.equal(NO_VAL, outboundMessageIssuer)) {
            return null;
        }

        final BasicMessageMetadataContext metadataCtx = new BasicMessageMetadataContext();
        outboundMsgCtx.addSubcontext(metadataCtx);

        if (Objects.equal(NO_VAL, outboundMessageId)) {
            metadataCtx.setMessageId(ActionTestingSupport.OUTBOUND_MSG_ID);
        } else {
            metadataCtx.setMessageId(outboundMessageId);
        }

        metadataCtx.setMessageIssueInstant(outboundMessageIssueInstant);

        if (Objects.equal(NO_VAL, outboundMessageIssuer)) {
            metadataCtx.setMessageIssuer(ActionTestingSupport.OUTBOUND_MSG_ISSUER);
        } else {
            metadataCtx.setMessageIssuer(outboundMessageIssuer);
        }

        return metadataCtx;
    }

    /**
     * Builds {@link RelyingPartyContext}.
     * 
     * The default implementations builds a {@link RelyingPartyContext} with:
     * <ul>
     * <li>a relying party ID provided by {@link #setInboundMessageIssuer(String)} or
     * {@link ActionTestingSupport#INBOUND_MSG_ISSUER} if none is given</li>
     * <li>a relying party configuration built by {@link #buildRelyingPartyConfiguration()}</li>
     * <li>the active profile selected, out of the profiles registered with the built relying party configuration, by
     * {@link #selectProfileConfiguration(Map)}</li>
     * </ul>
     * 
     * @return the constructed {@link RelyingPartyContext}
     */
    @Nonnull protected RelyingPartyContext buildRelyingPartyContext(
            @Nonnull final ProfileRequestContext profileRequestContext) {
        final RelyingPartyContext rpCtx;
        if (Objects.equal(NO_VAL, inboundMessageIssuer) || inboundMessageIssuer == null) {
            rpCtx = new RelyingPartyContext(ActionTestingSupport.INBOUND_MSG_ISSUER);
        } else {
            rpCtx = new RelyingPartyContext(inboundMessageIssuer);
        }
        profileRequestContext.addSubcontext(rpCtx);

        final RelyingPartyConfiguration rpConfig = buildRelyingPartyConfiguration();
        rpCtx.setRelyingPartyConfiguration(rpConfig);
        rpCtx.setProfileConfiguration(selectProfileConfiguration(rpConfig.getProfileConfigurations()));

        return rpCtx;
    }

    /**
     * Builds a {@link RelyingPartyConfiguration}.
     * 
     * The default implementation of this method builds a {@link RelyingPartyConfiguration} such that:
     * <ul>
     * <li>configuration ID is 'mock'</li>
     * <li>the responder ID provided by {@link #setOutboundMessageIssuer(String)} or
     * {@link ActionTestingSupport#OUTBOUND_MSG_ISSUER} if none is given</li>
     * <li>the activation criteria is {@link Predicates#alwaysTrue()}</li>
     * <li>the profile configurations provided {@link #setRelyingPartyProfileConfigurations(Collection)} or one
     * {@link MockProfileConfiguration} if none is provided</li>
     * </ul>
     * 
     * @return the constructed {@link RelyingPartyConfiguration}
     */
    @Nonnull protected RelyingPartyConfiguration buildRelyingPartyConfiguration() {
        String responderId;
        if (Objects.equal(NO_VAL, outboundMessageIssuer) || outboundMessageIssuer == null) {
            responderId = ActionTestingSupport.OUTBOUND_MSG_ISSUER;
        } else {
            responderId = outboundMessageIssuer;
        }

        if (relyingPartyProfileConfigurations == null) {
            relyingPartyProfileConfigurations = new ArrayList<ProfileConfiguration>();
        }
        
        ArrayList<ProfileConfiguration> profileConfigs =
                Lists.newArrayList(Iterables.filter(relyingPartyProfileConfigurations, Predicates.notNull()));
        if (profileConfigs.isEmpty()) {
            profileConfigs.add(new MockProfileConfiguration("mock"));
        }

        return new RelyingPartyConfiguration("mock", responderId, profileConfigs);
    }

    /**
     * Selects the active profile configurations from the set of registered profile configuration from the relying party
     * configuration built by {@link #buildRelyingPartyConfiguration()}.
     * 
     * The default implementation of this method simply returns the first value of the {@link Map#values()} collection.
     * 
     * @param rpProfileConfigs the set of profile configurations associated with the constructed relying party
     * 
     * @return the active {@link ProfileConfiguration}
     */
    @Nullable protected ProfileConfiguration selectProfileConfiguration(
            @Nonnull final Map<String, ProfileConfiguration> rpProfileConfigs) {
        return rpProfileConfigs.values().iterator().next();
    }
}