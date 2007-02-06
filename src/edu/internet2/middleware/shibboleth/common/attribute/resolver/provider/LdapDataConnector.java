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
package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import edu.internet2.middleware.shibboleth.common.attribute.impl.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.impl.AbstractResolutionPlugIn;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.impl.StatementCreator;
import edu.internet2.middleware.shibboleth.common.session.LogoutEvent;
import edu.vt.middleware.ldap.Ldap;
import edu.vt.middleware.ldap.LdapConfig;
import edu.vt.middleware.ldap.LdapUtil;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * <p>
 * <code>LdapDataConnector</code> provides a plugin to retrieve attributes from an LDAP.
 * <p>
 */
public class LdapDataConnector extends AbstractResolutionPlugIn implements ApplicationListener {

    /** Class logger. */
    private static Logger log = Logger.getLogger(LdapDataConnector.class);
    
    /** Engine for transforming templated filters into populated filters */
    private StatementCreator filterCreator;

    /** Whether multiple result sets should be merged */
    private boolean mergeMultipleResults;
    
    /** Whether an empty result set is an error */
    private boolean noResultsIsError = true;
    
    /** Whether to cache search results for the duration of the session */
    private boolean cacheResults;
    
    /** Ldap search filter */
    private String filter;

    /** Attributes to return from ldap searches */
    private String[] returnAttributes;
    
    /** Ldap configuration */
    private LdapConfig ldapConfig;

    /** Ldap object */
    private Ldap ldap;

    /** Data cache */
    private Map cache = new HashMap();
    

    /**
     * Default Constructor.
     */
    public LdapDataConnector() {
        this.ldap = new Ldap(this.ldapConfig);
        filterCreator = new StatementCreator();
    }
    
    
    /**
     * @return Returns the mergeMultipleResults.
     */
    public boolean isMergeMultipleResults() {
        return this.mergeMultipleResults;
    }


    /**
     * @param mergeMultipleResults The mergeMultipleResults to set.
     */
    public void setMergeMultipleResults(boolean mergeMultipleResults) {
        this.mergeMultipleResults = mergeMultipleResults;
    }
    
    
    /**
     * @return Returns the cacheResults.
     */
    public boolean isCacheResults() {
        return this.cacheResults;
    }


    /**
     * @param cacheResults The cacheResults to set.
     */
    public void setCacheResults(boolean cacheResults) {
        this.cacheResults = cacheResults;
    }
    
    
    /**
     * @return Returns the noResultsIsError.
     */
    public boolean isNoResultsIsError() {
        return this.noResultsIsError;
    }


    /**
     * @param noResultsIsError The noResultsIsError to set.
     */
    public void setNoResultsIsError(boolean noResultsIsError) {
        this.noResultsIsError = noResultsIsError;
    }


    /**
     * @return Returns the filter.
     */
    public String getFilter() {
        return this.filter;
    }


    /**
     * @param filter The filter to set.
     */
    public void setFilter(String filter) {
        this.filter = filter;
    }


    /**
     * @return Returns the useStartTls.
     */
    public boolean isUseStartTls() {
        return this.ldapConfig.isTlsEnabled();
    }


    /**
     * @param useStartTls The useStartTls to set.
     */
    public void setUseStartTls(boolean useStartTls) {
        this.ldapConfig.useTls(useStartTls);
    }

   
    /**
     * @return Returns the searchScope.
     */
    public int getSearchScope() {
        return this.ldapConfig.getSearchScope();
    }


    /**
     * @param searchScope The searchScope to set.
     */
    public void setSearchScope(int searchScope) {
        this.ldapConfig.setSearchScope(searchScope);
    }

   
    /**
     * @param searchScope The searchScope to set.
     */
    public void setSearchScope(String searchScope) {
        if (searchScope.equals("OBJECT_SCOPE")) {
            this.ldapConfig.useObjectScopeSearch();            
        } else if (searchScope.equals("SUBTREE_SCOPE")) {
            this.ldapConfig.useSubTreeSearch();
        } else if (searchScope.equals("ONELEVEL_SCOPE")) {
            this.ldapConfig.useOneLevelSearch();
        }
    }

   
    /**
     * @return Returns the returnAttributes.
     */
    public String[] getReturnAttributes() {
        return this.returnAttributes;
    }


    /**
     * @param returnAttributes The returnAttributes to set.
     */
    public void setReturnAttributes(String[] returnAttributes) {
        this.returnAttributes = returnAttributes;
    }

   
    /**
     * @param returnAttributes The comma delimited returnAttributes to set.
     */
    public void setReturnAttributes(String returnAttributes) {
        StringTokenizer st = new StringTokenizer(returnAttributes, ",");
        this.returnAttributes = new String[st.countTokens()];
        for (int count = 0; count < st.countTokens(); count++) {
            this.returnAttributes[count] = st.nextToken();
        }
    }

   
    /**
     * @return Returns the timeLimit.
     */
    public int getTimeLimit() {
        return this.ldapConfig.getTimeLimit();
    }


    /**
     * @param timeLimit The timeLimit to set.
     */
    public void setTimeLimit(int timeLimit) {
        this.ldapConfig.setTimeLimit(timeLimit);
    }

   
    /**
     * @return Returns the countLimit.
     */
    public long getCountLimit() {
        return this.ldapConfig.getCountLimit();
    }


    /**
     * @param countLimit The countLimit to set.
     */
    public void setCountLimit(long countLimit) {
        this.ldapConfig.setCountLimit(countLimit);
    }

   
    /**
     * @return Returns the returningObjects.
     */
    public boolean isReturningObjects() {
        return this.ldapConfig.getReturningObjFlag();
    }


