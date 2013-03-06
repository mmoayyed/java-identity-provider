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

package net.shibboleth.idp.attribute.resolver.impl.dc.ldap;

import java.util.Map;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.BaseDataConnector;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.ldaptive.Connection;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.LdapException;
import org.ldaptive.Response;
import org.ldaptive.SearchExecutor;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;

/** A {@link BaseDataConnector} that queries an LDAP in order to retrieve attribute data. */
public class LdapDataConnector extends BaseDataConnector {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(LdapDataConnector.class);

    /** Factory for retrieving LDAP connections. */
    private ConnectionFactory connectionFactory;

    /** For executing LDAP searches. */
    private SearchExecutor searchExecutor;

    /** Builder used to create the search filters executed against the LDAP. */
    private SearchFilterBuilder filterBuilder;

    /** Search filter for validating this connector. */
    private SearchFilter validateFilter;

    /** Strategy for mapping from a {@link SearchResult} to a collection of {@link Attribute}s. */
    private SearchResultMappingStrategy mappingStrategy = new StringAttributeValueMappingStrategy();

    /** Whether an empty result set is an error. */
    private boolean noResultIsAnError;

    /** Query result cache. */
    private Cache<String, Optional<Map<String, Attribute>>> resultsCache;

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
    public synchronized void setConnectionFactory(@Nonnull final ConnectionFactory factory) {
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
    public synchronized void setSearchExecutor(@Nonnull final SearchExecutor executor) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        searchExecutor = Constraint.isNotNull(executor, "LDAP search executor can not be null");
    }

    /**
     * Gets the builder used to create the search filters executed against the LDAP.
     * 
     * @return builder used to create the search filters executed against the LDAP
     */
    public SearchFilterBuilder getSearchFilterBuilder() {
        return filterBuilder;
    }

