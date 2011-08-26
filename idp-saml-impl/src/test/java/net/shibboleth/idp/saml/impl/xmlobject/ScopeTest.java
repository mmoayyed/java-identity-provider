/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.saml.impl.xmlobject;

import net.shibboleth.idp.saml.xmlobject.Scope;

import org.opensaml.xml.schema.XSBooleanValue;
import org.testng.Assert;

/**
 * Testing shibmd:Scope metadata extension.
 */
public class ScopeTest extends BaseShibObjectProviderTestCase {

    private String expectedContent;

    private Boolean expectedRegexp;

    /** Constructor. */
    public ScopeTest() {
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
        Scope scope = (Scope) buildXMLObject(Scope.DEFAULT_ELEMENT_NAME);

        scope.setValue(expectedContent);

        assertEquals(expectedDOM, scope);
    }

    /** {@inheritDoc} */
    public void testSingleElementOptionalAttributesMarshall() {
        Scope scope = (Scope) buildXMLObject(Scope.DEFAULT_ELEMENT_NAME);

        scope.setValue(expectedContent);
        scope.setRegexp(expectedRegexp);

        assertEquals(expectedOptionalAttributesDOM, scope);
    }

    /** {@inheritDoc} */
    public void testSingleElementUnmarshall() {
        Scope scope = (Scope) unmarshallElement(singleElementFile);

        Assert.assertNotNull(scope, "Unmarshalled object was null");
        Assert.assertEquals(expectedContent, scope.getValue(), "Scope value");
        Assert.assertEquals("Regexp attribute value", Boolean.FALSE, scope.getRegexp());
    }

    /** {@inheritDoc} */
    public void testSingleElementOptionalAttributesUnmarshall() {
        Scope scope = (Scope) unmarshallElement(singleElementOptionalAttributesFile);

        Assert.assertNotNull(scope, "Unmarshalled object was null");
        Assert.assertEquals(expectedContent, scope.getValue(), "Scope value");
        Assert.assertEquals(expectedRegexp, scope.getRegexp(), "Regexp attribute value");
    }

    /**
     * Test the proper behavior of the XSBooleanValue attributes.
     */
    public void testXSBooleanAttributes() {
        Scope scope = (Scope) buildXMLObject(Scope.DEFAULT_ELEMENT_NAME);

        // regexp attribute
        scope.setRegexp(Boolean.TRUE);
        Assert.assertEquals(Boolean.TRUE, scope.getRegexp(), "Unexpected value for boolean attribute found");
        Assert.assertNotNull(scope.getRegexpXSBoolean(), "XSBooleanValue was null");
        Assert.assertEquals(new XSBooleanValue(Boolean.TRUE, false), scope.getRegexpXSBoolean(),
                "XSBooleanValue was unexpected value");
        Assert.assertEquals("true", scope.getRegexpXSBoolean().toString(), "XSBooleanValue string was unexpected value");

        scope.setRegexp(Boolean.FALSE);
        Assert.assertEquals(Boolean.FALSE, scope.getRegexp(), "Unexpected value for boolean attribute found");
        Assert.assertNotNull(scope.getRegexpXSBoolean(), "XSBooleanValue was null");
        Assert.assertEquals(new XSBooleanValue(Boolean.FALSE, false), scope.getRegexpXSBoolean(),
                "XSBooleanValue was unexpected value");
        Assert.assertEquals("false", scope.getRegexpXSBoolean().toString(),
                "XSBooleanValue string was unexpected value");

        scope.setRegexp((Boolean) null);
        Assert.assertEquals(Boolean.FALSE, scope.getRegexp(), "Unexpected default value for boolean attribute found");
        Assert.assertNull(scope.getRegexpXSBoolean(), "XSBooleanValue was not null");
    }
}