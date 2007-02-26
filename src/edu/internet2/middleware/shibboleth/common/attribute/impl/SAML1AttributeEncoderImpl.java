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
package edu.internet2.middleware.shibboleth.common.attribute.impl;

import org.opensaml.saml1.core.impl.AttributeBuilder;
import org.opensaml.saml1.core.AttributeValue;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.schema.impl.XSStringBuilder;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.SAML1AttributeEncoder;

/**
 * Implementation of SAML 1.X attribute encoder.
 */
public class SAML1AttributeEncoderImpl extends AbstractAttributeEncoder implements SAML1AttributeEncoder<String> {
    
    /** Attribute factory. */
    private static AttributeBuilder attributeBuilder;
    
    /** XSString factory. */
    private static XSStringBuilder stringBuilder;

    /** Namespace of attribute. */
    private String namespace;
    
    /** Constructor. */
    public SAML1AttributeEncoderImpl() {
        attributeBuilder = new AttributeBuilder();
        stringBuilder = new XSStringBuilder();
    }
    
    /** {@inheritDoc} */
    public String getAttributeNamespace() {
        return namespace;
    }

    /** {@inheritDoc} */
    public void setAttributeNamespace(String newNamespace) {
        namespace = newNamespace;
    }

    /** {@inheritDoc} */
    public XMLObject encode(Attribute attribute) {
        org.opensaml.saml1.core.Attribute samlAttribute;
        samlAttribute = attributeBuilder.buildObject();
        
        for(Object o: attribute.getValues()) {
            XSString xsstring = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
            xsstring.setValue(o.toString());
            samlAttribute.getAttributeValues().add(xsstring);
        }
        
        // TODO support scoped attributes
        
        return samlAttribute;
    }

}