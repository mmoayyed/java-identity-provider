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

package net.shibboleth.idp.saml.relyingparty.idwsf;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.shibboleth.idp.saml.relyingparty.saml2.SsoProfileConfiguration;

import org.opensaml.util.StringSupport;

/** Configuration for constrained Liberty IDWSF SSOS requests. */
public class SsosProfileConfiguration extends SsoProfileConfiguration {

    /** ID for this profile configuration. */
    public static final String PROFILE_ID = "http://shibboleth.net/ns/profiles/liberty/ssos";

    /** Maximum number of times a given token is allowed to have been delegated. Default value: 0 */
    private int maximumTokenDelegationChainLength;

    /** Entities to which a token may be delegated. Default value: no delegates */
    private Set<String> allowedDelegates;

    /** Constructor. */
    public SsosProfileConfiguration() {
        this(PROFILE_ID);
    }

    /**
     * Constructor.
     * 
     * @param profileId unique ID for this profile
     */
    protected SsosProfileConfiguration(String profileId) {
        super(profileId);
        maximumTokenDelegationChainLength = 0;
        allowedDelegates = Collections.emptySet();
    }

    /**
     * Get the maximum number of times a given token is allowed to have been delegated.
     * 
     * @return maximum number of times a given token is allowed to have been delegated
     */
    public int getMaximumTokenDelegationChainLength() {
        return maximumTokenDelegationChainLength;
    }

    /**
     * Set the maximum number of times a given token is allowed to have been delegated.
     * 
     * @param length maximum number of times a given token is allowed to have been delegated
     */
    public void setMaximumTokenDelegationChainLength(int length) {
        maximumTokenDelegationChainLength = length;
    }

    /**
     * Get the unmodifiable set of allowed delegates.
     * 
     * @return the set of allowed delegates, never null nor containing null entries
     */
    public Set<String> getAllowedDelegates() {
        return allowedDelegates;
    }

    /**
     * Get the set of allowed delegates.
     * 
     * @param delegates the new set of allowed delegates, may be null or include null elements
     */
    public void setAllowedDelegates(Collection<String> delegates) {
        if (delegates == null || delegates.isEmpty()) {
            allowedDelegates = Collections.emptySet();
            return;
        }

        HashSet<String> newDelegates = new HashSet<String>();
        String trimmedDelegate;
        for (String delegate : delegates) {
            trimmedDelegate = StringSupport.trimOrNull(delegate);
            if (trimmedDelegate != null) {
                newDelegates.add(trimmedDelegate);
            }
        }

        if (newDelegates.isEmpty()) {
            allowedDelegates = Collections.emptySet();
        } else {
            allowedDelegates = Collections.unmodifiableSet(newDelegates);
        }
    }
}