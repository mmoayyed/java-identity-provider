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

package net.shibboleth.idp.authn.context;

import java.lang.reflect.Constructor;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicateFactoryRegistry;
import net.shibboleth.idp.authn.principal.PrincipalSupportingComponent;
import net.shibboleth.idp.authn.principal.ProxyAuthenticationPrincipal;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.annotation.constraint.Live;
import net.shibboleth.shared.annotation.constraint.NonNegative;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.MoreObjects;

/**
 * A context representing the state of an authentication attempt, this is the primary
 * input/output context for the action flow responsible for authentication, and
 * within that flow, the individual flows that carry out a specific kind of
 * authentication.
 * 
 * @parent {@link org.opensaml.profile.context.ProfileRequestContext}
 * @child {@link RequestedPrincipalContext}, {@link net.shibboleth.idp.attribute.context.AttributeContext},
 *  {@link UsernameContext}, {@link UsernamePasswordContext}, {@link UserAgentContext}, {@link CertificateContext},
 *  {@link ExternalAuthenticationContext}, {@link KerberosTicketContext}, {@link LDAPResponseContext},
 *  {@link AuthenticationErrorContext}, {@link AuthenticationWarningContext}
 * @added Before authentication flow runs
 */
public final class AuthenticationContext extends BaseContext {

    /** Time when the authentication process started. */
    @Nonnull private final Instant initiationInstant;

    /** Whether to require fresh subject interaction to succeed. */
    private boolean forceAuthn;

    /** Whether authentication must not involve subject interaction. */
    private boolean isPassive;
    
    /** A normative hint some protocols support to indicate who the subject MUST be. */
    @Nullable private String requiredName;

    /** A non-normative hint some protocols support to indicate who the subject might be. */
    @Nullable private String hintedName;

    /** Allowed time since an {@link AuthenticationResult} was created that it can be reused for this request. */
    @Nullable private Duration maxAge;
    
    /** Name of a proxied authentication source to use. */
    @Nullable private String authenticatingAuthority;
    
    /** Allowable proxy count upstream. */
    @Nullable @NonNegative private Integer proxyCount;

    /** Allowable proxied sources of authority. */
    @Nonnull @NonnullElements private Set<String> proxiableAuthorities;
    
    /** Lookup strategy for a fixed event to return from validators for testing. */
    @Nullable private Function<ProfileRequestContext,String> fixedEventLookupStrategy;
    
    /** Flows that are known to the system. */
    @Nonnull @NonnullElements private final Map<String,AuthenticationFlowDescriptor> availableFlows;

    /** Flows that could potentially be used to authenticate the user. */
    @Nonnull @NonnullElements private final Map<String,AuthenticationFlowDescriptor> potentialFlows;

    /** Authentication results associated with an active session and available for (re)use. */
    @Nonnull @NonnullElements private final Map<String,AuthenticationResult> activeResults;

    /** Previously attempted flows (could be failures or intermediate results). */
    @Nonnull @NonnullElements private final Map<String,AuthenticationFlowDescriptor> intermediateFlows;
    
    /** Instance of registry used for auto-creation of {@link RequestedPrincipalContext}. */
    @Nullable private PrincipalEvalPredicateFactoryRegistry evalRegistry;
    
    /** Authentication flow being attempted to authenticate the user. */
    @Nullable private AuthenticationFlowDescriptor attemptedFlow;
    
    /** Signals authentication flow to run next, to influence selection logic. */
    @Nullable private String signaledFlowId;

    /** Storage map for interflow communication. */
    @Nonnull private final Map<String,Object> stateMap;

    /** A successful authentication result (the output of the attempted flow, if any). */
    @Nullable private AuthenticationResult authenticationResult;

    /** Result may be cached for reuse in the normal way. */
    private boolean resultCacheable;
    
    /** Time when authentication process completed. */
    @Nullable private Instant completionInstant;

