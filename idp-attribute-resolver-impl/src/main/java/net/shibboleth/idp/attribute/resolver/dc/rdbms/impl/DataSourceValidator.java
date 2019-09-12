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
import java.sql.SQLException;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.resolver.dc.ValidationException;
import net.shibboleth.idp.attribute.resolver.dc.Validator;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Validator implementation that invokes {@link DataSource#getConnection()} to determine if the DataSource is properly
 * configured.
 */
public class DataSourceValidator extends AbstractInitializableComponent implements Validator {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(DataSourceValidator.class);

    /** JDBC data source to validate. */
    @NonnullAfterInit private DataSource dataSource;

    /** Whether validate should throw, default value is <code>true</code>. */
    private boolean throwOnValidateError;

    /**
     * Constructor.
     *
     */
    public DataSourceValidator() {
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (null == dataSource) {
            throw new  ComponentInitializationException("DataSourceValidator: Data Source should not be null");
        }
    }
    
    /**
     * Sets the data source.
     *
     * @param source the data source
     */
    public void setDataSource(@Nonnull final DataSource source) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        dataSource =  Constraint.isNotNull(source, "Data Source should not be null");
    }


    /**
     * Returns the data source.
     *
     * @return data source
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /** {@inheritDoc} */
    public void setThrowValidateError(final boolean value) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        throwOnValidateError = value;
    }


    /** {@inheritDoc} */
    public boolean isThrowValidateError() {
        return throwOnValidateError;
    }

    /** {@inheritDoc} */
    @Override public void validate() throws ValidationException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            if (connection == null) {
                log.error("Unable to retrieve connections from configured data source");
                if (isThrowValidateError()) {
                    throw new ValidationException("Unable to retrieve connections from configured data source");
                }
            }
        } catch (final SQLException e) {
            if (e.getSQLState() != null) {
                log.error("Datasource validation failed with SQL state: {}, SQL Code: {}",
                        new Object[] {e.getSQLState(), e.getErrorCode(), e});
            } else {
                log.error("Datasource validation failed", e);
            }
            if (isThrowValidateError()) {
                throw new ValidationException("Invalid connector configuration", e);
            }
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (final SQLException e) {
                log.error("Error closing database connection; SQL State: {}, SQL Code: {}",
                        new Object[] {e.getSQLState(), e.getErrorCode(), e});
            }
        }
    }
}
