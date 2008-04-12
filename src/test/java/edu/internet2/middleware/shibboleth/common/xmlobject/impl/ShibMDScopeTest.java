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

import org.opensaml.xml.schema.XSBooleanValue;

import edu.internet2.middleware.shibboleth.common.xmlobject.BaseShibObjectProviderTestCase;
import edu.internet2.middleware.shibboleth.common.xmlobject.ShibbolethMetadataScope;

/**
 * Testing shibmd:Scope metadata extension.
 */
public class ShibMDScopeTest extends BaseShibObjectProviderTestCase {
    
    private String expectedContent;
    private Boolean expectedRegexp;
    
    /** Constructor. */
    public ShibMDScopeTest() {
        singleElementFile = DATA_PATH + "/impl/ShibMDScope.xml";
        singleElementOptionalAttributesFile = DATA_PATH + "/impl/ShibMDScopeOptionalAttributes.xml";
    }

    /** {@inheritDoc} */
    protected void setUp() throws Exception {
        super.setUp();
        
        expectedContent = "ThisIsSomeScopeValue";
        expectedRegexp = Boolean.TRUE;
    }

    /** {@inheritDoc} */
    public void testSingleElementMarshall() {
        ShibbolethMetadataScope scope = 
            (ShibbolethMetadataScope) buildXMLObject(ShibbolethMetadataScope.DEFAULT_ELEMENT_NAME);
        
        scope.setValue(expectedContent);
        
        assertEquals(expectedDOM, scope);
    }

    /** {@inheritDoc} */
    public void testSingleElementOptionalAttributesMarshall() {
        ShibbolethMetadataScope scope = 
            (ShibbolethMetadataScope) buildXMLObject(ShibbolethMetadataScope.DEFAULT_ELEMENT_NAME);
        
        scope.setValue(expectedContent);
        scope.setRegexp(expectedRegexp);
        
        assertEquals(expectedOptionalAttributesDOM, scope);
    }
    
    /** {@inheritDoc} */
    public void testSingleElementUnmarshall() {
        ShibbolethMetadataScope scope = 
            (ShibbolethMetadataScope) unmarshallElement(singleElementFile);
        
        assertNotNull("Unmarshalled object was null", scope);
        assertEquals("Scope value", expectedContent, scope.getValue());
        assertEquals("Regexp attribute value", Boolean.FALSE, scope.getRegexp());
    }

    /** {@inheritDoc} */
    public void testSingleElementOptionalAttributesUnmarshall() {
        ShibbolethMetadataScope scope = 
            (ShibbolethMetadataScope) unmarshallElement(singleElementOptionalAttributesFile);
        
        assertNotNull("Unmarshalled object was null", scope);
        assertEquals("Scope value", expectedContent, scope.getValue());
        assertEquals("Regexp attribute value", expectedRegexp, scope.getRegexp());
    }
    
    /**
     * Test the proper behavior of the XSBooleanValue attributes.
     */
    public void testXSBooleanAttributes() {
        ShibbolethMetadataScope scope = 
            (ShibbolethMetadataScope) buildXMLObject(ShibbolethMetadataScope.DEFAULT_ELEMENT_NAME);
        
        // regexp attribute
        scope.setRegexp(Boolean.TRUE);
        assertEquals("Unexpected value for boolean attribute found", Boolean.TRUE, scope.getRegexp());
        assertNotNull("XSBooleanValue was null", scope.getRegexpXSBoolean());
        assertEquals("XSBooleanValue was unexpected value", new XSBooleanValue(Boolean.TRUE, false),
                scope.getRegexpXSBoolean());
        assertEquals("XSBooleanValue string was unexpected value", "true", scope.getRegexpXSBoolean().toString());
        
        scope.setRegexp(Boolean.FALSE);
        assertEquals("Unexpected value for boolean attribute found", Boolean.FALSE, scope.getRegexp());
        assertNotNull("XSBooleanValue was null", scope.getRegexpXSBoolean());
        assertEquals("XSBooleanValue was unexpected value", new XSBooleanValue(Boolean.FALSE, false),
                scope.getRegexpXSBoolean());
        assertEquals("XSBooleanValue string was unexpected value", "false", scope.getRegexpXSBoolean().toString());
        
        scope.setRegexp((Boolean) null);
        assertEquals("Unexpected default value for boolean attribute found", Boolean.FALSE, scope.getRegexp());
        assertNull("XSBooleanValue was not null", scope.getRegexpXSBoolean());
    }

}
