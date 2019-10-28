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

package net.shibboleth.idp.attribute.filter.policyrule.logic.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.filter.PolicyRequirementRule;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;

import com.google.common.base.MoreObjects;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

/**
 * Base class for {@link PolicyRequirementRule} implementations that are compositions of other
 * {@link PolicyRequirementRule}s.
 */
public abstract class AbstractComposedPolicyRule extends AbstractIdentifiableInitializableComponent implements
        PolicyRequirementRule, UnmodifiableComponent {

    /** The composed matchers. */
    private @NonnullAfterInit List<PolicyRequirementRule> rules;

    /** Set the rules to be composed.
     * Called "subsidiaries" to allow easier parsing.
     * @param theRules the rules to be composed.
     */
    public void setSubsidiaries(@Nullable @NullableElements final Collection<PolicyRequirementRule> theRules) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        if (theRules != null) {
            rules = List.copyOf(Collections2.filter(theRules, Predicates.notNull()));
        } else {
            rules = Collections.emptyList();
        }
    }
    
    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (rules == null) {
            throw new ComponentInitializationException("Rules not set up"); 
        }
    }

    /**
     * Get the composed matchers.
     * 
     * @return the composed matchers
     */
    @Nonnull @NonnullElements @Unmodifiable public List<PolicyRequirementRule> getComposedRules() {
        return rules;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return MoreObjects.toStringHelper(this).add("Composed Rules : ", getComposedRules()).toString();
    }
}