    /** Constructor. */
    public AuthenticationContext() {
        final Instant now = Instant.now();
        assert now!=null;
        initiationInstant = now;
        
        availableFlows = new HashMap<>();
        potentialFlows = new LinkedHashMap<>();
        activeResults = new HashMap<>();
        intermediateFlows = new HashMap<>();
        
        stateMap = new HashMap<>();
        
        resultCacheable = true;
        
        proxiableAuthorities = new HashSet<>();
    }

    /**
     * Get the time when the authentication process started.
     * 
     * @return time when the authentication process started
     */
    @Nonnull public Instant getInitiationInstant() {
        return initiationInstant;
    }

    /**
     * Get previous authentication results currently active for the subject.
     * 
     * <p>These should be used to identify SSO opportunities. Results produced during a particular
     * authentication run should not be included in this collection.</p>
     * 
     * @return authentication results currently active for the subject
     */
    @Nonnull @NonnullElements @Live public Map<String,AuthenticationResult> getActiveResults() {
        return activeResults;
    }

    /**
     * Set the authentication results currently active for the subject.
     * 
     * @param results authentication results currently active for the subject
     * 
     * @return this authentication context
     */
    @Nonnull public AuthenticationContext setActiveResults(
            @Nonnull @NonnullElements final Iterable<AuthenticationResult> results) {
        Constraint.isNotNull(results, "AuthenticationResult collection cannot be null");

        activeResults.clear();
        for (final AuthenticationResult result : results) {
            activeResults.put(result.getAuthenticationFlowId(), result);
        }

        return this;
    }

    
    /**
     * Get the set of flows known to the system overall.
     * 
     * <p>Authentication flows supplied by the configuration and gradually filtered down to
     * a collection that can be used to authenticate the subject.</p>
     * 
     * @return the available flows, independent of their potential for use at a given time
     * 
     * @since 3.3.0
     */
    @Nonnull @NonnullElements @Live public Map<String,AuthenticationFlowDescriptor> getAvailableFlows() {
        return availableFlows;
    }
    
    /**
     * Get the set of flows that could potentially be used for authentication.
     * 
     * <p>Initially the same as {@link #getAvailableFlows()}, it may be filtered down to a smaller set.</p>
     * 
     * @return the potential flows
     */
    @Nonnull @NonnullElements @Live public Map<String,AuthenticationFlowDescriptor> getPotentialFlows() {
        return potentialFlows;
    }

    
    /**
     * Get the set of flows that have been executed, successfully or otherwise, without producing a completed result.
     * 
     * <p>This tracks flows that have already been run to avoid unintentional repeated attempts to run the same
     * flow.</p>
     * 
     * @return the intermediately executed flows
     */
    @Nonnull @NonnullElements @Live public Map<String,AuthenticationFlowDescriptor> getIntermediateFlows() {
        return intermediateFlows;
    }
    
    /**
     * Get the registry of predicate factories for custom principal evaluation.
     * 
     * <p>This object is only needed when evaluating a {@link RequestedPrincipalContext}, so the presence of it at
     * this level of the tree is solely for use by the {@link #addRequestedPrincipalContext(String, List, boolean)}
     * helper method.</p>
     * 
     * @return predicate factory registry
     */
    @Nonnull public PrincipalEvalPredicateFactoryRegistry getPrincipalEvalPredicateFactoryRegistry() {
        
        final RequestedPrincipalContext rpCtx = getSubcontext(RequestedPrincipalContext.class);
        if (rpCtx != null) {
            return rpCtx.getPrincipalEvalPredicateFactoryRegistry();
        } else if (evalRegistry != null) {
            return evalRegistry;
        } else {
            return new PrincipalEvalPredicateFactoryRegistry();
        }
    }

