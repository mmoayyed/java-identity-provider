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

package net.shibboleth.idp.saml.profile.config;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSBase64Binary;
import org.opensaml.core.xml.schema.XSBoolean;
import org.opensaml.core.xml.schema.XSBooleanValue;
import org.opensaml.core.xml.schema.XSDateTime;
import org.opensaml.core.xml.schema.XSInteger;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.schema.XSURI;
import org.opensaml.saml.saml2.core.Attribute;
import org.slf4j.Logger;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * A strategy function that examines SAML metadata associated with a relying party and derives String-valued
 * configuration settings based on EntityAttribute extension tags.
 * 
 * @since 3.4.0
 */
public class StringConfigurationLookupStrategy extends AbstractMetadataDrivenConfigurationLookupStrategy<String> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(StringConfigurationLookupStrategy.class);

    /** {@inheritDoc} */
    @Override
    @Nullable protected String doTranslate(@Nonnull final IdPAttribute tag) {
        
        final List<IdPAttributeValue> values = tag.getValues();
        if (values.size() != 1) {
            log.error("Tag '{}' contained multiple values, returning none", tag.getId());
            return null;
        }

        log.debug("Converting tag '{}' to String property", tag.getId());
        
        final IdPAttributeValue value = values.get(0);
        if (value instanceof StringAttributeValue) {
            return ((StringAttributeValue) value).getValue();
        }
        log.error("Tag '{}' contained non-string value, returning null");
        return null;
    }
    
    /** {@inheritDoc} */
    @Override
    @Nullable protected String doTranslate(@Nonnull final Attribute tag) {
        
        final List<XMLObject> values = tag.getAttributeValues();
        if (values.size() != 1) {
            log.error("Tag '{}' contained multiple values, returning none", tag.getName());
            return null;
        }
        
        log.debug("Converting tag '{}' to String property", tag.getName());
        return xmlObjectToString(values.get(0));
    }
    
// Checkstyle: CyclomaticComplexity OFF
    /**
     * Convert an XMLObject to a String if the type is supported.
     * 
     * @param object object to convert
     * 
     * @return the converted value, or null
     */
    @Nullable private String xmlObjectToString(@Nonnull final XMLObject object) {
        if (object instanceof XSString) {
            return ((XSString) object).getValue();
        } else if (object instanceof XSURI) {
            return ((XSURI) object).getURI();
        } else if (object instanceof XSBoolean) {
            final XSBooleanValue value = ((XSBoolean) object).getValue();
            return value != null ? (value.getValue() ? "1" : "0") : null;
        } else if (object instanceof XSInteger) {
            final Integer value = ((XSInteger) object).getValue();
            return value != null ? value.toString() : null;
        } else if (object instanceof XSDateTime) {
            final Instant dt = ((XSDateTime) object).getValue();
            return dt != null ? Long.toString(dt.toEpochMilli()) : null;
        } else if (object instanceof XSBase64Binary) {
            return ((XSBase64Binary) object).getValue();
        } else if (object instanceof XSAny) {
            final XSAny wc = (XSAny) object;
            if (wc.getUnknownAttributes().isEmpty() && wc.getUnknownXMLObjects().isEmpty()) {
                return wc.getTextContent();
            }
        }
        
        log.error("Unsupported conversion to String from XMLObject type ({})", object.getClass().getName());
        return null;
    }
// Checkstyle: CyclomaticComplexity ON

    
}