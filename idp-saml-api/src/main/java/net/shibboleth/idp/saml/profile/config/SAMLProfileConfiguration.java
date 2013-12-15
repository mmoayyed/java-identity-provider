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

import java.util.Set;

import javax.annotation.Nonnull;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;

import com.google.common.base.Predicate;

/** Base class for SAML profile configurations. */
public interface SAMLProfileConfiguration extends ProfileConfiguration {

    /**
     * Get the predicate used to determine if the generated assertion should be signed.
     * 
     * @return predicate used to determine if the generated assertion should be signed
     */
    @Nonnull public Predicate<ProfileRequestContext> getSignAssertionsPredicate();

    /**
     * Get the predicate used to determine if the received request should be signed.
     * 
     * @return predicate used to determine if the received request should be signed
     */
    @Nonnull public Predicate<ProfileRequestContext> getSignedRequestsPredicate();

    /**
     * Get the predicate used to determine if the generated response should be signed.
     * 
     * @return predicate used to determine if the generated response should be signed
     */
    public Predicate<ProfileRequestContext> getSignResponsesPredicate();

    /**
     * Get the lifetime of an assertion in milliseconds.
     * 
     * @return lifetime of an assertion in milliseconds
     */
    @Positive public long getAssertionLifetime();

    /**
     * Get an unmodifiable set of audiences, in addition to the relying party(ies) to which the IdP is issuing the
     * assertion, with which an assertion may be shared.
     * 
     * @return additional audiences to which an assertion may be shared
     */
    @Nonnull @NonnullElements @NotLive public Set<String> getAdditionalAudiencesForAssertion();

}