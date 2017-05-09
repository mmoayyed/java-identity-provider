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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import net.shibboleth.idp.profile.spring.relyingparty.metadata.AbstractMetadataProviderParser;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.ScriptTypeBeanParser;
import net.shibboleth.utilities.java.support.logic.ScriptedPredicate;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.opensaml.saml.common.profile.logic.EntityIdPredicate;
import org.opensaml.saml.metadata.resolver.filter.impl.NameIDFormatFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/** Parser for a &lt;NameIDFormat&gt; filter. */
public class NameIDFormatFilterParser extends AbstractSingleBeanDefinitionParser {

    /** Element name. */
    @Nonnull public static final QName TYPE_NAME = new QName(AbstractMetadataProviderParser.METADATA_NAMESPACE,
            "NameIDFormat");

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(NameIDFormatFilterParser.class);

    /** {@inheritDoc} */
    @Override protected Class<?> getBeanClass(final Element element) {
        return NameIDFormatFilter.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(final Element element, final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {

        builder.setInitMethodName("initialize");
        builder.setDestroyMethodName("destroy");
        builder.setLazyInit(true);

        // Accumulate formats to attach as rule values.
        final List<String> accumulator = new ArrayList<>();

        final ManagedMap<Object, ManagedList<String>> ruleMap = new ManagedMap();

        Element child = ElementSupport.getFirstChildElement(element);
        while (child != null) {
            if (ElementSupport.isElementNamed(child, AbstractMetadataProviderParser.METADATA_NAMESPACE, "Format")) {
                accumulator.add(ElementSupport.getElementContentAsString(child));
            } else if (ElementSupport.isElementNamed(child,
                    AbstractMetadataProviderParser.METADATA_NAMESPACE, "Entity")) {
                final BeanDefinitionBuilder entityIdBuilder =
                        BeanDefinitionBuilder.genericBeanDefinition(EntityIdPredicate.class);
                entityIdBuilder.addConstructorArgValue(ElementSupport.getElementContentAsString(child));
                final ManagedList<String> forRule = new ManagedList(accumulator.size());
                forRule.addAll(accumulator);
                ruleMap.put(entityIdBuilder.getBeanDefinition(), forRule);
            } else if (ElementSupport.isElementNamed(child,
                    AbstractMetadataProviderParser.METADATA_NAMESPACE, "ConditionRef")) {
                final ManagedList<String> forRule = new ManagedList(accumulator.size());
                forRule.addAll(accumulator);
                ruleMap.put(new RuntimeBeanReference(ElementSupport.getElementContentAsString(child)), forRule);
            } else if (ElementSupport.isElementNamed(child, AbstractMetadataProviderParser.METADATA_NAMESPACE,
                    "ConditionScript")) {
                final ManagedList<String> forRule = new ManagedList(accumulator.size());
                forRule.addAll(accumulator);
                ruleMap.put(ScriptTypeBeanParser.parseScriptType(ScriptedPredicate.class, child).getBeanDefinition(),
                        forRule);
            }
            child = ElementSupport.getNextSiblingElement(child);
        }

        builder.addPropertyValue("rules", ruleMap);
    }

    /** {@inheritDoc} */
    @Override protected boolean shouldGenerateId() {
        return true;
    }

}