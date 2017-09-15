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

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.dc.ValidationException;
import net.shibboleth.idp.attribute.resolver.dc.Validator;
import net.shibboleth.idp.attribute.resolver.dc.impl.AbstractSearchDataConnector;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;

import org.ldaptive.Connection;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.LdapException;
import org.ldaptive.SearchExecutor;
import org.ldaptive.SearchResult;
import org.ldaptive.ssl.X509SSLContextInitializer;
import org.ldaptive.ssl.SslConfig;
import org.ldaptive.ssl.SSLContextInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        try {
            getValidator().validate();
        } catch (final ValidationException e) {
            log.error("{} Invalid connector configuration", getLogPrefix(), e);
            throw new ComponentInitializationException(getLogPrefix() + " Invalid connector configuration", e);
        }

        // TODO: remove deprecation warning in v4
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection();
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
                            DeprecationSupport.warn(
                                ObjectType.CONFIGURATION, "Use of default JVM trust store",
                                    getLogPrefix(), "trustFile attribute");
                        }
                    }
                }
            }
        } catch (final Exception e) {
            log.warn("{} Error inspecting SSL configuration", getLogPrefix(), e);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

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