    /**
     * Set the registry of predicate factories for custom principal evaluation to inject into instances of
     * {@link RequestedPrincipalContext} created via the {@link #addRequestedPrincipalContext(String, List, boolean)}
     * helper method.
     * 
     * <p>It also propagates this object into any existing {@link RequestedPrincipalContext} subcontext.</p>
     * 
     * @param registry predicate factory registry
     * 
     * @return this context
     */
    @Nonnull public AuthenticationContext setPrincipalEvalPredicateFactoryRegistry(
            @Nullable final PrincipalEvalPredicateFactoryRegistry registry) {
        
        evalRegistry = registry;
        
        final RequestedPrincipalContext rpCtx = getSubcontext(RequestedPrincipalContext.class);
        if (rpCtx != null && registry != null) {
            rpCtx.setPrincipalEvalPredicateFactoryRegistry(registry);
        }
        
        return this;
    }
    
    /**
     * Get whether subject interaction is allowed.
     * 
     * <p>Flows that support this feature <strong>MUST</strong> be implemented with awareness of
     * this value. If a flow doesn't examine this property, it should be marked as non-supporting
     * or would have to be universally lacking in subject interaction.</p>
     * 
     * @return whether subject interaction may occur
     */
    public boolean isPassive() {
        return isPassive;
    }

    /**
     * Set whether subject interaction is allowed.
     * 
     * @param passive whether subject interaction may occur
     * 
     * @return this authentication context
     */
    @Nonnull public AuthenticationContext setIsPassive(final boolean passive) {
        isPassive = passive;
        return this;
    }
    
    /**
     * Get whether to require fresh subject interaction to succeed.
     * 
     * <p>Flows may not explicitly be aware of this property, but if they include any
     * internal orchestration of other flows, then they <strong>MUST</strong> be aware of it
     * to avoid reuse of previous results.</p>
     * 
     * @return whether subject interaction must occur
     */
    public boolean isForceAuthn() {
        return forceAuthn;
    }

    /**
     * Set whether to require fresh subject interaction to succeed.
     * 
     * @param force whether subject interaction must occur
     * 
     * @return this authentication context
     */
    @Nonnull public AuthenticationContext setForceAuthn(final boolean force) {
        forceAuthn = force;
        return this;
    }
    
    /**
     * Get a normative hint provided by the request about the user's identity.
     * 
     * <p>This <strong>MUST BE</strong> a trustworthy value, and can only be set through some
     * normative protocol machinery or with the understanding that it is subsequently a governing
     * constraint on the canonical result of every flow.</p>
     * 
     * @return  the mandatory username
     * 
     * @since 4.1.0
     */
    @Nullable @NotEmpty public String getRequiredName() {
        return requiredName;
    }
    
    /**
     * Set a normative hint provided by the request about the user's identity.
     * 
     * <p>This <strong>MUST BE</strong> a trustworthy value, and can only be set through some
     * normative protocol machinery or with the understanding that it is subsequently a governing
     * constraint on the canonical result of every flow.</p>
     * 
     * @param name the required username
     * 
     * @return this authentication context
     * 
     * @since 4.1.0
     */
    @Nonnull public AuthenticationContext setRequiredName(@Nullable final String name) {
        requiredName = name;
        return this;
    }
    
    /**
     * Get a non-normative hint provided by the request about the user's identity.
     * 
     * <p>This is <strong>NOT</strong> a trustworthy value, but may be used to optimize
     * the user experience. There should be no assumptions about the safety or format of this
     * value, so care should be taken in using it.</p>
     * 
     * @return  the username hint
     */
    @Nullable @NotEmpty public String getHintedName() {
        return hintedName;
    }
    
    /**
     * Set a non-normative hint provided by the request about the user's identity.
     * 
     * @param hint the username hint
     * 
     * @return this authentication context
     */
    @Nonnull public AuthenticationContext setHintedName(@Nullable final String hint) {
        hintedName = StringSupport.trimOrNull(hint);
        return this;
    }
    
