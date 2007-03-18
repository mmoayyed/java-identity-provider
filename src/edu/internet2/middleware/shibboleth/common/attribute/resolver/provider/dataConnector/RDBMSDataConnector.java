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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector;

import java.lang.ref.SoftReference;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.opensaml.xml.util.DatatypeHelper;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.session.LogoutEvent;

/**
 * A data connector that can retrieve information from a relational database through JDBC, version 3.
 */
public class RDBMSDataConnector extends BaseDataConnector implements ApplicationListener {

    /** Data types understood by this connector. */
    public static enum DATA_TYPES {
        BigDecimal, Boolean, Byte, ByteArray, Date, Double, Float, Int, Long, Object, Short, String, Time, Timestamp, URL
    };

    /** Class logger. */
    private static Logger log = Logger.getLogger(RDBMSDataConnector.class);

    /** JDBC data source for retrieving connections. */
    private DataSource dataSource;

    /** Whether queries might use stored procedures. */
    private boolean usesStoredProcedure;

    /** Whether to cache query results. */
    private boolean cacheResults;

    /** Query to use to validate connectivity with the database. */
    private String validationQuery;

    /** Name the query template is registered under with the statement creator. */
    private String queryTemplateName;

    /** Template that produces the query to use. */
    private String queryTemplate;

    /** Set of column descriptors for managing returned data. [columnName => colmentDescriptr] */
    private Map<String, ColumnDescriptor> columnDescriptors;

    /** Query result cache. [principalName => [query => attributeSetReference]] */
    private Map<String, Map<String, SoftReference<Set<Attribute>>>> resultsCache;

    /** Template engine used to change query template into actual query. */
    private TemplateEngine queryCreator;

    /**
     * Constructor.
     * 
     * @param source data source used to retrieve connections
     * @param validation query used to validate connections to the database, should be very fast
     * @param resultCaching whether query results should be cached
     */
    public RDBMSDataConnector(DataSource source, String validation, boolean resultCaching) {
        dataSource = source;
        validationQuery = validation;
        cacheResults = resultCaching;
        usesStoredProcedure = false;
        columnDescriptors = new HashMap<String, ColumnDescriptor>();
    }

    /**
     * Intializes the connector and prepares it for use.
     */
    public void initialize() {
        registerTemplate();
        if (cacheResults) {
            resultsCache = new HashMap<String, Map<String, SoftReference<Set<Attribute>>>>();
        }
    }

    /**
     * Gets whether queries made use stored procedures.
     * 
     * @return whether queries made use stored procedures
     */
    public boolean getUsesStoredProcedure() {
        return usesStoredProcedure;
    }

    /**
     * Sets whether queries made use stored procedures.
     * 
     * @param storedProcedure whether queries made use stored procedures
     */
    public void setUsesStoredProcedure(boolean storedProcedure) {
        usesStoredProcedure = storedProcedure;
    }

    /**
     * Gets whether to cache query results.
     * 
     * @return whether to cache query results
     */
    public boolean getCacheResults() {
        return cacheResults;
    }

    /**
     * Gets the query used to validate connectivity with the database.
     * 
     * @return query used to validate connectivity with the database
     */
    public String getValidationQuery() {
        return validationQuery;
    }

    /**
     * Gets the engine used to evaluate the query template.
     * 
     * @return engine used to evaluate the query template
     */
    public TemplateEngine getTemplateEneing() {
        return queryCreator;
    }

    /**
     * Sets the engine used to evaluate the query template.
     * 
     * @param engine engine used to evaluate the query template
     */
    public void setTemplateEngine(TemplateEngine engine) {
        queryCreator = engine;
        registerTemplate();
        clearCache();
    }

    /**
     * Gets the template used to create queries.
     * 
     * @return template used to create queries
     */
    public String getQueryTemplate() {
        return queryTemplate;
    }

