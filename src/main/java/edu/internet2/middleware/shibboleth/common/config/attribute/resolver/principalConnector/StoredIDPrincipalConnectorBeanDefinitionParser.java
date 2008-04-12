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

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.opensaml.xml.util.DatatypeHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring bean definition for transient principal connectors.
 */
public class StoredIDPrincipalConnectorBeanDefinitionParser extends BasePrincipalConnectrBeanDefinitionParser {

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(PrincipalConnectorNamespaceHandler.NAMESPACE_URI, "StoredID");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element arg0) {
        return StoredIDPrincipalConnectorFactoryBean.class;
    }

    /** {@inheritDoc} */
    protected void doParse(String pluginId, Element pluginConfig, Map<QName, List<Element>> pluginConfigChildren,
            BeanDefinitionBuilder pluginBuilder, ParserContext parserContext) {
        super.doParse(pluginId, pluginConfig, pluginConfigChildren, pluginBuilder, parserContext);

        pluginBuilder.addPropertyReference("idProducer", DatatypeHelper.safeTrimOrNullString(pluginConfig
                .getAttributeNS(null, "storedIdDataConnectorRef")));
    }
}