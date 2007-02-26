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

import org.opensaml.saml2.core.AttributeValue;
import org.opensaml.saml2.core.impl.AttributeBuilder;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.schema.impl.XSStringBuilder;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.SAML2AttributeEncoder;

/**
 * Implementation of SAML 2.0 attribute encoder.
 */
public class SAML2AttributeEncoderImpl extends AbstractAttributeEncoder implements SAML2AttributeEncoder<String> {

    /** Attribute factory. */
    private static AttributeBuilder attributeBuilder;
    
    /** XSString factory. */
    private static XSStringBuilder stringBuilder;
    
    /** Format of attribute. */
    private String format;

    /** Friendly name of attribute. */
    private String friendlyName;
    
    /** Constructor. */
    public SAML2AttributeEncoderImpl() {
        attributeBuilder = new AttributeBuilder();
        stringBuilder = new XSStringBuilder();
        setEncoderCategory(SAML2AttributeEncoder.CATEGORY);
    }
    
    /** {@inheritDoc} */
    public String getAttributeFormat() {
        return format;
    }

    /** {@inheritDoc} */
    public String getFriendlyName() {
        return friendlyName;
    }

    /** {@inheritDoc} */
    public void setAttributeFormat(String newFormat) {
        format = newFormat;
    }

    /** {@inheritDoc} */
    public void setFriendlyName(String name) {
        friendlyName = name;
    }

    /** {@inheritDoc} */
    public XMLObject encode(Attribute attribute) {
        org.opensaml.saml2.core.Attribute samlAttribute;
        samlAttribute = attributeBuilder.buildObject();
        
        samlAttribute.setName(getAttributeName());
        samlAttribute.setNameFormat(getAttributeFormat());
        samlAttribute.setFriendlyName(getFriendlyName());
        
        for(Object o: attribute.getValues()) {
            XSString xsstring = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
            xsstring.setValue(o.toString());
            samlAttribute.getAttributeValues().add(xsstring);
        }
        
        // TODO support scoped attributes
        
        return samlAttribute;
    }

}