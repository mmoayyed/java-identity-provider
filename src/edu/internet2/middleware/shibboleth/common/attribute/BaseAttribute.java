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

package edu.internet2.middleware.shibboleth.common.attribute;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.internet2.middleware.shibboleth.common.attribute.encoding.AttributeEncoder;

/**
 * A resolved attribute.
 * 
 * @param <ValueType> the object type of the values for this attribute
 */
public abstract class BaseAttribute<ValueType> {
    
    /** Localized human intelligible attribute name. */
    private Map<Locale, String> displayNames;
    
    /** Localized human readable description of attribute. */
    private Map<Locale, String> displayDescriptions;
    
    /** Constructor. */
    protected BaseAttribute(){
        displayNames = new HashMap<Locale, String>();
        displayDescriptions = new HashMap<Locale, String>();
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
     * Gets the list of attribute encoders usable with this attribute.
     * 
     * @return attribute encoders usable with this attribute, must never be null
     */
    public abstract List<AttributeEncoder> getEncoders();

    /**
     * Gets the unique ID of the attribute.
     * 
     * @return unique ID of the attribute
     */
    public abstract String getId();

    /**
     * Gets the compartor used to sort values. If no compartor is set then the value set with be natural ordering
     * sorted.
     * 
     * @return compartor used to sort values
     */
    public abstract Comparator<ValueType> getValueComparator();

    /**
     * Gets the values of the attribute.
     * 
     * @return values of the attribute, must never be null
     */
    public abstract Collection<ValueType> getValues();

    /** {@inheritDoc} */
    public int hashCode() {
        return getId().hashCode();
    }
    
    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if(obj == this){
            return true;
        }
        
        if(obj instanceof BaseAttribute){
            return obj.hashCode() == hashCode();
        }
        
        return false;
    }
    
    /** {@inheritDoc} */
    public String toString() {
        return getId();
    }
}