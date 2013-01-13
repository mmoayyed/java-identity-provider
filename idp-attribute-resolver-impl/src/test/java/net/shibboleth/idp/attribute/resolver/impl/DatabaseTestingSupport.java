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

package net.shibboleth.idp.attribute.resolver.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;

import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.LoggerFactory;
import org.hsqldb.jdbc.JDBCDataSource;
import org.slf4j.Logger;

import com.google.common.io.CharStreams;

/**
 *
 */
public class DatabaseTestingSupport  {

    static Logger log = LoggerFactory.getLogger(DatabaseTestingSupport.class);

    protected static void InitializeDataSource(@Nullable String initializingSQLFile, DataSource source) {
        
        final String file = StringSupport.trimOrNull(initializingSQLFile);
        
        if (null == file) {
            return;
        }

        final InputStream is = DatabaseTestingSupport.class.getResourceAsStream(file);
        
        if (null == is) {
            log.warn("Could not locate SQL file called {} ", file);
            return;
        }
        String sql;
        try {
            sql = StringSupport.trimOrNull(CharStreams.toString(new InputStreamReader(is)));
        } catch (IOException e) {
            log.warn("Could not read SQL file called {}.", file);
            return;
        }
        
        if (null == sql) {
            log.warn("SQL file called {} was empty.", file);
            return;
        }
        
        log.debug("Applying SQL: \n {}", sql);
        
        try {
            Connection dbConn = source.getConnection();
            Statement statement = dbConn.createStatement();
            
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            log.warn("Could not contact data source {} or execute commands: {}", source, e);
            return;            
        }
    }
    
    public static DataSource GetMockDataSource(@Nullable String initializingSQLFile, @Nonnull String identifier) {
        
        return GetDataSourceFromUrl(initializingSQLFile, "jdbc:hsqldb:mem:" + identifier);
    }

    public static DataSource GetDataSourceFromHsqlServer(@Nullable String initializingSQLFile, @Nonnull String server) {
        
        return GetDataSourceFromUrl(initializingSQLFile, "jdbc:hsqldb:hsql:" + server);
    }

        JDBCDataSource jdbcSource = new JDBCDataSource();


    protected static DataSource GetDataSourceFromUrl(String initializingSQLFile, String JdbcUri) {
        JDBCDataSource jdbcSource = new JDBCDataSource();

        jdbcSource.setUrl(JdbcUri);
        jdbcSource.setUser("SA");
        jdbcSource.setPassword("");

        InitializeDataSource(initializingSQLFile, jdbcSource);
        
        return jdbcSource;
    }
}
