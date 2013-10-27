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

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.saml.attribute.encoding.AbstractSaml1NameIdentifierEncoder;

import org.opensaml.saml.saml1.core.NameIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

/**
 * {@link net.shibboleth.idp.attribute.AttributeEncoder} that produces the SAML 1 NameIdentifier used for the Subject
 * from the first non-null {@link NameIdentifier} value of an {@link net.shibboleth.idp.attribute.IdPAttribute}.
 */
// TODO Is this class redundant?
public class Saml1XmlObjectSubjectNameIdentifierEncoder extends AbstractSaml1NameIdentifierEncoder {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(Saml1XmlObjectSubjectNameIdentifierEncoder.class);

    /** {@inheritDoc} */
    @Nonnull public NameIdentifier encode(IdPAttribute attribute) throws AttributeEncodingException {
        final String attributeId = attribute.getId();

        final Collection<AttributeValue<?>> attributeValues = attribute.getValues();
        if (attributeValues == null || attributeValues.isEmpty()) {
            throw new AttributeEncodingException("Attribute " + attributeId + " contains no value, nothing to encode");
        }

        for (AttributeValue attrValue : attributeValues) {
            if (attrValue == null) {
                // Should not be null, but check anyway
                log.debug("Skipping null value of attribute {}", attributeId);
                continue;
            }
            Object value = attrValue.getValue();

            if (value instanceof NameIdentifier) {
                NameIdentifier identifier = (NameIdentifier) value;
                log.debug("Chose NameIdentifier, with value {}, of attribute {} for subject name identifier encoding",
                        identifier.getNameIdentifier(), attributeId);
                return identifier;
            } else {
                log.debug("Skipping value of type {} of attribute {}", value.getClass().getName(), attributeId);
                continue;
            }
        }

        throw new AttributeEncodingException("Attribute " + attributeId
                + " did not contain any NameIdentifier values, nothing to encode as subject name identifier");
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof Saml1XmlObjectSubjectNameIdentifierEncoder)) {
            return false;
        }

        Saml1XmlObjectSubjectNameIdentifierEncoder other = (Saml1XmlObjectSubjectNameIdentifierEncoder) obj;

        return Objects.equal(getProtocol(), other.getProtocol());
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return Objects.hashCode(getProtocol(), Saml1XmlObjectSubjectNameIdentifierEncoder.class);
    }
}