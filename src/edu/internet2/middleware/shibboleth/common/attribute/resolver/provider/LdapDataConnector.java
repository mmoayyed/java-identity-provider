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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import org.apache.log4j.Logger;
import edu.internet2.middleware.shibboleth.common.attribute.impl.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.impl.AbstractResolutionPlugIn;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.impl.StatementCreator;
import edu.vt.middleware.ldap.Ldap;
import edu.vt.middleware.ldap.LdapConfig;
import edu.vt.middleware.ldap.LdapUtil;

/**
 * <p>
 * <code>LdapDataConnector</code>
 * <p>
 */
public class LdapDataConnector extends AbstractResolutionPlugIn implements HttpSessionListener {

    /** Class logger. */
    private static Logger log = Logger.getLogger(LdapDataConnector.class);

    /** Whether multiple result sets should be merged */
    private boolean mergeMultipleResults;
    
    /** Whether an empty result set is an error */
    private boolean noResultsIsError;
    
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
    }
    
    
    /**
     * @param useStartTls The useStartTls to set.
     */
    public void setUseStartTls(boolean useStartTls) {
        this.ldapConfig.useTls(useStartTls);
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
     * @param ldapControls The ldapControls to set.
     */
    public void setLdapControls(Map<String, String> ldapControls) {
        Iterator i = ldapControls.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry pairs = (Map.Entry) i.next();
            if (pairs.getKey().equals("searchScope")) {
                this.ldapConfig.setSearchScope((String) pairs.getValue());
            } else if (pairs.getKey().equals("returningAttributes")) {
                StringTokenizer st = new StringTokenizer((String) pairs.getValue(), ",");
                this.returnAttributes = new String[st.countTokens()];
                for (int count = 0; count < st.countTokens(); count++) {
                    this.returnAttributes[count] = st.nextToken();
                }
            } else if (pairs.getKey().equals("timeLimit")) {
                this.ldapConfig.setTimeLimit((String) pairs.getValue());
            } else if (pairs.getKey().equals("countLimit")) {
                this.ldapConfig.setCountLimit((String) pairs.getValue());
            } else if (pairs.getKey().equals("returningObjects")) {
                this.ldapConfig.setReturningObjFlag((String) pairs.getValue());
            } else if (pairs.getKey().equals("linkDereferencing")) {
                this.ldapConfig.setDerefLinkFlag((String) pairs.getValue());
            } else {
                log.error("Unknown ldap control property: "+pairs.getKey());
            }
        }        
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
    public void sessionCreated(HttpSessionEvent event) {}


    /** {@inheritDoc} */
    public void sessionDestroyed(HttpSessionEvent event) {
        this.cache.remove(event.getSession().getId());
    }


    /** {@inheritDoc} */
    public List<Attribute> resolve(ResolutionContext resolutionContext) throws AttributeResolutionException
    {
        // create Attribute objects to return
        List attributes = new ArrayList();        

        // check for cached data
        if (this.cacheResults) {
            attributes = this.getCachedAttributes(resolutionContext);
        }
        if (attributes == null) { // results found in the cache
            Iterator results = null;
            try {
                results = this.ldap.search(this.createTemplate(resolutionContext), this.returnAttributes);
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
            Map attrs = this.mergeAttributes(new HashMap(), sr.getAttributes());
            // merge additional results if requested
            while (this.mergeMultipleResults && results.hasNext()) {
                SearchResult additionalResult = (SearchResult) results.next();
                attrs = this.mergeAttributes(attrs, additionalResult.getAttributes());
            }
            Iterator i = attrs.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry pairs = (Map.Entry) i.next();
                BaseAttribute attribute = new BaseAttribute();
                attribute.setID((String) pairs.getKey());
                attribute.getValues().addAll((List) pairs.getValue());
            }
            if (this.cacheResults && attributes != null) {
                this.setCachedAttributes(resolutionContext, attributes);
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
     * This creates a template that can be used for by a <code>StatementCreator</code>.
     * For this connector the template is the ldap filter for the supplied principal.
     * </p>
     * 
     * @param resolutionContext <code>ResolutionContext</code>
     * @return <code>String</code>
     */
    private String createTemplate(ResolutionContext resolutionContext) {
        // get principal from the context
        String principal = resolutionContext.getPrincipalName();        
        // replace key in filter
        return this.filter.replaceAll("%PRINCIPAL%", principal);        
    }
    
    
    /**
     * <p>
     * This creates a statement specific to this connector and the supplied resolution context.
     * </p>
     * 
     * @param resolutionContext <code>ResolutionContext</code>
     * @return <code>String</code>
     */
    private String createStatement(ResolutionContext resolutionContext) {
        return (new StatementCreator()).createStatement(this.createTemplate(resolutionContext), resolutionContext);        
    }
    
    
    /**
     * <p>
     * This stores the supplied attributes in the cache.
     * </p>
     * 
     * @param resolutionContext <code>ResolutionContext</code>
     * @param attributes <code>List</code> to store
     */
    private void setCachedAttributes(ResolutionContext resolutionContext, List attributes) {
        // retrieve user session id
        HttpServletRequest request = (HttpServletRequest) resolutionContext.getRequest();
        String cacheId = request.getSession().getId();        

        Map statements = null;
        if (this.cache.containsKey(cacheId)) {
            statements = (Map) this.cache.get(cacheId);            
        } else {
            statements = new HashMap();            
            this.cache.put(cacheId, statements);
        }
        statements.put(this.createStatement(resolutionContext), attributes);
    }
    

    /**
     * <p>
     * This retrieves any cached attributes for the supplied resolution context.
     * Returns null if nothing is cached.
     * </p>
     * 
     * @param resolutionContext <code>ResolutionContext</code>
     * @return <code>List</code> of attributes
     */
    private List getCachedAttributes(ResolutionContext resolutionContext) {
        // retrieve user session id
        HttpServletRequest request = (HttpServletRequest) resolutionContext.getRequest();
        String cacheId = request.getSession().getId();        

        List attributes = null;
        if (this.cache.containsKey(cacheId)) {
            Map statements = (Map) this.cache.get(cacheId);
            attributes = (List) statements.get(this.createStatement(resolutionContext));                                
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
    private Map mergeAttributes(Map attrs, Attributes newAttrs) throws AttributeResolutionException
    {
        // merge the new attributes
        try {
            Map newAttrsMap = LdapUtil.parseAttributes(newAttrs, true);
            
            Iterator i = newAttrsMap.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry pairs = (Map.Entry) i.next();
              String attrName = (String) pairs.getKey();
              List attrValues = (List) pairs.getValue();
              if (attrs.containsKey(attrName)) {
                List l = (List) attrs.get(attrName);
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
