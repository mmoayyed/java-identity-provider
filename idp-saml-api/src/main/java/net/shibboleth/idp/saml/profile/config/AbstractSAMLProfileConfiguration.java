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

package net.shibboleth.idp.saml.profile.config;

import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.config.AbstractInterceptorAwareProfileConfiguration;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.PredicateSupport;

import org.opensaml.profile.context.ProfileRequestContext;

/** Base class for SAML profile configurations. */
public abstract class AbstractSAMLProfileConfiguration extends AbstractInterceptorAwareProfileConfiguration implements
        SAMLProfileConfiguration {
    
    /** Predicate used to determine if the generated request should be signed. Default returns false. */
    @Nonnull private Predicate<ProfileRequestContext> signRequestsPredicate;

    /** Predicate used to determine if the generated response should be signed. Default returns false. */
    @Nonnull private Predicate<ProfileRequestContext> signResponsesPredicate;

    /**
     * Constructor.
     * 
     * @param profileId ID of the communication profile
     */
    public AbstractSAMLProfileConfiguration(@Nonnull @NotEmpty final String profileId) {
        super(profileId);

        signRequestsPredicate = PredicateSupport.alwaysFalse();
        signResponsesPredicate = PredicateSupport.alwaysFalse();
    }

    /** {@inheritDoc} */
    public boolean isSignRequests(@Nullable final ProfileRequestContext profileRequestContext) {
        return signRequestsPredicate.test(profileRequestContext);
    }

    /**
     * Set whether generated requests should be signed.
     * 
     * @param flag flag to set
     */
    public void setSignRequests(final boolean flag) {
        signRequestsPredicate = PredicateSupport.constant(flag);
    }
    
    /**
     * Set the predicate used to determine if generated requests should be signed.
     * 
     * @param predicate predicate used to determine if generated requests should be signed
     * 
     * @since 4.0.0
     */
    public void setSignRequestsPredicate(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        signRequestsPredicate = Constraint.isNotNull(predicate, "Condition cannot be null");
    }

    /** {@inheritDoc} */
    public boolean isSignResponses(@Nullable final ProfileRequestContext profileRequestContext) {
        return signResponsesPredicate.test(profileRequestContext);
    }

    /**
     * Set whether generated responses should be signed.
     * 
     * @param flag flag to set
     */
    public void setSignResponses(final boolean flag) {
        signResponsesPredicate = PredicateSupport.constant(flag);
    }
    
    /**
     * Set the predicate used to determine if generated responses should be signed.
     * 
     * @param predicate predicate used to determine if generated responses should be signed
     * 
     * @since 4.0.0
     */
    public void setSignResponsesPredicate(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        signResponsesPredicate = Constraint.isNotNull(predicate, "Condition cannot be null");
    }

}