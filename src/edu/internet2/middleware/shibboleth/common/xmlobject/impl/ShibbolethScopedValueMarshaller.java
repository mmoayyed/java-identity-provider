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
import org.opensaml.xml.io.AbstractXMLObjectMarshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.xmlobject.ShibbolethScopedValue;

/**
 * Thread-safe marshaller of {@link ShibbolethScopedValue} objects.
 */
public class ShibbolethScopedValueMarshaller extends AbstractXMLObjectMarshaller {

    /** {@inheritDoc} */
    protected void marshallAttributes(XMLObject xmlObject, Element domElement) throws MarshallingException {
        ShibbolethScopedValue scopedValue = (ShibbolethScopedValue) xmlObject;

        if (null != scopedValue.getScopeAttributeName()) {
            domElement.setAttribute(scopedValue.getScopeAttributeName(), scopedValue.getScope());
        }

    }

    /** {@inheritDoc} */
    protected void marshallElementContent(XMLObject xmlObject, Element domElement) throws MarshallingException {
        ShibbolethScopedValue scopedValue = (ShibbolethScopedValue) xmlObject;

        XMLHelper.appendTextContent(domElement, scopedValue.getValue());
    }
}