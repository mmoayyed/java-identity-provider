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
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.security.x509.X509Credential;

/**
 * Factory bean for building {@link X509Credential}s.
 */
public class X509CredentialFactoryBean extends AbstractCredentialFactoryBean {

    /** Private key respresented by this credential. */
    private PrivateKey privateKey;
    
    /** The end-entity certificate. */
    private X509Certificate entityCertificate;

    /** Certificate respresented by this credential. */
    private List<X509Certificate> certificates;

    /** CRL respresented by this credential. */
    private List<X509CRL> x509crls;

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        BasicX509Credential credential = new BasicX509Credential();
        
        credential.setUsageType(getUsageType());
        
        credential.setEntityId(getEntityID());
        
        if(getKeyNames() != null){
            credential.getKeyNames().addAll(getKeyNames());
        }
        
        if(certificates != null){
            credential.setEntityCertificateChain(new ArrayList<X509Certificate>(certificates));
            if (entityCertificate != null) {
                credential.setEntityCertificate(entityCertificate);
            } else {
                credential.setEntityCertificate(certificates.get(0));
            }
        }
        
        if(x509crls != null){
            credential.setCRLs(new ArrayList<X509CRL>(x509crls));
        }
        
        credential.setPrivateKey(privateKey);
        //TODO may adjust BasicX509Credential to make this unnecessary
        credential.setPublicKey(credential.getEntityCertificate().getPublicKey());
        
        
        return credential;
    }
    
    /** {@inheritDoc} */
    public Class getObjectType() {
        return X509Credential.class;
    }

    /**
     * Gets the end-entity cerificate respresented by this credential.
     * 
     * @return entity certificate respresented by this credential
     */
    public X509Certificate getEntityCertificate() {
        return entityCertificate;
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
     * Gets the private key respresented by this credential.
     * 
     * @return private key respresented by this credential
     */
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    /**
     * Sets the end-entity cerificate respresented by this credential.
     * 
     * @param newCert the new entity certificate respresented by this credential
     */
    public void setEntityCertificate(X509Certificate newCert) {
        entityCertificate = newCert;
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
     * Sets the private key respresented by this credential.
     * 
     * @param key private key respresented by this credential
     */
    public void setPrivateKey(PrivateKey key) {
        privateKey = key;
    }
    
}