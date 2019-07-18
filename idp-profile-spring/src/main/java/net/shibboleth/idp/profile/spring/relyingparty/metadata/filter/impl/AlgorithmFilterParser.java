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
import org.opensaml.saml.ext.saml2alg.DigestMethod;
import org.opensaml.saml.ext.saml2alg.SigningMethod;
import org.opensaml.saml.metadata.resolver.filter.impl.AlgorithmFilter;
import org.opensaml.saml.saml2.metadata.EncryptionMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/** 
 * Parser for Algorithm filter.
 * 
 * @since 4.0.0
 */
public class AlgorithmFilterParser extends AbstractSingleBeanDefinitionParser {

    /** Element name. */
    @Nonnull public static final QName TYPE_NAME = new QName(AbstractMetadataProviderParser.METADATA_NAMESPACE,
            "Algorithm");

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AlgorithmFilterParser.class);

    /** {@inheritDoc} */
    @Override protected Class<?> getBeanClass(final Element element) {
        return AlgorithmFilter.class;
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override protected void doParse(final Element element, final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {

        final Unmarshaller digestUnmarshaller = XMLObjectSupport.getUnmarshaller(DigestMethod.DEFAULT_ELEMENT_NAME);
        final Unmarshaller signingUnmarshaller =XMLObjectSupport.getUnmarshaller(SigningMethod.DEFAULT_ELEMENT_NAME);
        final Unmarshaller encryptionUnmarshaller =
                XMLObjectSupport.getUnmarshaller(EncryptionMethod.DEFAULT_ELEMENT_NAME);
        if (digestUnmarshaller == null || signingUnmarshaller == null || encryptionUnmarshaller == null) {
            throw new BeanCreationException("Unable to obtain Unmarshallers");
        }

        // Accumulate objects to attach as rule values.
        final List<XMLObject> accumulator = new ArrayList<>();

        final ManagedMap<Object, ManagedList<XMLObject>> ruleMap = new ManagedMap();

        Element child = ElementSupport.getFirstChildElement(element);

        while (child != null) {
            if (ElementSupport.isElementNamed(child, DigestMethod.DEFAULT_ELEMENT_NAME)) {
                try {
                    accumulator.add(digestUnmarshaller.unmarshall(child));
                } catch (final UnmarshallingException e) {
                    log.error("Error unmarshalling DigestMethod element", e);
                }
            } else if (ElementSupport.isElementNamed(child, SigningMethod.DEFAULT_ELEMENT_NAME)) {
                try {
                    accumulator.add(signingUnmarshaller.unmarshall(child));
                } catch (final UnmarshallingException e) {
                    log.error("Error unmarshalling SigningMethod element", e);
                }
            } else if (ElementSupport.isElementNamed(child, EncryptionMethod.DEFAULT_ELEMENT_NAME)) {
                try {
                    accumulator.add(encryptionUnmarshaller.unmarshall(child));
                } catch (final UnmarshallingException e) {
                    log.error("Error unmarshalling EncryptionMethod element", e);
                }
            } else if (ElementSupport
                    .isElementNamed(child, AbstractMetadataProviderParser.METADATA_NAMESPACE, "Entity")) {
                final BeanDefinitionBuilder entityIdBuilder =
                        BeanDefinitionBuilder.genericBeanDefinition(EntityIdPredicate.class);
                entityIdBuilder.addConstructorArgValue(ElementSupport.getElementContentAsString(child));
                final ManagedList<XMLObject> forRule = new ManagedList(accumulator.size());
                forRule.addAll(accumulator);
                ruleMap.put(entityIdBuilder.getBeanDefinition(), forRule);
            } else if (ElementSupport.isElementNamed(child, AbstractMetadataProviderParser.METADATA_NAMESPACE,
                    "ConditionRef")) {
                final ManagedList<XMLObject> forRule = new ManagedList(accumulator.size());
                forRule.addAll(accumulator);
                ruleMap.put(new RuntimeBeanReference(ElementSupport.getElementContentAsString(child)), forRule);
            } else if (ElementSupport.isElementNamed(child, AbstractMetadataProviderParser.METADATA_NAMESPACE,
                    "ConditionScript")) {
                final ManagedList<XMLObject> forRule = new ManagedList(accumulator.size());
                forRule.addAll(accumulator);
                ruleMap.put(ScriptTypeBeanParser.parseScriptType(ScriptedPredicate.class, child).getBeanDefinition(),
                        forRule);
            }
            child = ElementSupport.getNextSiblingElement(child);
        }

        builder.addPropertyValue("rules", ruleMap);
    }
// Checkstyle: CyclomaticComplexity ON

    /** {@inheritDoc} */
    @Override protected boolean shouldGenerateId() {
        return true;
    }

}