    /**
     * Get duration since an {@link AuthenticationResult} was created that
     * allows it to be reused for this request.
     * 
     * <p>If null, no constraint is applied.</p>
     * 
     * @return duration
     * 
     * @since 3.4.0
     */
    @Nullable public Duration getMaxAge() {
        return maxAge;
    }
    
    /**
     * Set duration since an {@link AuthenticationResult} was created that
     * allows it to be reused for this request.
     * 
     * <p>Set to null to apply no constraint.</p>
     * 
     * @param age duration
     * 
     * @return this context
     * 
     * @since 3.4.0
     */
    @Nonnull public AuthenticationContext setMaxAge(@Nullable final Duration age) {
        Constraint.isFalse(age != null && (age.isNegative() || age.isZero()), "MaxAge must be null or greater than 0");
        
        maxAge = age;
        return this;
    }
    
    /**
     * Gets the identifier or location, depending on protocol, of an authentication system to use
     * as a proxied source of authentication.
     * 
     * @return identifier/location of proxy authentication source
     * 
     * @since 4.0.0
     */
    @Nullable public String getAuthenticatingAuthority() {
        return authenticatingAuthority;
    }

    /**
     * Sets the identifier or location, depending on protocol, of an authentication system to use
     * as a proxied source of authentication.
     * 
     * <p>Used to track the result of a hardcoded or discovery-driven determination of a
     * third party authentication source.</p>
     * 
     * @param authority identifier/location of proxy authentication source
     * 
     * @return this context
     * 
     * @since 4.0.0
     */
    @Nonnull public AuthenticationContext setAuthenticatingAuthority(@Nullable final String authority) {
        authenticatingAuthority = StringSupport.trimOrNull(authority);
        return this;
    }
    
    /**
     * Get the allowable number of hops upstream to permit for proxied authentication.
     * 
     * <p>This follows SAML semantics, but is not strictly specific to it.</p>
     * 
     * @return proxy count, null for no limit, zero for no proxying
     * 
     * @since 4.0.0
     */
    @Nullable @NonNegative public Integer getProxyCount() {
        return proxyCount;
    }
    
    /**
     * Set the allowable number of hops upstream to permit for proxied authentication.
     * 
     * @param count proxy count, null for no limit, zero for no proxying
     * 
     * @return this context
     * 
     * @since 4.0.0
     */
    @Nonnull public AuthenticationContext setProxyCount(@Nullable @NonNegative final Integer count) {
        if (count != null) {
            Constraint.isGreaterThanOrEqual(0, count, "Proxy count cannot be negative");
        }
        proxyCount = count;
        return this;
    }
    
    /**
     * Get a live set of the authorities to which proxying is suggested.
     * 
     * <p>This follows SAML semantics and is non-critical, but is not strictly specific to it.</p>
     * 
     * @return advisory set of authorities
     * 
     * @since 4.0.0
     */
    @Nonnull @NonnullElements @Live public Set<String> getProxiableAuthorities() {
        return proxiableAuthorities;
    }
    
    /**
     * Get optional lookup strategy to return a fixed event to return from credential validation
     * to exercise error and warning logic.
     * 
     * @return lookup strategy, or null
     * 
     * @since 3.4.0
     */
    @Nullable public Function<ProfileRequestContext,String> getFixedEventLookupStrategy() {
        return fixedEventLookupStrategy;
    }
    
    /**
     * Set optional lookup strategy to return a fixed event to return from credential validation
     * to exercise error and warning logic.
     * 
     * @param strategy lookup strategy
     * 
     * @return this context
     * 
     * @since 3.4.0
     */
    @Nonnull public AuthenticationContext setFixedEventLookupStrategy(
            @Nullable final Function<ProfileRequestContext,String> strategy) {
        fixedEventLookupStrategy = strategy;
        return this;
    }

