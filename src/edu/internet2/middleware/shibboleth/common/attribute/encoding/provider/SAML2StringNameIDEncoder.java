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

import org.opensaml.Configuration;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.saml2.core.NameID;
import org.opensaml.xml.util.DatatypeHelper;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.encoding.AttributeEncodingException;
import edu.internet2.middleware.shibboleth.common.attribute.encoding.SAML2NameIDAttributeEncoder;

/**
 * An attribute encoder that takes the first value of an attribute and creates a {@link NameID} of it.  Attribute
 * values are turned into the values for the NameID by invoking the values {@link Object#toString()} method.
 */
public class SAML2StringNameIDEncoder extends AbstractAttributeEncoder<NameID> implements SAML2NameIDAttributeEncoder {

    /** NameID builder object. */
    private SAMLObjectBuilder<NameID> nameIdBuilder;

    /** Format of the NameID. */
    private String nameFormat;

    /** Name qualifier for the NameID. */
    private String nameQualifier;

    /** Constructor. */
    public SAML2StringNameIDEncoder() {
        nameIdBuilder = (SAMLObjectBuilder<NameID>) Configuration.getBuilderFactory().getBuilder(
                NameID.DEFAULT_ELEMENT_NAME);
    }

    /** {@inheritDoc} */
    public NameID encode(BaseAttribute attribute) throws AttributeEncodingException {
        NameID nameId = nameIdBuilder.buildObject();

        if (attribute.getValues() != null || attribute.getValues().size() < 1) {
            throw new AttributeEncodingException(attribute.getId() 
                    + " attribute does not contain any values to encode");
        }
        nameId.setValue(attribute.getValues().first().toString());

        if (nameFormat != null) {
            nameId.setFormat(nameFormat);
        }

        if (nameQualifier != null) {
            nameId.setNameQualifier(nameQualifier);
        }

        return nameId;
    }

    /** {@inheritDoc} */
    public String getNameFormat() {
        return nameFormat;
    }

    /** {@inheritDoc} */
    public void setNameFormat(String format) {
        nameFormat = DatatypeHelper.safeTrimOrNullString(format);
    }

    /** {@inheritDoc} */
    public String getNameQualifier() {
        return nameQualifier;
    }

    /** {@inheritDoc} */
    public void setNameQualifier(String qualifier) {
        nameQualifier = DatatypeHelper.safeTrimOrNullString(qualifier);
    }
}