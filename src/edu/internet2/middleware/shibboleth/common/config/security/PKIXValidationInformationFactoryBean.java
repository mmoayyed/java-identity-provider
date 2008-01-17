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

import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.opensaml.xml.security.x509.BasicPKIXValidationInformation;
import org.opensaml.xml.security.x509.PKIXValidationInformation;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Factory bean for building instances of {@link PKIXValidationInformation}.
 */
public class PKIXValidationInformationFactoryBean extends AbstractFactoryBean {

    /** Certificates respresented by this info set. */
    private List<X509Certificate> certificates;

    /** CRL respresented by this info set. */
    private List<X509CRL> x509crls;
    
    /** Max verify depth represented by this info set. */
    private Integer verifyDepth;
    

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        List<X509Certificate> certs = new ArrayList<X509Certificate>();
        if (getCertificates() != null) {
            certs.addAll(getCertificates());
        }
        List<X509CRL> crls = new ArrayList<X509CRL>();
        if (getCrls() != null) {
            crls.addAll(getCrls());
        }
        
        return new BasicPKIXValidationInformation(certs, crls, getVerifyDepth());
    }
    
    /** {@inheritDoc} */
    public Class getObjectType() {
        return PKIXValidationInformation.class;
    }

    /**
     * Gets the cerificates respresented by this info set.
     * 
     * @return cerificates respresented by this info set
     */
    public List<X509Certificate> getCertificates() {
        return certificates;
    }
    
    /**
     * Gets the CRLs respresented by this info set.
     * 
     * @return CRLs respresented by this info set
     */
    public List<X509CRL> getCrls() {
        return x509crls;
    }
    
    /**
     * Get the max verify depth represented by this info set.
     * 
     * @return the max verify depth
     */
    public Integer getVerifyDepth() {
        return verifyDepth;
    }

    /**
     * Sets the cerificates respresented by this info set.
     * 
     * @param certs cerificates respresented by this info set
     */
    public void setCertificates(List<X509Certificate> certs) {
        certificates = certs;
    }

    /**
     * Sets the CRLs respresented by this info set.
     * 
     * @param crls CRLs respresented by this info set
     */
    public void setCrls(List<X509CRL> crls) {
        this.x509crls = crls;
    }

    /**
     * Set the max verify depth represented by this info set.
     * 
     * @param newDepth the new max verify depth
     */
    public void setVerifyDepth(Integer newDepth) {
        verifyDepth = newDepth;
    }
 
}