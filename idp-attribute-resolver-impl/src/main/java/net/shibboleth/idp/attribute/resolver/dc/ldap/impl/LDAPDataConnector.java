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

import java.security.GeneralSecurityException;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.ldaptive.Connection;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.LdapException;
import org.ldaptive.SearchExecutor;
import org.ldaptive.SearchResult;
import org.ldaptive.pool.ConnectionPool;
import org.ldaptive.pool.PooledConnectionFactory;
import org.ldaptive.ssl.SSLContextInitializer;
import org.ldaptive.ssl.SslConfig;
import org.ldaptive.ssl.X509SSLContextInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.dc.ValidationException;
import net.shibboleth.idp.attribute.resolver.dc.Validator;
import net.shibboleth.idp.attribute.resolver.dc.impl.AbstractSearchDataConnector;
import net.shibboleth.idp.attribute.resolver.dc.ldap.ExecutableSearchFilter;
import net.shibboleth.idp.attribute.resolver.dc.ldap.SearchResultMappingStrategy;
import net.shibboleth.idp.attribute.resolver.dc.ldap.StringAttributeValueMappingStrategy;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * A {@link net.shibboleth.idp.attribute.resolver.DataConnector} that queries an LDAP in order to retrieve attribute
 * data.
 */
public class LDAPDataConnector extends AbstractSearchDataConnector<ExecutableSearchFilter,SearchResultMappingStrategy> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(LDAPDataConnector.class);

    /** Factory for retrieving LDAP connections. */
    private ConnectionFactory connectionFactory;

    /** For executing LDAP searches. */
    private SearchExecutor searchExecutor;

    /** Whether the default validator is being used. */
    private boolean defaultValidator = true;

    /** Whether the default mapping strategy is being used. */
    private boolean defaultMappingStrategy = true;

    /**
     * Constructor.
     */
    public LDAPDataConnector() {
    }

    /**
     * Gets the connection factory for retrieving {@link Connection}s.
     * 
     * @return connection factory for retrieving {@link Connection}s
     */
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * Sets the connection factory for retrieving {@link Connection}s.
     * 
     * @param factory connection factory for retrieving {@link Connection}s
     */
    public void setConnectionFactory(@Nonnull final ConnectionFactory factory) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        connectionFactory = Constraint.isNotNull(factory, "LDAP connection factory can not be null");
    }

    /**
     * Gets the search executor for executing searches.
     * 
     * @return search executor for executing searches
     */
    public SearchExecutor getSearchExecutor() {
        return searchExecutor;
    }

    /**
     * Sets the search executor for executing searches.
     * 
     * @param executor search executor for executing searches
     */
    public void setSearchExecutor(@Nonnull final SearchExecutor executor) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        searchExecutor = Constraint.isNotNull(executor, "LDAP search executor can not be null");
    }

    /** {@inheritDoc} */
    @Override public void setValidator(@Nonnull final Validator validator) {
        super.setValidator(validator);
        if (validator instanceof ConnectionFactoryValidator && connectionFactory != null) {
            ((ConnectionFactoryValidator) validator).setConnectionFactory(connectionFactory);
        }
        defaultValidator = false;
    }

    /** {@inheritDoc} */
    @Override public void setMappingStrategy(@Nonnull final SearchResultMappingStrategy strategy) {
        super.setMappingStrategy(strategy);
        defaultMappingStrategy = false;
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        if (connectionFactory == null) {
            throw new ComponentInitializationException(getLogPrefix() + " No connection factory was configured");
        }
        if (searchExecutor == null) {
            throw new ComponentInitializationException(getLogPrefix() + " No search executor was configured");
        }

        if (defaultValidator) {
            final ConnectionFactoryValidator validator = new ConnectionFactoryValidator();
            validator.setConnectionFactory(connectionFactory);
            super.setValidator(validator);
        }
        if (defaultMappingStrategy) {
            super.setMappingStrategy(new StringAttributeValueMappingStrategy());
        }
        super.doInitialize();

        // validator should defer to data connector fail-fast-initialize during #initialize
        final boolean throwValidateError = getValidator().isThrowValidateError();
        try {
            getValidator().setThrowValidateError(isFailFastInitialize());
            getValidator().validate();
        } catch (final ValidationException e) {
            log.error("{} Invalid connector configuration", getLogPrefix(), e);
            if (isFailFastInitialize()) {
                // Should always follow this leg.
                throw new ComponentInitializationException(getLogPrefix() + " Invalid connector configuration", e);
            }
        } finally {
            getValidator().setThrowValidateError(throwValidateError);
        }
        policeForJVMTrust();
    }
    
    /** {@inheritDoc} */
    @Override protected void doDestroy() {
        if (connectionFactory instanceof PooledConnectionFactory) {
            final ConnectionPool pool = ((PooledConnectionFactory) connectionFactory).getConnectionPool();
            if (pool != null) {
                log.info("{} Closing LDAP connection pool", getLogPrefix());
                pool.close();
            }
        }
        super.doDestroy();
    }

// CheckStyle: CyclomaticComplexity OFF
    /** Police TLS for JVM trust.
     * @throws ComponentInitializationException if we detect an SSL issue
     */
    private void policeForJVMTrust() throws ComponentInitializationException {
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection();
            if (conn == null) {
                log.debug("{} No connection to probe", getLogPrefix());
                return;
            }
            final ConnectionConfig connConfig = conn.getConnectionConfig();
            if (connConfig.getUseStartTLS() ||
                    connConfig.getUseSSL() ||
                    connConfig.getLdapUrl().toLowerCase().contains("ldaps://")) {
                final SslConfig sslConfig = connConfig.getSslConfig();
                if (sslConfig != null) {
                    final SSLContextInitializer cxtInit = sslConfig.getCredentialConfig() != null ?
                        sslConfig.getCredentialConfig().createSSLContextInitializer() : null;
                    if (cxtInit instanceof X509SSLContextInitializer) {
                        if (((X509SSLContextInitializer) cxtInit).getTrustCertificates() == null) {
                            throw new ComponentInitializationException(getLogPrefix() +
                                    ": Use of default JVM trust store not supported");
                        }
                    }
                }
            }
        } catch (final GeneralSecurityException | LdapException e) {
            log.debug("{} Failed to inspect TLS implementation", getLogPrefix(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (final Exception e) {
                    log.debug("{} Error closing LDAP connection", getLogPrefix(), e);
                }
            }
        }
    }
 // CheckStyle: CyclomaticComplexity ON

    /**
     * Attempts to retrieve attributes from the LDAP.
     * 
     * @param filter search filter used to retrieve data from the LDAP
     * 
     * @return search result from the LDAP
     * 
     * @throws ResolutionException thrown if there is a problem retrieving data from the LDAP
     */
    @Override @Nullable protected Map<String, IdPAttribute> retrieveAttributes(final ExecutableSearchFilter filter)
            throws ResolutionException {

        if (filter == null) {
            throw new ResolutionException(getLogPrefix() + " Search filter cannot be null");
        }
        try {
            final SearchResult result = filter.execute(searchExecutor, connectionFactory);
            log.trace("{} Search returned {}", getLogPrefix(), result);
            return getMappingStrategy().map(result);
        } catch (final LdapException e) {
            throw new ResolutionException(getLogPrefix() + " Unable to execute LDAP search", e);
        }
    }

}
