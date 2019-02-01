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

package net.shibboleth.idp.profile.config;

import java.util.function.Predicate;

import javax.annotation.Nonnull;

import com.google.common.base.Predicates;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import org.opensaml.profile.context.ProfileRequestContext;

/**
 * Base class for {@link ConditionalProfileConfiguration} implementations.
 *
 * @since 3.4.0
 */
public abstract class AbstractConditionalProfileConfiguration extends AbstractProfileConfiguration
        implements ConditionalProfileConfiguration {

    /** Activation condition. */
    @Nonnull private Predicate<ProfileRequestContext> activationCondition;

    /**
     * Constructor.
     * 
     * @param id ID of the communication profile, never null or empty
     */
    public AbstractConditionalProfileConfiguration(@Nonnull @NotEmpty @ParameterName(name="id") final String id) {
        super(id);
        
        activationCondition = Predicates.alwaysTrue();
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull public Predicate<ProfileRequestContext> getActivationCondition() {
        return activationCondition;
    }

    /**
     * Set an activation condition to control this profile.
     *
     * @param condition condition to apply
     */
    public void setActivationCondition(@Nonnull final Predicate<ProfileRequestContext> condition) {
        activationCondition = Constraint.isNotNull(condition, "Activation condition cannot be null");
    }
    
}