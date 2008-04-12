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
import java.util.Locale;
import java.util.Map;

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

    /** Localized human intelligible attribute name. */
    private Map<Locale, String> displayNames;

    /** Localized human readable description of attribute. */
    private Map<Locale, String> displayDescriptions;

    /**
     * Gets the encoders for the attributes.
     * 
     * @return encoders for the attributes
     */
    public List<AttributeEncoder> getAttributeEncoders() {
        return attributeEncoders;
    }

    /**
     * Gets the localized human readable description of attribute.
     * 
     * @return human readable description of attribute
     */
    public Map<Locale, String> getDisplayDescriptions() {
        return displayDescriptions;
    }

    /**
     * Gets the localized human readable name of the attribute.
     * 
     * @return human readable name of the attribute
     */
    public Map<Locale, String> getDisplayNames() {
        return displayNames;
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

        if (getDisplayNames() != null) {
            definition.getDisplayNames().putAll(getDisplayNames());
        }

        if (getDisplayDescriptions() != null) {
            definition.getDisplayDescriptions().putAll(getDisplayDescriptions());
        }

        if (getDependencyIds() != null) {
            definition.getDependencyIds().addAll(getDependencyIds());
        }

        if (getAttributeEncoders() != null) {
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
     * Sets the human readable description of attribute.
     * 
     * @param descriptions human readable descriptions of attribute
     */
    public void setDisplayDescriptions(Map<Locale, String> descriptions) {
        displayDescriptions = descriptions;
    }

    /**
     * Sets the human readable name of the attribute.
     * 
     * @param names human readable names of the attribute
     */
    public void setDisplayNames(Map<Locale, String> names) {
        displayNames = names;
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