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

import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.AbstractXMLObjectUnmarshaller;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.util.DatatypeHelper;
import org.w3c.dom.Attr;

import edu.internet2.middleware.shibboleth.common.xmlobject.ShibbolethScopedValue;

/**
 * Thread-safe unmarshaller for {@link org.opensaml.xml.schema.XSString} objects.
 */
public class ShibbolethScopedValueUnmarshaller extends AbstractXMLObjectUnmarshaller {

    /** {@inheritDoc} */
    protected void processChildElement(XMLObject parentXMLObject, XMLObject childXMLObject)
            throws UnmarshallingException {
        // no children
    }

    /** {@inheritDoc} */
    protected void processAttribute(XMLObject xmlObject, Attr attribute) throws UnmarshallingException {
        ShibbolethScopedValue sv = (ShibbolethScopedValue) xmlObject;

        if (DatatypeHelper.isEmpty(sv.getScopeAttributeName())) {
            sv.setScopeAttributeName(attribute.getName());
            sv.setScope(attribute.getValue());
        }

    }

    /** {@inheritDoc} */
    protected void processElementContent(XMLObject xmlObject, String elementContent) {
        XSString xsiString = (XSString) xmlObject;

        xsiString.setValue(elementContent);
    }
}