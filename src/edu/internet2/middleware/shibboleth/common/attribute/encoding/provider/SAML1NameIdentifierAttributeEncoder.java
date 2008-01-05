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
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.saml1.core.Attribute;
import org.opensaml.saml1.core.AttributeValue;
import org.opensaml.saml1.core.NameIdentifier;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.schema.XSAny;
import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.encoding.AttributeEncodingException;

/**
 * A SAML 1 attribute encoder that encodes an attribute's values as SAML 1 NameIdentifiers.
 */
public class SAML1NameIdentifierAttributeEncoder extends AbstractSAML1AttributeEncoder {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SAML1NameIdentifierAttributeEncoder.class);

    /** Builder of AttributeValue XMLObjects. */
    private final XMLObjectBuilder<XSAny> attributeValueBuilder;
    
    /** Builder of NameIdentifier XMLObjects. */
    private final SAMLObjectBuilder<NameIdentifier> nameIdBuilder;

    /** Format of the name identifier. */
    private String nameIdFormat;

    /** Name qualifier for the name identifier. */
    private String nameIdQualifier;

    /** Constructor. */
    public SAML1NameIdentifierAttributeEncoder() {
        super();
        attributeValueBuilder = Configuration.getBuilderFactory().getBuilder(XSAny.TYPE_NAME);
        nameIdBuilder = (SAMLObjectBuilder<NameIdentifier>) Configuration.getBuilderFactory().getBuilder(
                NameIdentifier.DEFAULT_ELEMENT_NAME);
    }

    /**
     * Gets the format for the NameID used as an attribute value.
     * 
     * @return Returns the nameIdFormat.
     */
    public String getNameIdFormat() {
        return nameIdFormat;
    }

    /**
     * Sets the format for the NameID used as an attribute value.
     * 
     * @param format format for the NameID used as an attribute value
     */
    public void setNameIdFormat(String format) {
        this.nameIdFormat = format;
    }

    /**
     * Gets the NameID qualifier for the NameID used as an attribute value.
     * 
     * @return NameID qualifier for the NameID used as an attribute value
     */
    public String getNameIdQualifier() {
        return nameIdQualifier;
    }

    /**
     * Sets the NameID qualifier for the NameID used as an attribute value.
     * 
     * @param qualifier NameID qualifier for the NameID used as an attribute value
     */
    public void setNameIdQualifier(String qualifier) {
        nameIdQualifier = qualifier;
    }

    /** {@inheritDoc} */
    public Attribute encode(BaseAttribute attribute) throws AttributeEncodingException {
        Attribute samlAttribute = attributeBuilder.buildObject();
        populateAttribute(samlAttribute);

        String attributeValue;
        XSAny samlAttributeValue;
        NameIdentifier nameIdValue;
        for (Object o : attribute.getValues()) {
            if (o == null) {
                continue;
            }

            attributeValue = o.toString();
            if (!(DatatypeHelper.isEmpty(attributeValue))) {
                nameIdValue = nameIdBuilder.buildObject();
                nameIdValue.setNameIdentifier(attributeValue);
                nameIdValue.setFormat(getNameIdFormat());
                nameIdValue.setNameQualifier(getNameIdQualifier());
                
                samlAttributeValue = attributeValueBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
                samlAttributeValue.getUnknownXMLObjects().add(nameIdValue);
                
                samlAttribute.getAttributeValues().add(samlAttributeValue);
            }
        }

        List<XMLObject> attributeValues = samlAttribute.getAttributeValues();
        if (attributeValues == null || attributeValues.isEmpty()) {
            log.debug("Unable to encode {} attribute.  It does not contain any values", attribute.getId());
            return null;
        }

        return samlAttribute;
    }
}