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

package net.shibboleth.idp.profile.spring.relyingparty.metadata;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.utilities.java.support.xml.DomTypeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Parser for the MetadataProviderType in the <code>urn:mace:shibboleth:2.0:metadata</code> namespace.
 */
public abstract class AbstractMetadataProviderParser extends AbstractSingleBeanDefinitionParser {

    /** Logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractMetadataProviderParser.class);

    /**
     * Handle attributes which are inapropriate for specific implementations. The chaining metadata provider cannot have
     * "requireValidMetadata" or "failFastInitialization" set, even though they are present in the schema. <br/>
     * This method detects whether these elements are present and if the element is not a chaining provider returns
     * true, otherwise it returns false and emits a warning.
     * 
     * @param element the element
     * @param attribute the attribute
     * @return true iff this is not a chaining resolver and the attribute is present
     */
    private boolean isPresentNotChaining(@Nonnull Element element, @Nonnull String attribute) {

        if (!element.hasAttributeNS(null, attribute)) {
            return false;
        }

        final String localPart = getLocalPartOfType(element);

        if (ChainingMetadataProviderParser.ELEMENT_NAME.getLocalPart().equals(localPart)) {
            log.warn("{} is not valid for {}", attribute, ChainingMetadataProviderParser.ELEMENT_NAME.getLocalPart());
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        builder.setInitMethodName("initialize");
        builder.setLazyInit(true);

        builder.addPropertyValue("id", element.getAttributeNS(null, "id"));

        if (isPresentNotChaining(element, "failFastInitialization")) {
            builder.addPropertyValue("failFastInitialization", element.getAttributeNS(null, "failFastInitialization"));
        }

        if (isPresentNotChaining(element, "requireValidMetadata")) {
            builder.addPropertyValue("requireValidMetadata", element.getAttributeNS(null, "requireValidMetadata"));
        }

        if (element.hasAttributeNS(null, "maxCacheDuration")) {
            throw new BeanDefinitionParsingException(new Problem("maxCacheDuration is not supported", new Location(
                    parserContext.getReaderContext().getResource())));
        }

        if (element.hasAttributeNS(null, "cacheDuration")) {
            throw new BeanDefinitionParsingException(new Problem("cacheDuration is not supported", new Location(
                    parserContext.getReaderContext().getResource())));
        }

        if (element.hasAttributeNS(null, "maintainExpiredMetadata")) {
            throw new BeanDefinitionParsingException(new Problem("maintainExpiredMetadata is not supported",
                    new Location(parserContext.getReaderContext().getResource())));
        }

        final List<Element> filters =
                ElementSupport.getChildElements(element, MetadataNamespaceHandler.METADATA_FILTER_ELEMENT_NAME);

        if (null != filters && filters.size() > 0) {
            // TODO
            log.warn("I do not know how to handle metadata filters (yet)");
        }
    }
    
    /**
     * Return the local part of the XSI type of the element.
     * 
     * @param element the element to inspect
     * @return the type as a String or null
     */
    @Nullable protected String getLocalPartOfType(Element element) {
        final QName name = DomTypeSupport.getXSIType(element);
        if (null == name) {
            return null;
        }
        return name.getLocalPart();
    }

}
