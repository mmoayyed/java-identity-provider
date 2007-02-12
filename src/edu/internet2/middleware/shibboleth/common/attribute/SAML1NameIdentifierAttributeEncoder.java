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

/**
 * SAML 1.X NameIdentifier attribute encoder.
 * 
 * @param <ValueType> the type of attribute values this encoder can operate on
 */
public interface SAML1NameIdentifierAttributeEncoder<ValueType> 
    extends XMLObjectAttributeEncoder {

    /**
     * Gets the name domain qualifier for the NameID.
     * 
     * @return name domain qualifier for the NameID
     */
    public String getNameQualifier();

    /**
     * Sets the name domain qualifier for the NameID.
     * 
     * @param qualifier name domain qualifier for the NameID
     */
    public void setNameQualifier(String qualifier);
}