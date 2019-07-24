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

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.profile.logic.ScriptedPredicate;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.ScriptTypeBeanParser;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Base class for Spring bean definition parser for attribute encoders.
 */
public abstract class BaseAttributeEncoderParser extends AbstractSingleBeanDefinitionParser {

    /** Local name of name attribute. */
    @Nonnull @NotEmpty public static final String NAME_ATTRIBUTE_NAME = "name";

    /** Log4j logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(BaseAttributeEncoderParser.class);

    /** {@inheritDoc} */
    @Override
    protected Class<TranscodingRule> getBeanClass(final Element element) {
        return TranscodingRule.class;
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean shouldGenerateId() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean shouldParseNameAsAliases() {
        return false;
    }
    
    /** {@inheritDoc} */
// Checkstyle: CyclomaticComplexity OFF
    @Override
    protected void doParse(final Element config, final ParserContext context, final BeanDefinitionBuilder builder) {

        final ManagedMap<String, Object> rule = new ManagedMap<>();

        builder.addConstructorArgValue(rule);
        
        final Node parentAttribute = config.getParentNode();
        
        if (parentAttribute instanceof Element && ((Element) parentAttribute).hasAttributeNS(null, "id")) {
            
            // Handle id property.
            rule.put(AttributeTranscoderRegistry.PROP_ID,
                    StringSupport.trimOrNull(((Element) config.getParentNode()).getAttributeNS(null, "id")));
            
            // Handle display metadata.
            final List<Element> displayNames =
                    ElementSupport.getChildElements(parentAttribute,
                            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "DisplayName"));
            if (displayNames != null && !displayNames.isEmpty()) {
                processLocalizedElement(displayNames, rule, AttributeTranscoderRegistry.PROP_DISPLAY_NAME);
            }

            final List<Element> displayDescriptions =
                    ElementSupport.getChildElements(parentAttribute,
                            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "DisplayDescription"));
            if (displayDescriptions != null && !displayDescriptions.isEmpty()) {
                processLocalizedElement(displayDescriptions, rule, AttributeTranscoderRegistry.PROP_DESCRIPTION);
            }
            
        } else {
            log.warn("Parsing AttributeEncoder with no parent element, resulting rule will be ignored");
        }

        if (config.hasAttributeNS(null, "activationConditionRef")) {
            if (config.hasAttributeNS(null, "relyingParties")) {
                log.warn("relyingParties ignored, using activationConditionRef");
            }
            rule.put(AttributeTranscoderRegistry.PROP_CONDITION, new RuntimeBeanReference(
                    StringSupport.trimOrNull(config.getAttributeNS(null, "activationConditionRef"))));
        } else if (config.hasAttributeNS(null, "relyingParties")) {
            rule.put(AttributeTranscoderRegistry.PROP_RELYINGPARTIES, config.getAttributeNS(null, "relyingParties"));
        } else {
            final Element child = ElementSupport.getFirstChildElement(config);
            if (child != null && ElementSupport.isElementNamed(child,
                    AttributeResolverNamespaceHandler.NAMESPACE, "ActivationConditionScript")) {
                rule.put(AttributeTranscoderRegistry.PROP_CONDITION,
                        ScriptTypeBeanParser.parseScriptType(ScriptedPredicate.class, child).getBeanDefinition());
            }
        }

        rule.put(AttributeTranscoderRegistry.PROP_TRANSCODER, buildTranscoder());
        
        doParse(config, context, rule);
    }
// Checkstyle: CyclomaticComplexity ON
    
    /**
     * Inject any necessary elements into the mapping rule based on the specific encoder type.
     * 
     * @param config the encoder element being parsed
     * @param parserContext the parser context
     * @param rule the mapping rule
     */
    protected abstract void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final Map<String,Object> rule);


    /**
     * Return a bean definition for the transcoder to include in the mapping.
     * 
     * @return bean definition for an AttributeTranscoder
     */
    @Nonnull protected abstract BeanReference buildTranscoder();
    
    /**
     * Used to process string elements that contain an xml:lang attribute expressing localization.
     * 
     * @param elements list of elements, must not be null, may be empty
     * @param rule the map of rules to add to
     * @param propertyPrefix the root property name to install
     */
    private void processLocalizedElement(@Nonnull final List<Element> elements,
            @Nonnull final ManagedMap<String, Object> rule,
            @Nonnull @NotEmpty final String propertyPrefix) {
        
        for (final Element element : elements) {
            final String value = element.getTextContent();
            if (value != null) {
                final String lang = StringSupport.trimOrNull(AttributeSupport.getXMLLang(element));
                if (lang != null) {
                    rule.put(propertyPrefix + '.' + lang, value);
                } else {
                    rule.put(propertyPrefix, value);
                }
            }
        }
    }

}