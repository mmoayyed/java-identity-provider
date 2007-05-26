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

package edu.internet2.middleware.shibboleth.common.attribute.provider;

import org.opensaml.xml.util.DatatypeHelper;

/**
 * An attribute value with an associated scope.
 */
public class ScopedAttributeValue {

    /** Value of the attribute. */
    private String value;

    /** Scope of the attribute value. */
    private String scope;

    /**
     * Constructor.
     * 
     * @param attributeValue value of the attribute
     * @param valueScope scope of the value
     */
    public ScopedAttributeValue(String attributeValue, String valueScope) {
        value = DatatypeHelper.safeTrimOrNullString(attributeValue);
        scope = DatatypeHelper.safeTrimOrNullString(valueScope);

        if (scope == null || value == null) {
            throw new IllegalArgumentException("Attribute value and scope may not be null");
        }
    }

    /**
     * Gets the value of the attribute.
     * 
     * @return value of the attribute
     */
    public String getValue() {
        return value;
    }

    /**
     * Gets the scope of the value.
     * 
     * @return scope of the value
     */
    public String getScope() {
        return scope;
    }
}