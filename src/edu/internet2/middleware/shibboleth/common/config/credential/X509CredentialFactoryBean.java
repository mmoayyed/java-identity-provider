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

package edu.internet2.middleware.shibboleth.common.config.credential;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.crypto.SecretKey;

import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.security.x509.X509Credential;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Factory bean for building {@link X509Credential}s.
 */
public class X509CredentialFactoryBean extends AbstractFactoryBean {
    
    /** Usage type of the credential. */
    private UsageType usageType;

    /** Names for the key represented by the credential. */
    private List<String> keyNames;

    /** Secret key respresented by this credential. */
    private SecretKey secretKey;

    /** Private key respresented by this credential. */
    private PrivateKey privateKey;

    /** Public key respresented by this credential. */
    private PublicKey publicKey;

    /** Certificate respresented by this credential. */
    private List<X509Certificate> certificates;

    /** CRL respresented by this credential. */
    private List<X509CRL> x509crls;

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        BasicX509Credential credential = new BasicX509Credential();
        
        credential.setUsageType(usageType);
        
        if(keyNames != null){
            credential.getKeyNames().addAll(keyNames);
        }

        credential.setSecretKey(secretKey);
        credential.setPrivateKey(privateKey);
        credential.setPublicKey(publicKey);
        
        if(certificates != null){
            credential.setEntityCertificate(certificates.get(0));
            credential.setEntityCertificateChain(certificates);
        }
        
        if(x509crls != null){
            credential.setCRLs(x509crls);
        }
        
        return credential;
    }

    /**
     * Gets the cerificates respresented by this credential.
     * 
     * @return cerificates respresented by this credential
     */
    public List<X509Certificate> getCertificates() {
        return certificates;
    }
    
    /**
     * Gets the CRLs respresented by this credential.
     * 
     * @return CRLs respresented by this credential
     */
    public List<X509CRL> getCrls() {
        return x509crls;
    }
    
    /**
     * Gets the names for the key represented by the credential.
     * 
     * @return names for the key represented by the credential
     */
    public List<String> getKeyNames() {
        return keyNames;
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return X509Credential.class;
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
     * Gets the usage type of the credential.
     * 
     * @return usage type of the credential
     */
    public UsageType getUsageType(){
        return usageType;
    }

    /**
     * Sets the cerificates respresented by this credential.
     * 
     * @param certs cerificates respresented by this credential
     */
    public void setCertificates(List<X509Certificate> certs) {
        certificates = certs;
    }

    /**
     * Sets the CRLs respresented by this credential.
     * 
     * @param crls CRLs respresented by this credential
     */
    public void setCrls(List<X509CRL> crls) {
        this.x509crls = crls;
    }

    /**
     * Sets the names for the key represented by the credential.
     * 
     * @param names names for the key represented by the credential
     */
    public void setKeyNames(List<String> names) {
        keyNames = names;
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

    /**
     * Sets the usage type of the credential.
     * 
     * @param type usage type of the credential
     */
    public void setUsageType(UsageType type){
        usageType = type;
    }
}