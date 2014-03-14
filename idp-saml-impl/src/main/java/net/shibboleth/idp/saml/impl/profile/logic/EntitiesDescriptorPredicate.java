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

package net.shibboleth.idp.saml.impl.profile.logic;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

/**
 * Predicate to determine whether the supplied ids are in the EntitiesDescriptor or any of its parents.
 */
public class EntitiesDescriptorPredicate extends AbstractIdentifiableInitializableComponent implements
        Predicate<ProfileRequestContext> {

    /** Relying parties to match against. */
    @Nonnull @NonnullElements private Set<String> entitiesDescriptorIds;
    
    /**
     * Set the relying parties to match against.
     * 
     * @param ids relying party IDs to match against
     */
    public synchronized void setEntitiesDescriptorIds(@Nonnull @NonnullElements final Collection<String> ids) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(ids, "Relying party ID collection cannot be null");
        
        Set<String> newIds = new HashSet<>(ids.size());
        for (final String id : ids) {
            final String trimmed = StringSupport.trimOrNull(id);
            if (trimmed != null) {
                newIds.add(trimmed);
            }
        }
        entitiesDescriptorIds = ImmutableSet.copyOf(newIds);
    }



    /** {@inheritDoc} */
    @Override public boolean apply(@Nullable ProfileRequestContext arg0) {
        // TODO
        return false;
    }

}
