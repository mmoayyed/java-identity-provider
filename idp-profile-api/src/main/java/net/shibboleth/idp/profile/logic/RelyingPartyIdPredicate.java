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

package net.shibboleth.idp.profile.logic;

import java.util.Collection;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import net.shibboleth.idp.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.StrategyIndirectedPredicate;
import net.shibboleth.shared.primitive.StringSupport;

import org.opensaml.profile.context.ProfileRequestContext;

/**
 * Predicate that evaluates a {@link ProfileRequestContext} by looking for a relying party ID
 * that matches one of a designated set, or a generic predicate. The ID is obtained from a
 * {@link RelyingPartyContext} child of the profile request context.
 */
public class RelyingPartyIdPredicate extends StrategyIndirectedPredicate<ProfileRequestContext,String> {

    /**
     * Constructor.
     * 
     * @param candidates hardwired set of values to check against
     */
    public RelyingPartyIdPredicate(
            @Nonnull @NonnullElements @ParameterName(name="candidates") final Collection<String> candidates) {
        super(new RelyingPartyIdLookupFunction(), StringSupport.normalizeStringCollection(candidates));
    }

    /**
     * Constructor.
     * 
     * @param candidate a single value to check against
     */
    public RelyingPartyIdPredicate(@Nonnull @NotEmpty @ParameterName(name="candidate") final String candidate) {
        this(CollectionSupport.singleton(candidate));
    }

    /**
     * Constructor.
     * 
     * @param pred generalized predicate
     */
    public RelyingPartyIdPredicate(@Nonnull @ParameterName(name="pred") final Predicate<String> pred) {
        super(new RelyingPartyIdLookupFunction(), pred);
    }
    
    /**
     * Workaround for Spring type conversion ambiguities.
     * 
     * @param candidates hardwired set of values to check against
     * 
     * @return the predicate
     * 
     * @since 3.4.0
     */
    @Nonnull public static RelyingPartyIdPredicate fromCandidates(
            @Nonnull @NonnullElements final Collection<String> candidates) {
        return new RelyingPartyIdPredicate(candidates);
    }
    
    /**
     * Workaround for Spring type conversion ambiguities.
     * 
     * @param candidate a single value to check against
     * 
     * @return the predicate
     * 
     *  @since 3.4.0
     */
    @Nonnull public static RelyingPartyIdPredicate fromCandidate(@Nonnull @NotEmpty final String candidate) {
        return new RelyingPartyIdPredicate(candidate);
    }

    /**
     * Workaround for Spring type conversion ambiguities.
     * 
     * @param pred generalized predicate
     * 
     * @return the predicate
     * 
     * @since 3.4.0
     */
    @Nonnull public static RelyingPartyIdPredicate fromPredicate(@Nonnull final Predicate<String> pred) {
        return new RelyingPartyIdPredicate(pred);
    }
    
}