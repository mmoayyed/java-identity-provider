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

package edu.internet2.middleware.shibboleth.common.xmlobject;

import javax.xml.namespace.QName;

import org.opensaml.Configuration;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilder;
import org.w3c.dom.Document;

import edu.internet2.middleware.shibboleth.common.TestCaseBase;

/**
 * Base abstract class for testing object providers.
 */
public abstract class BaseShibObjectProviderTestCase extends TestCaseBase {

    /** Base path for data files. */
    public static final String DATA_PATH = "/data/edu/internet2/middleware/shibboleth/common/xmlobject";

    /** Location of file containing a single element with NO optional attributes. */
    protected String singleElementFile;

    /** Location of file containing a single element with all optional attributes. */
    protected String singleElementOptionalAttributesFile;

    /** Location of file containing a single element with child elements. */
    protected String childElementsFile;

    /** The expected result of a marshalled single element with no optional attributes. */
    protected Document expectedDOM;

    /** The expected result of a marshalled single element with all optional attributes. */
    protected Document expectedOptionalAttributesDOM;

    /** The expected result of a marshalled single element with child elements. */
    protected Document expectedChildElementsDOM;

    /**
     * Builds the requested XMLObject.
     * 
     * @param objectQName name of the XMLObject
     * 
     * @return the build XMLObject
     */
    public XMLObject buildXMLObject(QName objectQName) {
        XMLObjectBuilder builder = Configuration.getBuilderFactory().getBuilder(objectQName);
        if (builder == null) {
            fail("Unable to retrieve builder for object QName " + objectQName);
        }
        return builder.buildObject(objectQName.getNamespaceURI(), objectQName.getLocalPart(), objectQName.getPrefix());
    }

    /** {@inheritDoc} */
    protected void setUp() throws Exception {
        super.setUp();

        if (singleElementFile != null) {
            expectedDOM = parser.parse(BaseShibObjectProviderTestCase.class.getResourceAsStream(singleElementFile));
        }

        if (singleElementOptionalAttributesFile != null) {
            expectedOptionalAttributesDOM = parser.parse(BaseShibObjectProviderTestCase.class
                    .getResourceAsStream(singleElementOptionalAttributesFile));
        }

        if (childElementsFile != null) {
            expectedChildElementsDOM = parser.parse(BaseShibObjectProviderTestCase.class
                    .getResourceAsStream(childElementsFile));
        }
    }

    /**
     * Tests marshalling the contents of a single element with child elements to a DOM document.
     */
    public void testChildElementsMarshall() {
        assertNull("No testSingleElementChildElementsMarshall", expectedChildElementsDOM);
    }

    /**
     * Tests unmarshalling a document that contains a single element with children.
     */
    public void testChildElementsUnmarshall() {
        assertNull("No testSingleElementChildElementsUnmarshall present", childElementsFile);
    }

    /**
     * Tests marshalling the contents of a single element, with no optional attributes, to a DOM document.
     */
    public abstract void testSingleElementMarshall();

    /**
     * Tests marshalling the contents of a single element, with all optional attributes, to a DOM document.
     */
    public void testSingleElementOptionalAttributesMarshall() {
        assertNull("No testSingleElementOptionalAttributesMarshall", expectedOptionalAttributesDOM);
    }

    /**
     * Tests unmarshalling a document that contains a single element (no children) with all that element's optional
     * attributes.
     */
    public void testSingleElementOptionalAttributesUnmarshall() {
        assertNull("No testSingleElementOptionalAttributesUnmarshall present", singleElementOptionalAttributesFile);
    }

    /**
     * Tests unmarshalling a document that contains a single element (no children) with no optional attributes.
     */
    public abstract void testSingleElementUnmarshall();
}
