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

package net.shibboleth.idp.attribute.resolver.spring.enc.impl;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.idp.attribute.resolver.spring.enc.BaseSAML2AttributeEncoderParser;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.idp.saml.attribute.transcoding.impl.SAML2ScopedStringAttributeTranscoder;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Spring Bean Definition Parser for {@link SAML2ScopedStringAttributeTranscoder}.
 */
public class SAML2ScopedStringAttributeEncoderParser extends BaseSAML2AttributeEncoderParser {

    /** Schema type name.. */
    @Nonnull public static final QName TYPE_NAME_RESOLVER = new QName(AttributeResolverNamespaceHandler.NAMESPACE,
            "SAML2ScopedString");

    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final Map<String,Object> rule) {
        super.doParse(config, parserContext, rule);

        if (config.hasAttributeNS(null, "scopeType")) {
            rule.put(SAML2ScopedStringAttributeTranscoder.PROP_SCOPE_TYPE,
                    StringSupport.trimOrNull(config.getAttributeNS(null, "scopeType")));
        }
        
        if (config.hasAttributeNS(null, "scopeDelimiter")) {
            rule.put(SAML2ScopedStringAttributeTranscoder.PROP_SCOPE_DELIMITER,
                    StringSupport.trimOrNull(config.getAttributeNS(null, "scopeDelimiter")));
        }

        if (config.hasAttributeNS(null, "scopeAttribute")) {
            rule.put(SAML2ScopedStringAttributeTranscoder.PROP_SCOPE_ATTR_NAME,
                    StringSupport.trimOrNull(config.getAttributeNS(null, "scopeAttribute")));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected BeanReference buildTranscoder() {
        return new RuntimeBeanReference("SAML2ScopedStringTranscoder");
    }

}