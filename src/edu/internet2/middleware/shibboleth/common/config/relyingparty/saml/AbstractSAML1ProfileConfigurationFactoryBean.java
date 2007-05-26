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

import edu.internet2.middleware.shibboleth.common.attribute.SAML1AttributeAuthority;
import edu.internet2.middleware.shibboleth.common.relyingparty.provider.saml1.AbstractSAML1ProfileConfiguration;

/**
 * Base Spring factory bean for SAML 1 profile configurations.
 */
abstract class AbstractSAML1ProfileConfigurationFactoryBean extends AbstractSAMLProfileConfigurationFactoryBean {
    
    /** Attribute authority for the profile configuration. */
    private SAML1AttributeAuthority attributeAuthority;

    /**
     * Gets the attribute authority for the profile configuration.
     * 
     * @return attribute authority for the profile configuration
     */
    public SAML1AttributeAuthority getAttributeAuthority(){
        return attributeAuthority;
    }
    
    /**
     * Sets the attribute authority for the profile configuration.
     * 
     * @param authority attribute authority for the profile configuration
     */
    public void setAttributeAuthority(SAML1AttributeAuthority authority){
        attributeAuthority = authority;
    }
    
    /**
     * Populates the given profile configuration with standard information.
     * 
     * @param configuration configuration to populate
     */
    protected void populateBean(AbstractSAML1ProfileConfiguration configuration){
        super.populateBean(configuration);
        configuration.setAttributeAuthority(getAttributeAuthority());
    }
}