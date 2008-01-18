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

package edu.internet2.middleware.shibboleth.common.config.security;

import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.SecretKey;

import org.opensaml.xml.security.SecurityHelper;
import org.opensaml.xml.security.credential.BasicCredential;
import org.opensaml.xml.security.x509.X509Credential;

/**
 * Factory bean for building {@link X509Credential}s.
 */
public class BasicCredentialFactoryBean extends AbstractCredentialFactoryBean {

    /** Secret key respresented by this credential. */
    private SecretKey secretKey;

    /** Private key respresented by this credential. */
    private PrivateKey privateKey;

    /** Public key respresented by this credential. */
    private PublicKey publicKey;

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        BasicCredential credential = new BasicCredential();
        
        credential.setUsageType(getUsageType());
        
        credential.setEntityId(getEntityID());
        
        if(getKeyNames() != null){
            credential.getKeyNames().addAll(getKeyNames());
        }

        credential.setSecretKey(secretKey);
        credential.setPrivateKey(privateKey);
        if (publicKey != null) {
            credential.setPublicKey(publicKey);
        } else if (privateKey != null) {
            credential.setPublicKey(SecurityHelper.derivePublicKey(privateKey));
        }
        
        return credential;
    }
    
    /** {@inheritDoc} */
    public Class getObjectType() {
        return BasicCredential.class;
    }
    
    /**
     * Gets the private key respresented by this credential.
     * 
     * @return private key respresented by this credential
     */
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    /**
     * Gets the public key respresented by this credential.
     * 
     * @return public key respresented by this credential
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Gets the secret key respresented by this credential.
     * 
     * @return secret key respresented by this credential
     */
    public SecretKey getSecretKey() {
        return secretKey;
    }
    
    /**
     * Sets the private key respresented by this credential.
     * 
     * @param key private key respresented by this credential
     */
    public void setPrivateKey(PrivateKey key) {
        privateKey = key;
    }

    /**
     * Sets the public key respresented by this credential.
     * 
     * @param key public key respresented by this credential
     */
    public void setPublicKey(PublicKey key) {
        publicKey = key;
    }

    /**
     * Sets the secret key respresented by this credential.
     * 
     * @param key secret key respresented by this credential
     */
    public void setSecretKey(SecretKey key) {
        secretKey = key;
    }
 
}