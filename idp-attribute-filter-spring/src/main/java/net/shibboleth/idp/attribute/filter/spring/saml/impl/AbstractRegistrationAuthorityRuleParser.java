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

package net.shibboleth.idp.attribute.filter.spring.saml.impl;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.attribute.filter.spring.policyrule.BasePolicyRuleParser;

/** Spring bean definition parser that creates RegistrationAuthorityPolicyRule beans. */
public abstract class AbstractRegistrationAuthorityRuleParser extends BasePolicyRuleParser {

    /** Name of the attribute carrying the Issuers list. */
    public static final String REGISTRARS_ATTR_NAME = "registrars";

    /** Name of the attribute carrying the boolean to flag behaviour if the metadata MDRPI. */
    public static final String MATCH_IF_METADATA_SILENT_ATTR_NAME = "matchIfMetadataSilent";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractRegistrationAuthorityRuleParser.class);

    /** {@inheritDoc} */
    @Override protected void doNativeParse(@Nonnull final Element element, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {

        if (element.hasAttributeNS(null, MATCH_IF_METADATA_SILENT_ATTR_NAME)) {
            final String matchIfSilent =element.getAttributeNS(null, MATCH_IF_METADATA_SILENT_ATTR_NAME);
            log.debug("Registration Authority Filter: Match if Metadata silent = {}", matchIfSilent);
            builder.addPropertyValue("matchIfMetadataSilent", SpringSupport.getStringValueAsBoolean(matchIfSilent));
        }

        final Attr attr = element.getAttributeNodeNS(null, REGISTRARS_ATTR_NAME);
        if (attr != null) {
            final AbstractBeanDefinition registrars = SpringSupport.getAttributeValueAsList(attr);
            log.debug("Registration Authority Filter: registrars = {}", attr.getValue());
            builder.addPropertyValue("registrars", registrars);
        }
    }

}