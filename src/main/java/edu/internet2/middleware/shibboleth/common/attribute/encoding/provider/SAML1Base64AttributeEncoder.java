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

import org.opensaml.Configuration;
import org.opensaml.saml1.core.Attribute;
import org.opensaml.saml1.core.AttributeValue;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;

/**
 * Implementation of SAML 1.X attribute encoder.
 * 
 * This attribute encoder only operates of {@link BaseAttribute}s with values of type <code>byte[]</code>.
 */
public class SAML1Base64AttributeEncoder extends AbstractSAML1AttributeEncoder {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SAML1Base64AttributeEncoder.class);

    /** XSString factory. */
    private final XMLObjectBuilder<XSString> stringBuilder;

    /** Constructor. */
    public SAML1Base64AttributeEncoder() {
        super();

        stringBuilder = Configuration.getBuilderFactory().getBuilder(XSString.TYPE_NAME);
    }

    /** {@inheritDoc} */
    public Attribute encode(BaseAttribute attribute) {
        Attribute samlAttribute = attributeBuilder.buildObject();
        populateAttribute(samlAttribute);

        byte[] attributeValue;
        XSString samlAttributeValue;
        for (Object o : attribute.getValues()) {
            if (o == null || !(o instanceof byte[])) {
                log.debug("Skipping attribute value because it is either null or not a byte[]");
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