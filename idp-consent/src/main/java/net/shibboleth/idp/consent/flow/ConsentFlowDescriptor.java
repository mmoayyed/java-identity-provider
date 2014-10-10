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

package net.shibboleth.idp.consent.flow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.interceptor.ProfileInterceptorFlowDescriptor;
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonNegative;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Abstract descriptor for a consent flow.
 * 
 * TODO details
 */
public abstract class ConsentFlowDescriptor extends ProfileInterceptorFlowDescriptor {

    /** Whether consent equality includes comparing consent values. */
    private boolean compareValues;

    /** Maximum amount of time, in milliseconds, before a consent storage record expires. */
    @Nullable @Duration @NonNegative private Long lifetime;

    /**
     * Whether consent equality includes comparing consent values.
     * 
     * @return true if consent equality includes comparing consent values
     */
    public boolean compareValues() {
        return compareValues;
    }

    /**
     * Get maximum amount of time, in milliseconds, before a consent storage record expires.
     * 
     * @return maximum amount of time, in milliseconds, before a consent storage record expires
     */
    @Nullable @NonNegative public Long getLifetime() {
        return lifetime;
    }

    /**
     * Set whether consent equality includes comparing consent values.
     * 
     * @param flag true if consent equality includes comparing consent values
     */
    public void setCompareValues(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        compareValues = flag;
    }

    /**
     * Set maximum amount of time, in milliseconds, before a consent storage record expires.
     * 
     * @param consentLifetime maximum amount of time, in milliseconds, before a consent storage record expires
     */
    public void setLifetime(@Nonnull @Duration @NonNegative final Long consentLifetime) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(consentLifetime, "Lifetime cannot be null");

        lifetime = Constraint.isGreaterThanOrEqual(0, consentLifetime, "Lifetime must be greater than or equal to 0");
    }
}
