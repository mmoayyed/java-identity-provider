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

package net.shibboleth.idp.attribute.resolver.spring.ad;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.attribute.resolver.spring.AttributeResolverNamespaceHandler;
import net.shibboleth.idp.attribute.resolver.spring.BaseResolverPluginParser;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Base spring bean definition parser for attribute definitions. AttributeDefinition implementations should provide a
 * custom BeanDefinitionParser by extending this class and overriding the doParse() method to parse any additional
 * attributes or elements it requires. Standard attributes and elements defined by the ResolutionPlugIn and
 * AttributeDefinition schemas will automatically attempt to be parsed.
 */
public abstract class BaseAttributeDefinitionParser extends BaseResolverPluginParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(AttributeResolverNamespaceHandler.NAMESPACE,
            "AttributeDefinition");

    /** Local name of attribute encoder. */
    public static final QName ATTRIBUTE_ENCODER_ELEMENT_NAME = new QName(AttributeResolverNamespaceHandler.NAMESPACE,
            "AttributeEncoder");

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(BaseAttributeDefinitionParser.class);

    /** cache for the log prefix - to save multiple recalculations. */
    private String logPrefix;

    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        final List<Element> displayNames =
                ElementSupport.getChildElements(config, new QName(AttributeResolverNamespaceHandler.NAMESPACE,
                        "DisplayName"));
        if (displayNames != null && !displayNames.isEmpty()) {
            final Map<Locale, String> names = processLocalizedElement(displayNames);
            log.debug("{} setting displayNames {}.", getLogPrefix(), names);
            builder.addPropertyValue("displayNames", names);
        }

        final List<Element> displayDescriptions =
                ElementSupport.getChildElements(config, new QName(AttributeResolverNamespaceHandler.NAMESPACE,
                        "DisplayDescription"));
        if (displayDescriptions != null && !displayDescriptions.isEmpty()) {
            final Map<Locale, String> names = processLocalizedElement(displayDescriptions);
            log.debug("{} setting displayDescriptions {}.", getLogPrefix(), names);
            builder.addPropertyValue("displayDescriptions", names);
        }

        Boolean dependencyOnly = new Boolean(false);
        if (config.hasAttributeNS(null, "dependencyOnly")) {
            dependencyOnly =
                    AttributeSupport.getAttributeValueAsBoolean(config.getAttributeNodeNS(null, "dependencyOnly"));
            if (null == dependencyOnly) {
                log.error("{} value for 'dependencyOnly' should be 'true','1','false', or '0'.", getLogPrefix());
                dependencyOnly = new Boolean(false);
            }
        }
        log.debug("{} setting displayDescriptions {}.", getLogPrefix(), dependencyOnly);
        builder.addPropertyValue("dependencyOnly", dependencyOnly);

        final String sourceAttributeId;
        if (config.hasAttributeNS(null, "sourceAttributeID")) {
            sourceAttributeId = config.getAttributeNodeNS(null, "sourceAttributeID").getValue();
            log.debug("{} setting sourceAttributeId {}.", getLogPrefix(), sourceAttributeId);
        } else {
            sourceAttributeId = getDefinitionId();
        }
        builder.addPropertyValue("sourceAttributeId", sourceAttributeId);

        final List<Element> attributeEncoders =
                ElementSupport.getChildElements(config, new QName(AttributeResolverNamespaceHandler.NAMESPACE,
                        "AttributeEncoder"));

        if (attributeEncoders != null && !attributeEncoders.isEmpty()) {
            log.debug("{} adding {} encoders.", getLogPrefix(), attributeEncoders.size());
            builder.addPropertyValue("attributeEncoders",
                    SpringSupport.parseCustomElements(attributeEncoders, parserContext));
        }
    }

    /**
     * Used to process string elements that contain an xml:lang attribute expressing localization.
     * Eeturns a {@link ManagedMap} to allow property replacement to work.
     * 
     * @param elements list of elements, must not be null, may be empty
     * 
     * @return the localized string indexed by locale
     */
    protected Map<Locale, String> processLocalizedElement(@Nonnull final List<Element> elements) {
        Map<Locale, String> localizedString = new ManagedMap<>(elements.size());
        for (Element element : elements) {
            localizedString.put(AttributeSupport.getXMLLangAsLocale(element), element.getTextContent());
        }

        return localizedString;
    }

    /**
     * return a string which is to be prepended to all log messages.
     * 
     * @return "Attribute Definition '<definitionID>' :"
     */
    @Nonnull @NotEmpty protected String getLogPrefix() {
        if (null == logPrefix) {
            StringBuilder builder = new StringBuilder("Attribute Definition '").append(getDefinitionId()).append("':");
            logPrefix = builder.toString();
        }
        return logPrefix;
    }
}