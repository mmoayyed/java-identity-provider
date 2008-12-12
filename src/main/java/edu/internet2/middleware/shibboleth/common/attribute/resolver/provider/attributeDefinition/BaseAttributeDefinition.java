/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.opensaml.xml.util.LazyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.encoding.AttributeEncoder;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.AbstractResolutionPlugIn;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;

/**
 * Base class for {@link AttributeDefinition} plug-ins.
 */
public abstract class BaseAttributeDefinition extends AbstractResolutionPlugIn<BaseAttribute> implements
        AttributeDefinition {
    
    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(BaseAttributeDefinition.class);

    /** Whether this attribute definition is only a dependency and thus its values should never be released. */
    private boolean dependencyOnly;

    /** Attribute encoders associated with this definition. */
    private ArrayList<AttributeEncoder> encoders;

    /** Name of the attribute from data connectors to use to populate this definition. */
    private String sourceAttributeID;

    /** Localized human intelligible attribute name. */
    private Map<Locale, String> displayNames;
    
    /** Localized human readable description of attribute. */
    private Map<Locale, String> displayDescriptions;

    /** Constructor. */
    public BaseAttributeDefinition() {
        dependencyOnly = false;
        encoders = new ArrayList<AttributeEncoder>(3);
        displayNames = new LazyMap<Locale, String>();
        displayDescriptions = new LazyMap<Locale, String>();
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

    /** {@inheritDoc} */
    public boolean isDependencyOnly() {
        return dependencyOnly;
    }

    /**
     * Sets whether this attribute definition is only a dependency and thus its values should never be released outside
     * the resolver.
     * 
     * @param isDependencyOnly whether this attribute definition is only a dependency
     */
    public void setDependencyOnly(boolean isDependencyOnly) {
        dependencyOnly = isDependencyOnly;
    }

    /** {@inheritDoc} */
    public List<AttributeEncoder> getAttributeEncoders() {
        return encoders;
    }

    /** {@inheritDoc} */
    public BaseAttribute resolve(ShibbolethResolutionContext resolutionContext) throws AttributeResolutionException {
        BaseAttribute resolvedAttribute = doResolve(resolutionContext);

        if(resolvedAttribute == null){
            log.error("{} produced a null attribute, this is not allowed", getId());
            throw new AttributeResolutionException(getId() + " produced a null attribute");
        }
        
        if(getDisplayNames() != null) {
            resolvedAttribute.getDisplayNames().putAll(displayNames);
        }
        
        if(getDisplayDescriptions() != null){
            resolvedAttribute.getDisplayDescriptions().putAll(displayDescriptions);
        }
        
        if (getAttributeEncoders() != null) {
            resolvedAttribute.getEncoders().addAll(getAttributeEncoders());
        }

        return resolvedAttribute;
    }

    /**
     * Creates and populates the values for the resolved attribute. Implementations should *not* set, or otherwise
     * manage, the attribute encoders for the resolved attribute.
     * 
     * @param resolutionContext current attribute resolution context
     * 
     * @return resolved attribute
     * 
     * @throws AttributeResolutionException thrown if there is a problem resolving and creating the attribute
     */
    protected abstract BaseAttribute doResolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException;

    /**
     * Get values from dependencies.
     * 
     * @param context resolution context
     * @return collection of values
     */
    protected Collection<Object> getValuesFromAllDependencies(ShibbolethResolutionContext context) {
        return getValuesFromAllDependencies(context, getSourceAttributeID());
    }

    /**
     * Return the source attribute. If the source attribute is null, return the definition ID.
     * 
     * @return Returns the sourceAttribute.
     */
    public String getSourceAttributeID() {
        if (sourceAttributeID != null) {
            return sourceAttributeID;
        } else {
            return getId();
        }
    }

    /**
     * Set the source attribute.
     * 
     * @param newSourceAttributeID The sourceAttribute to set.
     */
    public void setSourceAttributeID(String newSourceAttributeID) {
        sourceAttributeID = newSourceAttributeID;
    }
}