    /**
     * Sets the template used to create queries.
     * 
     * @param template template used to create queries
     */
    public void setQueryTemplate(String template) {
        queryTemplate = template;
        clearCache();
    }

    /**
     * Gets the set of column descriptors used to deal with result set data. The name of the database column is the
     * map's key. This list is unmodifiable.
     * 
     * @return column descriptors used to deal with result set data
     */
    public Map<String, ColumnDescriptor> getColumnDescriptor() {
        return Collections.unmodifiableMap(columnDescriptors);
    }

    /**
     * Sets the column descriptors used to deal with result set data. The name of the database column is the map's key.
     * 
     * @param descriptors column descriptors used to deal with result set data
     */
    public void setColumnDescriptors(Map<String, ColumnDescriptor> descriptors) {
        columnDescriptors = new HashMap<String, ColumnDescriptor>(descriptors);
        clearCache();
    }

    /** {@inheritDoc} */
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof LogoutEvent) {
            LogoutEvent logoutEvent = (LogoutEvent) event;
            if (cacheResults) {
                resultsCache.remove(logoutEvent.getUserSession().getPrincipalID());
            }
        }
    }

    /** {@inheritDoc} */
    public Map<String, Attribute> resolve(ShibbolethResolutionContext resolutionContext) throws AttributeResolutionException {
        String query = queryCreator.createStatement(queryTemplateName, resolutionContext,
                getDataConnectorDependencyIds(), getDataConnectorDependencyIds());

        Set<Attribute> resolvedAttributes = null;

        resolvedAttributes = retrieveAttributesFromCache(resolutionContext.getAttributeRequestContext().getPrincipalName(), query);

        if (resolvedAttributes == null) {
            resolvedAttributes = retrieveAttributesFromDatabase(query);
        }

        if (cacheResults) {
            Map<String, SoftReference<Set<Attribute>>> individualCache = resultsCache.get(resolutionContext
                    .getAttributeRequestContext().getPrincipalName());
            if (individualCache == null) {
                individualCache = new HashMap<String, SoftReference<Set<Attribute>>>();
                resultsCache.put(resolutionContext.getAttributeRequestContext().getPrincipalName(), individualCache);
            }
            individualCache.put(query, new SoftReference<Set<Attribute>>(resolvedAttributes));
        }
        
        // TODO: this was kind of a quick and dirty way to get convert a Map.  Ideally the whole class 
        // should probably be updated to use Maps throughout
        Map<String, Attribute> attributeMap = new HashMap<String, Attribute>();
        for(Attribute attr : resolvedAttributes) {
            attributeMap.put(attr.getId(), attr);
        }

        return attributeMap;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        if (log.isDebugEnabled()) {
            log.debug("Validating RDBMS data connector " + getId() + " configuration.");
        }
        try {
            Connection connection = dataSource.getConnection();

            DatabaseMetaData dbmd = connection.getMetaData();
            if (!dbmd.supportsStoredProcedures() && usesStoredProcedure) {
                log.error("RDBMS data connector " + getId()
                        + " is configured to use stored procedures but database does not support them.");
                throw new AttributeResolutionException("RDBMS data connector " + getId()
                        + " is configured to use stored procedures but database does not support them.");
            }

            Statement validationStatement = connection.createStatement();
            ResultSet result = validationStatement.executeQuery(validationQuery);
            if (!result.first()) {
                log.error("Validation query for RDBMS data connector " + getId() + " did not return any results");
                throw new AttributeResolutionException("Validation query for RDBMS data connector " + getId()
                        + " did not return any results");
            }

            if (log.isDebugEnabled()) {
                log.debug("Validating RDBMS data connector " + getId() + " configuration is valid.");
            }
        } catch (SQLException e) {
            log.error("Unable to validate RDBMS data connector " + getId() + " configuration", e);
            throw new AttributeResolutionException("Unable to validate RDBMS data connector " + getId()
                    + " configuration", e);
        }
    }

    /** Clears the result cache. */
    protected void clearCache() {
        if (cacheResults) {
            resultsCache.clear();
        }
    }
    
    /** Registers the query template with template engine. */
    protected void registerTemplate(){
        queryTemplateName = "shibboleth.resolver.dc." + getId();
        queryCreator.registerTemplate(queryTemplateName, queryTemplate);
    }

    /**
     * Attempts to retrieve the attributes from the cache.
     * 
     * @param princpal the principal name of the user the attributes are for
     * @param query query used to generate the attributes
     * 
     * @return cached attributes
     * 
     * @throws AttributeResolutionException thrown if there is a problem retrieving data from the cache
     */
    protected Set<Attribute> retrieveAttributesFromCache(String princpal, String query)
            throws AttributeResolutionException {
        if (!cacheResults) {
            return null;
        }

        return resultsCache.get(princpal).get(query).get();
    }

    /**
     * Attempts to retrieve the attribute from the database.
     * 
     * @param query query used to get the attributes
     * 
     * @return attributes gotten from the database
     * 
     * @throws AttributeResolutionException thrown if there is a problem retrieving data from the database or
     *             transforming that data into {@link Attribute}s
     */
    protected Set<Attribute> retrieveAttributesFromDatabase(String query) throws AttributeResolutionException {
        Set<Attribute> resolvedAttributes;
        Connection connection = null;
        ResultSet queryResult = null;
        try {
            connection = dataSource.getConnection();
            queryResult = connection.createStatement().executeQuery(query);
            resolvedAttributes = processResultSet(queryResult);
            queryResult.close();
            connection.close();
            return resolvedAttributes;
        } catch (SQLException e) {
            log.error("RDBMS Data Connector " + getId() + ": Unable to execute SQL query\n" + query, e);
            throw new AttributeResolutionException("RDBMS Data Connector " + getId() + ": Unable to execute SQL query",
                    e);
        } finally {
            try {
                if (queryResult != null) {
                    queryResult.close();
                }

                if (connection != null) {
                    connection.close();
                }

            } catch (SQLException e) {
                log.error("RDBMS Data Connector " + getId() + ": Unable to close connection to database", e);
            }
        }
    }

    /**
     * Converts a SQL query results set into a set of {@link Attribute}s.
     * 
     * @param resultSet the result set to convert
     * 
     * @return the resultant set of attributes
     * 
     * @throws AttributeResolutionException thrown if there is a problem converting the result set into attributes
     */
    protected Set<Attribute> processResultSet(ResultSet resultSet) throws AttributeResolutionException {
        try {
            if (!resultSet.first()) {
                return new HashSet<Attribute>();
            }

            ResultSetMetaData resultMD = resultSet.getMetaData();
            int numOfCols = resultMD.getColumnCount();
            Map<String, Attribute> attributes = prepareAttributeSetFromResultSet(resultMD);

            // loop over result and add values to attributes
            String columnName;
            ColumnDescriptor columnDescriptor;
            Set attributeValueSet;
            while (resultSet.next()) {
                for (int i = 0; i < numOfCols; i++) {
                    columnName = resultMD.getColumnName(i);
                    columnDescriptor = columnDescriptors.get(columnName);

                    attributeValueSet = attributes.get(columnName).getValues();
                    if (columnDescriptor == null || columnDescriptor.getDataType() == null) {
                        attributeValueSet.add(resultSet.getObject(i));
                    } else {
                        addValueByType(attributeValueSet, columnDescriptor.getDataType(), resultSet, i);
                    }
                }
            }

        } catch (SQLException e) {
            log.error("RDBMS Data Connector " + getId() + ": Unable to read data from query result set");
        }
        return null;
    }

    /**
     * Prepares skeletal attributes ready to recieve values.
     * 
     * @param resultMetadata result set metadata
     * 
     * @return attributes ready to recieve values indexed by column name
     * 
     * @throws SQLException thrown if the given result set metadata can not be read
     */
    protected Map<String, Attribute> prepareAttributeSetFromResultSet(ResultSetMetaData resultMetadata)
            throws SQLException {
        int numOfCols = resultMetadata.getColumnCount();

        BasicAttribute attribute;
        HashMap<String, Attribute> attributes = new HashMap<String, Attribute>();
        String columnName = null;
        String attributeName = null;

        // Create attributes that can place values in when looping through the result set
        for (int i = 0; i < numOfCols; i++) {
            columnName = resultMetadata.getColumnName(i);
            if (columnDescriptors.containsKey(columnName)) {
                attributeName = columnDescriptors.get(columnName).getAttributeName();
            }
            if (DatatypeHelper.isEmpty(attributeName)) {
                attributeName = columnName;
            }

            attribute = new BasicAttribute();
            attribute.setId(attributeName);
            attributes.put(columnName, attribute);
        }

        return attributes;
    }

    /**
     * Adds a value extracted from the result set as a specific type into the value set.
     * 
     * @param valueSet set to add values into
     * @param type type the value should be extracted as
     * @param resultSet result set, on the current row, to extract the value from
     * @param columnIndex index of the column from which to extract the attribute
     * 
     * @throws SQLException thrown if value can not retrieved from the result set
     */
    protected void addValueByType(Set valueSet, DATA_TYPES type, ResultSet resultSet, int columnIndex)
            throws SQLException {
        switch (type) {
            case BigDecimal:
                valueSet.add(resultSet.getBigDecimal(columnIndex));
                break;
            case Boolean:
                valueSet.add(resultSet.getBoolean(columnIndex));
                break;
            case Byte:
                valueSet.add(resultSet.getByte(columnIndex));
                break;
            case ByteArray:
                valueSet.add(resultSet.getBytes(columnIndex));
                break;
            case Date:
                valueSet.add(resultSet.getDate(columnIndex));
                break;
            case Double:
                valueSet.add(resultSet.getDouble(columnIndex));
                break;
            case Float:
                valueSet.add(resultSet.getFloat(columnIndex));
                break;
            case Int:
                valueSet.add(resultSet.getInt(columnIndex));
                break;
            case Long:
                valueSet.add(resultSet.getLong(columnIndex));
                break;
            case Object:
                valueSet.add(resultSet.getObject(columnIndex));
                break;
            case Short:
                valueSet.add(resultSet.getShort(columnIndex));
                break;
            case Time:
                valueSet.add(resultSet.getTime(columnIndex));
                break;
            case Timestamp:
                valueSet.add(resultSet.getTimestamp(columnIndex));
                break;
            case URL:
                valueSet.add(resultSet.getURL(columnIndex));
                break;
            default:
                valueSet.add(resultSet.getString(columnIndex));
        }
    }

    /**
     * Describes how to express a given result set column as an attribute and value.
     */
    public class ColumnDescriptor {

        /** Name of the database column. */
        private String columnName;

        /** Name of the attribute to map the column to. */
        private String attributeName;

        /** Java data type to express the database value as. */
        private DATA_TYPES dataType;

        /**
         * Constructor.
         * 
         * @param column name of the database column
         * @param attribute name of the attribute to map the column to
         * @param type Java data type to express the database value as
         */
        public ColumnDescriptor(String column, String attribute, DATA_TYPES type) {
            columnName = column;
            attributeName = attribute;
            dataType = type;
        }

        /**
         * Gets the name of the database column.
         * 
         * @return name of the database column
         */
        public String getColumnName() {
            return columnName;
        }

        /**
         * Gets the name of the attribute to map the column to.
         * 
         * @return name of the attribute to map the column to
         */
        public String getAttributeName() {
            return attributeName;
        }

        /**
         * Gets the Java data type to express the database value as.
         * 
         * @return Java data type to express the database value as
         */
        public DATA_TYPES getDataType() {
            return dataType;
        }
    }
}