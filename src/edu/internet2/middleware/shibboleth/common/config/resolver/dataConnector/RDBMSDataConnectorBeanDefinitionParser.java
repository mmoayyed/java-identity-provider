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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import javax.xml.namespace.QName;

import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ResolutionPlugIn;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.RDBMSColumnDescriptor;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.RDBMSDataConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.RDBMSDataConnector.DATA_TYPES;

/**
 * 
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

    /** {@inheritDoc} */
    protected Class<? extends ResolutionPlugIn> getBeanClass(Element element) {
        return RDBMSDataConnector.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        Map<QName, List<Element>> children = XMLHelper.getChildElements(element);

        processConnectionManagement(children, builder);
        builder.addConstructorArg(element.getAttributeNS(null, "validationQuery"));
        builder.addConstructorArg(XMLHelper.getAttributeValueAsBoolean(element.getAttributeNodeNS(null, "cacheResults")));
        
        processesQueryTemplate(children, builder);
        
        builder.addPropertyValue("usesStoredProcedure", XMLHelper.getAttributeValueAsBoolean(element.getAttributeNodeNS(null, "queryUsesStoredProcedure")));
        builder.addPropertyValue("connectionReadOnly",  XMLHelper.getAttributeValueAsBoolean(element.getAttributeNodeNS(null, "readOnlyConnection")));
    }

    protected void processConnectionManagement(Map<QName, List<Element>> configElements, BeanDefinitionBuilder builder) {
        DataSource dataSource;

        List<Element> cmc = configElements.get(CONTAINER_MANAGED_CONNECTION_ELEMENT_NAME);
        if (cmc != null && cmc.get(0) != null) {
            dataSource = buildContainerManagedConnection(cmc.get(0));
        } else {
            dataSource = buildApplicationManagedConnection(configElements.get(
                    APPLICATION_MANAGED_CONNECTION_ELEMENT_NAME).get(0));
        }

        builder.addConstructorArg(dataSource);
    }

    protected DataSource buildContainerManagedConnection(Element cmc) {
        return null;
    }

    protected DataSource buildApplicationManagedConnection(Element amc) {
        return null;
    }
    
    protected void processesQueryTemplate(Map<QName, List<Element>> configElements, BeanDefinitionBuilder builder){
        List<Element> queryTemplateElems = configElements.get(QUERY_TEMPLATE_ELEMENT_NAME);
        String queryTempalte = queryTemplateElems.get(0).getTextContent();
        builder.addPropertyValue("queryTemplate", queryTempalte);
    }
    
    protected void processColumnDescriptors(Map<QName, List<Element>> configElements, BeanDefinitionBuilder builder){
        Map<String, RDBMSColumnDescriptor> columnDescriptors = new HashMap<String, RDBMSColumnDescriptor>();
        
        RDBMSColumnDescriptor columnDescriptor;
        String columnName;
        String attributeId;
        String dataType;
        if(configElements.containsKey(COLUMN_ELEMENT_NAME)){
            for(Element columnElem : configElements.get(COLUMN_ELEMENT_NAME)){
                columnName = columnElem.getAttributeNS(null, "columnName");
                attributeId = columnElem.getAttributeNS(null, "attributeID");
                dataType = columnElem.getAttributeNS(null, "type");
                columnDescriptors.put(columnName, new RDBMSColumnDescriptor(columnName, attributeId, DATA_TYPES.valueOf(dataType)));
            }
            
            builder.addPropertyValue("columnDescriptors", columnDescriptors);
        }
    }
}