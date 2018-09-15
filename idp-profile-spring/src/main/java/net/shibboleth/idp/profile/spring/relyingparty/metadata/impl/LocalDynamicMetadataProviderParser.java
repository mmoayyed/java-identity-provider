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
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.EntityIDDigestGenerator;
import org.opensaml.saml.metadata.resolver.impl.LocalDynamicMetadataResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            if (element.hasAttributeNS(null, "sourceDirectory")) {
                log.warn("Presence of sourceManagerRef will override sourceDirectory");
            }
        } else if (element.hasAttributeNS(null, "sourceDirectory")) {
            final BeanDefinitionBuilder sourceManagerBuilder = 
                    BeanDefinitionBuilder.genericBeanDefinition(FilesystemLoadSaveManager.class);
            sourceManagerBuilder.addConstructorArgValue(
                    StringSupport.trimOrNull(element.getAttributeNS(null, "sourceDirectory")));
            sourceManagerBuilder.addPropertyValue("checkModifyTime", "true");
            sourceManagerRefOrBean = sourceManagerBuilder.getBeanDefinition();
            
            if (sourceKeyGeneratorRefOrBean == null) {
                sourceKeyGeneratorRefOrBean = new EntityIDDigestGenerator(null, null, ".xml", null);
            }
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

}
