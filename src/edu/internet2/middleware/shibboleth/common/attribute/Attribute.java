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

import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;

/**
 * A resolved attribute.
 * 
 * @param <ValueType> the object type of the values for this attribute
 */
public interface Attribute<ValueType>{
    
    /**
     * Gets the unique ID of the attribute.
     * 
     * @return unique ID of the attribute
     */
    public String getId();
    
    /**
     * Gets the compartor used to sort values.  If no compartor is set then the value 
     * set with be natural ordering sorted.
     * 
     * @return compartor used to sort values
     */
    public Comparator<ValueType> getValueComparator();
    
    /**
     * Gets the values of the attribute.
     * 
     * @return values of the attribute
     */
    public SortedSet<ValueType> getValues();
    
    /**
     * Gets the encoder registered under a specific category.
     * 
     * @param category the category of the encoder
     * 
     * @return encoder registered under a specific category
     */
    public AttributeEncoder<Attribute<ValueType>, ?> getEncoderByCategory(String category);
    
    /**
     * Gets the list of attribute encoders usable with this attribute.
     * 
     * @return attribute encoders usable with this attribute
     */
    public List<AttributeEncoder<Attribute<ValueType>, ?>> getEncoders();
}