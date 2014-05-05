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
import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 * Predicate that evaluates a {@link ProfileRequestContext} by looking for relying party ID
 * that matches one of a designated set, obtained from a lookup function, by default from
 * a {@link net.shibboleth.idp.profile.context.RelyingPartyContext} child.
 */
public class RelyingPartyIdPredicate implements Predicate<ProfileRequestContext> {

    /** Lookup strategy for relying party ID. */
    @Nonnull private Function<ProfileRequestContext,String> relyingPartyIdLookupStrategy;
    
    /** Relying parties to match against. */
    @Nonnull @NonnullElements private Set<String> relyingPartyIds;

    /** Constructor. */
    public RelyingPartyIdPredicate() {
        relyingPartyIdLookupStrategy = new RelyingPartyIdLookupFunction();
        relyingPartyIds = Collections.emptySet();
    }

    /**
     * Set the strategy used to obtain the relying party ID for this request.
     * 
     * @param strategy  lookup strategy
     */
    public synchronized void setRelyingPartyIdLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,String> strategy) {
        
        relyingPartyIdLookupStrategy = Constraint.isNotNull(strategy,
                "Relying party ID lookup strategy cannot be null");
    }
    
    /**
     * Set the relying parties to match against.
     * 
     * @param ids relying party IDs to match against
     */
    public synchronized void setRelyingPartyIds(@Nonnull @NonnullElements final Collection<String> ids) {
        Constraint.isNotNull(ids, "Relying party ID collection cannot be null");
        
        relyingPartyIds = Sets.newHashSet();
        for (final String id : ids) {
            final String trimmed = StringSupport.trimOrNull(id);
            if (trimmed != null) {
                relyingPartyIds.add(trimmed);
            }
        }
    }

    /** {@inheritDoc} */
    @Override public boolean apply(@Nullable final ProfileRequestContext input) {

        if (input != null) {
            final String id = relyingPartyIdLookupStrategy.apply(input);
            if (id != null) {
                return relyingPartyIds.contains(id);
            }
        }

        return false;
    }

}