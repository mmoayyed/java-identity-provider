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

import org.opensaml.xml.util.DatatypeHelper;

import edu.internet2.middleware.shibboleth.common.attribute.encoding.AttributeEncoder;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.BaseAttributeDefinition;
import edu.internet2.middleware.shibboleth.common.config.attribute.resolver.AbstractResolutionPluginFactoryBean;

/**
 * Base Spring factory bean that produces attribute definitions.
 */
public abstract class BaseAttributeDefinitionFactoryBean extends AbstractResolutionPluginFactoryBean {

    /** Attribute ID of the source attribute. */
    private String sourceAttributeId;

    /** Whether attributes produced by the definition should be released outside the resolver. */
    private boolean dependencyOnly;

    /** Encoders for the attributes. */
    private List<AttributeEncoder> attributeEncoders;

    /** Human intelligible attribute name. */
    private String displayName;

    /** Human readbale description of attribute. */
    private String displayDescription;

    /**
     * Gets the encoders for the attributes.
     * 
     * @return encoders for the attributes
     */
    public List<AttributeEncoder> getAttributeEncoders() {
        return attributeEncoders;
    }

    /**
     * Gets the human readbale description of attribute.
     * 
     * @return human readbale description of attribute
     */
    public String getDisplayDescription() {
        return displayDescription;
    }

    /**
     * Gets the human readable name of the attribute.
     * 
     * @return human readable name of the attribute
     */
    public String getDisplayName() {
        return displayName;
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
     * Gets whether attributes produced by the definition should be released outside the resolver.
     * 
     * @return whether attributes produced by the definition should be released outside the resolver
     */
    public boolean isDependencyOnly() {
        return dependencyOnly;
    }

    /**
     * Populates the attribute definition with information from this factory.
     * 
     * @param definition attribute definition to populate
     */
    protected void populateAttributeDefinition(BaseAttributeDefinition definition) {
        definition.setDependencyOnly(isDependencyOnly());
        definition.setDisplayDescription(getDisplayDescription());
        definition.setDisplayName(getDisplayName());

        if (getDependencyIds() != null) {
            definition.getDependencyIds().addAll(getDependencyIds());
        }
        
        if(getAttributeEncoders() != null){
            definition.getAttributeEncoders().addAll(getAttributeEncoders());
        }
        
        definition.setId(getPluginId());
        definition.setSourceAttributeID(getSourceAttributeId());
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
     * Sets whether attributes produced by the definition should be released outside the resolver.
     * 
     * @param isDependencyOnly whether attributes produced by the definition should be released outside the resolver
     */
    public void setDependencyOnly(boolean isDependencyOnly) {
        dependencyOnly = isDependencyOnly;
    }

    /**
     * Sets the human readbale description of attribute.
     * 
     * @param description human readbale description of attribute
     */
    public void setDisplayDescription(String description) {
        displayDescription = DatatypeHelper.safeTrimOrNullString(description);
    }

    /**
     * Sets the human readable name of the attribute.
     * 
     * @param name human readable name of the attribute
     */
    public void setDisplayName(String name) {
        displayName = DatatypeHelper.safeTrimOrNullString(name);
    }

    /**
     * Sets the ID of the attribute that serves as the source of information for the attribute definition.
     * 
     * @param id ID of the attribute that serves as the source of information for the attribute definition
     */
    public void setSourceAttributeId(String id) {
        sourceAttributeId = DatatypeHelper.safeTrimOrNullString(id);
    }
}