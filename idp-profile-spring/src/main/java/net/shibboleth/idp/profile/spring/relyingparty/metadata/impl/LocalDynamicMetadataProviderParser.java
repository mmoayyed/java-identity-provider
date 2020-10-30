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

package net.shibboleth.idp.profile.spring.relyingparty.metadata.impl;

import javax.xml.namespace.QName;

import org.opensaml.core.xml.persist.FilesystemLoadSaveManager;
import org.opensaml.core.xml.persist.impl.PassthroughSourceStrategy;
import org.opensaml.core.xml.persist.impl.SegmentingIntermediateDirectoryStrategy;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.DefaultLocalDynamicSourceKeyGenerator;
import org.opensaml.saml.metadata.resolver.impl.LocalDynamicMetadataResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.idp.profile.spring.relyingparty.metadata.AbstractMetadataProviderParser;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Parser for {@link LocalDynamicMetadataResolver}.
 */
public class LocalDynamicMetadataProviderParser extends AbstractDynamicMetadataProviderParser {
    
    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(AbstractMetadataProviderParser.METADATA_NAMESPACE,
            "LocalDynamicMetadataProvider");

    /** Logger. */
    private Logger log = LoggerFactory.getLogger(LocalDynamicMetadataProviderParser.class);
    
    /** {@inheritDoc} */
    protected Class<? extends MetadataResolver> getNativeBeanClass(final Element element) {
        return LocalDynamicMetadataResolver.class;
    }
    
