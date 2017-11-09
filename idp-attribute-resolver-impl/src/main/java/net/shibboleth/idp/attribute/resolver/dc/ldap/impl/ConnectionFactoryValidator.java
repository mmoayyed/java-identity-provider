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

import net.shibboleth.idp.attribute.resolver.dc.ValidationException;
import net.shibboleth.idp.attribute.resolver.dc.Validator;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;

import org.ldaptive.Connection;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.LdapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator implementation that invokes {@link Connection#open()} to determine if the ConnectionFactory is properly
 * configured.
 */
public class ConnectionFactoryValidator extends AbstractInitializableComponent implements Validator {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ConnectionFactoryValidator.class);

    /** Connection factory to validate. */
    @Nonnull private ConnectionFactory connectionFactory;
    
    /** Whether validate should throw, default value is <code>true</code>. */
    private boolean throwOnValidateError = true;
    
    /**
     * Creates a new connection factory validator.
     *
     * @param factory to validate
     * @deprecated - use the property setters
     */
    @Deprecated public ConnectionFactoryValidator(@Nonnull final ConnectionFactory factory) {
        DeprecationSupport.warn(ObjectType.METHOD, "ConnectionFactoryValidator(ConnectionFactory)", null, null);
        LoggerFactory.getLogger(ConnectionFactoryValidator.class).warn("Using Deprecated Constructor");
        setConnectionFactory(factory);
        setThrowValidateError(true);
        try {
            initialize();
        } catch (final ComponentInitializationException e) {
            throw new ConstraintViolationException("Invalid parameterization to deprecated structure");
        }
    }

    /**
     * Creates a new connection factory validator.
     * 
     * @param factory to validate
     * @param throwOnError whether {@link #validate()} should throw or log errors
     * @deprecated - use the property setters
     */
    @Deprecated public ConnectionFactoryValidator(@Nonnull final ConnectionFactory factory, 
            final boolean throwOnError) {
        DeprecationSupport.warn(ObjectType.METHOD, "ConnectionFactoryValidator(ConnectionFactory, boolean)",
                null, null);
        setConnectionFactory(factory);
        setThrowValidateError(throwOnError);
        try {
            initialize();
        } catch (final ComponentInitializationException e) {
            throw new ConstraintViolationException("Invalid parameterization to deprecated structure");
        }
    }
    
    /**
     * Constructor.  
     *
     */
    public ConnectionFactoryValidator() {  
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        Constraint.isNotNull(connectionFactory, "Connection factory must be non-null");
        super.doInitialize();
    }
    
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

    /**
     * Sets whether {@link #validate()} should throw or log errors.
     *
     * @param what whether {@link #validate()} should throw or log errors
     */
    public void setThrowValidateError(final Boolean what) {
        if (null != what) {
            throwOnValidateError = what;
        }
    }

    /**
     * Returns whether {@link #validate()} should throw or log errors.
     *
     * @return whether {@link #validate()} should throw or log errors
     */
    public boolean isThrowValidateError() {
        return throwOnValidateError;
    }

    /** {@inheritDoc} */
    @Override public void validate() throws ValidationException {
        Connection connection = null;
        try {
            connection = connectionFactory.getConnection();
            if (connection == null) {
                log.error("Unable to retrieve connections from configured connection factory");
                if (throwOnValidateError) {
                    throw new LdapException("Unable to retrieve connection from connection factory");
                }
            }
            connection.open();
        } catch (final LdapException e) {
            log.error("Connection factory validation failed", e);
            if (throwOnValidateError) {
                throw new ValidationException(e);
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }
}
