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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.opensaml.xml.security.keyinfo.BasicProviderKeyInfoCredentialResolver;
import org.opensaml.xml.security.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xml.security.keyinfo.KeyInfoProvider;
import org.opensaml.xml.security.keyinfo.provider.DSAKeyValueProvider;
import org.opensaml.xml.security.keyinfo.provider.InlineX509DataProvider;
import org.opensaml.xml.security.keyinfo.provider.RSAKeyValueProvider;
import org.opensaml.xml.security.x509.PKIXValidationInformation;
import org.opensaml.xml.security.x509.StaticPKIXValidationInformationResolver;
import org.opensaml.xml.signature.impl.PKIXSignatureTrustEngine;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Spring factory bean used to create {@link PKIXSignatureTrustEngine}s based on a static 
 * PKIXValidationInformation resolver.
 */
public class StaticPKIXSignatureTrustEngineFactoryBean extends AbstractFactoryBean {
    
    /** List of PKIX validation info. */
    private List<PKIXValidationInformation> pkixInfo;
    
    /** Set of trusted names. */
    private Set<String> trustedNames;

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

    /**
     * Gets the set of trusted names.
     * 
     * @return the set of trusted names
     */
    public Set<String> getTrustedNames() {
        return trustedNames;
    }

    /**
     * Sets the set of trusted names.
     * 
     * @param newTrustedNames the set of trusted names
     */
    public void setTrustedNames(Set<String> newTrustedNames) {
        trustedNames = newTrustedNames;
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return PKIXSignatureTrustEngine.class;
    }
    
    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        Set<String> names = getTrustedNames();
        if (names == null) {
            names = Collections.emptySet();
        }
        StaticPKIXValidationInformationResolver pkixResolver = 
            new StaticPKIXValidationInformationResolver(getPKIXInfo(), names);
        
        List<KeyInfoProvider> keyInfoProviders = new ArrayList<KeyInfoProvider>();
        keyInfoProviders.add(new DSAKeyValueProvider());
        keyInfoProviders.add(new RSAKeyValueProvider());
        keyInfoProviders.add(new InlineX509DataProvider());
        KeyInfoCredentialResolver keyInfoCredResolver = new BasicProviderKeyInfoCredentialResolver(keyInfoProviders);
        
        return new PKIXSignatureTrustEngine(pkixResolver, keyInfoCredResolver);
    }
}