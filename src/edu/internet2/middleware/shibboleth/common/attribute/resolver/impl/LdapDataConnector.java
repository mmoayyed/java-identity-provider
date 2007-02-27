/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.impl.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionContext;
import edu.internet2.middleware.shibboleth.common.session.LogoutEvent;
import edu.vt.middleware.ldap.Ldap;
import edu.vt.middleware.ldap.LdapConfig;
import edu.vt.middleware.ldap.LdapPool;
import edu.vt.middleware.ldap.LdapUtil;

/**
 * <code>LdapDataConnector</code> provides a plugin to retrieve attributes from an LDAP.
 */
public class LdapDataConnector extends BaseDataConnector implements ApplicationListener {

    /** Class logger. */
    private static Logger log = Logger.getLogger(LdapDataConnector.class);

    /** Whether multiple result sets should be merged. */
    private boolean mergeMultipleResults;

    /** Whether an empty result set is an error. */
    private boolean noResultsIsError = true;

    /** Whether to cache search results for the duration of the session. */
    private boolean cacheResults;

    /** Template that produces the query to use. */
    private String filterTemplate;

    /** Template engine used to change filter template into actual filter. */
    private TemplateEngine filterCreator;

    /** Ldap search filter. */
    private String filter;

    /** Attributes to return from ldap searches. */
    private String[] returnAttributes;

    /** Ldap configuration. */
    private LdapConfig ldapConfig;

    /** LdapPool object. */
    private LdapPool ldapPool;

    /** Maximum number of idle objects in the ldap pool. */
    private int poolMaxIdle;

    /** Initial capacity of the the ldap pool. */
    private int poolInitIdleCapacity;

    /** Data cache. */
    private Map<String, Map<String, Map<String, Attribute>>> cache;

    /** Whether this data connector has been initialized. */
    private boolean initialized;

    /**
     * This creates a new ldap data connector with the supplied pool properties.
     * 
     * @param startTls <code>boolean</code> whether connection should startTls
     * @param maxIdle <code>int</code> maximum number of idle pool objects
     * @param initIdleCapacity <code>int</code> initial capacity of the pool
     */
    public LdapDataConnector(boolean startTls, int maxIdle, int initIdleCapacity) {
        ldapConfig = new LdapConfig();
        ldapConfig.useTls(startTls);
        poolMaxIdle = maxIdle;
        poolInitIdleCapacity = initIdleCapacity;
    }

    /**
     * Initializes the connector and prepares it for use.
     */
    public void initialize() {
        initialized = true;
        registerTemplate();
        initializeLdapPool();
        initializeCache();
    }

    /**
     * Initializes the ldap pool and prepares it for use. {@link #initialize()} must be called first or this method does
     * nothing.
     */
    protected void initializeLdapPool() {
        if (initialized) {
            ldapPool = new LdapPool(ldapConfig, poolMaxIdle, poolInitIdleCapacity);
        }
    }

    /**
     * Initializes the cache and prepares it for use. {@link #initialize()} must be called first or this method does
     * nothing.
     */
    protected void initializeCache() {
        if (cacheResults && initialized) {
            cache = new HashMap<String, Map<String, Map<String, Attribute>>>();
        }
    }

    /**
     * This removes all entries from the cache. {@link #initialize()} must be called first or this method does nothing.
     */
    protected void clearCache() {
        if (cacheResults && initialized) {
            cache.clear();
        }
    }

    /**
     * Registers the query template with template engine. {@link #initialize()} must be called first or this method does
     * nothing.
     */
    protected void registerTemplate() {
        if (initialized) {
            filterCreator.registerTemplate("shibboleth.resolver.dc." + getId(), filterTemplate);
        }
    }

    /**
     * Returns the mergeMultipleResults.
     * 
     * @return <code>boolean</code>
     */
    public boolean isMergeMultipleResults() {
        return mergeMultipleResults;
    }

    /**
     * Sets the mergeMultipleResults.
     * 
     * @param b <code>boolean</code>
     */
    public void setMergeMultipleResults(boolean b) {
        mergeMultipleResults = b;
        clearCache();
    }

    /**
     * Returns the cacheResults.
     * 
     * @return <code>boolean</code>
     */
    public boolean isCacheResults() {
        return cacheResults;
    }

    /**
     * Sets the cacheResults.
     * 
     * @param b <code>boolean</code>
     */
    public void setCacheResults(boolean b) {
        cacheResults = b;
        if (!cacheResults) {
            cache = null;
        } else {
            initializeCache();
        }
    }

