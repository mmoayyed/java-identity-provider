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

package edu.internet2.middleware.shibboleth.common.attribute.encoding;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;


/**
 * Attribute encoders convert {@link BaseAttribute}s into protocol specific representations.
 * 
 * Encoders may have contain a category that can be used to distingush encoder types from
 * each other.  This inteded to be used to look up an encoder that can be used to encode 
 * attributes in accordance with a defined specification or tranmission protcol.
 * 
 * Encoders MUST be thread-safe and stateless.
 * 
 * @param <EncodedType> the type of object created by encoding the attribute
 */
public interface AttributeEncoder<EncodedType> {

    /**
     * Gets the category for this encoder.
     * 
     * @return category for this encoder
     */
    public String getEncoderCategory();
    
    /**
     * Sets the category for this encoder.
     * 
     * @param category category for this encoder
     */
    public void setEncoderCategory(String category);
    
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
     * 
     * @throws AttributeEncodingException if unable to successfully encode attribute
     */
    public EncodedType encode(BaseAttribute attribute) throws AttributeEncodingException;
}