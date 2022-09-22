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

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.principal.PrincipalEvalPredicate;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicateFactory;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicateFactoryRegistry;
import net.shibboleth.idp.authn.principal.PrincipalSupportingComponent;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.messaging.context.BaseContext;

/**
 * A context that holds information about an authentication request's
 * requirement for a specific custom {@link Principal}.
 * 
 * <p>Authentication protocols with features for requesting specific forms of
 * authentication will populate this context type with an expression of those
 * requirements in the form of a protocol-specific operator string and an ordered
 * list of custom {@link Principal} objects.</p>
 * 
 * <p>During the authentication process, interactions with
 * {@link net.shibboleth.idp.authn.principal.PrincipalSupportingComponent}-supporting objects
 * will depend on them satisfying context requirements, via the use of registered
 * {@link net.shibboleth.idp.authn.principal.PrincipalEvalPredicateFactory} objects.</p>
 * 
 * <p>Upon successful authentication the most appropriate "matching" {@link Principal} will be
 * saved back to this context for use in generating a protocol response.</p>
 * 
 * @parent {@link AuthenticationContext}
 * @added Before the authentication process begins
 */
public final class RequestedPrincipalContext extends BaseContext {

    /** The registry of predicate factories for custom principal evaluation. */
    @Nonnull private PrincipalEvalPredicateFactoryRegistry evalRegistry;

    /** Comparison operator specific to request protocol. */
    @Nullable private String operatorString;

    /** The principals reflecting the request requirements. */
    @Nonnull @NonnullElements private List<Principal> requestedPrincipals;
    
    /** The principal that satisfied the request, if any. */
    @Nullable private Principal matchingPrincipal;
    
    /** Constructor. */
    public RequestedPrincipalContext() {
        evalRegistry = new PrincipalEvalPredicateFactoryRegistry();
        requestedPrincipals = Collections.emptyList();
    }

    /**
     * Get the registry of predicate factories for custom principal evaluation.
     * 
     * @return predicate factory registry
     * 
     * @since 3.3.0
     */
    @Nonnull public PrincipalEvalPredicateFactoryRegistry getPrincipalEvalPredicateFactoryRegistry() {
        return evalRegistry;
    }

    /**
     * Set the registry of predicate factories for custom principal evaluation.
     * 
     * @param registry predicate factory registry
     * 
     * @return this context
     * 
     * @since 3.3.0
     */
    @Nonnull public RequestedPrincipalContext setPrincipalEvalPredicateFactoryRegistry(
            @Nonnull final PrincipalEvalPredicateFactoryRegistry registry) {
        
        evalRegistry = Constraint.isNotNull(registry, "PrincipalEvalPredicateFactoryRegistry cannot be null");
        return this;
    }
    
    /**
     * Get the comparison operator for matching requested principals. 
     * 
     * @return comparison operator
     */
    @Nullable @NotEmpty public String getOperator() {
        return operatorString;
    }
    
    /**
     * Set the comparison operator for matching requested principals.
     * 
     * @param operator comparison operator
     * 
     * @return this context
     */
    @Nonnull public RequestedPrincipalContext setOperator(@Nullable @NotEmpty final String operator) {
        
        operatorString = StringSupport.trimOrNull(operator);
        return this;
    }

    /**
     * Get an immutable list of principals reflecting the request requirements.
     * 
     * @return  immutable list of principals 
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public List<Principal> getRequestedPrincipals() {
        return requestedPrincipals;
    }
    
    /**
     * Set list of principals reflecting the request requirements.
     * 
     * @param principals list of principals
     * 
     * @return this context
     */
    @Nonnull public RequestedPrincipalContext setRequestedPrincipals(
            @Nonnull @NonnullElements final List<Principal> principals) {
        
        requestedPrincipals = List.copyOf(Constraint.isNotNull(principals, "Principal list cannot be null"));
        return this;
    }
    
    /**
     * Get the principal that matched the request's requirements, if any.
     * 
     * @return  a matching principal, or null
     */
    @Nullable public Principal getMatchingPrincipal() {
        return matchingPrincipal;
    }
    
    /**
     * Set the principal that matched the request's requirements, if any.
     * 
     * @param principal a matching principal, or null
     * 
     * @return this context
     */
    @Nonnull public RequestedPrincipalContext setMatchingPrincipal(@Nullable final Principal principal) {
        
       matchingPrincipal = principal;
       return this;
    }
    
    /**
     * Get a predicate to apply based on a principal type and the content of this context.
     * 
     * @param principal principal to obtain predicate for
     * 
     * @return predicate or null
     */
    @Nullable public PrincipalEvalPredicate getPredicate(@Nonnull final Principal principal) {
        
        if (operatorString != null) {
            final PrincipalEvalPredicateFactory factory = evalRegistry.lookup(principal.getClass(), operatorString);
            return factory != null ? factory.getPredicate(principal) : null;
        }
        return null;
    }
    
    /**
     * Helper method that evaluates a {@link PrincipalSupportingComponent} against
     * this context to determine if the input is compatible with it.
     * 
     * @param component component to evaluate
     * 
     * @return true iff the input is compatible with the requested authentication requirements
     * 
     * @since 3.3.0
     */
    public boolean isAcceptable(@Nonnull final PrincipalSupportingComponent component) {
        for (final Principal requestedPrincipal : requestedPrincipals) {
            final PrincipalEvalPredicate predicate = getPredicate(requestedPrincipal);
            if (predicate != null) {
                if (predicate.test(component)) {
                    return true;
                }
            }
        }
        
        // Nothing matched the candidate.
        return false;
    }

    /**
     * Helper method that evaluates {@link Principal} objects against this context
     * to determine if the input is compatible with it.
     * 
     * @param principals principal(s) to evaluate
     * 
     * @return true iff the input is compatible with the requested authentication requirements
     *  
     *  @since 3.3.0
     */
    public boolean isAcceptable(@Nonnull @NonnullElements final Collection<Principal> principals) {
        return isAcceptable(new PrincipalSupportingComponent() {
            public <T extends Principal> Set<T> getSupportedPrincipals(final Class<T> c) {
                final HashSet<T> set = new HashSet<>();
                for (final Principal p : principals) {
                    if (c.isAssignableFrom(p.getClass())) {
                        set.add(c.cast(p));
                    }
                }
                return set;
            }
        });
    }

    /**
     * Helper method that evaluates a {@link Principal} object against this context
     * to determine if the input is compatible with it.
     * 
     * @param <T> type of principal
     * @param principal principal to evaluate
     * 
     * @return true iff the input is compatible with the requested authentication requirements
     */
    public <T extends Principal> boolean isAcceptable(@Nonnull final T principal) {
        return isAcceptable(new PrincipalSupportingComponent() {
            public <TT extends Principal> Set<TT> getSupportedPrincipals(final Class<TT> c) {
                if (c.isAssignableFrom(principal.getClass())) {
                    return Collections.singleton(c.cast(principal));
                }
                return Collections.emptySet();
            }
        });
    }

}