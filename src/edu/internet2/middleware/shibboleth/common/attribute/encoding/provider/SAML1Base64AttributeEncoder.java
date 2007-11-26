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

import java.util.List;

import org.opensaml.saml1.core.AttributeValue;
import org.opensaml.saml1.core.impl.AttributeBuilder;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.schema.impl.XSStringBuilder;
import org.opensaml.xml.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.encoding.SAML1AttributeEncoder;

/**
 * Implementation of SAML 1.X attribute encoder.
 * 
 * This attribute encoder only operates of {@link BaseAttribute}s with values of type <code>byte[]</code>.
 */
public class SAML1Base64AttributeEncoder extends AbstractAttributeEncoder<org.opensaml.saml1.core.Attribute> implements
        SAML1AttributeEncoder {

    /** Attribute factory. */
    private static AttributeBuilder attributeBuilder;

    /** XSString factory. */
    private static XSStringBuilder stringBuilder;

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SAML1Base64AttributeEncoder.class);

    /** Namespace of attribute. */
    private String namespace;

    /** Constructor. */
    public SAML1Base64AttributeEncoder() {
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

        byte[] attributeValue;
        XSString samlAttributeValue;
        for (Object o : attribute.getValues()) {
            if (o == null || !(o instanceof byte[])) {
                continue;
            }
            attributeValue = (byte[]) o;
            samlAttributeValue = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
            samlAttributeValue.setValue(Base64.encodeBytes(attributeValue));
            samlAttribute.getAttributeValues().add(samlAttributeValue);
        }

        List<XMLObject> attributeValues = samlAttribute.getAttributeValues();
        if (attributeValues == null || attributeValues.isEmpty()) {
            log.debug("Unable to encode {} attribute.  It does not contain any values", attribute.getId());
            return null;
        }

        return samlAttribute;
    }

}