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
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.impl.dc.AbstractSearchDataConnector;
import net.shibboleth.idp.attribute.resolver.impl.dc.ValidationException;
import net.shibboleth.idp.attribute.resolver.impl.dc.Validator;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/** A {@link BaseDataConnector} that queries a relation database in order to retrieve attribute data. */
public class RdbmsDataConnector extends AbstractSearchDataConnector<ExecutableStatement> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(RdbmsDataConnector.class);

    /** JDBC data source for retrieving {@link Connection}s. */
    private DataSource dataSource;

    /**
     * Constructor.
     */
    public RdbmsDataConnector() {
        setValidator(new DefaultValidator());
        setMappingStrategy(new StringResultMappingStrategy());
    }

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

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (dataSource == null) {
            throw new ComponentInitializationException("Data connector '" + getId()
                    + "': no data source was configured");
        }

        try {
            getValidator().validate();
        } catch (ValidationException e) {
            log.error("Data connector '{}': invalid connector configuration", getId(), e);
            throw new ComponentInitializationException("Data connector '" + getId()
                    + "': invalid connector configuration", e);
        }
    }

    /** {@inheritDoc} */
    protected void doValidate() throws ComponentValidationException {
        try {
            getValidator().validate();
        } catch (ValidationException e) {
            log.error("Data connector '{}': invalid connector configuration", getId(), e);
            throw new ComponentValidationException("Data connector '" + getId() + "': invalid connector configuration",
                    e);
        }
    }

    /**
     * Attempts to retrieve the attribute from the database.
     * 
     * @param statement statement used to retrieve data from the database
     * 
     * @return attributes gotten from the database
     * 
     * @throws ResolutionException thrown if there is a problem retrieving data from the database or transforming that
     *             data into {@link Attribute}s
     */
    protected Optional<Map<String, Attribute>> retrieveAttributes(final ExecutableStatement statement)
            throws ResolutionException {

        if (statement == null) {
            throw new ResolutionException("Executable statement cannot be null");
        }
        Connection connection = null;
        ResultSet queryResult = null;
        try {
            connection = dataSource.getConnection();
            queryResult = statement.execute(connection);
            log.trace("Data connector '{}': search returned {}", getId(), queryResult);

            if (!queryResult.isBeforeFirst()) {
                if (isNoResultAnError()) {
                    throw new ResolutionException("No attributes returned from query");
                } else {
                    return Optional.<Map<String, Attribute>> absent();
                }
            }
            return getMappingStrategy().map(queryResult);
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

    /** Validator that opens a connection. */
    public class DefaultValidator implements Validator {

        /** {@inheritDoc} */
        public void validate() throws ValidationException {
            Connection connection = null;
            try {
                connection = dataSource.getConnection();
                if (connection == null) {
                    throw new ValidationException("Data connector '" + getId()
                            + "': unable to retrieve connections from configured data source");
                }
            } catch (SQLException e) {
                if (e.getSQLState() != null) {
                    log.error("Data connector '{}': invalid connector configuration; SQL state: {}, SQL Code: {}",
                            new Object[] {getId(), e.getSQLState(), e.getErrorCode(), e});
                } else {
                    log.error("Data connector '{}': invalid connector configuration", getId(), e);
                }
                throw new ValidationException("Data connector '" + getId() + "': invalid connector configuration", e);
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
    }
}