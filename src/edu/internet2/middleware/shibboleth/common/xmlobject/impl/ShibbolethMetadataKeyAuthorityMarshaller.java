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

import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.opensaml.xml.Configuration;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.AbstractXMLObjectMarshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.ShibbolethConstants;
import edu.internet2.middleware.shibboleth.common.xmlobject.ShibbolethMetadataKeyAuthority;

/**
 * A thread-safe Marshaller for {@link ShibbolethMetadataKeyAuthority}.
 */
public class ShibbolethMetadataKeyAuthorityMarshaller extends AbstractXMLObjectMarshaller {
    
    /** Constructor. */
    public ShibbolethMetadataKeyAuthorityMarshaller() {
        super(ShibbolethConstants.SHIB_MDEXT10_NS, ShibbolethMetadataKeyAuthority.DEFAULT_ELEMENT_LOCAL_NAME);
    }

    /**
     * Constructor.
     * 
     * @param namespaceURI the namespace URI
     * @param elementLocalName the element local name
     */
    protected ShibbolethMetadataKeyAuthorityMarshaller(String namespaceURI, String elementLocalName) {
        super(namespaceURI, elementLocalName);
    }

    /** {@inheritDoc} */
    protected void marshallAttributes(XMLObject xmlObject, Element domElement) throws MarshallingException {
        ShibbolethMetadataKeyAuthority keyAuthority = (ShibbolethMetadataKeyAuthority) xmlObject;
        
        if (keyAuthority.getVerifyDepth() != null) {
            domElement.setAttributeNS(null, ShibbolethMetadataKeyAuthority.VERIFY_DEPTH_ATTRIB_NAME, 
                    keyAuthority.getVerifyDepth().toString());
        }
        
        Attr attr;
        for(Entry<QName, String> entry: keyAuthority.getUnknownAttributes().entrySet()){
            attr = XMLHelper.constructAttribute(domElement.getOwnerDocument(), entry.getKey());
            attr.setValue(entry.getValue());
            domElement.setAttributeNodeNS(attr);
            if (Configuration.isIDAttribute(entry.getKey()) 
                    || keyAuthority.getUnknownAttributes().isIDAttribute(entry.getKey())) {
                attr.getOwnerElement().setIdAttributeNode(attr, true);
            }
        }

    }

    /** {@inheritDoc} */
    protected void marshallElementContent(XMLObject xmlObject, Element domElement) throws MarshallingException {
        // nothing to implement
    }


}
