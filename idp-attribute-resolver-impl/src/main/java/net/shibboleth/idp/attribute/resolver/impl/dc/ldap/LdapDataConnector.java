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
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseDataConnector;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.ldaptive.Connection;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.LdapException;
import org.ldaptive.LdapResult;
import org.ldaptive.SearchOperation;
import org.ldaptive.SearchRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;

//TODO(lajoie): log messages need to log plugin ID
//TODO(lajoie): need to review to ensure code meets coding standards
//TODO(lajoie): doInitialize and doValidate are identical except for the exception type they throw, refactor code
//TODO(lajoie): doInitialize and doValidate check for a null connection from the factory but retrieveAttributesFromLdap does not, which is the correct model?
//TODO(lajoie): should have a default mapping strategy

/** A {@link BaseDataConnector} that queries an LDAP in order to retrieve attribute data. */
public class LdapDataConnector extends BaseDataConnector {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(LdapDataConnector.class);

    /** Factory for retrieving LDAP connections. */
    private ConnectionFactory connectionFactory;

    /** Builder used to create the search requests executed against the LDAP. */
    private SearchRequestBuilder requestBuilder;

    /** Strategy for mapping from a {@link ResultSet} to a collection of {@link Attribute}s. */
    private LdapResultMappingStrategy mappingStrategy;

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
     * Gets the builder used to create the search requests executed against the LDAP.
     * 
     * @return builder used to create the search requests executed against the LDAP
     */
    public SearchRequestBuilder getSearchRequestBuilder() {
        return requestBuilder;
    }

    /**
     * Sets the builder used to create the search requests executed against the LDAP.
     * 
     * @param builder builder used to create the search requests executed against the LDAP
     */
    public void setSearchRequestBuilder(@Nonnull final SearchRequestBuilder builder) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        requestBuilder = Constraint.isNotNull(builder, "Search request builder can not be null");
    }

    /**
     * Gets the strategy for mapping from a {@link LdapResult} to a collection of {@link Attribute}s.
     * 
     * @return strategy for mapping from a {@link LdapResult} to a collection of {@link Attribute}s
     */
    public LdapResultMappingStrategy getLdapResultMappingStrategy() {
        return mappingStrategy;
    }

    /**
     * Sets the strategy for mapping from a {@link LdapResult} to a collection of {@link Attribute}s.
     * 
     * @param strategy strategy for mapping from a {@link LdapResult} to a collection of {@link Attribute}s
     */
    public void setLdapResultMappingStrategy(@Nonnull final LdapResultMappingStrategy strategy) {
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
    protected Optional<Map<String, Attribute>> doDataConnectorResolve(AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        final SearchRequest request = requestBuilder.build(resolutionContext);

        //TODO(lajoie): this method needs debug logging statements for each step
        
        //TODO(lajoie): by using the hash of the request might we have collisions?
        String cacheKey = String.valueOf(request.hashCode());
        Optional<Map<String, Attribute>> resolvedAttributes = resultsCache.getIfPresent(
                cacheKey);
        if (resolvedAttributes == null) {
            resolvedAttributes = retrieveAttributesFromLdap(request);
            resultsCache.put(cacheKey, resolvedAttributes);
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

        Connection connection = null;
        try {
            connection = connectionFactory.getConnection();
            if (connection == null) {
                throw new ComponentInitializationException("Data connector '" + getId()
                        + "': unable to retrieve connections from configured connection factory");
            }
            connection.open();
        } catch (LdapException e) {
            log.error("Data connector '{}': invalid connector configuration", getId(), e);
            throw new ComponentInitializationException("Data connector '" + getId()
                    + "': invalid connector configuration", e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    /** {@inheritDoc} */
    protected void doValidate() throws ComponentValidationException {
        Connection connection = null;
        try {
            connection = connectionFactory.getConnection();
            if (connection == null) {
                throw new ComponentValidationException("Data connector '" + getId()
                        + "': unable to retrieve connections from configured connection factory");
            }
            connection.open();
        } catch (LdapException e) {
            log.error("Data connector '{}': invalid connector configuration", getId(), e);
            throw new ComponentValidationException("Data connector '" + getId() 
                    + "': invalid connector configuration", e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    /**
     * Attempts to retrieve attributes from the LDAP.
     * 
     * @param request request used to retrieve data from the LDAP
     * 
     * @return attributes gotten from the database
     * 
     * @throws AttributeResolutionException thrown if there is a problem retrieving data from the database or
     *             transforming that data into {@link Attribute}s
     */
    protected Optional<Map<String, Attribute>> retrieveAttributesFromLdap(final SearchRequest request)
            throws AttributeResolutionException {

        //TODO(lajoie): this method needs debug logging statements for each step
        
        Connection connection = null;
        try {
            connection = connectionFactory.getConnection();
            connection.open();
            final SearchOperation search = new SearchOperation(connection);
            final LdapResult result = search.execute(request).getResult();

            final Optional<Map<String, Attribute>> resolvedAttributes = mappingStrategy.map(result);
            if (!resolvedAttributes.isPresent() && noResultIsAnError) {
                //TODO should this be checked before or after mapping?
                throw new AttributeResolutionException("No attributes returned from search");
            }

            return resolvedAttributes;
        } catch (LdapException e) {
            throw new AttributeResolutionException("Unable to execute LDAP search", e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }
}