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

package edu.internet2.middleware.shibboleth.common.config.resolver.dataConnector;

import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ResolutionPlugIn;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.RDBMSColumnDescriptor;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.RDBMSDataConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.RDBMSDataConnector.DATA_TYPES;

/**
 * Spring bean definition parser for configuring a {@link RDBMSDataConnector}.
 */
public class RDBMSDataConnectorBeanDefinitionParser extends BaseDataConnectorBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(RDBMSDataConnectorNamespaceHandler.NAMESPACE, "RelationalDatabase");

    /** ContainerManagedApplication element name. */
    public static final QName CONTAINER_MANAGED_CONNECTION_ELEMENT_NAME = new QName(
            RDBMSDataConnectorNamespaceHandler.NAMESPACE, "ContainerManagedConnection");

    /** ApplicationManagedApplication element name. */
    public static final QName APPLICATION_MANAGED_CONNECTION_ELEMENT_NAME = new QName(
            RDBMSDataConnectorNamespaceHandler.NAMESPACE, "ApplicationManagedConnection");

    /** QueryTemplate element name. */
    public static final QName QUERY_TEMPLATE_ELEMENT_NAME = new QName(RDBMSDataConnectorNamespaceHandler.NAMESPACE,
            "QueryTemplate");

    /** Column element name. */
    public static final QName COLUMN_ELEMENT_NAME = new QName(RDBMSDataConnectorNamespaceHandler.NAMESPACE, "Column");

    /** Class logger. */
    private static Logger log = Logger.getLogger(RDBMSDataConnectorBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class<? extends ResolutionPlugIn> getBeanClass(Element element) {
        return RDBMSDataConnector.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        builder.setInitMethodName("initialize");
        
        String id = DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null, "id"));
        if (log.isInfoEnabled()) {
            log.info("Configuring data connector " + id);
        }
        builder.addPropertyValue("id", id);

        Map<QName, List<Element>> children = XMLHelper.getChildElements(element);

        processConnectionManagement(id, children, builder);

        String validationQuery = DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null, "validationQuery"));
        if (log.isDebugEnabled()) {
            log.debug("Data connector " + id + " database connection validation query: " + validationQuery);
        }
        builder.addConstructorArg(validationQuery);

        boolean cacheResults = XMLHelper.getAttributeValueAsBoolean(element.getAttributeNodeNS(null, "cacheResults"));
        if (log.isDebugEnabled()) {
            log.debug("Data connector " + id + " cache results: " + cacheResults);
        }
        builder.addConstructorArg(cacheResults);

        processesQueryTemplate(id, children, builder);
        processColumnDescriptors(id, children, builder);

        boolean useSP = XMLHelper.getAttributeValueAsBoolean(element.getAttributeNodeNS(null,
                "queryUsesStoredProcedure"));
        if (log.isDebugEnabled()) {
            log.debug("Data connector " + id + " query uses stored procedures: " + useSP);
        }
        builder.addPropertyValue("usesStoredProcedure", useSP);

        boolean readOnlyCtx = XMLHelper.getAttributeValueAsBoolean(element.getAttributeNodeNS(null,
                "readOnlyConnection"));
        if (log.isDebugEnabled()) {
            log.debug("Data connector " + id + " connections are read only: " + readOnlyCtx);
        }
        builder.addPropertyValue("connectionReadOnly", readOnlyCtx);

        builder.addPropertyReference("templateEngine", DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null,
                "templateEngine")));
    }

    /**
     * Processes the connection management configuraiton.
     * 
     * @param connectorId ID of this data connector
     * @param configElements configuration elements for this connector
     * @param builder bean definition builder
     */
    protected void processConnectionManagement(String connectorId, Map<QName, List<Element>> configElements,
            BeanDefinitionBuilder builder) {
        DataSource dataSource;

        List<Element> cmc = configElements.get(CONTAINER_MANAGED_CONNECTION_ELEMENT_NAME);
        if (cmc != null && cmc.get(0) != null) {
            dataSource = buildContainerManagedConnection(connectorId, cmc.get(0));
        } else {
            dataSource = buildApplicationManagedConnection(connectorId, configElements.get(
                    APPLICATION_MANAGED_CONNECTION_ELEMENT_NAME).get(0));
        }

        builder.addConstructorArg(dataSource);
    }

    /**
     * Builds a JDBC {@link DataSource} from a ContainerManagedConnection configuration element.
     * 
     * @param connectorId ID of this data connector
     * @param cmc the container managed configuration element
     * 
     * @return the built data source
     */
    protected DataSource buildContainerManagedConnection(String connectorId, Element cmc) {
        String jndiResource = cmc.getAttributeNS(null, "resourceName");
        jndiResource = DatatypeHelper.safeTrim(jndiResource);

        Hashtable<String, String> initCtxProps = buildProperties(XMLHelper.getChildElementsByTagNameNS(cmc,
                RDBMSDataConnectorNamespaceHandler.NAMESPACE, "JNDIConnectionProperty"));
        try {
            InitialContext initCtx = new InitialContext(initCtxProps);
            DataSource dataSource = (DataSource) initCtx.lookup(jndiResource);
            if (log.isDebugEnabled()) {
                log.debug("Retrieved data source for data connector " + connectorId + " from JNDI location "
                        + jndiResource + " using properties " + initCtxProps);
            }
            return dataSource;
        } catch (NamingException e) {
            String error = "Unable to retrieve data source for data connector " + connectorId + " from JNDI location "
                    + jndiResource + " using properties " + initCtxProps;
            log.error(error, e);
            return null;
        }
    }

    /**
     * Builds a JDBC {@link DataSource} from an ApplicationManagedConnection configuration element.
     * 
     * @param connectorId ID of this data connector
     * @param amc the application managed configuration element
     * 
     * @return the built data source
     */
    protected DataSource buildApplicationManagedConnection(String connectorId, Element amc) {
        ComboPooledDataSource datasource = new ComboPooledDataSource();

        String driverClass = DatatypeHelper.safeTrim(amc.getAttributeNS(null, "jdbcDriver"));
        try {
            datasource.setDriverClass(driverClass);
            datasource.setJdbcUrl(DatatypeHelper.safeTrim(amc.getAttributeNS(null, "jdbcURL")));
            datasource.setUser(DatatypeHelper.safeTrim(amc.getAttributeNS(null, "jdbcUserName")));
            datasource.setPassword(DatatypeHelper.safeTrim(amc.getAttributeNS(null, "jdbcPassword")));

            datasource.setAcquireIncrement(Integer.parseInt(DatatypeHelper.safeTrim(amc.getAttributeNS(null,
                    "poolAcquireIncrement"))));
            datasource.setAcquireRetryAttempts(Integer.parseInt(DatatypeHelper.safeTrim(amc.getAttributeNS(null,
                    "poolAcquireRetryAttempts"))));
            datasource.setAcquireRetryDelay(Integer.parseInt(DatatypeHelper.safeTrim(amc.getAttributeNS(null,
                    "poolAcquireRetryDelay"))));
            datasource.setBreakAfterAcquireFailure(XMLHelper.getAttributeValueAsBoolean(amc.getAttributeNodeNS(null,
                    "poolBreakAfterAcquireFailure")));

            datasource.setMinPoolSize(Integer
                    .parseInt(DatatypeHelper.safeTrim(amc.getAttributeNS(null, "poolMinSize"))));
            datasource.setMaxPoolSize(Integer
                    .parseInt(DatatypeHelper.safeTrim(amc.getAttributeNS(null, "poolMaxSize"))));
            datasource.setMaxIdleTime(Integer.parseInt(DatatypeHelper.safeTrim(amc.getAttributeNS(null,
                    "poolMaxIdleTime"))));
            datasource.setIdleConnectionTestPeriod(Integer.parseInt(DatatypeHelper.safeTrim(amc.getAttributeNS(null,
                    "poolIdleTestPeriod"))));
            if (log.isDebugEnabled()) {
                log.debug("Created data source for data connector " + connectorId + " with properties "
                        + datasource.getProperties());
            }
            return datasource;
        } catch (PropertyVetoException e) {
            if (log.isDebugEnabled()) {
                log.error("Unable to create data source for data connector " + connectorId + " with JDBC driver class "
                        + driverClass);
            }
            return null;
        }
    }

    /**
     * Processes the QueryTemplate configuration element.
     * 
     * @param connectorId ID of this data connector
     * @param configElements configuration elements
     * @param builder the bean definition builder
     */
    protected void processesQueryTemplate(String connectorId, Map<QName, List<Element>> configElements,
            BeanDefinitionBuilder builder) {
        List<Element> queryTemplateElems = configElements.get(QUERY_TEMPLATE_ELEMENT_NAME);
        String queryTempalte = DatatypeHelper.safeTrimOrNullString(queryTemplateElems.get(0).getTextContent());
        if (log.isDebugEnabled()) {
            log.debug("Data connector " + connectorId + " query template: " + queryTempalte);
        }
        builder.addPropertyValue("queryTemplate", queryTempalte);
    }

    /**
     * Processes the Column descriptor configuration elements.
     * 
     * @param connectorId ID of this data connector
     * @param configElements configuration elements
     * @param builder the bean definition parser
     */
    protected void processColumnDescriptors(String connectorId, Map<QName, List<Element>> configElements,
            BeanDefinitionBuilder builder) {
        Map<String, RDBMSColumnDescriptor> columnDescriptors = new HashMap<String, RDBMSColumnDescriptor>();

        RDBMSColumnDescriptor columnDescriptor;
        String columnName;
        String attributeId;
        String dataType;
        if (configElements.containsKey(COLUMN_ELEMENT_NAME)) {
            for (Element columnElem : configElements.get(COLUMN_ELEMENT_NAME)) {
                columnName = columnElem.getAttributeNS(null, "columnName");
                attributeId = columnElem.getAttributeNS(null, "attributeID");
                dataType = columnElem.getAttributeNS(null, "type");
                columnDescriptor = new RDBMSColumnDescriptor(columnName, attributeId, DATA_TYPES.valueOf(dataType));
                columnDescriptors.put(columnName, columnDescriptor);
            }

            if (log.isDebugEnabled()) {
                log.debug("Data connector " + connectorId + " column descriptors: " + columnDescriptors.values());
            }
            builder.addPropertyValue("columnDescriptors", columnDescriptors);
        }
    }

    /**
     * Builds a hash from PropertyType elements.
     * 
     * @param propertyElements properties elements
     * 
     * @return properties extracted from elements, key is the property name.
     */
    protected Hashtable<String, String> buildProperties(List<Element> propertyElements) {
        if (propertyElements == null || propertyElements.size() < 1) {
            return null;
        }

        Hashtable<String, String> properties = new Hashtable<String, String>();

        String propName;
        String propValue;
        for (Element propertyElement : propertyElements) {
            propName = DatatypeHelper.safeTrim(propertyElement.getAttributeNS(null, "name"));
            propValue = DatatypeHelper.safeTrim(propertyElement.getAttributeNS(null, "value"));
            properties.put(propName, propValue);
        }

        return properties;
    }
}