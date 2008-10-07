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

package edu.internet2.middleware.shibboleth.common.config.service;

import javax.xml.namespace.QName;

import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/** Bean definition parser for IdP services config root element. */
public class ServicesBeanDefinitionParser implements BeanDefinitionParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(ServiceNamespaceHandler.NAMESPACE, "Services");

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(ServiceNamespaceHandler.NAMESPACE, "ServicesType");

    /** {@inheritDoc} */
    public BeanDefinition parse(Element config, ParserContext context) {
        SpringConfigurationUtils.parseCustomElements(XMLHelper.getChildElementsByTagNameNS(config,
                ServiceNamespaceHandler.NAMESPACE, "Service"), context);
        return null;
    }
}