    /**
     * Get the authentication flow that was attempted in order to authenticate the user.
     * 
     * <p>This field will hold the flow being run while it is executing, and will continue to contain
     * that value until/unless another flow is run. It is not set if an existing result was reused
     * <strong>by the IdP's own machinery</strong> for SSO, and subsequent to authentication will
     * inform as to the fact that SSO was or was not done, and which flow was used.</p>
     * 
     * @return authentication flow that was attempted in order to authenticate the user
     */
    @Nullable public AuthenticationFlowDescriptor getAttemptedFlow() {
        return attemptedFlow;
    }

    /**
     * Set the authentication flow that was attempted in order to authenticate the user.
     * 
     * <p>Do not set if an existing result was reused for SSO.</p>
     * 
     * @param flow authentication flow that was attempted in order to authenticate the user
     * 
     * @return this authentication context
     */
    @Nonnull public AuthenticationContext setAttemptedFlow(@Nullable final AuthenticationFlowDescriptor flow) {
        attemptedFlow = flow;
        return this;
    }
    
    /**
     * Get the flow ID signaled as the next selection.
     * 
     * <p>A login flow may set this value to signal the authentication flow to transfer control
     * immediately to another login flow instead of proceeding in ordered fashion picking flows
     * to attempt. Generally it is more effective to actually call a login flow from within
     * another flow and subsume it than to rely on this signaling mechanism.</p>
     * 
     * @return  ID of flow to run next
     */
    @Nullable @NotEmpty public String getSignaledFlowId() {
        return signaledFlowId;
    }
    
    /**
     * Set the flow ID signaled as the next selection.
     * 
     * @param id ID of flow to run next
     * 
     * @return this authentication context
     */
    @Nonnull public AuthenticationContext setSignaledFlowId(@Nullable final String id) {
        signaledFlowId = StringSupport.trimOrNull(id);
        return this;
    }    

    /**
     * Get the map of intermediate state that flows can use to pass information amongst themselves.
     * 
     * <p>This is a simple string-based map of attributes that can be used to carry information between
     * login flows or for subsequent use, without relying on native Spring WebFlow mechanisms.</p>
     * 
     * @return the state map
     */
    @Nonnull @Live public Map<String,Object> getAuthenticationStateMap() {
        return stateMap;
    }
    
    /**
     * Get the authentication result produced by the attempted flow, or reused for SSO.
     * 
     * <p>The last flow to complete successfully should have its results stored here. Composite
     * flows should be aware that they may need to preserve intermediate results, and the only get
     * to produce one single result at the end.</p>
     * 
     * @return authentication result, if any
     */
    @Nullable public AuthenticationResult getAuthenticationResult() {
        return authenticationResult;
    }

    /**
     * Set the authentication result produced by the attempted flow, or reused for SSO.
     * 
     * @param result authentication result, if any
     * 
     * @return this authentication context
     */
    @Nonnull public AuthenticationContext setAuthenticationResult(@Nullable final AuthenticationResult result) {
        authenticationResult = result;
        return this;
    }

    /**
     * Get whether the result is suitable for caching (such as in a session) for reuse.
     * 
     * <p>Allows flows to indicate at runtime if their results should be cached for future
     * use, or thrown away after a single use.</p>
     * 
     * @return  true iff the result may be cached/reused, subject to other policy
     */
    public boolean isResultCacheable() {
        return resultCacheable;
    }
    
    /**
     * Set whether the result is suitable for caching (such as in a session) for reuse.
     * 
     * @param flag  flag to set
     */
    public void setResultCacheable(final boolean flag) {
        resultCacheable = flag;
    }
        
    /**
     * Get the time when the authentication process ended.
     * 
     * @return time when the authentication process ended
     */
    @Nullable public Instant getCompletionInstant() {
        return completionInstant;
    }

    /**
     * Set the completion time of the authentication attempt to the current time.
     * 
     * @return this authentication context
     */
    @Nonnull public AuthenticationContext setCompletionInstant() {
        completionInstant = Instant.now();
        return this;
    }

