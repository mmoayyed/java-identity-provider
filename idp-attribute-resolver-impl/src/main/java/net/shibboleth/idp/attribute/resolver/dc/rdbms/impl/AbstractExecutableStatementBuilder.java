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

package net.shibboleth.idp.attribute.resolver.dc.rdbms.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.dc.ExecutableSearchBuilder;
import net.shibboleth.idp.attribute.resolver.dc.rdbms.ExecutableStatement;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Basis of statement builder. The derived classes just have to provide the per request sql string.
 */
public abstract class AbstractExecutableStatementBuilder extends AbstractInitializableComponent implements
        ExecutableSearchBuilder<ExecutableStatement> {

    /** Query timeout. */
    @Nonnull private Duration queryTimeout;

    /** Constructor. */
    public AbstractExecutableStatementBuilder() {
        queryTimeout = Duration.ofSeconds(5);
    }
    
    /**
     * Gets the timeout of the SQL query.
     * 
     * @return timeout of the SQL query
     */
    @Nonnull public Duration getQueryTimeout() {
        return queryTimeout;
    }

    /**
     * Sets the timeout of the SQL query.
     * 
     * @param timeout of the SQL query
     */
    public void setQueryTimeout(@Nonnull final Duration timeout) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(timeout, "Query timeout cannot be null");
        Constraint.isFalse(timeout.isNegative(), "Query timeout cannot be negative");
        
        queryTimeout = timeout;
    }

    /**
     * Method to return the query SQL.
     * 
     * @param resolutionContext the context of the resolution
     * @param dependencyAttributes made available to the executable search
     * @throws ResolutionException if we get an error building the exception.
     * 
     * @return the SQL string
     */
    protected abstract String getSQLQuery(@Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final Map<String, List<IdPAttributeValue>> dependencyAttributes) throws ResolutionException;

    /** {@inheritDoc} */
    @Override public ExecutableStatement build(@Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final Map<String, List<IdPAttributeValue>> dependencyAttributes) throws ResolutionException {
        final String query = getSQLQuery(resolutionContext, dependencyAttributes);

        return new ExecutableStatement() {

            /** {@inheritDoc} */
            @Nullable public String getResultCacheKey() {
                return query;
            }

            /** {@inheritDoc} */
            @Nonnull public ResultSet execute(@Nonnull final Connection connection) throws SQLException {
                final Statement stmt = connection.createStatement();
                stmt.setQueryTimeout((int) queryTimeout.toSeconds());
                return stmt.executeQuery(query);
            }

            /** {@inheritDoc} */
            public String toString() {
                return query;
            }
        };
    }

}
