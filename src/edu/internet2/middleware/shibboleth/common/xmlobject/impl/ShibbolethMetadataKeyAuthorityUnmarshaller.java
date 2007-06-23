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

import org.apache.log4j.Logger;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.AbstractXMLObjectUnmarshaller;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Attr;

import edu.internet2.middleware.shibboleth.common.xmlobject.ShibbolethMetadataKeyAuthority;

/**
 * A thread-safe Unmarshaller for {@link ShibbolethMetadataKeyAuthority}.
 */
public class ShibbolethMetadataKeyAuthorityUnmarshaller extends AbstractXMLObjectUnmarshaller {
    
    /** Logger. */
    private static Logger log = Logger.getLogger(ShibbolethMetadataKeyAuthorityUnmarshaller.class);

    /** {@inheritDoc} */
    protected void processAttribute(XMLObject xmlObject, Attr attribute) throws UnmarshallingException {
        ShibbolethMetadataKeyAuthority authority = (ShibbolethMetadataKeyAuthority) xmlObject;
        
        if (attribute.getLocalName().equals(ShibbolethMetadataKeyAuthority.VERIFY_DEPTH_ATTRIB_NAME)) {
            authority.setVerifyDepth(Integer.valueOf(attribute.getValue()));
        } else {
            QName attribQName = XMLHelper.getNodeQName(attribute);
            if (attribute.isId()) {
               authority.getUnknownAttributes().registerID(attribQName);
            }
            authority.getUnknownAttributes().put(attribQName, attribute.getValue());
        }

    }

    /** {@inheritDoc} */
    protected void processChildElement(XMLObject parentXMLObject, XMLObject childXMLObject)
            throws UnmarshallingException {
        ShibbolethMetadataKeyAuthority authority = (ShibbolethMetadataKeyAuthority) parentXMLObject;
        
        if (childXMLObject instanceof KeyInfo) {
            authority.getKeyInfos().add((KeyInfo) childXMLObject);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Ignorning unknown child element " + childXMLObject.getElementQName().toString());
            }
        }

    }

    /** {@inheritDoc} */
    protected void processElementContent(XMLObject xmlObject, String elementContent) {
        if (log.isDebugEnabled()) {
            log.debug("Ignorning unsupported element text content");
        }
    }

}