    /**
     * @param returningObjects The returningObjects to set.
     */
    public void setReturningObjects(boolean returningObjects) {
        this.ldapConfig.setReturningObjFlag(returningObjects);
    }
    
    
    /**
     * @return Returns the linkDereferencing
     */
    public boolean isLinkDereferencing() {
        return this.ldapConfig.getDerefLinkFlag();
    }


    /**
     * @param linkDereferencing The linkDereferencing to set.
     */
    public void setLinkDereferencing(boolean linkDereferencing) {
        this.ldapConfig.setDerefLinkFlag(linkDereferencing);
    }
    
    
    /**
     * @param ldapProperties The ldapProperties to set.
     */
    public void setLdapProperties(Map<String, String> ldapProperties) {
        Iterator i = ldapProperties.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry pairs = (Map.Entry) i.next();
            this.ldapConfig.setExtraProperties((String) pairs.getKey(), (String) pairs.getValue());            
        }
    }

    
    /** {@inheritDoc} */
    public void setNotificationAddress(String notificationAddress) {}


    /** {@inheritDoc} */
    public void onApplicationEvent(ApplicationEvent evt) {
        if (evt instanceof LogoutEvent) {
            LogoutEvent logoutEvent = (LogoutEvent) evt;
            this.cache.remove(logoutEvent.getUserSession().getPrincipalID());
        }
    }

    
    /** {@inheritDoc} */
    public List<Attribute> resolve(ResolutionContext resolutionContext) throws AttributeResolutionException
    {
        String searchFilter = filterCreator.createStatement(filter, resolutionContext, getDataConnectorDependencyIds(),
                getAttributeDefinitionDependencyIds()); 
        
        // create Attribute objects to return
        List attributes = new ArrayList<BaseAttribute>();        

        // check for cached data
        if (this.cacheResults) {
            attributes = this.getCachedAttributes(resolutionContext, searchFilter);
        }
        if (attributes == null) { // results not found in the cache
            Iterator results = null;
            try {
                results = this.ldap.search(searchFilter, this.returnAttributes);
            } catch (NamingException e) {
                log.error("An error occured when atteming to search the LDAP: "+this.ldapConfig.getEnvironment(), e);
                throw new AttributeResolutionException("An error occurred when attemping to search the LDAP");            
            }
            // check for empty result set
            if (this.noResultsIsError && !results.hasNext()) {
                throw new AttributeResolutionException(
                        "No LDAP entry found for "+resolutionContext.getPrincipalName());
            }
            SearchResult sr = (SearchResult) results.next();
            Map attrs = this.mergeAttributes(new HashMap<String,List>(), sr.getAttributes());
            // merge additional results if requested
            while (this.mergeMultipleResults && results.hasNext()) {
                SearchResult additionalResult = (SearchResult) results.next();
                attrs = this.mergeAttributes(attrs, additionalResult.getAttributes());
            }
            // populate list of attributes
            Iterator i = attrs.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry pairs = (Map.Entry) i.next();
                BaseAttribute attribute = new BaseAttribute();
                attribute.setID((String) pairs.getKey());
                attribute.getValues().addAll((List<String>) pairs.getValue());
                attributes.add(attribute);
            }
            if (this.cacheResults && attributes != null) {
                this.setCachedAttributes(resolutionContext, searchFilter, attributes);
            }
        }
        return attributes;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException
    {
        try {
            if (!this.ldap.connect()) {
                throw new NamingException();
            }
        } catch (NamingException e) {
            throw new AttributeResolutionException("Cannot connect to the LDAP");
        }
    }
    
    /**
     * <p>
     * This stores the supplied attributes in the cache.
     * </p>
     * 
     * @param resolutionContext <code>ResolutionContext</code>
     * @param searchFiler the searchFilter that produced the attributes
     * @param attributes <code>List</code> to store
     */
    private void setCachedAttributes(ResolutionContext resolutionContext, String searchFiler, List attributes) {
        Map<String,List> results = null;
        String principal = resolutionContext.getPrincipalName();
        if (this.cache.containsKey(principal)) {
            results = (Map) this.cache.get(principal);            
        } else {
            results = new HashMap();            
            this.cache.put(principal, results);
        }
        results.put(searchFiler, attributes);
    }
    

    /**
     * <p>
     * This retrieves any cached attributes for the supplied resolution context.
     * Returns null if nothing is cached.
     * </p>
     * 
     * @param resolutionContext <code>ResolutionContext</code>
     * @param searchFilter the search filter the produced the attributes
     * 
     * @return <code>List</code> of attributes
     */
    private List getCachedAttributes(ResolutionContext resolutionContext, String searchFilter) {
        List attributes = null;
        String principal = resolutionContext.getPrincipalName();
        if (this.cache.containsKey(principal)) {
            Map results = (Map) this.cache.get(principal);
            attributes = (List) results.get(searchFilter);                                
        }
        return attributes;
    }
    
    
    /**
     * <p>
     * This updates the supplied attribute map with the names and values found in the
     * supplied <code>Attributes</code> object.
     * </p>
     * 
     * @param attrs <code>Map</code> to update
     * @param newAttrs <code>Attributes</code> to parse
     * @throws AttributeResolutionException if the supplied attributes cannot be parsed
     */
    private Map mergeAttributes(Map<String,List> attrs, Attributes newAttrs) throws AttributeResolutionException
    {
        // merge the new attributes
        try {
            Map<String,List> newAttrsMap = LdapUtil.parseAttributes(newAttrs, true);
            
            Iterator i = newAttrsMap.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry pairs = (Map.Entry) i.next();
                String attrName = (String) pairs.getKey();
                List attrValues = (List) pairs.getValue();
                if (attrs.containsKey(attrName)) {
                    List<String> l = (List) attrs.get(attrName);
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
