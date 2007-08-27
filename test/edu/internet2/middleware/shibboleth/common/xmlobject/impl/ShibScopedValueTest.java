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

import edu.internet2.middleware.shibboleth.common.xmlobject.BaseShibObjectProviderTestCase;
import edu.internet2.middleware.shibboleth.common.xmlobject.ShibbolethMetadataScope;
import edu.internet2.middleware.shibboleth.common.xmlobject.ShibbolethScopedValue;

/**
 * Testing shib:ScopedValue encoder extension.
 */
public class ShibScopedValueTest extends BaseShibObjectProviderTestCase {

    private String expectedValue;
    private String expectedScope;
    
    private String scopeAttribute;
    private String scopeDelimiter;

    /** Constructor. */
    public ShibScopedValueTest() {
        singleElementFile = DATA_PATH + "/impl/ShibScopedValue.xml";
        singleElementOptionalAttributesFile = DATA_PATH + "/impl/ShibScopedValueOptionalAttributes.xml";
    }

    /** {@inheritDoc} */
    protected void setUp() throws Exception {
        super.setUp();

        expectedValue = "member";
        expectedScope = "example.edu";
        scopeAttribute = "scope";
        scopeDelimiter = "@";
    }

    /** {@inheritDoc} */
    public void testSingleElementMarshall() {
        ShibbolethScopedValue sv = (ShibbolethScopedValue) buildXMLObject(ShibbolethScopedValue.TYPE_NAME);

        sv.setValue(expectedValue + scopeDelimiter + expectedScope);

        assertEquals(expectedDOM, sv);
    }
    
    /** {@inheritDoc} */
    public void testSingleElementOptionalAttributesMarshall() {
        ShibbolethScopedValue sv = (ShibbolethScopedValue) buildXMLObject(ShibbolethScopedValue.TYPE_NAME);
        
        sv.setValue(expectedValue);
        sv.setScopeAttributeName(scopeAttribute);
        sv.setScope(expectedScope);
        
        assertEquals(expectedOptionalAttributesDOM, sv);
    }

    /** {@inheritDoc} */
    public void testSingleElementUnmarshall() {
        ShibbolethScopedValue sv = (ShibbolethScopedValue) unmarshallElement(singleElementFile);

        assertNotNull("Unmarshalled object was null", sv);
        assertEquals("Scoped value", expectedValue + scopeDelimiter + expectedScope, sv.getValue());
    }

    /** {@inheritDoc} */
    public void testSingleElementOptionalAttributesUnmarshall() {
        ShibbolethScopedValue sv = (ShibbolethScopedValue) unmarshallElement(singleElementOptionalAttributesFile);
        sv.setScopeAttributeName(scopeAttribute);
        
        assertNotNull("Unmarshalled object was null", sv);
        assertEquals("Scoped value", expectedValue, sv.getValue());
        assertEquals("Scope value", expectedScope, sv.getScope());
        assertEquals("Scope attribute name", scopeAttribute, sv.getScopeAttributeName());
    }
}
