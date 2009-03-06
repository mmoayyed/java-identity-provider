/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.config.attribute.resolver.dataConnector;

import java.util.Map;

import org.opensaml.xml.security.x509.X509Credential;
import org.opensaml.xml.util.DatatypeHelper;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.LdapDataConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.TemplateEngine;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.LdapDataConnector.AUTHENTICATION_TYPE;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.LdapDataConnector.SEARCH_SCOPE;

/**
 * Spring factory for creating {@link LdapDataConnector} beans.
 */
public class LdapDataConnectorFactoryBean extends BaseDataConnectorFactoryBean {

    /** Template engine used to construct filter queries. */
    private TemplateEngine templateEngine;

    /** URL of the LDAP server. */
    private String ldapURL;

    /** Base search DN. */
    private String baseDN;

    /** DN of the principal to bind to the directory as. */
    private String principal;

    /** Credential for the binding principal. */
    private String principalCredential;
    
    /** LDAP authentication type. */
    private AUTHENTICATION_TYPE authenticationType;

    /** LDAP query filter template. */
    private String filterTemplate;

    /** LDAP directory search scope. */
    private SEARCH_SCOPE searchScope;

    /** Name of the LDAP attributes to return. */
    private String[] returnAttributes;

    /** LDAP connection provider specific properties. */
    private Map<String, String> ldapProperties;

    /** Whether to use StartTLS when connecting to the LDAP. */
    private boolean useStartTLS;

    /** Trust material used when connecting to the LDAP over SSL/TLS. */
    private X509Credential trustCredential;

    /** Client authentication material used when connecting to the LDAP over SSL/TLS. */
    private X509Credential connectionCredential;

    /** Initial number of connections in the pool. */
    private int poolInitialSize;

    /** Maximum number of idle connections that will be kept in the pool. */
    private int poolMaxIdle;

    /** Time, in milliseconds, to wait for a search to return. */
    private int searchTimeLimit;

    /** Maximum number of results to return. */
    private int maxResultSize;

    /** Whether to cache query the results. */
    private boolean cacheResults;

    /** Whether to merge multiple results into a single set of attributes. */
    private boolean mergeResults;

    /** Whether a search returning no results should be considered an error. */
    private boolean noResultsIsError;

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        LdapDataConnector connector = new LdapDataConnector(ldapURL, baseDN, useStartTLS, poolMaxIdle, poolInitialSize);
        populateDataConnector(connector);
        connector.setAuthenticationType(authenticationType);
        connector.setPrincipal(principal);
        connector.setPrincipalCredential(principalCredential);
        connector.setLdapProperties(ldapProperties);
        
        if(trustCredential != null){
            connector.setSslTrustManagers(trustCredential);
        }
        
        if(connectionCredential != null){
            connector.setSslKeyManagers(connectionCredential);
        }
        
        connector.setCacheResults(cacheResults);
        connector.setFilterTemplate(filterTemplate);
        connector.setMaxResultSize(maxResultSize);
        connector.setMergeResults(mergeResults);
        connector.setNoResultsIsError(noResultsIsError);
        connector.setReturnAttributes(returnAttributes);
        connector.setSearchScope(searchScope);
        connector.setSearchTimeLimit(searchTimeLimit);
        connector.setTemplateEngine(templateEngine);

        connector.initialize();

