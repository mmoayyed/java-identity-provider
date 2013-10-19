/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.saml.impl.attribute.encoding;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.saml.attribute.encoding.AbstractSaml2NameIDEncoder;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml2.core.NameID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

/**
 * {@link net.shibboleth.idp.attribute.AttributeEncoder} that produces the SAML 1 NameIdentifier used for the Subject
 * from the first non-null {@link NameID} value of an {@link net.shibboleth.idp.attribute.IdPAttribute}.
 */
public class Saml2StringSubjectNameIDEncoder extends AbstractSaml2NameIDEncoder {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(Saml2StringSubjectNameIDEncoder.class);

    /** Identifier builder. */
    private final SAMLObjectBuilder<NameID> identifierBuilder;

    /** The format of the name identifier. */
    private String format;

    /** The security or administrative domain that qualifies the name identifier. */
    private String qualifier;

    /** Constructor. */
    public Saml2StringSubjectNameIDEncoder() {
        identifierBuilder =
                (SAMLObjectBuilder<NameID>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                        NameID.DEFAULT_ELEMENT_NAME);
    }

    /**
     * Gets the format of the name identifier.
     * 
     * @return format of the name identifier
     */
    @Nullable public final String getNameFormat() {
        return format;
    }

    /**
     * Sets the format of the name identifier.
     * 
     * @param nameFormat format of the name identifier
     */
    public final void setNameFormat(@Nullable final String nameFormat) {
        format = StringSupport.trimOrNull(nameFormat);
    }

    /**
     * Gets the security or administrative domain that qualifies the name identifier.
     * 
     * @return security or administrative domain that qualifies the name identifier
     */
    @Nullable public final String getNameQualifier() {
        return qualifier;
    }

    /**
     * Sets the security or administrative domain that qualifies the name identifier.
     * 
     * @param nameQualifier security or administrative domain that qualifies the name identifier
     */
    @Nullable public final void setNameQualifier(final String nameQualifier) {
        qualifier = StringSupport.trimOrNull(nameQualifier);
    }

    /** {@inheritDoc} */
    @Nonnull public NameID encode(IdPAttribute attribute) throws AttributeEncodingException {
        final String attributeId = attribute.getId();

        final Collection<AttributeValue> attributeValues = attribute.getValues();
        if (attributeValues == null || attributeValues.isEmpty()) {
            throw new AttributeEncodingException("Attribute " + attribute.getId()
                    + " does not contain any values to encode");
        }

        NameID nameId = identifierBuilder.buildObject();

        if (format != null) {
            nameId.setFormat(format);
        }

        if (qualifier != null) {
            nameId.setNameQualifier(qualifier);
        }

        for (AttributeValue attrValue : attributeValues) {
            if (attrValue == null || attrValue.getValue() == null) {
                // Should not be null, but check anyway
                log.debug("Skipping null value of attribute {}", attributeId);
                continue;
            }
            Object value = attrValue.getValue();

            if (value instanceof String) {
                String valueAsString = (String) value;

                nameId.setValue(valueAsString);

                return nameId;
            } else {
                log.debug("Skipping value of type {} of attribute {}", value.getClass().getName(), attributeId);
                continue;
            }
        }
        throw new AttributeEncodingException("Attribute '" + attributeId + "' did not contain any encodable values");
    }
    
    /** {@inheritDoc} */
    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof Saml2StringSubjectNameIDEncoder)) {
            return false;
        }

        Saml2StringSubjectNameIDEncoder other = (Saml2StringSubjectNameIDEncoder) obj;

        return Objects.equal(getNameFormat(), other.getNameFormat())
                && Objects.equal(getNameQualifier(), other.getNameQualifier());
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return Objects.hashCode(getNameFormat(), getNameQualifier(), getProtocol(),
                Saml2StringSubjectNameIDEncoder.class);
    }
}