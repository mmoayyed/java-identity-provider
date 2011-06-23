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

package net.shibboleth.idp.relyingparty.impl.saml2;

import java.util.Collection;

import net.shibboleth.idp.relyingparty.impl.AbstractSAMLProfileConfiguration;

import org.opensaml.util.collections.LazySet;

/** Base class for SAML 2 profile configurations. */
public abstract class AbstractSAML2ProfileConfiguration extends AbstractSAMLProfileConfiguration {

    /** Whether to encrypt NameIDs. */
    // TODO
    // private CryptoOperationRequirementLevel encryptNameID;

    /** Whether to encrypt Assertions. */
    // TODO
    // private CryptoOperationRequirementLevel encryptAssertion;

    /** Maximum proxy count for an assertion. */
    private int proxyCount;

    /** Audiences for the proxy. */
    private Collection<String> proxyAudiences;

    /**
     * Constructor.
     * 
     * @param profileId ID of the the communication profile, never null or empty
     */
    public AbstractSAML2ProfileConfiguration(String profileId) {
        super(profileId);
        proxyAudiences = new LazySet<String>();
    }

    /**
     * Gets the maximum number of times an assertion may be proxied.
     * 
     * @return maximum number of times an assertion may be proxied
     */
    public int getProxyCount() {
        return proxyCount;
    }

    /**
     * Gets the maximum number of times an assertion may be proxied.
     * 
     * @param count maximum number of times an assertion may be proxied
     */
    public void setProxyCount(int count) {
        proxyCount = count;
    }

    /**
     * Gets the audiences for a proxied assertion.
     * 
     * @return audiences for a proxied assertion
     */
    public Collection<String> getProxyAudiences() {
        return proxyAudiences;
    }
}