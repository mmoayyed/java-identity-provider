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

package net.shibboleth.idp.profile.spring.relyingparty.metadata.filter.impl;

import javax.xml.namespace.QName;

import net.shibboleth.idp.profile.spring.relyingparty.metadata.AbstractMetadataProviderParser;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Parser for a &lt;RequiredValidUntil&gt; filter.
 */
public class RequiredValidUntilParser extends AbstractSingleBeanDefinitionParser {

    /** Element name. */
    public static final QName TYPE_NAME = new QName(AbstractMetadataProviderParser.METADATA_NAMESPACE,
            "RequiredValidUntil");
    
    /** log */
    private Logger log = LoggerFactory.getLogger(RequiredValidUntilParser.class);

    /** {@inheritDoc} */
    @Override protected Class<?> getBeanClass(final Element element) {
        // IDP-693 maxValidityInterval is a duration or a value in seconds - so we need a factory
        return RequiredValidUntilFactoryBean.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(final Element element, final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {
        if (element.hasAttributeNS(null, "maxValidity")) {
            if (element.hasAttributeNS(null, "maxValidityInterval")) {
                log.warn("Only one of maxValidity and maxValidityInterval can be supplied");
            }
            builder.addPropertyValue("maxValidity",
                    StringSupport.trimOrNull(element.getAttributeNS(null, "maxValidity")));
        } else if (element.hasAttributeNS(null, "maxValidityInterval")) {
            log.warn("maxValidityInterval is deprecated, used maxValidity");
            builder.addPropertyValue("maxValidityInterval",
                    StringSupport.trimOrNull(element.getAttributeNS(null, "maxValidityInterval")));
        } else {
            log.warn("Metadata filter " + TYPE_NAME.getLocalPart() + " without maxValdity is a no-op.");
        }
    }

    /** {@inheritDoc} */
    @Override protected boolean shouldGenerateId() {
        return true;
    }
}
