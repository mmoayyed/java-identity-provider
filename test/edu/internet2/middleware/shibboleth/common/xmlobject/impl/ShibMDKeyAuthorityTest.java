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

import javax.xml.namespace.QName;

import org.opensaml.xml.signature.KeyInfo;

import edu.internet2.middleware.shibboleth.common.xmlobject.BaseShibObjectProviderTestCase;
import edu.internet2.middleware.shibboleth.common.xmlobject.ShibbolethMetadataKeyAuthority;

/**
 * Testing shibmd:KeyAuthority metadata extension.
 */
public class ShibMDKeyAuthorityTest extends BaseShibObjectProviderTestCase {
    
    private Integer expectedVerifyDepth;
    private int expectedNumKeyInfos;
    
    private QName unknownAttribName;
    private String unknownAttribValue;
    
    /** Constructor. */
    public ShibMDKeyAuthorityTest() {
        singleElementFile = DATA_PATH + "/impl/ShibMDKeyAuthority.xml";
        singleElementOptionalAttributesFile = DATA_PATH + "/impl/ShibMDKeyAuthorityOptionalAttributes.xml";
        childElementsFile = DATA_PATH + "/impl/ShibMDKeyAuthorityChildElements.xml";
    }

    /** {@inheritDoc} */
    protected void setUp() throws Exception {
        super.setUp();
        
        expectedVerifyDepth = new Integer(5);
        expectedNumKeyInfos  = 4;
        
        unknownAttribName = new QName("http://www.example.org/testObjects", "UnknownAttrib", "test");
        unknownAttribValue = "FooBar";
    }

    /** {@inheritDoc} */
    public void testSingleElementMarshall() {
        ShibbolethMetadataKeyAuthority keyAuthority = 
            (ShibbolethMetadataKeyAuthority) buildXMLObject(ShibbolethMetadataKeyAuthority.DEFAULT_ELEMENT_NAME);
        
        assertEquals(expectedDOM, keyAuthority);
    }

    /** {@inheritDoc} */
    public void testSingleElementOptionalAttributesMarshall() {
        ShibbolethMetadataKeyAuthority keyAuthority = 
            (ShibbolethMetadataKeyAuthority) buildXMLObject(ShibbolethMetadataKeyAuthority.DEFAULT_ELEMENT_NAME);
        
        keyAuthority.setVerifyDepth(expectedVerifyDepth);
        keyAuthority.getUnknownAttributes().put(unknownAttribName, unknownAttribValue);
        
        assertEquals(expectedOptionalAttributesDOM, keyAuthority);
    }
    
    /** {@inheritDoc} */
    public void testChildElementsMarshall() {
        ShibbolethMetadataKeyAuthority keyAuthority = 
            (ShibbolethMetadataKeyAuthority) buildXMLObject(ShibbolethMetadataKeyAuthority.DEFAULT_ELEMENT_NAME);
        
        for (int i = 0; i< expectedNumKeyInfos; i++) {
            keyAuthority.getKeyInfos().add( (KeyInfo) buildXMLObject(KeyInfo.DEFAULT_ELEMENT_NAME)  );
        }
        
        assertEquals(expectedChildElementsDOM, keyAuthority);
    }

    /** {@inheritDoc} */
    public void testSingleElementUnmarshall() {
        ShibbolethMetadataKeyAuthority keyAuthority = 
            (ShibbolethMetadataKeyAuthority) unmarshallElement(singleElementFile);
        
        assertNotNull("Unmarshalled object was null", keyAuthority);
        assertNull("VerifyDepth attribute value", keyAuthority.getVerifyDepth());
        
        assertTrue("Extension attribute map was not empty", keyAuthority.getUnknownAttributes().isEmpty());
        
        assertTrue("KeyInfo list was not empty", keyAuthority.getKeyInfos().isEmpty());
    }

    /** {@inheritDoc} */
    public void testSingleElementOptionalAttributesUnmarshall() {
        ShibbolethMetadataKeyAuthority keyAuthority = 
            (ShibbolethMetadataKeyAuthority) unmarshallElement(singleElementOptionalAttributesFile);
        
        assertNotNull("Unmarshalled object was null", keyAuthority);
        assertEquals("VerifyDepth attribute value", expectedVerifyDepth, keyAuthority.getVerifyDepth());
        
        assertFalse("Extension attribute map was empty", keyAuthority.getUnknownAttributes().isEmpty());
        assertTrue("AttributeMap did not contain expected unknown attribute name", 
                keyAuthority.getUnknownAttributes().containsKey(unknownAttribName));
        assertEquals("AttributeMap did not contain expected unknown attribute value", unknownAttribValue,
                keyAuthority.getUnknownAttributes().get(unknownAttribName));
        
        assertTrue("KeyInfo list was not empty", keyAuthority.getKeyInfos().isEmpty());
        
    }

    /** {@inheritDoc} */
    public void testChildElementsUnmarshall() {
        ShibbolethMetadataKeyAuthority keyAuthority = 
            (ShibbolethMetadataKeyAuthority) unmarshallElement(childElementsFile);
        
        assertNotNull("Unmarshalled object was null", keyAuthority);
        assertNull("VerifyDepth attribute value", keyAuthority.getVerifyDepth());
        
        assertTrue("Extension attribute map was not empty", keyAuthority.getUnknownAttributes().isEmpty());
        
        assertFalse("KeyInfo list was empty", keyAuthority.getKeyInfos().isEmpty());
        assertEquals("# of KeyInfo child elements", expectedNumKeyInfos, keyAuthority.getKeyInfos().size());
    }
 
}