    /**
     * Returns the noResultsIsError.
     * 
     * @return <code>boolean</code>
     */
    public boolean isNoResultsIsError() {
        return noResultsIsError;
    }

    /**
     * Sets the noResultsIsError.
     * 
     * @param b <code>boolean</code>
     */
    public void setNoResultsIsError(boolean b) {
        noResultsIsError = b;
    }

    /**
     * Gets the engine used to evaluate the query template.
     * 
     * @return engine used to evaluate the query template
     */
    public TemplateEngine getTemplateEngine() {
        return filterCreator;
    }

    /**
     * Sets the engine used to evaluate the query template.
     * 
     * @param engine engine used to evaluate the query template
     */
    public void setTemplateEngine(TemplateEngine engine) {
        filterCreator = engine;
        registerTemplate();
        clearCache();
    }

    /**
     * Gets the template used to create queries.
     * 
     * @return template used to create queries
     */
    public String getFilterTemplate() {
        return filterTemplate;
    }

    /**
     * Sets the template used to create queries.
     * 
     * @param template template used to create queries
     */
    public void setFilterTemplate(String template) {
        filterTemplate = template;
        clearCache();
    }

    /**
     * Returns the filter.
     * 
     * @return <code>String</code>
     */
    public String getFilter() {
        return filter;
    }

    /**
     * Sets the filter.
     * 
     * @param s <code>String</code>
     */
    public void setFilter(String s) {
        filter = s;
        clearCache();
    }

    /**
     * Returns the useStartTls.
     * 
     * @return <code>boolean</code>
     */
    public boolean isUseStartTls() {
        return ldapConfig.isTlsEnabled();
    }

    /**
     * Returns the searchScope.
     * 
     * @return <code>int</code>
     */
    public int getSearchScope() {
        return ldapConfig.getSearchScope();
    }

    /**
     * Sets the searchScope.
     * 
     * @param i <code>int</code>
     */
    public void setSearchScope(int i) {
        ldapConfig.setSearchScope(i);
        clearCache();
        initializeLdapPool();
    }

    /**
     * Sets the searchScope.
     * 
     * @param s <code>String</code>
     */
    public void setSearchScope(String s) {
        if (s.equals("OBJECT_SCOPE")) {
            ldapConfig.useObjectScopeSearch();
        } else if (s.equals("SUBTREE_SCOPE")) {
            ldapConfig.useSubTreeSearch();
        } else if (s.equals("ONELEVEL_SCOPE")) {
            ldapConfig.useOneLevelSearch();
        }
        clearCache();
        initializeLdapPool();
    }

    /**
     * Returns the returnAttributes.
     * 
     * @return <code>String[]</code>
     */
    public String[] getReturnAttributes() {
        return returnAttributes;
    }

    /**
     * Sets the returnAttributes.
     * 
     * @param s <code>String[]</code>
     */
    public void setReturnAttributes(String[] s) {
        returnAttributes = s;
        clearCache();
    }

    /**
     * Sets the returnAttributes.
     * 
     * @param s <code>String[]</code> comma delimited returnAttributes
     */
    public void setReturnAttributes(String s) {
        StringTokenizer st = new StringTokenizer(s, ",");
        String[] ra = new String[st.countTokens()];
        for (int count = 0; count < st.countTokens(); count++) {
            ra[count] = st.nextToken();
        }
        setReturnAttributes(ra);
    }

    /**
     * Returns the timeLimit.
     * 
     * @return <code>int</code>
     */
    public int getTimeLimit() {
        return ldapConfig.getTimeLimit();
    }

    /**
     * Sets the timeLimit.
     * 
     * @param i <code>int</code>
     */
    public void setTimeLimit(int i) {
        ldapConfig.setTimeLimit(i);
        clearCache();
        initializeLdapPool();
    }

    /**
     * Returns the countLimit.
     * 
     * @return <code>long</code>
     */
    public long getCountLimit() {
        return ldapConfig.getCountLimit();
    }

    /**
     * Sets the countLimit.
     * 
     * @param l <code>long</code>
     */
    public void setCountLimit(long l) {
        ldapConfig.setCountLimit(l);
        clearCache();
        initializeLdapPool();
    }

    /**
     * Returns the returningObjects.
     * 
     * @return <code>boolean</code>
     */
    public boolean isReturningObjects() {
        return ldapConfig.getReturningObjFlag();
    }

