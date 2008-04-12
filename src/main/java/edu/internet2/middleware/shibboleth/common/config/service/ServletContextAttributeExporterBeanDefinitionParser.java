/*
 * Copyright 2008 University Corporation for Advanced Internet Development, Inc.
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

import java.util.ArrayList;

import javax.xml.namespace.QName;

import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/** Bean parser for service servlet attribute exporter service. */
public class ServletContextAttributeExporterBeanDefinitionParser extends AbstractServiceBeanDefinitionParser {

    /** Type name. */
    public static final QName TYPE_NAME = new QName(ServiceNamespaceHandler.NAMESPACE,
            "ServletContextAttributeExporter");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return ServletContextAttributeExporter.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element configElement, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(configElement, parserContext, builder);

        ArrayList<String> services = new ArrayList<String>();
        for (String dependency : XMLHelper
                .getAttributeValueAsList(configElement.getAttributeNodeNS(null, "depends-on"))) {
            services.add(dependency);
        }
        builder.addConstructorArg(services);
    }

    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }
}