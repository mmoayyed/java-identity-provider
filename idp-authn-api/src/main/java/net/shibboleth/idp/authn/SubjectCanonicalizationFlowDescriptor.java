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

package net.shibboleth.idp.authn;

import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.MoreObjects;

import net.shibboleth.idp.profile.FlowDescriptor;
import net.shibboleth.shared.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.PredicateSupport;

/**
 * A descriptor for a subject canonicalization flow.
 * 
 * <p>
 * A flow models a sequence of profile actions that performs canonicalization of a {@link javax.security.auth.Subject}
 * into a string-form principal name. Flows can do essentially anything, including interact with the subject, but must
 * include an activation predicate to indicate their suitability based on the content of the
 * {@link ProfileRequestContext}, particularly the required
 * {@link net.shibboleth.idp.authn.context.SubjectCanonicalizationContext} child context.
 * </p>
 */
public class SubjectCanonicalizationFlowDescriptor extends AbstractIdentifiableInitializableComponent implements
        FlowDescriptor, Predicate<ProfileRequestContext> {

    /** Predicate that must be true for this flow to be usable for a given request. */
    @Nonnull private Predicate<ProfileRequestContext> activationCondition;

    /** Constructor. */
    public SubjectCanonicalizationFlowDescriptor() {
        activationCondition = PredicateSupport.alwaysTrue();
    }

    /**
     * Set the activation condition in the form of a {@link Predicate} such that iff the condition evaluates to true
     * should the corresponding flow be allowed/possible.
     * 
     * @param condition predicate that controls activation of the flow
     */
    public void setActivationCondition(@Nonnull final Predicate<ProfileRequestContext> condition) {
        checkSetterPreconditions();
        activationCondition = Constraint.isNotNull(condition, "Activation condition predicate cannot be null");
    }

    /** {@inheritDoc} */
    @Override public boolean test(final ProfileRequestContext input) {
        return activationCondition.test(input);
    }
    
    /** {@inheritDoc} */
    @Override public int hashCode() {
        return getId().hashCode();
    }

    /** {@inheritDoc} */
    @Override public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof SubjectCanonicalizationFlowDescriptor) {
            return getId().equals(((SubjectCanonicalizationFlowDescriptor) obj).getId());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return MoreObjects.toStringHelper(this).add("flowId", getId()).toString();
    }

}