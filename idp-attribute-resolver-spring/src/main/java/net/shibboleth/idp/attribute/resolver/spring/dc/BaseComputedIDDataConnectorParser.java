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

package net.shibboleth.idp.attribute.resolver.spring.dc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring bean definition parser for configuring
 * {@link net.shibboleth.idp.saml.impl.attribute.resolver.ComputedIDDataConnector} and
 * {@link net.shibboleth.idp.saml.impl.attribute.resolver.StoredIDDataConnector}.
 */
public abstract class BaseComputedIDDataConnectorParser extends AbstractDataConnectorParser {

    /** log. */
    private final Logger log = LoggerFactory.getLogger(BaseComputedIDDataConnectorParser.class);

    /**
     * Parse the common definitions for {@link net.shibboleth.idp.saml.impl.attribute.resolver.ComputedIDDataConnector}
     * and {@link net.shibboleth.idp.saml.impl.attribute.resolver.StoredIDDataConnector}.
     * 
     * @param config the DOM element under consideration.
     * @param parserContext Spring's context.
     * @param builder Spring's bean builder.
     * @param generatedIdDefaultName the name to give the generated Attribute if none was provided.
     */
    protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder, @Nullable String generatedIdDefaultName) {
        super.doParse(config, parserContext, builder);

        final String generatedAttribute;
        if (config.hasAttributeNS(null, "generatedAttributeID")) {
            generatedAttribute = config.getAttributeNS(null, "generatedAttributeID");
        } else {
            generatedAttribute = generatedIdDefaultName;
        }

        final String sourceAttribute = config.getAttributeNS(null, "sourceAttributeID");

        final String salt = StringSupport.trimOrNull(config.getAttributeNS(null, "salt"));
        final byte[] saltBytes;
        if (null == salt) {
            saltBytes = null;
            log.debug("{} generated Attribute : '{}', sourceAttribute = '{}', no salt provided.", new Object[] {
                    getLogPrefix(), generatedAttribute, sourceAttribute,});
        } else {
            saltBytes = salt.getBytes();
            log.debug("{} generated Attribute : '{}', sourceAttribute = '{}', salt: '{}'.", new Object[] {
                    getLogPrefix(), generatedAttribute, sourceAttribute, saltBytes,});
        }

        builder.addPropertyValue("generatedAttributeId", generatedAttribute);
        builder.addPropertyValue("sourceAttributeId", sourceAttribute);
        builder.addPropertyValue("salt", saltBytes);
    }
}