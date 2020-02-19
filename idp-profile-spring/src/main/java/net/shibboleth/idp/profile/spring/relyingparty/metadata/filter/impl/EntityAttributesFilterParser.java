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

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.profile.logic.EntityIdPredicate;
import org.opensaml.saml.metadata.resolver.filter.impl.EntityAttributesFilter;
import org.opensaml.saml.saml2.core.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/** Parser for a &lt;EntityAttributes&gt; filter. */
public class EntityAttributesFilterParser extends AbstractSingleBeanDefinitionParser {

    /** Element name. */
    @Nonnull public static final QName TYPE_NAME = new QName(AbstractMetadataProviderParser.METADATA_NAMESPACE,
            "EntityAttributes");

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(EntityAttributesFilterParser.class);

    /** {@inheritDoc} */
    @Override protected Class<?> getBeanClass(final Element element) {
        return EntityAttributesFilter.class;
    }

// Checkstyle: CyclomaticComplexity|MethodLength OFF
    /** {@inheritDoc} */
    @Override protected void doParse(final Element element, final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {

        final Unmarshaller unmarshaller = XMLObjectSupport.getUnmarshaller(Attribute.DEFAULT_ELEMENT_NAME);
        if (unmarshaller == null) {
            throw new BeanCreationException("Unable to obtain Unmarshaller for Attribute objects");
        }

        // Accumulate Attribute objects to attach as rule values.
        final List<Attribute> accumulator = new ArrayList<>();

        // Accumulated map of predicates to objects to attach to inject into filter.
        final ManagedMap<Object, ManagedList<Attribute>> ruleMap = new ManagedMap<>();

        // Acumulation of entityIDs to use in the next automated Predicate.
        // Interrupting a sequence of <Entity> elements will end the accumulation.
        ManagedSet<String> entitySet = new ManagedSet<>();

        Element child = ElementSupport.getFirstChildElement(element);
        
        // Check for Predicate at the top.
        if (ElementSupport.isElementNamed(child, AbstractMetadataProviderParser.METADATA_NAMESPACE,
                "AttributeFilterRef")) {
            builder.addPropertyReference("attributeFilter", ElementSupport.getElementContentAsString(child));
            child = ElementSupport.getNextSiblingElement(child);
        } else if (ElementSupport.isElementNamed(child, AbstractMetadataProviderParser.METADATA_NAMESPACE,
                "AttributeFilterScript")) {
            builder.addPropertyValue("attributeFilter",
                    ScriptTypeBeanParser.parseScriptType(ScriptedPredicate.class, child).getBeanDefinition());
            child = ElementSupport.getNextSiblingElement(child);
        }
        
        // Loop over remaining children.
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
                final ManagedList<Attribute> forRule = new ManagedList<>(accumulator.size());
                forRule.addAll(accumulator);
                ruleMap.put(entityIdBuilder.getBeanDefinition(), forRule);
                
                entitySet = new ManagedSet<>();
            }
            
            if (ElementSupport.isElementNamed(child, Attribute.DEFAULT_ELEMENT_NAME)) {
                try {
                    final XMLObject attribute = unmarshaller.unmarshall(child);
                    if (attribute instanceof Attribute) {
                        accumulator.add((Attribute) attribute);
                    }
                } catch (final UnmarshallingException e) {
                    log.error("Error unmarshalling Attribute", e);
                }
            } else if (ElementSupport.isElementNamed(child, AbstractMetadataProviderParser.METADATA_NAMESPACE,
                    "ConditionRef")) {
                final ManagedList<Attribute> forRule = new ManagedList<>(accumulator.size());
                forRule.addAll(accumulator);
                ruleMap.put(new RuntimeBeanReference(ElementSupport.getElementContentAsString(child)), forRule);
            } else if (ElementSupport.isElementNamed(child, AbstractMetadataProviderParser.METADATA_NAMESPACE,
                    "ConditionScript")) {
                final ManagedList<Attribute> forRule = new ManagedList<>(accumulator.size());
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
            final ManagedList<Attribute> forRule = new ManagedList<>(accumulator.size());
            forRule.addAll(accumulator);
            ruleMap.put(entityIdBuilder.getBeanDefinition(), forRule);
        }

        builder.addPropertyValue("rules", ruleMap);
    }
// Checkstyle: CyclomaticComplexity|MethodLength ON

    /** {@inheritDoc} */
    @Override protected boolean shouldGenerateId() {
        return true;
    }

}