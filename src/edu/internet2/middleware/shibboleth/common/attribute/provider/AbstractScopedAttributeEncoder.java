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

/**
 * Base class for scoped attribute encoders.
 */
public abstract class AbstractScopedAttributeEncoder extends AbstractAttributeEncoder {

    /** Type of scoping to use. */
    private String scopeType;

    /** Delimeter used for "inline" scopeType. */
    private String scopeDelimiter;

    /** Attribute name used for "attribute" scopeType. */
    private String scopeAttribute;

    /**
     * Get the scope attribute.
     * 
     * @return Returns the scopeAttribute.
     */
    public String getScopeAttribute() {
        return scopeAttribute;
    }

    /**
     * Get the scope delimiter.
     * 
     * @return Returns the scopeDelimiter.
     */
    public String getScopeDelimiter() {
        return scopeDelimiter;
    }

    /**
     * Get the scope type.
     * 
     * @return Returns the scopeType.
     */
    public String getScopeType() {
        return scopeType;
    }

    /**
     * Set the scope attribute.
     * 
     * @param newScopeAttribute The scopeAttribute to set.
     */
    public void setScopeAttribute(String newScopeAttribute) {
        scopeAttribute = newScopeAttribute;
    }

    /**
     * Set the scope delimiter.
     * 
     * @param newScopeDelimiter The scopeDelimiter to set.
     */
    public void setScopeDelimiter(String newScopeDelimiter) {
        scopeDelimiter = newScopeDelimiter;
    }

    /**
     * Set the scope type.
     * 
     * @param newScopeType The scopeType to set.
     */
    public void setScopeType(String newScopeType) {
        scopeType = newScopeType;
    }

}