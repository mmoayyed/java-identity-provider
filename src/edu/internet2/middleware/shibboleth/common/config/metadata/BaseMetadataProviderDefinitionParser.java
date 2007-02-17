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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/**
 * Base class for metadata provider configuration parser.
 */
public abstract class BaseMetadataProviderDefinitionParser extends AbstractBeanDefinitionParser {

    /** Name of attribute for requiring valid metadata. */
    public static final String REQUIRE_VALID_METADATA_ATTRIBUTE_NAME = "requireValidMetadata";

    /** Local name of metadata filter. */
    public static final String METADATA_FILTER_ELEMENT_LOCAL_NAME = "MetadataFilter";

    /**
     * Parses the metadata provider config element and sets the require validate metadata and metadata filter
     * properties.
     * 
     * @param builder metadata provider definition builder
     * @param element configuration of the provider
     * @param context current parsing context
     */
    protected void parseCommonConfig(BeanDefinitionBuilder builder, Element element, ParserContext context) {

        String requireValidMDStr = element.getAttributeNS(null, REQUIRE_VALID_METADATA_ATTRIBUTE_NAME);
        if (DatatypeHelper.safeEquals(requireValidMDStr, "true") || DatatypeHelper.safeEquals(requireValidMDStr, "1")) {
            builder.addPropertyValue("requireValidMetadata", Boolean.TRUE);
        } else {
            builder.addPropertyValue("requireValidMetadata", Boolean.FALSE);
        }

        NodeList childElems = element.getElementsByTagNameNS(MetadataNamespaceHandler.NAMESPACE,
                METADATA_FILTER_ELEMENT_LOCAL_NAME);
        if (childElems.getLength() > 0) {
            Element filterElem = (Element) childElems.item(0);
            BeanDefinition filterDef = SpringConfigurationUtils.parseCustomElement(filterElem, context);
            builder.addPropertyValue("metadataFilter", filterDef);
        }
    }
}