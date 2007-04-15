/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.config.relyingparty.saml;

import java.util.List;

/**
 * Base Spring factory bean for SAML 2 profile configurations.
 */
public abstract class AbstractSAML2ProfileConfigurationFactoryBean extends AbstractSAMLProfileConfigurationFactoryBean {

    /** Whether to encrypt NameIDs. */
    private boolean encryptNameIds;

    /** Whether to encryptAssertions. */
    private boolean encryptAssertions;

    /** Maximum number of times an assertion may be proxied. */
    private int assertionProxyCount;
    
    /** Audiences for proxied assertions. */
    private List<String> proxyAudiences;

    /**
     * Gets the maximum number of times an assertion may be proxied.
     * 
     * @return maximum number of times an assertion may be proxied
     */
    public int getAssertionProxyCount() {
        return assertionProxyCount;
    }

    /**
     * Sets the maximum number of times an assertion may be proxied.
     * 
     * @param count maximum number of times an assertion may be proxied
     */
    public void setAssertionProxyCount(int count) {
        assertionProxyCount = count;
    }

    /**
     * Gets whether to encryption assertions.
     * 
     * @return whether to encryption assertions
     */
    public boolean isEncryptAssertions() {
        return encryptAssertions;
    }

    /**
     * Sets whether to encryption assertions.
     * 
     * @param encrypt whether to encryption assertions
     */
    public void setEncryptAssertions(boolean encrypt) {
        encryptAssertions = encrypt;
    }

    /**
     * Gets whether to encrypt NameIDs.
     * 
     * @return whether to encrypt NameIDs
     */
    public boolean isEncryptNameIds() {
        return encryptNameIds;
    }

    /**
     * Sets whether to encrypt NameIDs.
     * 
     * @param encrypt whether to encrypt NameIDs
     */
    public void setEncryptNameIds(boolean encrypt) {
        encryptNameIds = encrypt;
    }
    
    /**
     * Gets the audiences for proxied assertions.
     * 
     * @return audiences for proxied assertions
     */
    public List<String> getProxyAudiences(){
        return proxyAudiences;
    }
    
    /**
     * Sets the audiences for proxied assertions.
     * 
     * @param audiences audiences for proxied assertions
     */
    public void setProxyAudiences(List<String> audiences){
        proxyAudiences = audiences;
    }
}