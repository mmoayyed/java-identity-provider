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

package net.shibboleth.idp.attribute.resolver.spring.ad.impl;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.ext.spring.factory.EvaluableScriptFactoryBean;
import net.shibboleth.idp.attribute.resolver.ad.impl.ScriptedAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring bean definition parser for scripted attribute configuration elements.
 */
public class ScriptedAttributeDefinitionParser extends AbstractWarningAttributeDefinitionParser {

    /** Schema type name - ad: (legacy). */
    @Nonnull public static final QName TYPE_NAME_AD =
            new QName(AttributeDefinitionNamespaceHandler.NAMESPACE, "Script");

    /** Schema type name - resolver: . */
    @Nonnull public static final QName TYPE_NAME_RESOLVER =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "ScriptedAttribute");

    /** Script file element name. */
    @Nonnull public static final QName SCRIPT_FILE_ELEMENT_NAME_AD =
            new QName(AttributeDefinitionNamespaceHandler.NAMESPACE, "ScriptFile");

    /** Inline Script element name. */
    @Nonnull public static final QName SCRIPT_ELEMENT_NAME_AD =
            new QName(AttributeDefinitionNamespaceHandler.NAMESPACE, "Script");

    /** Script file element name. */
    @Nonnull public static final QName SCRIPT_FILE_ELEMENT_NAME_RESOLVER =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "ScriptFile");

    /** Inline Script element name. */
    @Nonnull public static final QName SCRIPT_ELEMENT_NAME_RESOLVER =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "Script");

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ScriptedAttributeDefinitionParser.class);

    /** {@inheritDoc} */
    @Override protected Class<ScriptedAttributeDefinition> getBeanClass(@Nullable final Element element) {
        return ScriptedAttributeDefinition.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        final BeanDefinitionBuilder scriptBuilder =
                BeanDefinitionBuilder.genericBeanDefinition(EvaluableScriptFactoryBean.class);
        scriptBuilder.addPropertyValue("sourceId", getLogPrefix());
        if (config.hasAttributeNS(null, "language")) {
            final String scriptLanguage = StringSupport.trimOrNull(config.getAttributeNS(null, "language"));
            log.debug("{} Scripting language: {}", getLogPrefix(), scriptLanguage);
            scriptBuilder.addPropertyValue("engineName", scriptLanguage);
        }

        final List<Element> scriptElem = ElementSupport.getChildElements(config, SCRIPT_ELEMENT_NAME_AD);
        scriptElem.addAll(ElementSupport.getChildElements(config, SCRIPT_ELEMENT_NAME_RESOLVER));
        final List<Element> scriptFileElem = ElementSupport.getChildElements(config, SCRIPT_FILE_ELEMENT_NAME_AD);
        scriptFileElem.addAll(ElementSupport.getChildElements(config, SCRIPT_FILE_ELEMENT_NAME_RESOLVER));
        if (scriptElem != null && scriptElem.size() > 0) {
            if (scriptFileElem != null && scriptFileElem.size() > 0) {
                log.warn("{} definition contains both <Script> and <ScriptFile> elements, taking the "
                        + "first <Script> element",
                        getLogPrefix(), getDefinitionId());
            }
            final String script = scriptElem.get(0).getTextContent();
            log.debug("{} Script: {}", getLogPrefix(), script);
            scriptBuilder.addPropertyValue("script", script);
        } else if (scriptFileElem != null && scriptFileElem.size() > 0) {
            if (scriptFileElem.size() > 1) {
                log.warn("{} Attribute definition {}: definition contains multiple " 
                        + "<ScriptFile> elements, taking the first only.",
                        getLogPrefix(), getDefinitionId());                
            }
            
            final String scriptFile = scriptFileElem.get(0).getTextContent();
            log.debug("{} Script file: {}", getLogPrefix(), scriptFile);
            scriptBuilder.addPropertyValue("resource", scriptFile);
        } else {
            log.error("{} No script or script file specified for this attribute definition", getLogPrefix());
            throw new BeanCreationException("No script or script file specified for this attribute definition");
        }

        final String customRef = StringSupport.trimOrNull(config.getAttributeNS(null, "customObjectRef"));
        if (null != customRef) {
            builder.addPropertyReference("customObject", customRef);
        }

        builder.addPropertyValue("script", scriptBuilder.getBeanDefinition());
    }

    /** {@inheritDoc}. No input. */
    @Override protected boolean needsAttributeSourceID() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected QName getPreferredName() {
        return TYPE_NAME_RESOLVER;
    }

}