    /**
     * Helper method that evaluates a {@link PrincipalSupportingComponent} against a
     * {@link RequestedPrincipalContext} child of this context, if present, to determine
     * if the input is compatible with it.
     * 
     * @param component component to evaluate
     * 
     * @return true iff the input is compatible with the requested authentication requirements or if
     *  no such requirements have been imposed
     */
    public boolean isAcceptable(@Nonnull final PrincipalSupportingComponent component) {
        final RequestedPrincipalContext rpCtx = getSubcontext(RequestedPrincipalContext.class);
        if (rpCtx != null) {
            return rpCtx.isAcceptable(component);
        }
        // No requirements so anything is acceptable.
        return checkProxyRestrictions(component.getSupportedPrincipals(ProxyAuthenticationPrincipal.class));
    }
        
    /**
     * Helper method that evaluates {@link Principal} objects against a {@link RequestedPrincipalContext} child
     * of this context, if present, to determine if the input is compatible with them.
     * 
     * @param principals principal(s) to evaluate
     * 
     * @return true iff the input is compatible with the requested authentication requirements or if
     *  no such requirements have been imposed
     */
    public boolean isAcceptable(@Nonnull @NonnullElements final Collection<Principal> principals) {
        final RequestedPrincipalContext rpCtx = getSubcontext(RequestedPrincipalContext.class);
        if (rpCtx != null) {
            return rpCtx.isAcceptable(principals);
        }
        // No requirements so anything is acceptable.
        return checkProxyRestrictions(principals
                .stream()
                .filter(ProxyAuthenticationPrincipal.class::isInstance)
                .map(ProxyAuthenticationPrincipal.class::cast)
                .collect(Collectors.toUnmodifiableList()));
    }

    /**
     * Helper method that evaluates a {@link Principal} object against a {@link RequestedPrincipalContext} child
     * of this context, if present, to determine if the input is compatible with it.
     * 
     * @param <T> type of principal
     * @param principal principal to evaluate
     * 
     * @return true iff the input is compatible with the requested authentication requirements or if
     *  no such requirements have been imposed
     */
    public <T extends Principal> boolean isAcceptable(@Nonnull final T principal) {
        final RequestedPrincipalContext rpCtx = getSubcontext(RequestedPrincipalContext.class);
        if (rpCtx != null) {
            return rpCtx.isAcceptable(principal);
        }
        // No requirements so anything is acceptable.
        if (principal instanceof ProxyAuthenticationPrincipal) {
            return checkProxyRestrictions(CollectionSupport.singletonList((ProxyAuthenticationPrincipal) principal));
        }
        return true;
    }

    /**
     * Add (or replace) a {@link RequestedPrincipalContext} as a child of this context using the
     * supplied parameters and the previously established {@link PrincipalEvalPredicateFactoryRegistry}
     * for comparison handling.
     * 
     * @param operator matching operator
     * @param className name of class to wrap principal names
     * @param principal name of principal to request
     * @param replace whether to replace an existing context or simply return false
     * 
     * @return true iff a new context was created
     * 
     * @throws Exception if the principal class can't be loaded or instantiated as required
     */
    public boolean addRequestedPrincipalContext(@Nonnull @NotEmpty final String operator,
            @Nonnull @NotEmpty final String className, @Nonnull @NotEmpty final String principal,
            final boolean replace) throws Exception {
        
        return addRequestedPrincipalContext(operator, className, CollectionSupport.singletonList(principal), replace);
    }
    
