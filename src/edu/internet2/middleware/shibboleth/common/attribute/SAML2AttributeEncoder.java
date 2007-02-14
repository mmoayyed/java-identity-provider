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

import org.opensaml.common.xml.SAMLConstants;

/**
 * SAML 2.0 attribute encoder.
 * 
 * @param <ValueType> the type of attribute values this encoder can operate on
 */
public interface SAML2AttributeEncoder<ValueType> extends XMLObjectAttributeEncoder {

    /** Category for attribute encoders. */
    public static final String CATEGORY = SAMLConstants.SAML20P_NS;
    
    /**
     * Gets the attribute's name format.
     * 
     * @return attribute's name format
     */
    public String getAttributeFormat();
    
    /**
     * Sets the attribute's name format.
     * 
     * @param format attribute's name format
     */
    public void setAttributeFormat(String format);
    
    /**
     * Gets the human friendly name of the attribute.
     * 
     * @return human friendly name of the attribute
     */
    public String getFriendlyName();
    
    /**
     * Sets the human friendly name of the attribute.
     * 
     * @param name human friendly name of the attribute
     */
    public void setFriendlyName(String name);
}