    /**
     * Sets the returningObjects.
     * 
     * @param b <code>boolean</code>
     */
    public void setReturningObjects(boolean b) {
        ldapConfig.setReturningObjFlag(b);
        clearCache();
        initializeLdapPool();
    }

    /**
     * Returns the linkDereferencing.
     * 
     * @return <code>boolean</code>
     */
    public boolean isLinkDereferencing() {
        return ldapConfig.getDerefLinkFlag();
    }

    /**
     * Sets the linkDereferencing.
     * 
     * @param b <code>boolean</code>
     */
    public void setLinkDereferencing(boolean b) {
        ldapConfig.setDerefLinkFlag(b);
        clearCache();
        initializeLdapPool();
    }

    /**
     * Sets the ldapProperties.
     * 
     * @param ldapProperties <code>Map</code> of name/value pairs
     */
    public void setLdapProperties(Map<String, String> ldapProperties) {
        for (Map.Entry<String, String> entry : ldapProperties.entrySet()) {
            ldapConfig.setExtraProperties(entry.getKey(), entry.getValue());
        }
        clearCache();
        initializeLdapPool();
    }

    /** {@inheritDoc} */
    public void setNotificationAddress(String notificationAddress) {
    }

    /** {@inheritDoc} */
    public void onApplicationEvent(ApplicationEvent evt) {
        if (evt instanceof LogoutEvent) {
            LogoutEvent logoutEvent = (LogoutEvent) evt;
            cache.remove(logoutEvent.getUserSession().getPrincipalID());
        }
    }

    /** {@inheritDoc} */
    public Map<String, Attribute> resolve(ResolutionContext resolutionContext) throws AttributeResolutionException {
        if (log.isDebugEnabled()) {
            log.debug("Begin resolve for " + resolutionContext.getPrincipalName());
        }

        String searchFilter = filterCreator.createStatement(filter, resolutionContext, getDataConnectorDependencyIds(),
                getDataConnectorDependencyIds());
        if (log.isDebugEnabled()) {
            log.debug("search filter = " + searchFilter);
        }

        // create Attribute objects to return
        Map<String, Attribute> attributes = null;

        // check for cached data
        if (cacheResults) {
            attributes = getCachedAttributes(resolutionContext, searchFilter);
            if (attributes != null && log.isDebugEnabled()) {
                log.debug("Returning attributes from cache");
            }
        }
        // results not found in the cache
        if (attributes == null) {
            if (log.isDebugEnabled()) {
                log.debug("Retrieving attributes from LDAP");
            }
            Iterator<SearchResult> results = searchLdap(searchFilter);
            // check for empty result set
            if (noResultsIsError && !results.hasNext()) {
                throw new AttributeResolutionException("No LDAP entry found for "
                        + resolutionContext.getPrincipalName());
            }
            // build resolved attributes from LDAP attributes
            attributes = buildBaseAttributes(results);
            if (cacheResults && attributes != null) {
                setCachedAttributes(resolutionContext, searchFilter, attributes);
                if (log.isDebugEnabled()) {
                    log.debug("Stored results in the cache");
                }
            }
        }
        return attributes;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        Ldap ldap = null;
        try {
            ldap = (Ldap) ldapPool.borrowObject();
            if (!ldap.connect()) {
                throw new NamingException();
            }
        } catch (NamingException e) {
            log.error("An error occured when atteming to search the LDAP: " + ldapConfig.getEnvironment(), e);
            throw new AttributeResolutionException("An error occurred when attemping to search the LDAP");
        } catch (Exception e) {
            log.error("Could not retrieve Ldap object from pool", e);
            throw new AttributeResolutionException(
                    "An error occurred when attemping to retrieve a LDAP connection from the pool");
        } finally {
            if (ldap != null) {
                try {
                    ldapPool.returnObject(ldap);
                } catch (Exception e) {
                    log.error("Could not return Ldap object back to pool", e);
                }
            }
        }
    }

    /**
     * This searches the LDAP with the supplied filter.
     * 
     * @param searchFilter <code>String</code> the searchFilter that produced the attributes
     * @return <code>Iterator</code> of search results
     * @throws AttributeResolutionException if an error occurs performing the search
     */
    protected Iterator<SearchResult> searchLdap(String searchFilter) throws AttributeResolutionException {
        Ldap ldap = null;
        try {
            ldap = (Ldap) ldapPool.borrowObject();
            return ldap.search(searchFilter, returnAttributes);
        } catch (NamingException e) {
            log.error("An error occured when atteming to search the LDAP: " + ldapConfig.getEnvironment(), e);
            throw new AttributeResolutionException("An error occurred when attemping to search the LDAP");
        } catch (Exception e) {
            log.error("Could not retrieve Ldap object from pool", e);
            throw new AttributeResolutionException(
                    "An error occurred when attemping to retrieve a LDAP connection from the pool");
        } finally {
            if (ldap != null) {
                try {
                    ldapPool.returnObject(ldap);
                } catch (Exception e) {
                    log.error("Could not return Ldap object back to pool", e);
                }
            }
        }
    }

