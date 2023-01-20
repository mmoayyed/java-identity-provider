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

import java.time.Duration;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSInteger;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.Attribute;
import org.slf4j.Logger;

import org.springframework.core.convert.converter.Converter;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.spring.config.StringToDurationConverter;

/**
 * A strategy function that examines SAML metadata associated with a relying party and derives Long-valued
 * configuration settings that are durations, based on EntityAttribute extension tags.
 * 
 * @since 3.4.0
 */
public class DurationConfigurationLookupStrategy extends AbstractMetadataDrivenConfigurationLookupStrategy<Duration> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(DurationConfigurationLookupStrategy.class);

    /** Converter to handle duration strings. */
    @Nonnull private final Converter<String,Duration> durationConverter;

    /** Constructor. */
    public DurationConfigurationLookupStrategy() {
        durationConverter = new StringToDurationConverter();
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected Duration doTranslate(@Nonnull final IdPAttribute tag) {
        
        final List<IdPAttributeValue> values = tag.getValues();
        if (values.size() != 1) {
            log.error("Tag '{}' contained multiple values, returning none", tag.getId());
            return null;
        }

        log.debug("Converting tag '{}' to Duration property", tag.getId());
        
        final IdPAttributeValue value = values.get(0);
        if (value instanceof StringAttributeValue) {
            try {
                return durationConverter.convert(((StringAttributeValue) value).getValue());
            } catch (final IllegalArgumentException e) {
                log.error("Error converting duration", e);
                return null;
            }
        }
        log.error("Tag '{}' contained non-string value, returning null");
        return null;
    }
    
    /** {@inheritDoc} */
    @Override
    @Nullable protected Duration doTranslate(@Nonnull final Attribute tag) {
        
        final List<XMLObject> values = tag.getAttributeValues();
        if (values.size() != 1) {
            log.error("Tag '{}' contained multiple values, returning none", tag.getName());
            return null;
        }
        
        log.debug("Converting tag '{}' to Duration property", tag.getName());
        return xmlObjectToDuration(values.get(0));
    }
    
// Checkstyle: CyclomaticComplexity OFF
    /**
     * Convert an XMLObject to a Long based on a duration if the type is supported.
     * 
     * @param object object to convert
     * 
     * @return the converted value, or null
     */
    @Nullable private Duration xmlObjectToDuration(@Nonnull final XMLObject object) {
        if (object instanceof XSString) {
            final String value = ((XSString) object).getValue();
            if (value != null) {
                try {
                    return durationConverter.convert(value);
                } catch (final IllegalArgumentException e) {
                    log.error("Error converting duration", e);
                    return null;
                }
            }
            return null;
        } else if (object instanceof XSInteger) {
            final Integer value = ((XSInteger) object).getValue();
            return value != null ? Duration.ofMillis(value.longValue()) : null;
        } else if (object instanceof XSAny) {
            final XSAny wc = (XSAny) object;
            if (wc.getUnknownAttributes().isEmpty() && wc.getUnknownXMLObjects().isEmpty()) {
                final String value = wc.getTextContent();
                if (value != null) {
                    try {
                        return durationConverter.convert(value);
                    } catch (final IllegalArgumentException e) {
                        log.error("Error converting duration", e);
                        return null;
                    }
                }
                return null;
            }
        }
        
        log.error("Unsupported conversion to Duration from XMLObject type ({})", object.getClass().getName());
        return null;
    }
// Checkstyle: CyclomaticComplexity ON
    
}