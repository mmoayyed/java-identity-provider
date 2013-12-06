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
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.saml.attribute.encoding.AbstractSAML2NameIDEncoder;

import org.opensaml.saml.saml2.core.NameID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

/**
 * {@link net.shibboleth.idp.saml.nameid.NameIdentifierAttributeEncoder} that encodes the first {@link NameID}
 * value of an {@link net.shibboleth.idp.attribute.IdPAttribute} to a SAML 2 {@link NameID}.
 */
// TODO this class is redundant.
public class SAML2XMLObjectNameIDEncoder extends AbstractSAML2NameIDEncoder {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SAML2XMLObjectNameIDEncoder.class);

    /** {@inheritDoc} */
    @Nonnull public NameID encode(@Nonnull final IdPAttribute attribute) throws AttributeEncodingException {
        final String attributeId = attribute.getId();

        final Collection<IdPAttributeValue<?>> attributeValues = attribute.getValues();
        if (attributeValues == null || attributeValues.isEmpty()) {
            throw new AttributeEncodingException("Attribute " + attributeId + " contains no value, nothing to encode");
        }

        for (IdPAttributeValue attrValue : attributeValues) {
            if (attrValue == null) {
                log.debug("Skipping null value of attribute {}", attributeId);
                continue;
            }
            Object value = attrValue.getValue();

            if (value instanceof NameID) {
                NameID identifier = (NameID) value;
                log.debug("Chose NameID, with value {}, of attribute {} for Subject NameID encoding",
                        identifier.getValue(), attributeId);
                return identifier;
            } else {
                log.debug("Skipping unsupported value of type {} of attribute {}", value.getClass().getName(),
                        attributeId);
                continue;
            }
        }

        throw new AttributeEncodingException("Attribute " + attributeId
                + " did not contain any NameID values, nothing to encode as Subject NameID");
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof SAML2XMLObjectNameIDEncoder)) {
            return false;
        }

        SAML2XMLObjectNameIDEncoder other = (SAML2XMLObjectNameIDEncoder) obj;

        return Objects.equal(getProtocol(), other.getProtocol());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(getProtocol(), SAML2XMLObjectNameIDEncoder.class);
    }
    
}