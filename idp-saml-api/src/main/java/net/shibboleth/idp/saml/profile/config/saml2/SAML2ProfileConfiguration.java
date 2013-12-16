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

package net.shibboleth.idp.saml.profile.config.saml2;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.annotation.constraint.NonNegative;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;

import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Predicate;

/**
 * Base interface for SAML 2 profile configurations. 
 */
public interface SAML2ProfileConfiguration {

    /**
     * Get the maximum number of times an assertion may be proxied.
     * 
     * @return maximum number of times an assertion may be proxied
     */
    @NonNegative public long getProxyCount();

    /**
     * Get the unmodifiable collection of audiences for a proxied assertion.
     * 
     * @return audiences for a proxied assertion
     */
    @Nonnull @NonnullElements @NotLive public Collection<String> getProxyAudiences();

    /**
     * Get the predicate used to determine if assertions should be encrypted.
     * 
     * @return predicate used to determine if assertions should be encrypted
     */
    @Nonnull public Predicate<ProfileRequestContext> getEncryptAssertionsPredicate();

    /**
     * Get the predicate used to determine if name identifiers should be encrypted.
     * 
     * @return predicate used to determine if name identifiers should be encrypted
     */
    @Nonnull public Predicate<ProfileRequestContext> getEncryptNameIDsPredicate();

    /**
     * Get the predicate used to determine if attributes should be encrypted.
     * 
     * @return predicate used to determine if attributes should be encrypted
     */
    @Nonnull public Predicate<ProfileRequestContext> getEncryptAttributesPredicate();
}