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

package net.shibboleth.idp.attribute.resolver.spring.dc.impl;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.namespace.QName;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

/** Utility class for parsing v2 managed connection configuration. */
public class ManagedConnectionParser {

    /** resolver:ContainerManagedConnection.*/
    @Nonnull public static final QName CONTAINER_MANAGED_CONNECTION_RESOLVER =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "ContainerManagedConnection");
    
    /** resolver:BeanManagedConnection.*/
    @Nonnull public static final QName BEAN_MANAGED_CONNECTION_RESOLVER =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "BeanManagedConnection");

    /** resolver:SimpleManagedConnection.*/
    @Nonnull public static final QName SIMPLE_MANAGED_CONNECTION_RESOLVER =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "SimpleManagedConnection");

    /** Logger. */
    @Nonnull private static final Logger LOG = LoggerFactory.getLogger(ManagedConnectionParser.class);
    

    /** Data source XML element. */
    @Nonnull private final Element configElement;
    
    /**
     * Creates a new ManagedConnectionParser with the supplied element.
     * 
     * @param config element
     */
    public ManagedConnectionParser(@Nonnull final Element config) {
        Constraint.isNotNull(config, "Element cannot be null");
        configElement = config;
    }

    /**
     * Creates a data source bean definition from a v2 XML configuration.
     * 
     * @return data source bean definition
     */
    @Nullable public BeanDefinition createDataSource() {
        final List<Element> containerManagedElements =
                ElementSupport.getChildElements(configElement, CONTAINER_MANAGED_CONNECTION_RESOLVER);

        final List<Element> simpleManagedElements =
                ElementSupport.getChildElements(configElement, SIMPLE_MANAGED_CONNECTION_RESOLVER);

        if ((simpleManagedElements.size() + containerManagedElements.size() ) > 1) {
            LOG.warn("Only one <SimpleManagedConnection> or <ContainerManagedConnection> is allowed per DataConnector");
        }
        
        if (!simpleManagedElements.isEmpty()) {
            return createSimpleManagedDataSource(simpleManagedElements.get(0));
        }

        if (!containerManagedElements.isEmpty()) {
            return createContainerManagedDataSource(containerManagedElements.get(0));
        }

        return null;
    }

    /**
     * Creates a container managed data source bean definition.
     * 
     * @param containerManagedElement to parse
     * 
     * @return data source bean definition
     */
    @Nonnull protected BeanDefinition createContainerManagedDataSource(@Nonnull final Element containerManagedElement) {
        Constraint.isNotNull(containerManagedElement, "ContainerManagedConnection element cannot be null");

        final String resourceName =
                AttributeSupport.getAttributeValue(containerManagedElement, new QName("resourceName"));

        final BeanDefinitionBuilder dataSource =
                BeanDefinitionBuilder.rootBeanDefinition(ManagedConnectionParser.class, "buildDataSource");
        dataSource.addConstructorArgValue(resourceName);
        return dataSource.getBeanDefinition();
    }

    /**
     * Creates an simple managed data source bean definition based on dbcp2.
     *
     * @param simpleManagedElement to parse
     *
     * @return data source bean definition
     */

    @Nonnull protected BeanDefinition createSimpleManagedDataSource(
            @Nonnull final Element simpleManagedElement) {
        Constraint.isNotNull(simpleManagedElement, "SimpleManagedConnection element cannot be null");
        final BeanDefinitionBuilder dataSource =
                BeanDefinitionBuilder.genericBeanDefinition(BasicDataSource.class);

        final String driverName = StringSupport.trimOrNull(AttributeSupport.getAttributeValue(simpleManagedElement,
                null, "jdbcDriver"));
        if (driverName == null) {
            LOG.warn("<SimpleManagedConnection> jdbcDriver attribute should be present and non empty");
            throw new BeanCreationException("<SimpleManagedConnection> jdbcDriver attribute should be"+
                                            " present and non empty");
        }
        dataSource.addPropertyValue("driverClassName", driverName);

        final String url = StringSupport.trimOrNull(AttributeSupport.getAttributeValue(simpleManagedElement,
                null, "jdbcURL"));
        if (url == null) {
            LOG.warn("<SimpleManagedConnection> jdbcURL attribute should be present and non empty");
            throw new BeanCreationException("<SimpleManagedConnection> jdbcURL attribute should be present"
                                            + " and non empty");
        }
        dataSource.addPropertyValue("url", url);

        final String user = AttributeSupport.getAttributeValue(simpleManagedElement, null, "jdbcUserName");
        if (user != null && !"".equals(user)) {
            dataSource.addPropertyValue("username", user);
        }

        final String password = AttributeSupport.getAttributeValue(simpleManagedElement, null, "jdbcPassword");
        if (password != null && !"".equals(password)) {
            dataSource.addPropertyValue("password", password);
        }
        dataSource.addPropertyValue("maxTotal", "20");
        dataSource.addPropertyValue("maxIdle", "5");
        dataSource.addPropertyValue("maxWaitMillis", "5000");
        return dataSource.getBeanDefinition();
    }


    /**
     * Factory builder a container managed datasource.
     *
     * @param resourceName of the data source
     *
     * @return data source or null if the data source cannot be looked up
     */
    @Nullable public static DataSource buildDataSource(final String resourceName) {
        try {
            final InitialContext initCtx = new InitialContext();
            final DataSource dataSource = (DataSource) initCtx.lookup(resourceName);
            return dataSource;
        } catch (final NamingException e) {
            LOG.error("Managed data source '{}' could not be found", resourceName, e);
            return null;
        }
    }

    /**
     * Get the bean ID of an externally defined data source.
     * 
     * @param config the config element
     * @return data source bean ID
     */
    @Nullable public static String getBeanDataSourceID(@Nonnull final Element config) {
        
        final List<Element> beanManagedElements = ElementSupport.getChildElements(config,
                BEAN_MANAGED_CONNECTION_RESOLVER); 
        
        if (beanManagedElements.isEmpty()) {
            return null;
        }
        
        if (beanManagedElements.size() > 1) {
            LOG.warn("Only one <BeanManagedConnection> should be specified; the first one has been consulted");
        }

        final List<Element> managedElements = ElementSupport.getChildElements(config, 
                CONTAINER_MANAGED_CONNECTION_RESOLVER);
        managedElements.addAll(ElementSupport.getChildElements(config, SIMPLE_MANAGED_CONNECTION_RESOLVER));
        
        if (managedElements.size() > 0) {
            LOG.warn("<BeanManagedConnection> is incompatible with <ContainerManagedConnection>"
                    + "or <SimpleManagedConnection>. The <BeanManagedConnection> has been used");
        }
        
        return StringSupport.trimOrNull(ElementSupport.getElementContentAsString(beanManagedElements.get(0)));
    }

}