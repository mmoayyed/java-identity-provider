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

package net.shibboleth.idp.attribute.resolver.dc.ldap.impl;

import javax.annotation.Nonnull;

import org.ldaptive.Connection;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.LdapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.resolver.dc.ValidationException;
import net.shibboleth.idp.attribute.resolver.dc.Validator;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Validator implementation that invokes {@link Connection#open()} to determine if the ConnectionFactory is properly
 * configured.
 */
public class ConnectionFactoryValidator implements Validator {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ConnectionFactoryValidator.class);

    /** Connection factory to validate. */
    @Nonnull private ConnectionFactory connectionFactory;
    
    /** Whether validate should throw, default value is <code>true</code>. */
    private boolean throwOnValidateError;
       
    /**
     * Sets the connection factory.
     *
     * @param factory the connection factory
     */
    @Nonnull public void setConnectionFactory(@Nonnull final ConnectionFactory factory) {
        connectionFactory = Constraint.isNotNull(factory, "Connection factory must be non-null");
    }

    /**
     * Returns the connection factory.
     *
     * @return connection factory
     */
    @Nonnull public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /** {@inheritDoc} */
    public void setThrowValidateError(final boolean what) {
        throwOnValidateError = what;
    }

    /** {@inheritDoc} */
    public boolean isThrowValidateError() {
        return throwOnValidateError;
    }

    /** {@inheritDoc} */
    @Override public void validate() throws ValidationException {
        if (connectionFactory == null) {
            log.error("No connection factory installed");
            if (isThrowValidateError()) {
                throw new ValidationException("Connection factory is not set");
            }
        } else {
            assert connectionFactory != null;
            Connection connection = null;
            try {
                connection = connectionFactory.getConnection();
                if (connection == null) {
                    log.error("Unable to retrieve connections from configured connection factory");
                    if (isThrowValidateError()) {
                        throw new LdapException("Unable to retrieve connection from connection factory");
                    }
                } else {
                    connection.open();
                }
            } catch (final LdapException e) {
                log.error("Connection factory validation failed", e);
                if (isThrowValidateError()) {
                    throw new ValidationException(e);
                }
            } finally {
                if (connection != null) {
                    connection.close();
                }
            }
        }
    }
}
