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

import org.opensaml.saml.common.profile.logic.EntityIdPredicate;
import org.opensaml.saml.metadata.resolver.filter.impl.NameIDFormatFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.ext.spring.util.AbstractCustomBeanDefinitionParser;
import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.AbstractMetadataProviderParser;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.ScriptTypeBeanParser;
import net.shibboleth.utilities.java.support.logic.ScriptedPredicate;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

/** Parser for a &lt;NameIDFormat&gt; filter. */
public class NameIDFormatFilterParser extends AbstractCustomBeanDefinitionParser {

    /** Element name. */
    @Nonnull public static final QName TYPE_NAME = new QName(AbstractMetadataProviderParser.METADATA_NAMESPACE,
            "NameIDFormat");

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(NameIDFormatFilterParser.class);

    /** {@inheritDoc} */
    @Override protected Class<?> getBeanClass(final Element element) {
        return NameIDFormatFilter.class;
    }

// Checkstyle: MethodLength OFF
    /** {@inheritDoc} */
    @Override protected void doParse(final Element element, final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {

        builder.setInitMethodName("initialize");
        builder.setDestroyMethodName("destroy");
        builder.setLazyInit(true);

        if (element.hasAttributeNS(null, "removeExistingFormats")) {
            builder.addPropertyValue("removeExistingFormats", 
                    SpringSupport.getStringValueAsBoolean(element.getAttributeNS(null, "removeExistingFormats")));
        }
        
        // Accumulate formats to attach as rule values.
        final List<String> accumulator = new ArrayList<>();

        // Accumulated map of predicates to objects to attach to inject into filter.
        final ManagedMap<Object, ManagedList<String>> ruleMap = new ManagedMap<>();

        // Acumulation of entityIDs to use in the next automated Predicate.
        // Interrupting a sequence of <Entity> elements will end the accumulation.
        ManagedSet<String> entitySet = new ManagedSet<>();

        Element child = ElementSupport.getFirstChildElement(element);
        while (child != null) {
            
            if (ElementSupport.isElementNamed(child, AbstractMetadataProviderParser.METADATA_NAMESPACE, "Entity")) {
                // Add to the active entity set.
                entitySet.add(ElementSupport.getElementContentAsString(child));
                child = ElementSupport.getNextSiblingElement(child);
                continue;
                
            } else if (!entitySet.isEmpty()) {
                // "Commit" the current entity set as a single condition against the current accumulator.
                // Then reset the entity set. Use a new object rather than clearing to ensure no cross-contamination.
                final BeanDefinitionBuilder entityIdBuilder =
                        BeanDefinitionBuilder.genericBeanDefinition(EntityIdPredicate.class);
                entityIdBuilder.addConstructorArgValue(entitySet);
                final ManagedList<String> forRule = new ManagedList<>(accumulator.size());
                forRule.addAll(accumulator);
                ruleMap.put(entityIdBuilder.getBeanDefinition(), forRule);
                
                entitySet = new ManagedSet<>();
            }
            
            if (ElementSupport.isElementNamed(child, AbstractMetadataProviderParser.METADATA_NAMESPACE, "Format")) {
                accumulator.add(ElementSupport.getElementContentAsString(child));
            } else if (ElementSupport.isElementNamed(child,
                    AbstractMetadataProviderParser.METADATA_NAMESPACE, "ConditionRef")) {
                final ManagedList<String> forRule = new ManagedList<>(accumulator.size());
                forRule.addAll(accumulator);
                ruleMap.put(new RuntimeBeanReference(ElementSupport.getElementContentAsString(child)), forRule);
            } else if (ElementSupport.isElementNamed(child, AbstractMetadataProviderParser.METADATA_NAMESPACE,
                    "ConditionScript")) {
                final ManagedList<String> forRule = new ManagedList<>(accumulator.size());
                forRule.addAll(accumulator);
                ruleMap.put(ScriptTypeBeanParser.parseScriptType(ScriptedPredicate.class, child).getBeanDefinition(),
                        forRule);
            }
            
            child = ElementSupport.getNextSiblingElement(child);
        }

        // Do a final check and commit for a non-empty entity set.
        if (!entitySet.isEmpty()) {
            final BeanDefinitionBuilder entityIdBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(EntityIdPredicate.class);
            entityIdBuilder.addConstructorArgValue(entitySet);
            final ManagedList<String> forRule = new ManagedList<>(accumulator.size());
            forRule.addAll(accumulator);
            ruleMap.put(entityIdBuilder.getBeanDefinition(), forRule);
        }
        
        builder.addPropertyValue("rules", ruleMap);
    }
// Checkstyle: MethodLength ON

    /** {@inheritDoc} */
    @Override protected boolean shouldGenerateId() {
        return true;
    }

}