    /** {@inheritDoc} */
    @Override protected void doNativeParse(final Element element, final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {
        super.doNativeParse(element, parserContext, builder);
        
        Object sourceKeyGeneratorRefOrBean = null;
        if (element.hasAttributeNS(null, "sourceKeyGeneratorRef")) {
            sourceKeyGeneratorRefOrBean = 
                    StringSupport.trimOrNull(element.getAttributeNS(null, "sourceKeyGeneratorRef"));
        }
        
        Object sourceManagerRefOrBean = null;
        if (element.hasAttributeNS(null, "sourceManagerRef")) {
            sourceManagerRefOrBean = StringSupport.trimOrNull(element.getAttributeNS(null, "sourceManagerRef"));
            checkAndLogSourceDirectoryOverrides(element);
        } else if (element.hasAttributeNS(null, "sourceDirectory")) {
            // Default in source key generator if not supplied
            boolean isDefaultSourceKeyGenerator = false;
            if (sourceKeyGeneratorRefOrBean == null) {
                sourceKeyGeneratorRefOrBean = new DefaultLocalDynamicSourceKeyGenerator(null, ".xml", null);
                isDefaultSourceKeyGenerator = true;
            }

            sourceManagerRefOrBean = buildFilesystemManagerDefinition(element, parserContext,
                    isDefaultSourceKeyGenerator);
        } else {
            log.error("{} LocalDynamicMetadataProvider contained neither a sourceManagerRef nor a sourceDirectory", 
                    parserContext.getReaderContext().getResource().getDescription());
            throw new BeanDefinitionParsingException(
                    new Problem("Neither a sourceManagerRef nor sourceDirectory was supplied",
                            new Location( parserContext.getReaderContext().getResource())));
        }
        
        if (sourceManagerRefOrBean instanceof String) {
            builder.addConstructorArgReference((String) sourceManagerRefOrBean);
        } else {
            builder.addConstructorArgValue(sourceManagerRefOrBean);
        }
        
        if (sourceKeyGeneratorRefOrBean instanceof String) {
            builder.addConstructorArgReference((String) sourceKeyGeneratorRefOrBean);
        } else {
            builder.addConstructorArgValue(sourceKeyGeneratorRefOrBean);
        }

    }

    /**
     * Check and log the options related to 'sourceDirectory' which are being overridden by 'sourceManagerRef'.
     *
     * @param element the element being parsed
     */
    private void checkAndLogSourceDirectoryOverrides(final Element element) {
        if (element.hasAttributeNS(null, "sourceDirectory")) {
            log.warn("Presence of sourceManagerRef will override sourceDirectory");
        }

        if (element.hasAttributeNS(null, "sourceDirectoryIntermediateSegmentNumber")
                || element.hasAttributeNS(null, "sourceDirectoryIntermediateSegmentLength")
                || element.hasAttributeNS(null, "sourceDirectoryIntermediateStrategyRef")) {
            log.warn("Presence of sourceManagerRef will override sourceDirectoryIntermediateSegmentNumber, "
                    + "sourceDirectoryIntermediateSegmentLength and sourceDirectoryIntermediateStrategyRef");
        }
    }

    /**
     * Build bean definition for {@link FilesystemLoadSaveManager} source manager.
     *
     * @param element the element being parsed
     * @param parserContext the current parser context
     * @param isDefaultSourceKeyGenerator whether the internal default source key manager is being used
     *
     * @return the bean definition for the filesystem source manager
     */
    private BeanDefinition buildFilesystemManagerDefinition(final Element element, final ParserContext parserContext,
            final boolean isDefaultSourceKeyGenerator) {

        final BeanDefinitionBuilder sourceManagerBuilder =
                BeanDefinitionBuilder.genericBeanDefinition(FilesystemLoadSaveManager.class);

        sourceManagerBuilder.addConstructorArgValue(
                StringSupport.trimOrNull(element.getAttributeNS(null, "sourceDirectory")));

        sourceManagerBuilder.addConstructorArgValue(Boolean.TRUE);

       // Figure out intermediate directory strategy, if configured, and apply
        processIntermediateDirectoryStrategy(element, parserContext, sourceManagerBuilder,
                isDefaultSourceKeyGenerator);

        return sourceManagerBuilder.getBeanDefinition();
    }

    /**
     * Process options related to intermediate directory strategy and apply to the builder for the
     * {@link FilesystemLoadSaveManager}.
     *
     * @param element the element being parsed
     * @param parserContext the current parser context
     * @param sourceManagerBuilder the source build manager for the FilesystemLoadSaveManager being built
     * @param isDefaultSourceKeyGenerator whether the internal default source key manager is being used
     */
    private void processIntermediateDirectoryStrategy(final Element element, final ParserContext parserContext,
            final BeanDefinitionBuilder sourceManagerBuilder, final boolean isDefaultSourceKeyGenerator) {

        if (element.hasAttributeNS(null, "sourceDirectoryIntermediateStrategyRef")) {
            sourceManagerBuilder.addConstructorArgReference(
                    StringSupport.trimOrNull(
                            element.getAttributeNS(null, "sourceDirectoryIntermediateStrategyRef")));
        } else if (isDefaultSourceKeyGenerator) {
            final BeanDefinition intermediateDirectoryStrategy =
                    checkAndBuildPassthroughIntermediateDirectoryStrategy(element, parserContext);
            if (intermediateDirectoryStrategy != null) {
                sourceManagerBuilder.addConstructorArgValue(intermediateDirectoryStrategy);
            }
        } else {
            if (element.hasAttributeNS(null, "sourceDirectoryIntermediateSegmentNumber")
                    || element.hasAttributeNS(null, "sourceDirectoryIntermediateSegmentLength")) {
                log.warn("{} Non-default source key manger in use, sourceDirectoryIntermediateSegmentNumber and "
                    + "sourceDirectoryIntermediateSegmentLength will be ignored. "
                    + "Use sourceDirectoryIntermediateStrategyRef instead");
            }
        }
    }

    /**
     * Return bean definition for the default intermediate directory strategy with passthrough source strategy,
     * if segmenting configuration params specified.
     *
     * @param element the element being parsed
     * @param parserContext the current parser context
     * @return the strategy bean definition, or null if not configured
     */
    private BeanDefinition checkAndBuildPassthroughIntermediateDirectoryStrategy(final Element element,
            final ParserContext parserContext) {

        if (!element.hasAttributeNS(null, "sourceDirectoryIntermediateSegmentNumber")
                && !element.hasAttributeNS(null, "sourceDirectoryIntermediateSegmentLength") ) {
            return null;
        }

        // Must have both params
        if (!element.hasAttributeNS(null, "sourceDirectoryIntermediateSegmentNumber")
                || !element.hasAttributeNS(null, "sourceDirectoryIntermediateSegmentLength")) {

            log.error("{} LocalDynamicMetadataProvider contained either sourceDirectoryIntermediateSegmentNumber "
                    + "or sourceDirectoryIntermediateSegmentLength without the other",
                    parserContext.getReaderContext().getResource().getDescription());
            throw new BeanDefinitionParsingException(
                    new Problem("Either sourceDirectoryIntermediateSegmentNumber or "
                            + "sourceDirectoryIntermediateSegmentLength was supplied without the other",
                            new Location( parserContext.getReaderContext().getResource())));
        }

        final BeanDefinitionBuilder strategyBuilder =
                BeanDefinitionBuilder.genericBeanDefinition(SegmentingIntermediateDirectoryStrategy.class);
        strategyBuilder.addConstructorArgValue(StringSupport.trimOrNull(
                element.getAttributeNS(null, "sourceDirectoryIntermediateSegmentNumber")));
        strategyBuilder.addConstructorArgValue(StringSupport.trimOrNull(
                element.getAttributeNS(null, "sourceDirectoryIntermediateSegmentLength")));
        strategyBuilder.addConstructorArgValue(new PassthroughSourceStrategy());
        return strategyBuilder.getBeanDefinition();
    }

}
