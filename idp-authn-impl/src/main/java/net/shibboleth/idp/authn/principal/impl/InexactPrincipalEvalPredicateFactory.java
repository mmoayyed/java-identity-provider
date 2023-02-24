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

package net.shibboleth.idp.authn.principal.impl;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.principal.PrincipalEvalPredicate;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicateFactory;
import net.shibboleth.idp.authn.principal.PrincipalSupportingComponent;
import net.shibboleth.shared.annotation.constraint.Live;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.logic.Constraint;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * {@link PrincipalEvalPredicateFactory} that implements inexact matching of principals,
 * based on an arbitrary set of "matches" configured at runtime.
 * 
 * <p>Matching is based on string equality between two principals via the {@link Principal#getName()}
 * method.</p>
 * 
 * <p>This component implements essentially arbitrary comparison rules by being unaware of
 * the actual semantics implied by a match. For example, a given instance of the component
 * could be configured with rules for matching principals that are "at least as good as"
 * or "better than" a candidate value, but the component doesn't know or care about that
 * meaning.</p>
 */
public class InexactPrincipalEvalPredicateFactory implements PrincipalEvalPredicateFactory {

    /** Rules for matching. */
    @Nonnull @NonnullElements private final HashMultimap<String,String> matchingRules;

    /** Constructor. */
    InexactPrincipalEvalPredicateFactory() {
        final HashMultimap<String,String> hm = HashMultimap.create();
        assert hm != null;
        matchingRules = hm;
    }
    
    /**
     * Get the matching rules to apply.
     * 
     * @return  a mutable multimap of the matching rules to apply
     */
    @Nonnull @NonnullElements @Live Multimap<String,String> getMatchingRules() {
        return matchingRules;
    }
    
    /**
     * Set the matching rules to apply.
     * 
     * <p>The input is a map of sets for bean compatibility.</p>
     * 
     * @param rules matching rules
     */
    public void setMatchingRules(@Nonnull @NonnullElements final Map<String,Collection<String>> rules) {
        Constraint.isNotNull(rules, "Map cannot be null");
        matchingRules.clear();
        
        for (final Map.Entry<String,Collection<String>> e : rules.entrySet()) {
            if (!Strings.isNullOrEmpty(e.getKey()) && e.getValue() != null) {
                matchingRules.putAll(e.getKey(), List.copyOf(e.getValue()));
            }
        }
    }
    
    /** {@inheritDoc} */
    @Nonnull public PrincipalEvalPredicate getPredicate(@Nonnull final Principal candidate) {
        return new InexactMatchPredicate(candidate);
    }
    
    /** Implementation of an inexact-matching predicate. */
    private class InexactMatchPredicate implements PrincipalEvalPredicate {

        /** The principal object to compare against. */
        @Nonnull private final Principal principal;
        
        /** The principal object that matched. */
        @Nullable private Principal theMatch;
        
        /**
         * Constructor.
         *
         * @param candidate principal to compare against
         */
        public InexactMatchPredicate(@Nonnull final Principal candidate) {
            principal = Constraint.isNotNull(candidate, "Principal cannot be null");
        }

        /** {@inheritDoc} */
        public boolean test(final PrincipalSupportingComponent input) {
            assert input != null;
            final Set<String> matches = matchingRules.get(principal.getName());
            final Set<? extends Principal> inputs = input.getSupportedPrincipals(principal.getClass());
            
            for (final Principal p : inputs) {
                if (matches.contains(p.getName())) {
                    theMatch = p;
                    return true;
                }
            }
            
            return false;
        }

        /** {@inheritDoc} */
        @Nullable public Principal getMatchingPrincipal() {
            return theMatch;
        }
    }
    
}
