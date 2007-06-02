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
import edu.internet2.middleware.shibboleth.common.config.attribute.resolver.AbstractResolutionPluginFactoryBean;

/**
 *Base Spring factory bean that produces attribute definitions.
 */
public abstract class BaseAttributeDefinitionFactoryBean extends AbstractResolutionPluginFactoryBean {

    /** Attribute ID of the source attribute. */
    private String sourceAttributeId;
    
    /** Whether attributes produced by the definition should be released outside the resolver. */
    private boolean dependencyOnly;
    
    /** Encoders for the attributes. */
    private List<AttributeEncoder> attributeEncoders;

    /**
     * Gets the encoders for the attributes.
     * 
     * @return encoders for the attributes
     */
    public List<AttributeEncoder> getAttributeEncoders() {
        return attributeEncoders;
    }

    /**
     * Sets the encoders for the attributes.
     * 
     * @param encoders encoders for the attributes
     */
    public void setAttributeEncoders(List<AttributeEncoder> encoders) {
        attributeEncoders = encoders;
    }

    /**
     * Gets whether attributes produced by the definition should be released outside the resolver.
     * 
     * @return whether attributes produced by the definition should be released outside the resolver
     */
    public boolean isDependencyOnly() {
        return dependencyOnly;
    }

    /**
     * Sets whether attributes produced by the definition should be released outside the resolver.
     * 
     * @param isDependencyOnly whether attributes produced by the definition should be released outside the resolver
     */
    public void setDependencyOnly(boolean isDependencyOnly) {
        dependencyOnly = isDependencyOnly;
    }

    /**
     * Gets the ID of the attribute that serves as the source of information for the attribute definition.
     * 
     * @return ID of the attribute that serves as the source of information for the attribute definition
     */
    public String getSourceAttributeId() {
        return sourceAttributeId;
    }

    /**
     * Sets the ID of the attribute that serves as the source of information for the attribute definition.
     * 
     * @param id ID of the attribute that serves as the source of information for the attribute definition
     */
    public void setSourceAttributeId(String id) {
        sourceAttributeId = id;
    }    
}