        return connector;
    }

    /**
     * Gets the authentication type used when connecting to the directory.
     * 
     * @return authentication type used when connecting to the directory
     */
    public AUTHENTICATION_TYPE getAuthenticationType() {
        return authenticationType;
    }
    
    /**
     * Gets the base search DN.
     * 
     * @return the base search DN
     */
    public String getBaseDN() {
        return baseDN;
    }
    
    /**
     * Gets the client authentication material used when connecting to the LDAP via SSL or TLS.
     * 
     * @return client authentication material used when connecting to the LDAP via SSL or TLS
     */
    public X509Credential getConnectionCredential() {
        return connectionCredential;
    }

    /**
     * Gets the LDAP query filter template.
     * 
     * @return LDAP query filter template
     */
    public String getFilterTemplate() {
        return filterTemplate;
    }

    /**
     * Gets the LDAP connection provider specific properties.
     * 
     * @return LDAP connection provider specific properties
     */
    public Map<String, String> getLdapProperties() {
        return ldapProperties;
    }

    /**
     * Gets the LDAP server's URL.
     * 
     * @return LDAP server's URL
     */
    public String getLdapUrl() {
        return ldapURL;
    }

    /**
     * Gets the maximum number of results to return from a query.
     * 
     * @return maximum number of results to return from a query
     */
    public int getMaxResultSize() {
        return maxResultSize;
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return LdapDataConnector.class;
    }

    /**
     * Gets the initial number of connection to create in the connection pool.
     * 
     * @return initial number of connection to create in the connection pool
     */
    public int getPoolInitialSize() {
        return poolInitialSize;
    }

    /**
     * Gets the maximum number of idle connection that will be kept in the connection pool.
     * 
     * @return maximum number of idle connection that will be kept in the connection pool
     */
    public int getPoolMaxIdleSize() {
        return poolMaxIdle;
    }

    /**
     * Gets the principal DN used to bind to the directory.
     * 
     * @return principal DN used to bind to the directory
     */
    public String getPrincipal() {
        return principal;
    }

    /**
     * Gets the credential of the principal DN used to bind to the directory.
     * 
     * @return credential of the principal DN used to bind to the directory
     */
    public String getPrincipalCredential() {
        return principalCredential;
    }

    /**
     * Gets the attributes to return from a query.
     * 
     * @return attributes to return from a query
     */
    public String[] getReturnAttributes() {
        return returnAttributes;
    }

    /**
     * Gets the search scope of a query.
     * 
     * @return search scope of a query
     */
    public SEARCH_SCOPE getSearchScope() {
        return searchScope;
    }

    /**
     * Gets the maximum amount of time, in milliseconds, to wait for a search to complete.
     * 
     * @return maximum amount of time, in milliseconds, to wait for a search to complete
     */
    public int getSearchTimeLimit() {
        return searchTimeLimit;
    }

    /**
     * Gets the template engine used to construct query filters.
     * 
     * @return template engine used to construct query filters
     */
    public TemplateEngine getTemplateEngine() {
        return templateEngine;
    }

    /**
     * Gets the trust material used when connecting to the LDAP via SSL or TLS.
     * 
     * @return trust material used when connecting to the LDAP via SSL or TLS
     */
    public X509Credential getTrustCredential() {
        return trustCredential;
    }

    /**
     * Gets whether to use StartTLS when connecting to the LDAP.
     * 
     * @return whether to use StartTLS when connecting to the LDAP
     */
    public boolean getUseStartTLS() {
        return useStartTLS;
    }

    /**
     * Gets whether to cache query results.
     * 
     * @return whether to cache query results.
     */
    public boolean isCacheResults() {
        return cacheResults;
    }

    /**
     * Gets whether to merge multiple results into a single result.
     * 
     * @return whether to merge multiple results into a single result
     */
    public boolean isMergeResults() {
        return mergeResults;
    }

    /**
     * Gets whether a query that returns no results is an error condition.
     * 
     * @return whether a query that returns no results is an error condition
     */
    public boolean isNoResultsIsError() {
        return noResultsIsError;
    }

    /**
     * Sets the authentication type used when connecting to the directory.
     * 
     * @param type authentication type used when connecting to the directory
     */
    public void setAuthenticationType(AUTHENTICATION_TYPE type) {
        authenticationType = type;
    }

    /**
     * Sets the base search DN.
     * 
     * @param dn the base search DN
     */
    public void setBaseDN(String dn) {
        baseDN = DatatypeHelper.safeTrimOrNullString(dn);
    }

    /**
     * Sets whether to cache query results.
     * 
     * @param cache whether to cache query results
     */
    public void setCacheResults(boolean cache) {
        cacheResults = cache;
    }

    /**
     * Sets the client authentication material used when connecting to the LDAP via SSL or TLS.
     * 
     * @param credential client authentication material used when connecting to the LDAP via SSL or TLS
     */
    public void setConnectionCredential(X509Credential credential) {
        connectionCredential = credential;
    }

    /**
     * Sets the LDAP query filter template.
     * 
     * @param template LDAP query filter template
     */
    public void setFilterTemplate(String template) {
        filterTemplate = DatatypeHelper.safeTrimOrNullString(template);
    }

    /**
     * Sets the LDAP connection provider specific properties.
     * 
     * @param properties LDAP connection provider specific properties
     */
    public void setLdapProperties(Map<String, String> properties) {
        ldapProperties = properties;
    }

    /**
     * Sets the LDAP server's URL.
     * 
     * @param url LDAP server's URL
     */
    public void setLdapUrl(String url) {
        ldapURL = DatatypeHelper.safeTrimOrNullString(url);
    }

    /**
     * Sets the maximum number of results to return from a query.
     * 
     * @param max maximum number of results to return from a query
     */
    public void setMaxResultSize(int max) {
        maxResultSize = max;
    }

    /**
     * Sets whether to merge multiple results into a single result.
     * 
     * @param merge Twhether to merge multiple results into a single result
     */
    public void setMergeResults(boolean merge) {
        mergeResults = merge;
    }

    /**
     * Sets whether a query that returns no results is an error condition.
     * 
     * @param isError whether a query that returns no results is an error condition
     */
    public void setNoResultsIsError(boolean isError) {
        noResultsIsError = isError;
    }

    /**
     * Sets the initial number of connection to create in the connection pool.
     * 
     * @param initialSize initial number of connection to create in the connection pool
     */
    public void setPoolInitialSize(int initialSize) {
        poolInitialSize = initialSize;
    }

    /**
     * Sets the maximum number of idle connection that will be kept in the connection pool.
     * 
     * @param maxIdle maximum number of idle connection that will be kept in the connection pool
     */
    public void setPoolMaxIdleSize(int maxIdle) {
        poolMaxIdle = maxIdle;
    }

    /**
     * Sets the principal DN used to bind to the directory.
     * 
     * @param principalName principal DN used to bind to the directory
     */
    public void setPrincipal(String principalName) {
        principal = DatatypeHelper.safeTrimOrNullString(principalName);
    }

    /**
     * Sets the credential of the principal DN used to bind to the directory.
     * 
     * @param credential credential of the principal DN used to bind to the directory
     */
    public void setPrincipalCredential(String credential) {
        principalCredential = DatatypeHelper.safeTrimOrNullString(credential);
    }

    /**
     * Sets the attributes to return from a query.
     * 
     * @param attributes attributes to return from a query
     */
    public void setReturnAttributes(String[] attributes) {
        returnAttributes = attributes;
    }

    /**
     * Sets the search scope of a query.
     * 
     * @param scope search scope of a query
     */
    public void setSearchScope(SEARCH_SCOPE scope) {
        searchScope = scope;
    }

    /**
     * Sets the maximum amount of time, in milliseconds, to wait for a search to complete.
     * 
     * @param timeLimit maximum amount of time, in milliseconds, to wait for a search to complete
     */
    public void setSearchTimeLimit(int timeLimit) {
        searchTimeLimit = timeLimit;
    }

    /**
     * Sets the template engine used to construct query filters.
     * 
     * @param engine template engine used to construct query filters
     */
    public void setTemplateEngine(TemplateEngine engine) {
        templateEngine = engine;
    }

    /**
     * Sets the trust material used when connecting to the LDAP via SSL or TLS.
     * 
     * @param credential trust material used when connecting to the LDAP via SSL or TLS
     */
    public void setTrustCredential(X509Credential credential) {
        trustCredential = credential;
    }
    
    /**
     * Sets whether to use StartTLS when connecting to the LDAP.
     * 
     * @param startTLS whether to use StartTLS when connecting to the LDAP
     */
    public void setUseStartTLS(boolean startTLS) {
        useStartTLS = startTLS;
    }
}