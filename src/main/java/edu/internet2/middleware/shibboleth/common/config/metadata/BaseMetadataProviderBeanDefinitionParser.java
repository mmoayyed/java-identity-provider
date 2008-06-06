/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.config.metadata;

import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/**
 * Base class for metadata provider configuration parser.
 */
public abstract class BaseMetadataProviderBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(BaseMetadataProviderBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext parserContext, BeanDefinitionBuilder builder) {
        String id = DatatypeHelper.safeTrimOrNullString(config.getAttributeNS(null, "id"));
        log.debug("Parsing configuration for {} metadata provider with ID: {}", XMLHelper.getXSIType(config)
                .getLocalPart(), id);

        if (config.hasAttributeNS(null, "requireValidMetadata")) {
            boolean requireValidMDBool = XMLHelper.getAttributeValueAsBoolean(config.getAttributeNodeNS(null,
                    "requireValidMetadata"));
            if (log.isDebugEnabled()) {
                log.debug("Metadata provider {} requires valid metadata: {}", id, requireValidMDBool);
            }
            builder.addPropertyValue("requireValidMetadata", requireValidMDBool);
        }

        NodeList childElems = config.getElementsByTagNameNS(MetadataNamespaceHandler.NAMESPACE, "MetadataFilter");
        if (childElems.getLength() > 0) {
            Element filterElem = (Element) childElems.item(0);
            BeanDefinition filterDef = SpringConfigurationUtils.parseCustomElement(filterElem, parserContext);
            builder.addPropertyValue("metadataFilter", filterDef);
        }
    }
}