    /**
     * Add (or replace) a {@link RequestedPrincipalContext} as a child of this context using the
     * supplied parameters and the previously established {@link PrincipalEvalPredicateFactoryRegistry}
     * for comparison handling.
     * 
     * @param operator matching operator
     * @param className name of class to wrap principal names
     * @param principals names of principals to request
     * @param replace whether to replace an existing context or simply return false
     * 
     * @return true iff a new context was created
     * 
     * @throws Exception if the principal class can't be loaded or instantiated as required
     */
    public boolean addRequestedPrincipalContext(@Nonnull @NotEmpty final String operator,
            @Nonnull @NotEmpty final String className, @Nonnull final Collection<String> principals,
            final boolean replace) throws Exception {
        
        final Class<? extends Principal> claz = Class.forName(className).asSubclass(Principal.class);
        final Constructor<? extends Principal> ctor = claz.getConstructor(String.class);
        
        final List<Principal> prins = new ArrayList<>(principals.size());
        for (final String prin : principals) {
            prins.add(ctor.newInstance(prin));
        }
        
        return addRequestedPrincipalContext(operator, prins, replace);
    }

    /**
     * Add (or replace) a {@link RequestedPrincipalContext} as a child of this context using the
     * supplied parameters and the previously established {@link PrincipalEvalPredicateFactoryRegistry}
     * for comparison handling.
     * 
     * @param operator matching operator
     * @param principal principal to request
     * @param replace whether to replace an existing context or simply return false
     * 
     * @return true iff a new context was created
     */
    public boolean addRequestedPrincipalContext(@Nonnull @NotEmpty final String operator,
            @Nonnull final Principal principal, final boolean replace) {
        
        return addRequestedPrincipalContext(operator, CollectionSupport.singletonList(principal), replace);
    }
    
    /**
     * Add (or replace) a {@link RequestedPrincipalContext} as a child of this context using the
     * supplied parameters and the previously established {@link PrincipalEvalPredicateFactoryRegistry}
     * for comparison handling.
     * 
     * @param operator matching operator
     * @param principals principals to request
     * @param replace whether to replace an existing context or simply return false
     * 
     * @return true iff a new context was created
     */
    public boolean addRequestedPrincipalContext(@Nonnull @NotEmpty final String operator,
            @Nonnull @NonnullElements final List<Principal> principals, final boolean replace) {
        
        RequestedPrincipalContext rpCtx = getSubcontext(RequestedPrincipalContext.class);
        if (rpCtx != null && !replace) {
            return false;
        }
        
        rpCtx = new RequestedPrincipalContext();
        rpCtx.setOperator(operator)
            .setRequestedPrincipals(principals);
        
        if (evalRegistry != null) {
            rpCtx.setPrincipalEvalPredicateFactoryRegistry(evalRegistry);
        }
        
        addSubcontext(rpCtx, true);
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("initiationInstant", initiationInstant)
                .add("isPassive", isPassive)
                .add("forceAuthn", forceAuthn)
                .add("requiredName", requiredName)
                .add("hintedName", hintedName)
                .add("maxAge", maxAge)
                .add("potentialFlows", potentialFlows.keySet())
                .add("activeResults", activeResults.keySet())
                .add("attemptedFlow", attemptedFlow)
                .add("signaledFlowId", signaledFlowId)
                .add("authenticationStateMap", stateMap)
                .add("resultCacheable", resultCacheable)
                .add("authenticationResult", authenticationResult)
                .add("completionInstant", completionInstant)
                .toString();
    }

    /**
     * Check for proxy restrictions and evaluate them against the associated {@link RelyingPartyContext}.
     * 
     * @param principals the proxy restrictions
     * 
     * @return true iff proxying is permissible or inapplicable
     */
    private boolean checkProxyRestrictions(
            @Nullable @NonnullElements final Collection<ProxyAuthenticationPrincipal> principals) {
        
        if (principals == null || principals.isEmpty()) {
            return true;
        }

        final BaseContext prc = getParent();
        if (!(prc instanceof ProfileRequestContext)) {
            return true;
        }
        
        for (final ProxyAuthenticationPrincipal proxied : principals) {
            if (!proxied.test((ProfileRequestContext) prc)) {
                return false;
            }
        }
        
        return true;
    }
    
}