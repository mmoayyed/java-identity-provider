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

package edu.internet2.middleware.shibboleth.common.config.attribute.resolver.principalConnector;

import org.opensaml.xml.util.DatatypeHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.config.attribute.resolver.AbstractResolutionPlugInBeanDefinitionParser;

/**
 * Base spring bean definition parser for principal connectors. PrincipalConnector implementations should provide
 * a custom BeanDefinitionParser by extending this class and overriding the doParse() method to parse any additional
 * attributes or elements it requires. Standard attributes and elements defined by the ResolutionPlugIn and
 * PrincipalConnector schemas will automatically attempt to be parsed.
 */
public abstract class BasePrincipalConnectrBeanDefinitionParser extends AbstractResolutionPlugInBeanDefinitionParser {

    /** NameID format attribute name. */
    public static final String NAMEID_FORMAT_ATTRIBUTE_NAME = "nameIDFormat";

    /** {@inheritDoc} */
    protected void doParse(String pluginId, Element pluginConfig,
            java.util.Map<javax.xml.namespace.QName, java.util.List<Element>> pluginConfigChildren,
            BeanDefinitionBuilder pluginBuilder, ParserContext parserContext) {
        
        pluginBuilder.addPropertyValue("nameIdFormat", DatatypeHelper.safeTrimOrNullString(pluginConfig.getAttributeNS(
                null, NAMEID_FORMAT_ATTRIBUTE_NAME)));
    }
}