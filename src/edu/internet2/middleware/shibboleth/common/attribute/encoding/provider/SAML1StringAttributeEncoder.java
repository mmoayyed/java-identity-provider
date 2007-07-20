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

package edu.internet2.middleware.shibboleth.common.attribute.encoding.provider;

import org.opensaml.saml1.core.AttributeValue;
import org.opensaml.saml1.core.impl.AttributeBuilder;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.schema.impl.XSStringBuilder;
import org.opensaml.xml.util.DatatypeHelper;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.encoding.SAML1AttributeEncoder;

/**
 * Implementation of SAML 1.X attribute encoder.
 */
public class SAML1StringAttributeEncoder extends AbstractAttributeEncoder<org.opensaml.saml1.core.Attribute> implements
        SAML1AttributeEncoder {

    /** Attribute factory. */
    private static AttributeBuilder attributeBuilder;

    /** XSString factory. */
    private static XSStringBuilder stringBuilder;

    /** Namespace of attribute. */
    private String namespace;

    /** Constructor. */
    public SAML1StringAttributeEncoder() {
        attributeBuilder = new AttributeBuilder();
        stringBuilder = new XSStringBuilder();
    }

    /** {@inheritDoc} */
    public String getNamespace() {
        return namespace;
    }

    /** {@inheritDoc} */
    public void setNamespace(String newNamespace) {
        namespace = newNamespace;
    }

    /** {@inheritDoc} */
    public org.opensaml.saml1.core.Attribute encode(BaseAttribute attribute) {
        org.opensaml.saml1.core.Attribute samlAttribute;
        samlAttribute = attributeBuilder.buildObject();

        samlAttribute.setAttributeName(getAttributeName());
        samlAttribute.setAttributeNamespace(getNamespace());

        String attributeValue;
        XSString samlAttributeValue;
        for (Object o : attribute.getValues()) {
            if (o == null) {
                continue;
            }
            
            attributeValue = o.toString();
            if (!DatatypeHelper.isEmpty(attributeValue)) {
                samlAttributeValue = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
                samlAttributeValue.setValue(attributeValue);
                samlAttribute.getAttributeValues().add(samlAttributeValue);
            }
        }

        return samlAttribute;
    }

}