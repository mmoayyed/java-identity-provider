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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;

import edu.internet2.middleware.shibboleth.common.xmlobject.ShibbolethMetadataScope;

/**
 * A thread-safe Unmarshaller for {@link ShibbolethMetadataScope}.
 */
public class ShibbolethMetadataScopeUnmarshaller extends AbstractXMLObjectUnmarshaller {
    
    /** Logger. */
    private final Logger log = LoggerFactory.getLogger(ShibbolethMetadataScopeUnmarshaller.class);

    /** {@inheritDoc} */
    protected void processAttribute(XMLObject xmlObject, Attr attribute) throws UnmarshallingException {
        ShibbolethMetadataScope scope = (ShibbolethMetadataScope) xmlObject;
        
        if (attribute.getLocalName().equals(ShibbolethMetadataScope.REGEXP_ATTRIB_NAME)) {
            scope.setRegexp(Boolean.valueOf(attribute.getValue()));
        } else {
            log.debug("Ignorning unknown attribute {}", attribute.getLocalName());
        }
        
    }

    /** {@inheritDoc} */
    protected void processChildElement(XMLObject parentXMLObject, XMLObject childXMLObject)
            throws UnmarshallingException {
        log.debug("Ignorning unknown child element {}", childXMLObject.getElementQName());
    }

    /** {@inheritDoc} */
    protected void processElementContent(XMLObject xmlObject, String elementContent) {
        ShibbolethMetadataScope scope = (ShibbolethMetadataScope) xmlObject;
        scope.setValue(elementContent);
    }

}