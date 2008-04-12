/*
 * Copyright 2008 University Corporation for Advanced Internet Development, Inc.
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
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeValue;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.schema.XSAny;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.encoding.AttributeEncodingException;

/** A SAML 2 encoder that uses {@link XMLObject} as the value for attribute values. */
public class SAML2XMLObjectAttributeEncoder extends AbstractSAML2AttributeEncoder {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SAML2XMLObjectAttributeEncoder.class);

    /** Builder of AttributeValue XMLObjects. */
    private final XMLObjectBuilder<XSAny> attributeValueBuilder;

    /** Constructor. */
    public SAML2XMLObjectAttributeEncoder() {
        super();
        attributeValueBuilder = Configuration.getBuilderFactory().getBuilder(XSAny.TYPE_NAME);
    }

    /** {@inheritDoc} */
    public Attribute encode(BaseAttribute attribute) throws AttributeEncodingException {
        Attribute samlAttribute = attributeBuilder.buildObject();
        populateAttribute(samlAttribute);

        XSAny samlAttributeValue;
        for (Object o : attribute.getValues()) {
            if (o == null || !(o instanceof XMLObject)) {
                continue;
            }

            samlAttributeValue = attributeValueBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
            samlAttributeValue.getUnknownXMLObjects().add((XMLObject) o);
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