    /**
     * Sets the builder used to create the search filters executed against the LDAP.
     * 
     * @param builder builder used to create the search filters executed against the LDAP
     */
    public void setSearchFilterBuilder(@Nonnull final SearchFilterBuilder builder) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        filterBuilder = Constraint.isNotNull(builder, "Search filter builder can not be null");
    }

    /**
     * Gets the filter used to validate this connector.
     * 
     * @return filter used to validate this connector
     */
    public SearchFilter getValidateFilter() {
        return validateFilter;
    }

    /**
     * Sets the filter used to validate this connector.
     * 
     * @param filter used to validate this connector
     */
    public void setValidateFilter(@Nonnull final SearchFilter filter) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        validateFilter = Constraint.isNotNull(filter, "Validate search filter can not be null");
    }

    /**
     * Gets the strategy for mapping from a {@link SearchResult} to a collection of {@link Attribute}s.
     * 
     * @return strategy for mapping from a {@link SearchResult} to a collection of {@link Attribute}s
     */
    public SearchResultMappingStrategy getSearchResultMappingStrategy() {
        return mappingStrategy;
    }

    /**
     * Sets the strategy for mapping from a {@link SearchResult} to a collection of {@link Attribute}s.
     * 
     * @param strategy strategy for mapping from a {@link SearchResult} to a collection of {@link Attribute}s
     */
    public void setSearchResultMappingStrategy(@Nonnull final SearchResultMappingStrategy strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        mappingStrategy = Constraint.isNotNull(strategy, "Result mapping strategy can not be null");
    }

    /**
     * Gets whether an empty result set is treated as an error.
     * 
     * @return whether an empty result set is treated as an error
     */
    public boolean isNoResultAnError() {
        return noResultIsAnError;
    }

    /**
     * Sets whether an empty result set is treated as an error.
     * 
     * @param isAnError whether an empty result set is treated as an error
     */
    public synchronized void setNoResultAnError(final boolean isAnError) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        noResultIsAnError = isAnError;
    }

    /**
     * Gets the cache used to cache search results.
     * 
     * @return cache used to cache search results
     */
    @Nonnull public Cache<String, Optional<Map<String, Attribute>>> getResultCache() {
        return resultsCache;
    }

    /**
     * Sets the cache used to cache search results. Note, all entries in the cache are invalidated prior to use.
     * 
     * @param cache cache used to cache search results
     */
    public void setResultsCache(@Nonnull final Cache<String, Optional<Map<String, Attribute>>> cache) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        cache.invalidateAll();
        resultsCache = cache;
    }

    /** {@inheritDoc} */
    protected Optional<Map<String, Attribute>> doDataConnectorResolve(
            @Nonnull final AttributeResolutionContext resolutionContext) throws ResolutionException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        final SearchFilter filter = filterBuilder.build(resolutionContext);
        log.debug("Data connector '{}': built search request {} with builder {}", new Object[] {getId(), filter,
                filterBuilder,});

        Optional<Map<String, Attribute>> resolvedAttributes = null;
        if (resultsCache != null) {
            // TODO(lajoie): by using the hash of the filter might we have collisions?
            final String cacheKey = String.valueOf(filter.hashCode());
            resolvedAttributes = resultsCache.getIfPresent(cacheKey);
            log.debug("Data connector '{}': cache found resolved attributes {} using cache {}", new Object[] {getId(),
                    resolvedAttributes, resultsCache,});
            if (resolvedAttributes == null) {
                final Optional<SearchResult> result = retrieveAttributesFromLdap(filter);
                resolvedAttributes = mappingStrategy.map(result.get());
                log.debug("Data connector '{}': resolved LDAP attributes {}", getId(), resolvedAttributes);
                resultsCache.put(cacheKey, resolvedAttributes);
            }
        } else {
            final Optional<SearchResult> result = retrieveAttributesFromLdap(filter);
            resolvedAttributes = mappingStrategy.map(result.get());
            log.debug("Data connector '{}': resolved LDAP attributes {}", getId(), resolvedAttributes);
        }

        return resolvedAttributes;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (connectionFactory == null) {
            throw new ComponentInitializationException("Data connector '" + getId()
                    + "': no connection factory was configured");
        }
        if (searchExecutor == null) {
            throw new ComponentInitializationException("Data connector '" + getId()
                    + "': no search executor was configured");
        }
        if (filterBuilder == null) {
            throw new ComponentInitializationException("Data connector '" + getId()
                    + "': no filter builder was configured");
        }

        try {
            validateConnectionFactory();
        } catch (LdapException e) {
            log.error("Data connector '{}': invalid connector configuration", getId(), e);
            throw new ComponentInitializationException("Data connector '" + getId()
                    + "': invalid connector configuration", e);
        }
    }

    /** {@inheritDoc} */
    protected void doValidate() throws ComponentValidationException {
        try {
            validateConnectionFactory();
        } catch (LdapException e) {
            log.error("Data connector '{}': invalid connector configuration", getId(), e);
            throw new ComponentValidationException("Data connector '" + getId() + "': invalid connector configuration",
                    e);
        }
    }

    /**
     * Attempts to retrieve a connection from the connection factory. The connection is opened and closed.
     * 
     * @throws LdapException if the connection cannot be opened
     */
    private void validateConnectionFactory() throws LdapException {
        if (validateFilter != null) {
            searchExecutor.search(connectionFactory, validateFilter);
        } else {
            // validate filter new for v3, this block is for v2 compatibility
            Connection connection = null;
            try {
                connection = connectionFactory.getConnection();
                if (connection == null) {
                    throw new LdapException("Unable to retrieve connection from connection factory");
                }
                connection.open();
            } finally {
                if (connection != null) {
                    connection.close();
                }
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
    protected Optional<SearchResult> retrieveAttributesFromLdap(final SearchFilter filter) throws ResolutionException {

        if (filter == null) {
            throw new ResolutionException("Search filter cannot be null");
        }
        try {
            final Response<SearchResult> response = searchExecutor.search(connectionFactory, filter);
            log.trace("Data connector '{}': search returned {}", getId(), response);
            final SearchResult result = response.getResult();
            if (result.size() == 0) {
                if (noResultIsAnError) {
                    throw new ResolutionException("No attributes returned from search");
                } else {
                    return Optional.<SearchResult> absent();
                }
            }
            return Optional.of(result);
        } catch (LdapException e) {
            throw new ResolutionException("Unable to execute LDAP search", e);
        }
    }
}