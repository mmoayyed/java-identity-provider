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
import java.sql.Statement;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.resolver.AttributeRecipientContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.impl.dc.ExecutableSearchBuilder;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * An {@link ExecutableStatementBuilder} that generates the SQL statement to be executed by invoking
 * {@link String#format(String, Object...) with {@link AttributeRecipientContext#getPrincipal()}.
 */
public class FormatExecutableStatementBuilder implements ExecutableSearchBuilder<ExecutableStatement> {

    /** SQL query string. */
    private final String sqlQuery;

    /** Query timeout. */
    private int queryTimeout;

    /**
     * Constructor.
     * 
     * @param query to search the database
     */
    public FormatExecutableStatementBuilder(@Nonnull final String query) {
        sqlQuery = Constraint.isNotNull(query, "SQL query can not be null");
    }

    /**
     * Constructor.
     * 
     * @param query to search the database
     * @param timeout search timeout
     */
    public FormatExecutableStatementBuilder(@Nonnull final String query, @Nonnull final int timeout) {
        sqlQuery = Constraint.isNotNull(query, "SQL query can not be null");
        queryTimeout = (int) Constraint.isGreaterThanOrEqual(0, timeout, "Query timeout must be greater than zero");
    }

    /** 
     * Gets the timeout of the SQL query.
     * 
     * @return timeout of the SQL query in seconds
     */
    public int getQueryTimeout() {
        return queryTimeout;
    }

    /** 
     * Sets the timeout of the SQL query.
     * 
     * @param timeout of the SQL query in seconds
     */
    public void setQueryTimeout(final int timeout) {
        queryTimeout = timeout;
    }

    /** {@inheritDoc} */
    public ExecutableStatement build(AttributeResolutionContext resolutionContext) throws ResolutionException {
        final AttributeRecipientContext subContext = resolutionContext.getSubcontext(AttributeRecipientContext.class);
        final String query = String.format(sqlQuery, subContext);

        return new ExecutableStatement() {

            /** {@inheritDoc} */
            @Nonnull public String getResultCacheKey() {
                return query;
            }

            /** {@inheritDoc} */
            @Nonnull public ResultSet execute(@Nonnull Connection connection) throws SQLException {
                final Statement stmt = connection.createStatement();
                stmt.setQueryTimeout(queryTimeout);
                return stmt.executeQuery(query);
            }

            /** {@inheritDoc} */
            public String toString() {
                return query;
            }
        };
    }
}