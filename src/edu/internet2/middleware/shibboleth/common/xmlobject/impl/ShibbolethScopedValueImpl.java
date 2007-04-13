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

package edu.internet2.middleware.shibboleth.common.xmlobject.impl;

import org.opensaml.xml.schema.impl.XSStringImpl;

import edu.internet2.middleware.shibboleth.common.xmlobject.ShibbolethScopedValue;

/**
 * Concrete implementation of {@link org.opensaml.xml.schema.XSString}.
 */
public class ShibbolethScopedValueImpl extends XSStringImpl implements ShibbolethScopedValue {

    /** Scope of this string element. */
    private String scope;

    /** Scope attribute naem for this element. */
    private String scopeAttributeName;

    /**
     * Constructor.
     * 
     * @param namespaceURI the namespace the element is in
     * @param elementLocalName the local name of the XML element this Object represents
     * @param namespacePrefix the prefix for the given namespace
     */
    protected ShibbolethScopedValueImpl(String namespaceURI, String elementLocalName, String namespacePrefix) {
        super(namespaceURI, elementLocalName, namespacePrefix);
    }

    /** {@inheritDoc} */
    public String getScope() {
        return scope;
    }

    /** {@inheritDoc} */
    public String getScopeAttributeName() {
        return scopeAttributeName;
    }

    /** {@inheritDoc} */
    public void setScope(String newScope) {
        scope = prepareForAssignment(scope, newScope);
    }

    /** {@inheritDoc} */
    public void setScopeAttributeName(String newScopeAttributeName) {
        scopeAttributeName = prepareForAssignment(scopeAttributeName, newScopeAttributeName);
    }
}