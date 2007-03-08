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

package edu.internet2.middleware.shibboleth.common.config.resolver;

import java.util.Collection;

import edu.internet2.middleware.shibboleth.common.attribute.AttributeEncoder;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeDefinition;

/**
 * Base factory bean for data connectors.
 * 
 * @param <PlugInType> type of data connector
 */
public abstract class BaseAttributeDefinitionFactoryBean<PlugInType extends AttributeDefinition> extends
        AbstractResolutionPlugInFactoryBean<PlugInType> {

    /** Attribute Encoders. */
    private Collection<AttributeEncoder> encoders;

    /** {@inheritDoc} */
    protected void buildPlugin(PlugInType internalPlugin) {
        super.buildPlugin(internalPlugin);

        if (encoders != null && !encoders.isEmpty()) {
            for (AttributeEncoder encoder : encoders) {
                if (encoder != null) {
                    internalPlugin.getAttributeEncoders().add(encoder);
                }
            }
        }
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return AttributeDefinition.class;
    }

    /**
     * Set encoders.
     * 
     * @param newEncoders attribute encoders
     */
    public void setEncoders(Collection<AttributeEncoder> newEncoders) {
        encoders = newEncoders;
    }

}