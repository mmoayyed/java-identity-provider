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

package net.shibboleth.idp.authn.proxy.impl;

import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;

import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.profile.context.navigate.IssuerLookupFunction;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.component.AbstractInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;

/**
 * A {@link Function} that produces a discovery request URL using the protocol defined in
 * {@linkplain "https://wiki.oasis-open.org/security/IdpDiscoSvcProtonProfile"}.
 */
@ThreadSafe
public class DiscoveryProfileRequestFunction extends AbstractInitializableComponent
        implements BiFunction<RequestContext,ProfileRequestContext,String> {

    /** URL query parameter escaper. */
    @Nonnull private Escaper escaper;
    
    /** Lookup strategy for determining the "base" discovery URL. */
    @NonnullAfterInit private Function<ProfileRequestContext,String> discoveryURLLookupStrategy;
    
    /** Overrides this function via an injected bean. */
    @Nullable private BiFunction<RequestContext,ProfileRequestContext,String> delegatedRequestFunction;

    /** A strategy function to call to obtain the entityID to use when invoking the DS. */
    @NonnullAfterInit private Function<ProfileRequestContext,String> entityIDLookupStrategy;
    
    /** Constructor. */
    public DiscoveryProfileRequestFunction() {
        final Escaper esc = UrlEscapers.urlFormParameterEscaper();
        assert esc != null;
        escaper = esc;
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
    
    /**
     * Set a function to call in place of this built-in class to generate the request.
     * 
     * <p>This is a mechanism to account for how this function gets used in the discovery flow.</p>
     * 
     * @param delegate the function to delegate to
     * 
     * @since 5.0.0
     */
    public void setDelegatedRequestFunction(
            @Nullable final BiFunction<RequestContext,ProfileRequestContext,String> delegate) {
        checkSetterPreconditions();
        delegatedRequestFunction = delegate;
    }
    
    /**
     * Set a lookup strategy for the entityID to use when invoking the DS.
     * 
     * <p>In the absence of an alternative source, the default is to obtain the entityID from the
     * "downstream-facing" profile/RP configurations, which may not result in the correct value.</p>
     * 
     * @param strategy lookup strategy
     * 
     * @since 5.1.0
     */
    public void setEntityIDLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        entityIDLookupStrategy = strategy;
    }
    
    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (discoveryURLLookupStrategy == null) {
            throw new ComponentInitializationException("Discovery URL lookup strategy cannot be null");
        }
        
        if (entityIDLookupStrategy == null) {
            entityIDLookupStrategy = new IssuerLookupFunction();
        }
    }
    
    /** {@inheritDoc} */
    @Nullable public String apply(@Nullable final RequestContext springRequestContext,
            @Nullable final ProfileRequestContext profileRequestContext) {
        
        if (delegatedRequestFunction != null) {
            return delegatedRequestFunction.apply(springRequestContext, profileRequestContext);
        }
        
        if (springRequestContext == null) {
            throw new IllegalArgumentException("Spring RequestContext cannot be null");
        }

        // We need the entityID to use for the DS request.
        final String entityID = entityIDLookupStrategy.apply(profileRequestContext);
        Constraint.isNotNull(entityID, "Unable to obtain entityID to use for DS request");

        final String baseURL = discoveryURLLookupStrategy.apply(profileRequestContext);
        Constraint.isNotEmpty(baseURL, "Discovery URL cannot be null or empty");

        final StringBuilder builder = new StringBuilder(baseURL);
        
        builder.append(baseURL.contains("?") ? '&' : '?').append("entityID=").append(escaper.escape(entityID));
        
        final AuthenticationContext authenticationContext =
                profileRequestContext != null ? profileRequestContext.getSubcontext(AuthenticationContext.class) : null;
        if (authenticationContext != null && authenticationContext.isPassive()) {
            builder.append("&isPassive=true");
        }
        
        final HttpServletRequest httpServletRequest =
                (HttpServletRequest) springRequestContext.getExternalContext().getNativeRequest();
        
        final StringBuilder selfBuilder = new StringBuilder(httpServletRequest.getScheme());
        selfBuilder.append("://").append(httpServletRequest.getServerName());
        
        final int port = httpServletRequest.getServerPort();
        if (port != (httpServletRequest.isSecure() ? 443 : 80)) {
            selfBuilder.append(':').append(port);
        }
        
        selfBuilder.append(springRequestContext.getFlowExecutionUrl()).append("&_eventId_proceed=1");
        
        builder.append("&return=").append(escaper.escape(selfBuilder.toString()));
        
        return builder.toString();
    }

}