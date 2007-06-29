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

package edu.internet2.middleware.shibboleth.common.config.attribute.resolver.attributeDefinition;

import java.util.List;

import edu.internet2.middleware.shibboleth.common.attribute.encoding.AttributeEncoder;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.PrincipalAuthenticationMethodDefinition;

/**
 * Factory bean for creating simple attribute definitions.
 */
public class PrincipalAuthenticationMethodAttributeDefinitionFactoryBean extends BaseAttributeDefinitionFactoryBean {

    /** {@inheritDoc} */
    public Class getObjectType() {
        return PrincipalAuthenticationMethodDefinition.class;
    }

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        PrincipalAuthenticationMethodDefinition definition = new PrincipalAuthenticationMethodDefinition();
        definition.setId(getPluginId());
        
        if(getAttributeDefinitionDependencyIds() != null){
            definition.getAttributeDefinitionDependencyIds().addAll(getAttributeDefinitionDependencyIds());
        }
        
        if(getDataConnectorDependencyIds() != null){
            definition.getDataConnectorDependencyIds().addAll(getDataConnectorDependencyIds());
        }
        
        definition.setSourceAttributeID(getSourceAttributeId());

        List<AttributeEncoder> encoders = getAttributeEncoders();
        if (encoders != null && encoders.size() > 0) {
            for (AttributeEncoder encoder : encoders) {
                definition.getAttributeEncoders().put(encoder.getEncoderCategory(), encoder);
            }
        }

        return definition;
    }
}