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

import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;

/**
 * Predicate that evaluates a {@link ProfileRequestContext} by looking for
 * a {@link RelyingPartyContext} with a relying party ID that matches one of
 * a designated set.
 */
public class RelyingPartyIdPredicate extends AbstractIdentifiableInitializableComponent
        implements Predicate<ProfileRequestContext> {

    /** Strategy function to lookup RelyingPartyContext. */
    @Nonnull private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextLookupStrategy;
    
    /** Relying parties to match against. */
    @Nonnull @NonnullElements private Set<String> relyingPartyIds;
    
    /** Constructor. */
    public RelyingPartyIdPredicate() {
        super.setId(getClass().getName());
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
        relyingPartyIds = Collections.emptySet();
    }
    
    /** {@inheritDoc} */
    @Override
    public void setId(@Nonnull @NotEmpty final String id) {
        super.setId(id);
    }
    
    /**
     * Set the lookup strategy to use to locate the {@link RelyingPartyContext}.
     * 
     * @param strategy lookup function to use
     */
    public synchronized void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        relyingPartyContextLookupStrategy = Constraint.isNotNull(strategy,
                "RelyingPartyContext lookup strategy cannot be null");
    }
    
    /**
     * Set the relying parties to match against.
     * 
     * @param ids   relying party IDs to match against
     */
    public synchronized void setRelyingPartyIds(@Nonnull @NonnullElements final Collection<String> ids) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(ids, "Relying party ID collection cannot be null");
        
        relyingPartyIds = Sets.newHashSet(Collections2.filter(ids, Predicates.notNull()));
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean apply(@Nullable final ProfileRequestContext input) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        if (input != null) {
            final RelyingPartyContext rpContext = relyingPartyContextLookupStrategy.apply(input);
            if (rpContext != null) {
                final String id = rpContext.getRelyingPartyId();
                if (id != null) {
                    return relyingPartyIds.contains(id);
                }
            }
        }
        
        return false;
    }

}
