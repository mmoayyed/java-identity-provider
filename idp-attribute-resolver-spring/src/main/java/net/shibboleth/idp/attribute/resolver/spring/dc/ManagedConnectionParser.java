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

import java.beans.PropertyVetoException;
import java.util.Hashtable;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.namespace.QName;

import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.mchange.v2.c3p0.ComboPooledDataSource;

/** Utility class for parsing v2 managed connection configuration. */
public class ManagedConnectionParser {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ManagedConnectionParser.class);

    /** Data source XML element. */
    private final Element configElement;

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
     * Creates a data source from a v2 XML configuration.
     * 
     * @return data source
     */
    public DataSource createDataSource() {
        final Element containerManagedElement =
                ElementSupport.getFirstChildElement(configElement, new QName(
                        DataConnectorNamespaceHandler.NAMESPACE, "ContainerManagedConnection"));
        if (containerManagedElement != null) {
            return createContainerManagedDataSource(containerManagedElement);
        }

        final Element applicationManagedElement =
                ElementSupport.getFirstChildElement(configElement, new QName(
                        DataConnectorNamespaceHandler.NAMESPACE, "ApplicationManagedConnection"));
        return createApplicationManagedDataSource(applicationManagedElement);
    }

    /**
     * Creates a container managed data source.
     * 
     * @param containerManagedElement to parse
     * 
     * @return data source
     */
    @Nullable protected DataSource createContainerManagedDataSource(@Nonnull final Element containerManagedElement) {
        Constraint.isNotNull(containerManagedElement, "ContainerManagedConnection element cannot be null");

        final String resourceName =
                AttributeSupport.getAttributeValue(containerManagedElement, new QName("resourceName"));

        final Hashtable<String, String> ctxProps = new Hashtable<String, String>();
        final Element propertyElement =
                ElementSupport.getFirstChildElement(containerManagedElement, new QName(
                        DataConnectorNamespaceHandler.NAMESPACE, "JNDIConnectionProperty"));
        final List<Element> elements = ElementSupport.getChildElements(propertyElement);
        for (Element e : elements) {
            ctxProps.put(AttributeSupport.getAttributeValue(e, new QName("name")),
                    AttributeSupport.getAttributeValue(e, new QName("value")));
        }

        try {
            final InitialContext initCtx = new InitialContext(ctxProps);
            final DataSource dataSource = (DataSource) initCtx.lookup(resourceName);
            return dataSource;
        } catch (NamingException e) {
            return null;
        }
    }

    /**
     * Creates an application managed data source.
     * 
     * @param applicationManagedElement to parse
     * 
     * @return data source
     */
    // Checkstyle: CyclomaticComplexity OFF
    // Checkstyle: MethodLength OFF
    @Nonnull protected DataSource createApplicationManagedDataSource(
            @Nonnull final Element applicationManagedElement) {
        Constraint.isNotNull(applicationManagedElement, "ApplicationManagedConnection element cannot be null");
        final ComboPooledDataSource datasource = new ComboPooledDataSource();

        final String jdbcDriver =
                AttributeSupport.getAttributeValue(applicationManagedElement, new QName("jdbcDriver"));
        // JDBC driver must be loaded in order to register itself
        final ClassLoader classLoader = getClass().getClassLoader();
        try {
            classLoader.loadClass(jdbcDriver);
        } catch (ClassNotFoundException e) {
            log.error("JDBC driver could not be found");
        }
        try {
            datasource.setDriverClass(jdbcDriver);
            datasource.setJdbcUrl(AttributeSupport.getAttributeValue(applicationManagedElement,
                    new QName("jdbcURL")));
            datasource.setUser(AttributeSupport.getAttributeValue(applicationManagedElement, new QName(
                    "jdbcUserName")));
            datasource.setPassword(AttributeSupport.getAttributeValue(applicationManagedElement, new QName(
                    "jdbcPassword")));

            final String poolAcquireIncrement =
                    AttributeSupport
                            .getAttributeValue(applicationManagedElement, new QName("poolAcquireIncrement"));
            if (poolAcquireIncrement != null) {
                datasource.setAcquireIncrement(Integer.parseInt(poolAcquireIncrement));
            } else {
                datasource.setAcquireIncrement(3);
            }

            final String poolAcquireRetryAttempts =
                    AttributeSupport.getAttributeValue(applicationManagedElement, new QName(
                            "poolAcquireRetryAttempts"));
            if (poolAcquireRetryAttempts != null) {
                datasource.setAcquireRetryAttempts(Integer.parseInt(poolAcquireRetryAttempts));
            } else {
                datasource.setAcquireRetryAttempts(36);
            }

            final String poolAcquireRetryDelay =
                    AttributeSupport.getAttributeValue(applicationManagedElement,
                            new QName("poolAcquireRetryDelay"));
            if (poolAcquireRetryDelay != null) {
                datasource.setAcquireRetryDelay(Integer.parseInt(poolAcquireRetryDelay));
            } else {
                datasource.setAcquireRetryDelay(5000);
            }

            final Boolean poolBreakAfterAcquireFailure =
                    AttributeSupport.getAttributeValueAsBoolean(AttributeSupport.getAttribute(
                            applicationManagedElement, new QName("poolBreakAfterAcquireFailure")));
            if (poolBreakAfterAcquireFailure != null) {
                datasource.setBreakAfterAcquireFailure(poolBreakAfterAcquireFailure);
            } else {
                datasource.setBreakAfterAcquireFailure(true);
            }

            final String poolMinSize =
                    AttributeSupport.getAttributeValue(applicationManagedElement, new QName("poolMinSize"));
            if (poolMinSize != null) {
                datasource.setMinPoolSize(Integer.parseInt(poolMinSize));
            } else {
                datasource.setMinPoolSize(2);
            }

            final String poolMaxSize =
                    AttributeSupport.getAttributeValue(applicationManagedElement, new QName("poolMaxSize"));
            if (poolMaxSize != null) {
                datasource.setMaxPoolSize(Integer.parseInt(poolMaxSize));
            } else {
                datasource.setMaxPoolSize(50);
            }

            final String poolMaxIdleTime =
                    AttributeSupport.getAttributeValue(applicationManagedElement, new QName("poolMaxIdleTime"));
            if (poolMaxIdleTime != null) {
                datasource.setMaxIdleTime(Integer.parseInt(poolMaxIdleTime));
            } else {
                datasource.setMaxIdleTime(600);
            }

            final String poolIdleTestPeriod =
                    AttributeSupport.getAttributeValue(applicationManagedElement, new QName("poolIdleTestPeriod"));
            if (poolIdleTestPeriod != null) {
                datasource.setIdleConnectionTestPeriod(Integer.parseInt(poolIdleTestPeriod));
            } else {
                datasource.setIdleConnectionTestPeriod(180);
            }
        } catch (PropertyVetoException e) {
            log.error("Error setting data source property", e);
        }

        return datasource;
    }
    // Checkstyle: MethodLength ON
    // Checkstyle: CyclomaticComplexity ON
}
