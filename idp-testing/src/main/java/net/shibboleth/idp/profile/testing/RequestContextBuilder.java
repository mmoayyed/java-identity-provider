/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.profile.testing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.relyingparty.BasicRelyingPartyConfiguration;
import net.shibboleth.profile.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.ComponentInitializationException;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.test.MockRequestContext;

/**
 * Builder used to construct {@link RequestContext} used in {@link org.springframework.webflow.execution.Action}
 * executions.
 */
public class RequestContextBuilder {

    /** Value used to represent a string value that has not be set. */
    @Nonnull @NotEmpty private static final String NO_VAL = "novalue";

    /** The {@link ServletContext} used when building the request context. */
    @Nullable private ServletContext servletContext;

    /** The {@link HttpServletRequest} used when building the request context. */
    @Nullable private HttpServletRequest httpRequest;

    /** The {@link HttpServletResponse} used when building the request context. */
    @Nullable private HttpServletResponse httpResponse;

    /** The ID of the inbound message. */
    @Nullable private String inboundMessageId = NO_VAL;

    /** The issue instant of the inbound message. */
    @Nullable private Instant inboundMessageIssueInstant;

    /** The issuer of the inbound message. */
    @Nullable private String inboundMessageIssuer = NO_VAL;

    /** The inbound message. */
    @Nullable private Object inboundMessage;

    /** The ID of the outbound message. */
    @Nullable private String outboundMessageId = NO_VAL;

    /** The issue instant of the outbound message. */
    @Nullable private Instant outboundMessageIssueInstant;

    /** The issuer of the outbound message. */
    @Nullable private String outboundMessageIssuer = NO_VAL;

    /** The outbound message. */
    @Nullable private Object outboundMessage;

    /** The profile configurations associated with the relying party. */
    @Nullable private Collection<ProfileConfiguration> relyingPartyProfileConfigurations;

    /** Constructor. */
    public RequestContextBuilder() {

    }

    /**
     * Constructor.
     * 
     * @param prototype prototype whose properties are copied onto this builder
     */
    public RequestContextBuilder(@Nonnull final RequestContextBuilder prototype) {
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
                    new ArrayList<>(prototype.relyingPartyProfileConfigurations);
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
     * Sets the issue instant of the inbound message
     * 
     * @param instant issue instant of the inbound message
     * 
     * @return this builder
     */
    @Nonnull public RequestContextBuilder setInboundMessageIssueInstant(@Nullable final Instant instant) {
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
     * Sets the issue instant of the outbound message
     * 
     * @param instant issue instant of the outbound message
     * 
     * @return this builder
     */
    @Nonnull public RequestContextBuilder setOutboundMessageIssueInstant(@Nullable final Instant instant) {
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
     * 
     * @throws ComponentInitializationException ...
     */
    @Nonnull public RequestContext buildRequestContext() throws ComponentInitializationException {
        final MockRequestContext context = new MockRequestContext();
        context.setExternalContext(buildServletExternalContext());

        final MutableAttributeMap<Object> scope = context.getConversationScope();
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
            final MockHttpServletRequest hdr = new MockHttpServletRequest(servletContext);
            hdr.addHeader("Accept-Language", Locale.ENGLISH.getLanguage());
            httpRequest = hdr;
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
     * <li>{@link RelyingPartyContext} created by {@link #buildRelyingPartyContext(ProfileRequestContext)}</li>
     * </ul>
     * 
     * @return the constructed {
     * @throws ComponentInitializationException @link ProfileRequestContext

     */
    @Nonnull public ProfileRequestContext buildProfileRequestContext() throws ComponentInitializationException {
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
     * </ul>
     * 
     * @return the constructed {@link MessageContext}
     */
    @Nonnull protected MessageContext buildInboundMessageContext() {
        final MessageContext context = new MessageContext();
        context.setMessage(inboundMessage);
        return context;
    }

    /**
     * Builds a outbound {@link MessageContext}.
     * 
     * The default implementation builds a {@link MessageContext} that contains:
     * <ul>
     * <li>the message provided by {@link #setOutboundMessage(Object)}</li>
     * </ul>
     * 
     * @return the constructed {@link MessageContext}
     */
    @Nonnull protected MessageContext buildOutboundMessageContext() {
        final MessageContext context = new MessageContext();
        context.setMessage(outboundMessage);
        return context;

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
     * @param profileRequestContext ...
     * 
     * @return the constructed {@link RelyingPartyContext}
     * 
     * @throws ComponentInitializationException ...
     */
    @Nonnull protected RelyingPartyContext buildRelyingPartyContext(
            @Nonnull final ProfileRequestContext profileRequestContext) throws ComponentInitializationException {
        final RelyingPartyContext rpCtx = profileRequestContext.ensureSubcontext(RelyingPartyContext.class);
        if (Objects.equals(NO_VAL, inboundMessageIssuer) || inboundMessageIssuer == null) {
            rpCtx.setRelyingPartyId(ActionTestingSupport.INBOUND_MSG_ISSUER);
        } else {
            rpCtx.setRelyingPartyId(inboundMessageIssuer);
        }

        final RelyingPartyConfiguration rpConfig = buildRelyingPartyConfiguration();
        rpCtx.setConfiguration(rpConfig);
        rpCtx.setProfileConfig(selectProfileConfiguration(rpConfig.getProfileConfigurations(profileRequestContext)));

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
     * <li>the activation criteria is {@link com.google.common.base.Predicates#alwaysTrue()}</li>
     * <li>the profile configurations provided {@link #setRelyingPartyProfileConfigurations(Collection)} or one
     * {@link MockProfileConfiguration} if none is provided</li>
     * </ul>
     * 
     * @return the constructed {@link RelyingPartyConfiguration}
     * 
     * @throws ComponentInitializationException ...
     */
    @Nonnull protected RelyingPartyConfiguration buildRelyingPartyConfiguration() throws ComponentInitializationException {
        String responderId;
        if (Objects.equals(NO_VAL, outboundMessageIssuer) || outboundMessageIssuer == null) {
            responderId = ActionTestingSupport.OUTBOUND_MSG_ISSUER;
        } else {
            responderId = outboundMessageIssuer;
        }

        if (relyingPartyProfileConfigurations == null) {
            relyingPartyProfileConfigurations = new ArrayList<>();
        }
        assert relyingPartyProfileConfigurations != null;
        final List<ProfileConfiguration> profileConfigs =
                relyingPartyProfileConfigurations.
                stream().
                filter(e -> e!=null).
                collect(Collectors.toList());
        if (profileConfigs.isEmpty()) {
            profileConfigs.add(new MockProfileConfiguration("mock"));
        }

        BasicRelyingPartyConfiguration rp = new BasicRelyingPartyConfiguration();
        rp.setId("mock");
        rp.setIssuer(responderId);
        rp.setDetailedErrors(true);
        rp.setProfileConfigurations(profileConfigs);
        rp.initialize();
        return rp;
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