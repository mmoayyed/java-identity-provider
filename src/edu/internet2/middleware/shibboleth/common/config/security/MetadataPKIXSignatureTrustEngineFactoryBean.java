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
import java.util.List;

import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.xml.security.keyinfo.BasicProviderKeyInfoCredentialResolver;
import org.opensaml.xml.security.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xml.security.keyinfo.KeyInfoProvider;
import org.opensaml.xml.security.keyinfo.provider.DSAKeyValueProvider;
import org.opensaml.xml.security.keyinfo.provider.InlineX509DataProvider;
import org.opensaml.xml.security.keyinfo.provider.RSAKeyValueProvider;
import org.opensaml.xml.signature.impl.PKIXSignatureTrustEngine;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import edu.internet2.middleware.shibboleth.common.security.MetadataPKIXValidationInformationResolver;

/**
 * Spring factory bean used to created {@link PKIXSignatureTrustEngine}s based on a metadata provider.
 */
public class MetadataPKIXSignatureTrustEngineFactoryBean extends AbstractFactoryBean {

    /** Metadata provider used to look up PKIX information for peer entities. */
    private MetadataProvider metadataProvider;

    /**
     * Gets the metadata provider used to look up PKIX information for peer entities.
     * 
     * @return metadata provider used to look up PKIX information for peer entities
     */
    public MetadataProvider getMetadataProvider() {
        return metadataProvider;
    }

    /**
     * Sets the metadata provider used to look up PKIX information for peer entities.
     * 
     * @param provider metadata provider used to look up PKIX information for peer entities
     */
    public void setMetadataProvider(MetadataProvider provider) {
        metadataProvider = provider;
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return PKIXSignatureTrustEngine.class;
    }

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        MetadataPKIXValidationInformationResolver pviResolver = new MetadataPKIXValidationInformationResolver(
                getMetadataProvider());

        List<KeyInfoProvider> keyInfoProviders = new ArrayList<KeyInfoProvider>();
        keyInfoProviders.add(new DSAKeyValueProvider());
        keyInfoProviders.add(new RSAKeyValueProvider());
        keyInfoProviders.add(new InlineX509DataProvider());
        KeyInfoCredentialResolver keyInfoCredResolver = new BasicProviderKeyInfoCredentialResolver(keyInfoProviders);

        return new PKIXSignatureTrustEngine(pviResolver, keyInfoCredResolver);
    }
}