    /**
     * This returns a map of attribute ids to attributes from the supplied search results.
     * 
     * @param results <code>Iterator</code> of LDAP search results
     * @return <code>Map</code> of attribute ids to attributes
     * @throws AttributeResolutionException if an error occurs parsing attribute results
     */
    protected Map<String, Attribute> buildBaseAttributes(Iterator<SearchResult> results)
            throws AttributeResolutionException {
        Map<String, Attribute> attributes = new HashMap<String, Attribute>();
        SearchResult sr = results.next();
        Map<String, List<String>> attrs = mergeAttributes(new HashMap<String, List<String>>(), sr.getAttributes());
        // merge additional results if requested
        while (mergeMultipleResults && results.hasNext()) {
            SearchResult additionalResult = results.next();
            attrs = mergeAttributes(attrs, additionalResult.getAttributes());
        }
        // populate list of attributes
        for (Map.Entry<String, List<String>> entry : attrs.entrySet()) {
            if (log.isDebugEnabled()) {
                log.debug("Found the following attribute: " + entry);
            }
            BaseAttribute<String> attribute = new BaseAttribute<String>();
            attribute.setId(entry.getKey());
            attribute.getValues().addAll(entry.getValue());
            attributes.put(attribute.getId(), attribute);
        }
        return attributes;
    }

    /**
     * This stores the supplied attributes in the cache.
     * 
     * @param resolutionContext <code>ResolutionContext</code>
     * @param searchFiler the searchFilter that produced the attributes
     * @param attributes <code>Map</code> of attribute ids to attributes
     */
    protected void setCachedAttributes(ResolutionContext resolutionContext, String searchFiler,
            Map<String, Attribute> attributes) {
        Map<String, Map<String, Attribute>> results = null;
        String principal = resolutionContext.getPrincipalName();
        if (cache.containsKey(principal)) {
            results = cache.get(principal);
        } else {
            results = new HashMap<String, Map<String, Attribute>>();
            cache.put(principal, results);
        }
        results.put(searchFiler, attributes);
    }

    /**
     * This retrieves any cached attributes for the supplied resolution context. Returns null if nothing is cached.
     * 
     * @param resolutionContext <code>ResolutionContext</code>
     * @param searchFilter the search filter the produced the attributes
     * 
     * @return <code>Map</code> of attributes ids to attributes
     */
    protected Map<String, Attribute> getCachedAttributes(ResolutionContext resolutionContext, String searchFilter) {
        Map<String, Attribute> attributes = null;
        if (cacheResults) {
            String principal = resolutionContext.getPrincipalName();
            if (cache.containsKey(principal)) {
                Map<String, Map<String, Attribute>> results = cache.get(principal);
                attributes = results.get(searchFilter);
            }
        }
        return attributes;
    }

    /**
     * This updates the supplied attribute map with the names and values found in the supplied <code>Attributes</code>
     * object.
     * 
     * @param attrs <code>Map</code> to update
     * @param newAttrs <code>Attributes</code> to parse
     * @return <code>Map</code> of attributes
     * @throws AttributeResolutionException if the supplied attributes cannot be parsed
     */
    private Map<String, List<String>> mergeAttributes(Map<String, List<String>> attrs, Attributes newAttrs)
            throws AttributeResolutionException {
        // merge the new attributes
        try {
            Map<String, List<String>> newAttrsMap = LdapUtil.parseAttributes(newAttrs, true);

            for (Map.Entry<String, List<String>> entry : newAttrsMap.entrySet()) {
                String attrName = entry.getKey();
                List<String> attrValues = entry.getValue();
                if (attrs.containsKey(attrName)) {
                    List<String> l = attrs.get(attrName);
                    l.addAll(attrValues);
                } else {
                    attrs.put(attrName, attrValues);
                }
            }
        } catch (NamingException e) {
            log.error("Error parsing LDAP attributes", e);
            throw new AttributeResolutionException("Error parsing LDAP attributes");
        }
        return attrs;
    }
}
