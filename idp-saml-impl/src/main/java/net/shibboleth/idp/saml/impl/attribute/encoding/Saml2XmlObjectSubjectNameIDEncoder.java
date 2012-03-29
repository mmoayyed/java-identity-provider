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

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.saml.attribute.encoding.AbstractSaml2NameIdentifierEncoder;

import org.opensaml.saml.saml2.core.NameID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link net.shibboleth.idp.attribute.AttributeEncoder} that produces the SAML 2 NameID used for the Subject from the
 * first non-null {@link NameID} value of an {@link net.shibboleth.idp.attribute.Attribute}.
 */
public class Saml2XmlObjectSubjectNameIDEncoder extends AbstractSaml2NameIdentifierEncoder {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(Saml2XmlObjectSubjectNameIDEncoder.class);

    /** {@inheritDoc} */
    public NameID encode(Attribute attribute) throws AttributeEncodingException {
        final String attributeId = attribute.getId();

        final Collection<?> attributeValues = attribute.getValues();
        if (attributeValues == null || attributeValues.isEmpty()) {
            log.debug("Attribute {} contains no value, nothing to encode", attributeId);
            return null;
        }

        for (Object value : attributeValues) {
            if (value == null) {
                log.debug("Skipping null value of attribute {}", attributeId);
                continue;
            }

            if (value instanceof NameID) {
                NameID identifier = (NameID) value;
                log.debug("Chose NameID, with value {}, of attribute {} for subject name identifier encoding",
                        identifier.getValue(), attributeId);
                return identifier;
            } else {
                log.debug("Skipping value of type {} of attribute {}", value.getClass().getName(), attributeId);
                continue;
            }
        }

        log.debug("Attribute {} did not contain any NameID values, nothing to encode as subject name identifier",
                attributeId);
        return null;
    }
}