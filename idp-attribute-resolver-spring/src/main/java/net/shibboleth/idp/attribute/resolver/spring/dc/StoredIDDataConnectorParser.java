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

package net.shibboleth.idp.attribute.resolver.spring.dc;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.impl.dc.StoredIDDataConnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring bean definition parser for {@link StoredIDDataConnector}.
 */
public class StoredIDDataConnectorParser extends BaseComputedIDDataConnectorParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(DataConnectorNamespaceHandler.NAMESPACE, "StoredId");

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(StoredIDDataConnectorParser.class);

    /** {@inheritDoc} */
    protected Class<StoredIDDataConnector> getBeanClass(Element element) {
        return StoredIDDataConnector.class;
    }

    /** {@inheritDoc} */
    protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder, "storedId");
        log.debug("doParse {}", config);
        builder.addPropertyValue("dataSource", getDataSource(config));
        // TODO set queryTimeout?
    }
    
    /**
     * Get the dataSource from the configuration.
     * @param config the DOM element under consideration. 
     * @return the DataSource
     */
    protected DataSource getDataSource(@Nonnull Element config) {
        final Element springBeans = getSpringBeansElement(config);
        if (springBeans == null) {
            log.debug("parsing v2 configuration");
            final ManagedConnectionParser parser = new ManagedConnectionParser(config);
            return parser.createDataSource();
        } else {
            final BeanFactory beanFactory = createBeanFactory(springBeans);
            return beanFactory.getBean(DataSource.class);
        }
    }
}