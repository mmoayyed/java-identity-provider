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

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Assert;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/** A simple {@link ExecutableStatement} that executes a static SQL query string. */
public class SqlStringExecutableStatement implements ExecutableStatement {

    /** SQL query executed by this statement. */
    private final String sqlQuery;

    /**
     * Constructor.
     * 
     * @param query SQL query executed by this statement
     */
    public SqlStringExecutableStatement(@Nonnull @NotEmpty final String query) {
        sqlQuery = Assert.isNotNull(StringSupport.trimOrNull(query), "SQL query string can not be null or empty");
    }

    /** {@inheritDoc} */
    public String getResultCacheKey() {
        return sqlQuery;
    }

    /** {@inheritDoc} */
    public ResultSet execute(Connection connection) throws SQLException {
        return connection.createStatement().executeQuery(sqlQuery);
    }
}