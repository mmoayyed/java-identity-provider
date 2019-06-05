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

package net.shibboleth.idp.attribute.resolver.spring.enc;

import java.util.Map;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.saml.attribute.transcoding.SAML2AttributeTranscoder;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Base class for Spring bean definition parser for SAML 1 attribute encoders.
 */
public abstract class BaseSAML2AttributeEncoderParser extends BaseAttributeEncoderParser {
    
    /** {@inheritDoc} */
    @Override
    protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final Map<String,Object> rule) {

        if (config.hasAttributeNS(null, "name")) {
            rule.put(SAML2AttributeTranscoder.PROP_NAME,
                    StringSupport.trimOrNull(config.getAttributeNS(null, "name")));
        } else {
            throw new BeanDefinitionStoreException("Missing 'name' attribute, cannot create transcoder rule");
        }

        if (config.hasAttributeNS(null, "nameFormat")) {
            rule.put(SAML2AttributeTranscoder.PROP_NAME_FORMAT,
                    StringSupport.trimOrNull(config.getAttributeNS(null, "nameFormat")));
        }
        
        if (config.hasAttributeNS(null, "friendlyName")) {
            rule.put(SAML2AttributeTranscoder.PROP_FRIENDLY_NAME,
                    StringSupport.trimOrNull(config.getAttributeNS(null, "friendlyName")));
        }
        
        final String value = StringSupport.trimOrNull(config.getAttributeNS(null, "encodeType"));
        if (value != null) {
            rule.put(SAML2AttributeTranscoder.PROP_ENCODE_TYPE,SpringSupport.getStringValueAsBoolean(value));
        }
    }
}