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

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.opensaml.xml.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/** Bean definition parser for IdP services config root element. */
public class ServicesBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(ServiceNamespaceHandler.NAMESPACE, "Services");

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(ServiceNamespaceHandler.NAMESPACE, "ServicesType");

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ServicesBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext context, BeanDefinitionBuilder builder) {
        log.info("Beginning to load service configurations");
        Map<QName, List<Element>> configChildren = XMLHelper.getChildElements(config);
        List<Element> children = configChildren.get(new QName(ServiceNamespaceHandler.NAMESPACE, "Service"));
        SpringConfigurationUtils.parseCustomElements(children, context);
        log.info("Finished loading service configurations");
    }
    
    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }
}