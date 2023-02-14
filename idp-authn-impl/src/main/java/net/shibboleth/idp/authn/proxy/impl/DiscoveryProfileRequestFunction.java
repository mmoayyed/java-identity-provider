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

package net.shibboleth.idp.authn.proxy.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.component.AbstractInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;

/**
 * A {@link Function} that produces a discovery request URL using the protocol defined in
 * https://wiki.oasis-open.org/security/IdpDiscoSvcProtonProfile
 * 
 * <p>Since there is no upstream "relying party" yet, the identity of the system is derived
 * from the currently in-effect entityID that will be used to respond to the downstream
 * relying party.</p>
 */
@ThreadSafe
public class DiscoveryProfileRequestFunction extends AbstractInitializableComponent
        implements Function<Pair<RequestContext,ProfileRequestContext>,String> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(DiscoveryProfileRequestFunction.class);

    /** URL query parameter escaper. */
    @Nonnull private Escaper escaper;
    
    /** Lookup strategy for locating {@link RelyingPartyContext}. */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;
    
    /** Lookup strategy for determining the "base" discovery URL. */
    @NonnullAfterInit private Function<ProfileRequestContext,String> discoveryURLLookupStrategy;
    
    /** Constructor. */
    public DiscoveryProfileRequestFunction() {
        escaper = UrlEscapers.urlFormParameterEscaper();
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
    }
    
    /**
     * Set the lookup strategy for the {@link RelyingPartyContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        checkSetterPreconditions();
        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy cannot be null");
    }
    
    /**
     * Set the lookup strategy for the "base" discovery service URL to use.
     * 
     * @param strategy lookup strategy
     */
    public void setDiscoveryURLLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        discoveryURLLookupStrategy = Constraint.isNotNull(strategy, "Discovery URL lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (discoveryURLLookupStrategy == null) {
            throw new ComponentInitializationException("Discovery URL lookup strategy cannot be null");
        }
    }
    
    /** {@inheritDoc} */
    @Nullable public String apply(@Nonnull final Pair<RequestContext,ProfileRequestContext> input) {
        
        final RelyingPartyContext rpCtx = relyingPartyContextLookupStrategy.apply(input.getSecond());
        Constraint.isNotNull(rpCtx, "RelyingPartyContext cannot be null");
        Constraint.isNotNull(rpCtx.getConfiguration(), "RelyingPartyConfiguration cannot be null");
        
        final String baseURL = discoveryURLLookupStrategy.apply(input.getSecond());
        Constraint.isNotEmpty(baseURL, "Discovery URL cannot be null or empty");

        final RelyingPartyConfiguration rpConfig = rpCtx.getConfiguration();
        Constraint.isTrue(rpConfig instanceof net.shibboleth.idp.relyingparty.RelyingPartyConfiguration,
                "RelyingPartyConfiguration was not of expected subclass");
        final String entityID = ((net.shibboleth.idp.relyingparty.RelyingPartyConfiguration) rpConfig).getResponderId(
                input.getSecond());

        final StringBuilder builder = new StringBuilder(baseURL);
        
        builder.append(baseURL.contains("?") ? '&' : '?').append("entityID=").append(escaper.escape(entityID));
        
        final AuthenticationContext authenticationContext =
                input.getSecond().getSubcontext(AuthenticationContext.class);
        if (authenticationContext != null && authenticationContext.isPassive()) {
            builder.append("&isPassive=true");
        }
        
        final HttpServletRequest httpServletRequest =
                (HttpServletRequest) input.getFirst().getExternalContext().getNativeRequest();
        
        final StringBuilder selfBuilder = new StringBuilder(httpServletRequest.getScheme());
        selfBuilder.append("://").append(httpServletRequest.getServerName());
        
        final int port = httpServletRequest.getServerPort();
        if (port != (httpServletRequest.isSecure() ? 443 : 80)) {
            selfBuilder.append(':').append(port);
        }
        
        selfBuilder.append(input.getFirst().getFlowExecutionUrl()).append("&_eventId_proceed=1");
        
        builder.append("&return=").append(escaper.escape(selfBuilder.toString()));
        
        return builder.toString();
    }

}