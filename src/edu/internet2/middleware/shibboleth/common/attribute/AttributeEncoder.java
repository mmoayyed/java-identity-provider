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

import org.opensaml.xml.XMLObject;

/**
 * Attribute encoders convert {@link Attribute}s into protocol specific representations.
 * 
 * Encoders MUST be thread-safe and stateless.
 * 
 * @param <AttributeType> the type of attribute this encoder works on
 */
public interface AttributeEncoder<AttributeType extends Attribute> {

    /**
     * Get the name of the attribute.
     * 
     * @return name of the attribute
     */
    public String getAttributeName();
    
    /**
     * Sets the name of the attribute.
     * 
     * @param attributeName name of the attribute
     */
    public void setAttributeName(String attributeName);
    
    /**
     * Enocdes the attribute into a protocol specific representations.
     * 
     * @param attribute the attribute to encode
     * 
     * @return the Object the attribute was encoded into
     */
    public XMLObject encode(AttributeType attribute);
}