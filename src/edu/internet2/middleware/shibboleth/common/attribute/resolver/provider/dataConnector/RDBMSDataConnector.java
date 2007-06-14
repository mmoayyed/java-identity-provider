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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.opensaml.xml.util.DatatypeHelper;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
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
        BigDecimal, Boolean, Byte, ByteArray, Date, Double, Float, Integer, Long, Object, Short, String, Time, Timestamp, URL
    };

    /** Class logger. */
    private static Logger log = Logger.getLogger(RDBMSDataConnector.class);

    /** Indicates whether this connector has been initialized. */
    private boolean initialized;

    /** JDBC data source for retrieving connections. */
    private DataSource dataSource;

    /** Whether the JDBC connection is read-only. */
    private boolean readOnlyConnection;

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
    private Map<String, RDBMSColumnDescriptor> columnDescriptors;

    /** Query result cache. [principalName => [query => attributeResultSetReference]] */
    private Map<String, Map<String, SoftReference<Map<String, BaseAttribute>>>> resultsCache;

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
        super();
        initialized = false;
        dataSource = source;
        validationQuery = DatatypeHelper.safeTrimOrNullString(validation);
        cacheResults = resultCaching;
        usesStoredProcedure = false;
        columnDescriptors = new HashMap<String, RDBMSColumnDescriptor>();
    }

    /**
     * Intializes the connector and prepares it for use.
     */
    public void initialize() {
        registerTemplate();
        if (cacheResults) {
            resultsCache = new HashMap<String, Map<String, SoftReference<Map<String, BaseAttribute>>>>();
        }
        initialized = true;
    }

    /**
     * Gets whether this data connector uses read-only connections.
     * 
     * @return whether this data connector uses read-only connections
     */
    public boolean isConnectionReadOnly() {
        return readOnlyConnection;
    }

    /**
     * Sets whether this data connector uses read-only connections.
     * 
     * @param isReadOnly whether this data connector uses read-only connections
     */
    public void setConnectionReadOnly(boolean isReadOnly) {
        readOnlyConnection = isReadOnly;
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
    public TemplateEngine getTemplateEngine() {
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
    }

    /**
     * Gets the set of column descriptors used to deal with result set data. The name of the database column is the
     * map's key. This list is unmodifiable.
     * 
     * @return column descriptors used to deal with result set data
     */
    public Map<String, RDBMSColumnDescriptor> getColumnDescriptor() {
        return columnDescriptors;
    }

    /** {@inheritDoc} */
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof LogoutEvent) {
            LogoutEvent logoutEvent = (LogoutEvent) event;
            if (cacheResults) {
                resultsCache.remove(logoutEvent.getUserSession().getPrincipalName());
            }
        }
    }

    /** {@inheritDoc} */
    public Map<String, BaseAttribute> resolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        String query = queryCreator.createStatement(queryTemplateName, resolutionContext,
                getDataConnectorDependencyIds(), getDataConnectorDependencyIds());

        if (log.isDebugEnabled()) {
            log.debug("Data connector " + getId() + " resolving attributes with query: " + query);
        }

        Map<String, BaseAttribute> resolvedAttributes = null;

        resolvedAttributes = retrieveAttributesFromCache(resolutionContext.getAttributeRequestContext()
                .getPrincipalName(), query);

        if (resolvedAttributes == null) {
            resolvedAttributes = retrieveAttributesFromDatabase(query);
        }

        if (cacheResults) {
            Map<String, SoftReference<Map<String, BaseAttribute>>> individualCache = resultsCache.get(resolutionContext
                    .getAttributeRequestContext().getPrincipalName());
            if (individualCache == null) {
                individualCache = new HashMap<String, SoftReference<Map<String, BaseAttribute>>>();
                resultsCache.put(resolutionContext.getAttributeRequestContext().getPrincipalName(), individualCache);
            }
            individualCache.put(query, new SoftReference<Map<String, BaseAttribute>>(resolvedAttributes));
        }

        return resolvedAttributes;
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
    public void clearCache() {
        if (initialized && cacheResults) {
            resultsCache.clear();
        }
    }

    /** Registers the query template with template engine. */
    protected void registerTemplate() {
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
    protected Map<String, BaseAttribute> retrieveAttributesFromCache(String princpal, String query)
            throws AttributeResolutionException {
        if (!cacheResults) {
            return null;
        }

        Map<String, SoftReference<Map<String, BaseAttribute>>> queryCache = resultsCache.get(princpal);
        if (queryCache != null) {
            SoftReference<Map<String, BaseAttribute>> cachedAttributes = queryCache.get(query);
            if (cachedAttributes != null) {
                if (log.isDebugEnabled()) {
                    log.debug("RDBMS Data Connector " + getId() + ": Fetched attributes from cache for principal "
                            + princpal + " with query " + query);
                }
                return cachedAttributes.get();
            }
        }

        return null;
    }

    /**
     * Attempts to retrieve the attribute from the database.
     * 
     * @param query query used to get the attributes
     * 
     * @return attributes gotten from the database
     * 
     * @throws AttributeResolutionException thrown if there is a problem retrieving data from the database or
     *             transforming that data into {@link BaseAttribute}s
     */
    protected Map<String, BaseAttribute> retrieveAttributesFromDatabase(String query) throws AttributeResolutionException {
        Map<String, BaseAttribute> resolvedAttributes;
        Connection connection = null;
        ResultSet queryResult = null;
        try {
            connection = dataSource.getConnection();
            if (readOnlyConnection) {
                connection.setReadOnly(true);
            }
            if (log.isDebugEnabled()) {
                log.debug("RDBMS Data Connector " + getId() + ": Querying database for attributes with query: "
                                + query);
            }
            queryResult = connection.createStatement().executeQuery(query);
            resolvedAttributes = processResultSet(queryResult);
            if (log.isDebugEnabled()) {
                log.debug("RDBMS Data Connector " + getId() + ": Retrieved  " + resolvedAttributes.size()
                        + " attributes: " + resolvedAttributes.keySet());
            }
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
     * Converts a SQL query results set into a set of {@link BaseAttribute}s.
     * 
     * @param resultSet the result set to convert
     * 
     * @return the resultant set of attributes
     * 
     * @throws AttributeResolutionException thrown if there is a problem converting the result set into attributes
     */
    protected Map<String, BaseAttribute> processResultSet(ResultSet resultSet) throws AttributeResolutionException {
        Map<String, BaseAttribute> attributes = new HashMap<String, BaseAttribute>();
        try {
            if (!resultSet.next()) {
                return attributes;
            }

            ResultSetMetaData resultMD = resultSet.getMetaData();
            int numOfCols = resultMD.getColumnCount();
            String columnName;
            RDBMSColumnDescriptor columnDescriptor;
            BaseAttribute attribute;
            Set attributeValueSet;
            do {
                for (int i = 1; i <= numOfCols; i++) {
                    columnName = resultMD.getColumnName(i);
                    columnDescriptor = columnDescriptors.get(columnName);

                    if (columnDescriptor == null || columnDescriptor.getAttributeID() == null) {
                        attribute = attributes.get(columnName);
                        if (attribute == null) {
                            attribute = new BasicAttribute(columnName);
                        }
                    } else {
                        attribute = attributes.get(columnDescriptor.getAttributeID());
                        if (attribute == null) {
                            attribute = new BasicAttribute(columnDescriptor.getAttributeID());
                        }
                    }

                    attributes.put(attribute.getId(), attribute);
                    attributeValueSet = attribute.getValues();
                    if (columnDescriptor == null || columnDescriptor.getDataType() == null) {
                        attributeValueSet.add(resultSet.getObject(i));
                    } else {
                        addValueByType(attributeValueSet, columnDescriptor.getDataType(), resultSet, i);
                    }
                }
            } while (resultSet.next());
        } catch (SQLException e) {
            log.error("RDBMS Data Connector " + getId() + ": Unable to read data from query result set", e);
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
            case Integer:
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
}