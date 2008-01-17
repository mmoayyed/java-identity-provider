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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.opensaml.xml.security.x509.PKIXValidationInformation;
import org.opensaml.xml.security.x509.PKIXX509CredentialTrustEngine;
import org.opensaml.xml.security.x509.StaticPKIXValidationInformationResolver;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Spring factory bean used to create {@link PKIXX509CredentialTrustEngine}s based on a static 
 * PKIXValidationInformation resolver.
 */
public class StaticPKIXX509CredentialTrustEngineFactoryBean extends AbstractFactoryBean {
    
    /** List of PKIX validation info. */
    private List<PKIXValidationInformation> pkixInfo;

    /**
     * Gets the list of PKIX validation info.
     * 
     * @return the list of PKIX validation info 
     */
    public List<PKIXValidationInformation> getPKIXInfo() {
        return pkixInfo;
    }

    /**
     * Sets the list of PKIX validation info.
     * 
     * @param newPKIXInfo the new list of PKIX validation info
     */
    public void setPKIXInfo(List<PKIXValidationInformation> newPKIXInfo) {
        pkixInfo = newPKIXInfo;
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return PKIXX509CredentialTrustEngine.class;
    }
    
    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        Set<String> names = Collections.emptySet();
        StaticPKIXValidationInformationResolver pkixResolver = 
            new StaticPKIXValidationInformationResolver(getPKIXInfo(), names);
        
        return new PKIXX509CredentialTrustEngine(pkixResolver);
    }
}