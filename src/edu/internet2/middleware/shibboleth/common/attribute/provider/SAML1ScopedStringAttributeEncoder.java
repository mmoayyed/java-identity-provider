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

package edu.internet2.middleware.shibboleth.common.attribute.provider;

import org.apache.log4j.Logger;
import org.opensaml.saml1.core.AttributeValue;
import org.opensaml.saml1.core.impl.AttributeBuilder;
import org.opensaml.xml.XMLObjectBuilder;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.AttributeEncodingException;
import edu.internet2.middleware.shibboleth.common.attribute.SAML1AttributeEncoder;
import edu.internet2.middleware.shibboleth.common.xmlobject.ShibbolethScopedValue;

/**
 * Implementation of SAML 1.X scoped attribute encoder.
 */
public class SAML1ScopedStringAttributeEncoder extends
        AbstractScopedAttributeEncoder<org.opensaml.saml1.core.Attribute> implements SAML1AttributeEncoder {

    /** Log4j logger. */
    private static Logger log = Logger.getLogger(SAML2ScopedStringAttributeEncoder.class);

    /** Attribute factory. */
    private static AttributeBuilder attributeBuilder;

    /** XSString factory. */
    private static XMLObjectBuilder<ShibbolethScopedValue> valueBuilder;

    /** Namespace of attribute. */
    private String namespace;

    /** Constructor. */
    public SAML1ScopedStringAttributeEncoder() {
        attributeBuilder = new AttributeBuilder();
        // valueBuilder = new ShibbolethScopedValueBuilder();
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
    public org.opensaml.saml1.core.Attribute encode(Attribute attribute) throws AttributeEncodingException {

        if (!(attribute instanceof ScopedAttribute)) {
            log.error("This attribute encoder (" + getAttributeName() + ") expects a scoped attribute.");
            throw new AttributeEncodingException("This attribute encoder (" + getAttributeName()
                    + ") expects a scoped attribute.");
        }

        org.opensaml.saml1.core.Attribute samlAttribute;
        samlAttribute = attributeBuilder.buildObject();

        samlAttribute.setAttributeName(getAttributeName());
        samlAttribute.setAttributeNamespace(getNamespace());

        // get attribute values
        for (Object o : attribute.getValues()) {
            String stringValue = o.toString();
            
            ShibbolethScopedValue scopedValue;
            scopedValue = valueBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME,
                    ShibbolethScopedValue.TYPE_NAME);

            // handle "attribute" scopeType
            if ("attribute".equals(getScopeType())) {
                scopedValue.setScopeAttributeName(getScopeAttribute());
                scopedValue.setScope(((ScopedAttribute) attribute).getScope());
            }

            // handle "inline" scopeType
            if ("inline".equals(getScopeType())) {
                stringValue += getScopeDelimiter() + ((ScopedAttribute) attribute).getScope();
            }

            scopedValue.setValue(stringValue);
            samlAttribute.getAttributeValues().add(scopedValue);
        }

        return samlAttribute;
    }

}