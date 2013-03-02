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

package net.shibboleth.idp.attribute.resolver.impl.dc.rdbms;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseDataConnector;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;

/** A {@link BaseDataConnector} that queries a relation database in order to retrieve attribute data. */
public class RdbmsDataConnector extends BaseDataConnector {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(RdbmsDataConnector.class);

    /** JDBC data source for retrieving {@link Connection}s. */
    private DataSource dataSource;

    /** Builder used to create the statements executed against the database. */
    private ExecutableStatementBuilder statementBuilder;

    /** Strategy for mapping from a {@link ResultSet} to a collection of {@link Attribute}s. */
    private ResultMappingStrategy mappingStrategy;

    /** Whether an empty result set is an error. */
    private boolean noResultIsAnError;

    /** Query result cache. */
    private Cache<String, Optional<Map<String, Attribute>>> resultsCache;

    /**
     * Gets the JDBC data source for retrieving {@link Connection}s.
     * 
     * @return JDBC data source for retrieving {@link Connection}s
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Sets the JDBC data source for retrieving {@link Connection}s.
     * 
     * @param source JDBC data source for retrieving {@link Connection}s
     */
    public synchronized void setDataSource(@Nonnull final DataSource source) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        dataSource = Constraint.isNotNull(source, "JDBC data source can not be null");
    }

    /**
     * Gets the builder used to create the statements executed against the database.
     * 
     * @return builder used to create the statements executed against the database
     */
    public ExecutableStatementBuilder getExecutableStatementBuilder() {
        return statementBuilder;
    }

    /**
     * Sets the builder used to create the statements executed against the database.
     * 
     * @param builder builder used to create the statements executed against the database
     */
    public void setExecutableStatementBuilder(@Nonnull final ExecutableStatementBuilder builder) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        statementBuilder = Constraint.isNotNull(builder, "Executable statement builder can not be null");
    }

    /**
     * Gets the strategy for mapping from a {@link ResultSet} to a collection of {@link Attribute}s.
     * 
     * @return strategy for mapping from a {@link ResultSet} to a collection of {@link Attribute}s
     */
    public ResultMappingStrategy getResultMappingStrategy() {
        return mappingStrategy;
    }

    /**
     * Sets the strategy for mapping from a {@link ResultSet} to a collection of {@link Attribute}s.
     * 
     * @param strategy strategy for mapping from a {@link ResultSet} to a collection of {@link Attribute}s
     */
    public void setResultMappingStrategy(@Nonnull final ResultMappingStrategy strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        mappingStrategy = Constraint.isNotNull(strategy, "Result mapping strategy can not be null");
    }

    /**
     * Gets whether an empty result set is treated as an error.
     * 
     * @return whether an empty result set is treated as an error
     */
    public boolean isNoResultIsAnError() {
        return noResultIsAnError;
    }

    /**
     * Sets whether an empty result set is treated as an error.
     * 
     * @param isAnError whether an empty result set is treated as an error
     */
    public synchronized void setNoResultIsAnError(final boolean isAnError) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        noResultIsAnError = isAnError;
    }

    /**
     * Gets the cache used to cache search results.
     * 
     * @return cache used to cache search results
     */
    @Nonnull public Cache<String, Optional<Map<String, Attribute>>> getResultCache() {
        return resultsCache;
    }

    /**
     * Sets the cache used to cache search results. Note, all entries in the cache are invalidated prior to use.
     * 
     * @param cache cache used to cache search results
     */
    public void setResultsCache(@Nonnull final Cache<String, Optional<Map<String, Attribute>>> cache) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        cache.invalidateAll();
        resultsCache = cache;
    }

    /** {@inheritDoc} */
    protected Optional<Map<String, Attribute>> doDataConnectorResolve(AttributeResolutionContext resolutionContext)
            throws ResolutionException {
        ExecutableStatement statement = statementBuilder.build(resolutionContext);

        Optional<Map<String, Attribute>> resolvedAttributes = resultsCache.getIfPresent(statement.getResultCacheKey());
        if (resolvedAttributes == null) {
            resolvedAttributes = retrieveAttributesFromDatabase(statement);
            resultsCache.put(statement.getResultCacheKey(), resolvedAttributes);
        }

        return resolvedAttributes;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (dataSource == null) {
            throw new ComponentInitializationException("Data connector '" + getId()
                    + "': no data source was configured");
        }

        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            if (connection == null) {
                throw new ComponentInitializationException("Data connector '" + getId()
                        + "': unable to retrieve connections from configured data source");
            }
        } catch (SQLException e) {
            if (e.getSQLState() != null) {
                log.error("Data connector '{}': invalid connector configuration; SQL state: {}, SQL Code: {}",
                        new Object[] {getId(), e.getSQLState(), e.getErrorCode(), e});
            } else {
                log.error("Data connector '{}': invalid connector configuration", getId(), e);
            }
            throw new ComponentInitializationException("Data connector '" + getId()
                    + "': invalid connector configuration", e);
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.error("Data connector '{}': error closing database connection; SQL State: {}, SQL Code: {}",
                        new Object[] {getId(), e.getSQLState(), e.getErrorCode(), e});
            }
        }
    }

    /** {@inheritDoc} */
    protected void doValidate() throws ComponentValidationException {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            if (connection == null) {
                throw new ComponentValidationException("Data connector '" + getId()
                        + "': unable to retrieve connections from configured data source");
            }
        } catch (SQLException e) {
            if (e.getSQLState() != null) {
                log.error("Data connector '{}': invalid connector configuration; SQL state: {}, SQL Code: {}",
                        new Object[] {getId(), e.getSQLState(), e.getErrorCode(), e});
            } else {
                log.error("Data connector '{}': invalid connector configuration", getId(), e);
            }
            throw new ComponentValidationException("Data connector '" + getId() + "': invalid connector configuration",
                    e);
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.error("Data connector '{}': error closing database connection; SQL State: {}, SQL Code: {}",
                        new Object[] {getId(), e.getSQLState(), e.getErrorCode(), e});
            }
        }
    }

    /**
     * Attempts to retrieve the attribute from the database.
     * 
     * @param statement statement used to retrieve data from the database
     * 
     * @return attributes gotten from the database
     * 
     * @throws ResolutionException thrown if there is a problem retrieving data from the database or
     *             transforming that data into {@link Attribute}s
     */
    protected Optional<Map<String, Attribute>> retrieveAttributesFromDatabase(ExecutableStatement statement)
            throws ResolutionException {

        Connection connection = null;
        ResultSet queryResult = null;
        try {
            connection = dataSource.getConnection();
            queryResult = statement.execute(connection);

            Optional<Map<String, Attribute>> resolvedAttributes = mappingStrategy.map(queryResult);
            if (!resolvedAttributes.isPresent() && noResultIsAnError) {
                throw new ResolutionException("No attributes returned from query");
            }

            return resolvedAttributes;
        } catch (SQLException e) {
            throw new ResolutionException("Unable to execute SQL query", e);
        } finally {
            try {
                if (queryResult != null) {
                    queryResult.close();
                }

                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.debug("Data connector '{}': unable to close database connection; SQL State: {}, SQL Code: {}",
                        new Object[] {getId(), e.getSQLState(), e.getErrorCode()}, e);
            }
        }
    }
}