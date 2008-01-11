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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opensaml.xml.security.x509.X509Credential;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.TemplateEngine.CharacterEscapingStrategy;
import edu.internet2.middleware.shibboleth.common.session.LogoutEvent;
import edu.vt.middleware.ldap.Ldap;
import edu.vt.middleware.ldap.LdapConfig;
import edu.vt.middleware.ldap.LdapPool;
import edu.vt.middleware.ldap.LdapUtil;

/**
 * <code>LdapDataConnector</code> provides a plugin to retrieve attributes from an LDAP.
 */
public class LdapDataConnector extends BaseDataConnector implements ApplicationListener {

    /** Search scope values. */
    public static enum SEARCH_SCOPE {
        /** Object level search scope. */
        OBJECT,
        /** One level search scope. */
        ONELEVEL,
        /** Subtree search scope. */
        SUBTREE
    };

    /** Authentication type values. */
    public static enum AUTHENTICATION_TYPE {
        /** Anonymous authentication type. */
        ANONYMOUS,
        /** Simple authentication type. */
        SIMPLE,
        /** Strong authentication type. */
        STRONG,
        /** External authentication type. */
        EXTERNAL,
        /** Digest MD5 authentication type. */
        DIGEST_MD5,
        /** Cram MD5 authentication type. */
        CRAM_MD5,
        /** Kerberos authentication type. */
        GSSAPI
    };

    /** Class logger. */
    private static Logger log = LoggerFactory.getLogger(LdapDataConnector.class);

    /** SSL trust managers. */
    private TrustManager[] sslTrustManagers;

    /** SSL key managers. */
    private KeyManager[] sslKeyManagers;

    /** Whether multiple result sets should be merged. */
    private boolean mergeMultipleResults;

    /** Whether an empty result set is an error. */
    private boolean noResultsIsError;

    /** Whether to cache search results for the duration of the session. */
    private boolean cacheResults;

    /** Template engine used to change filter template into actual filter. */
    private TemplateEngine filterCreator;

    /** Name the filter template is registered under within the template engine. */
    private String filterTemplateName;

    /** Template that produces the query to use. */
    private String filterTemplate;

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
    private Map<String, Map<String, Map<String, BaseAttribute>>> cache;

    /** Whether this data connector has been initialized. */
    private boolean initialized;

    /** Filter value escaping strategy. */
    private final LDAPValueEscapingStrategy escapingStrategy;

    /**
     * This creates a new ldap data connector with the supplied properties.
     * 
     * @param ldapUrl <code>String</code> to connect to
     * @param ldapBaseDn <code>String</code> to begin searching at
     * @param startTls <code>boolean</code> whether connection should startTls
     * @param maxIdle <code>int</code> maximum number of idle pool objects
     * @param initIdleCapacity <code>int</code> initial capacity of the pool
     */
    public LdapDataConnector(String ldapUrl, String ldapBaseDn, boolean startTls, int maxIdle, int initIdleCapacity) {
        super();
        ldapConfig = new LdapConfig(ldapUrl, ldapBaseDn);
        ldapConfig.useTls(startTls);
        poolMaxIdle = maxIdle;
        poolInitIdleCapacity = initIdleCapacity;
        escapingStrategy = new LDAPValueEscapingStrategy();
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
            cache = new HashMap<String, Map<String, Map<String, BaseAttribute>>>();
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
            filterTemplateName = "shibboleth.resolver.dc." + getId();
            filterCreator.registerTemplate(filterTemplateName, filterTemplate);
        }
    }

    /**
     * This returns whether this connector will merge multiple search results into one result. The default is false.
     * 
     * @return <code>boolean</code>
     */
    public boolean isMergeResults() {
        return mergeMultipleResults;
    }

    /**
     * This sets whether this connector will merge multiple search results into one result. This method will remove any
     * cached results.
     * 
     * @see {@link #clearCache()}.
     * 
     * @param b <code>boolean</code>
     */
    public void setMergeResults(boolean b) {
        mergeMultipleResults = b;
        clearCache();
    }

    /**
     * This returns whether this connector will cache search results. The default is false.
     * 
     * @return <code>boolean</code>
     */
    public boolean isCacheResults() {
        return cacheResults;
    }

    /**
     * This sets whether this connector will cache search results.
     * 
     * @see {@link #initializeCache()}.
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
     * This returns whether this connector will throw an exception if no search results are found. The default is false.
     * 
     * @return <code>boolean</code>
     */
    public boolean isNoResultsIsError() {
        return noResultsIsError;
    }

    /**
     * This sets whether this connector will throw an exception if no search results are found.
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
     * This returns the URL this connector is using.
     * 
     * @return <code>String</code>
     */
    public String getLdapUrl() {
        return ldapConfig.getHost();
    }

    /**
     * This returns the base DN this connector is using.
     * 
     * @return <code>String</code>
     */
    public String getBaseDn() {
        return ldapConfig.getBase();
    }

    /**
     * This returns whether this connector will start TLS for all connections to the ldap.
     * 
     * @return <code>boolean</code>
     */
    public boolean isUseStartTls() {
        return ldapConfig.isTlsEnabled();
    }

    /**
     * This returns the SSL Socket Factory that will be used for all TLS and SSL connections to the ldap.
     * 
     * @return <code>SSLSocketFactory</code>
     */
    public SSLSocketFactory getSslSocketFactory() {
        return ldapConfig.getSslSocketFactory();
    }

    /**
     * This sets the SSL Socket Factory that will be used for all TLS and SSL connections to the ldap. This method will
     * remove any cached results and initialize the ldap pool.
     * 
     * @see {@link #clearCache()} and {@link #initializeLdapPool()}.
     * 
     * @param sf <code>SSLSocketFactory</code>
     */
    public void setSslSocketFactory(SSLSocketFactory sf) {
        ldapConfig.setSslSocketFactory(sf);
        clearCache();
        initializeLdapPool();
    }

    /**
     * This returns the trust managers that will be used for all TLS and SSL connections to the ldap.
     * 
     * @return <code>TrustManager[]</code>
     */
    public TrustManager[] getSslTrustManagers() {
        return sslTrustManagers;
    }

    /**
     * This sets the trust managers that will be used for all TLS and SSL connections to the ldap. This method will
     * remove any cached results and initialize the ldap pool.
     * 
     * @see {@link #clearCache()}, {@link #initializeLdapPool()}, and {@link #setSslSocketFactory(SSLSocketFactory)}.
     * 
     * @param tc <code>X509Credential</code> to create TrustManagers with
     */
    public void setSslTrustManagers(X509Credential tc) {
        if (tc != null) {
            try {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
                for (X509Certificate c : tc.getEntityCertificateChain()) {
                    keystore.setCertificateEntry("ldap_tls_trust_" + c.getSerialNumber(), c);
                }
                tmf.init(keystore);
                sslTrustManagers = tmf.getTrustManagers();
                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(sslKeyManagers, sslTrustManagers, null);
                ldapConfig.setSslSocketFactory(ctx.getSocketFactory());
                clearCache();
                initializeLdapPool();
            } catch (GeneralSecurityException e) {
                log.error("Error initializing trust managers", e);
            }
        }
    }

    /**
     * This returns the key managers that will be used for all TLS and SSL connections to the ldap.
     * 
     * @return <code>KeyManager[]</code>
     */
    public KeyManager[] getSslKeyManagers() {
        return sslKeyManagers;
    }

    /**
     * This sets the key managers that will be used for all TLS and SSL connections to the ldap. This method will remove
     * any cached results and initialize the ldap pool.
     * 
     * @see {@link #clearCache()}, {@link #initializeLdapPool()}, and {@link #setSslSocketFactory(SSLSocketFactory)}.
     * 
     * @param kc <code>X509Credential</code> to create KeyManagers with
     */
    public void setSslKeyManagers(X509Credential kc) {
        if (kc != null) {
            try {
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
                keystore.setKeyEntry("ldap_tls_client_auth", kc.getPrivateKey(), "changeit".toCharArray(), kc
                        .getEntityCertificateChain().toArray(new X509Certificate[0]));
                kmf.init(keystore, "changeit".toCharArray());
                sslKeyManagers = kmf.getKeyManagers();
                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(sslKeyManagers, sslTrustManagers, null);
                ldapConfig.setSslSocketFactory(ctx.getSocketFactory());
                clearCache();
                initializeLdapPool();
            } catch (GeneralSecurityException e) {
                log.error("Error initializing key managers", e);
            }
        }
    }

    /**
     * This returns the hostname verifier that will be used for all TLS and SSL connections to the ldap.
     * 
     * @return <code>HostnameVerifier</code>
     */
    public HostnameVerifier getHostnameVerifier() {
        return ldapConfig.getHostnameVerifier();
    }

    /**
     * This sets the hostname verifier that will be used for all TLS and SSL connections to the ldap. This method will
     * remove any cached results and initialize the ldap pool.
     * 
     * @see {@link #clearCache()} and {@link #initializeLdapPool()}.
     * 
     * @param hv <code>HostnameVerifier</code>
     */
    public void setHostnameVerifier(HostnameVerifier hv) {
        ldapConfig.setHostnameVerifier(hv);
        clearCache();
        initializeLdapPool();
    }

    /**
     * This returns the authentication type used when binding to the ldap.
     * 
     * @return <code>AUTHENTICATION_TYPE</code>
     */
    public AUTHENTICATION_TYPE getAuthenticationType() {
        AUTHENTICATION_TYPE type = null;
        if (ldapConfig.isAnonymousAuth()) {
            type = AUTHENTICATION_TYPE.ANONYMOUS;
        } else if (ldapConfig.isSimpleAuth()) {
            type = AUTHENTICATION_TYPE.SIMPLE;
        } else if (ldapConfig.isStrongAuth()) {
            type = AUTHENTICATION_TYPE.STRONG;
        } else if (ldapConfig.isExternalAuth()) {
            type = AUTHENTICATION_TYPE.EXTERNAL;
        } else if (ldapConfig.isDigestMD5Auth()) {
            type = AUTHENTICATION_TYPE.DIGEST_MD5;
        } else if (ldapConfig.isCramMD5Auth()) {
            type = AUTHENTICATION_TYPE.CRAM_MD5;
        } else if (ldapConfig.isGSSAPIAuth()) {
            type = AUTHENTICATION_TYPE.GSSAPI;
        }
        return type;
    }

    /**
     * This sets the authentication type used when binding to the ldap. This method will remove any cached results and
     * initialize the ldap pool.
     * 
     * @see {@link #clearCache()} and {@link #initializeLdapPool()}.
     * 
     * @param type <code>AUTHENTICATION_TYPE</code>
     */
    public void setAuthenticationType(AUTHENTICATION_TYPE type) {
        if (type == AUTHENTICATION_TYPE.ANONYMOUS) {
            ldapConfig.useAnonymousAuth();
        } else if (type == AUTHENTICATION_TYPE.SIMPLE) {
            ldapConfig.useSimpleAuth();
        } else if (type == AUTHENTICATION_TYPE.STRONG) {
            ldapConfig.useStrongAuth();
        } else if (type == AUTHENTICATION_TYPE.EXTERNAL) {
            ldapConfig.useExternalAuth();
        } else if (type == AUTHENTICATION_TYPE.DIGEST_MD5) {
            ldapConfig.useDigestMD5Auth();
        } else if (type == AUTHENTICATION_TYPE.CRAM_MD5) {
            ldapConfig.useCramMD5Auth();
        } else if (type == AUTHENTICATION_TYPE.GSSAPI) {
            ldapConfig.useGSSAPIAuth();
        }
        clearCache();
        initializeLdapPool();
    }

    /**
     * This returns the search scope used when searching the ldap.
     * 
     * @return <code>int</code>
     */
    public SEARCH_SCOPE getSearchScope() {
        SEARCH_SCOPE scope = null;
        if (ldapConfig.isObjectSearchScope()) {
            scope = SEARCH_SCOPE.OBJECT;
        } else if (ldapConfig.isOneLevelSearchScope()) {
            scope = SEARCH_SCOPE.ONELEVEL;
        } else if (ldapConfig.isSubTreeSearchScope()) {
            scope = SEARCH_SCOPE.SUBTREE;
        }
        return scope;
    }

    /**
     * This sets the search scope used when searching the ldap. This method will remove any cached results.
     * 
     * @see {@link #clearCache()}.
     * 
     * @param scope directory search scope
     */
    public void setSearchScope(SEARCH_SCOPE scope) {
        if (scope == SEARCH_SCOPE.OBJECT) {
            ldapConfig.useObjectSearchScope();
        } else if (scope == SEARCH_SCOPE.SUBTREE) {
            ldapConfig.useSubTreeSearchScope();
        } else if (scope == SEARCH_SCOPE.ONELEVEL) {
            ldapConfig.useOneLevelSearchScope();
        }
        clearCache();
    }

    /**
     * This returns the attributes that all searches will request from the ldap.
     * 
     * @return <code>String[]</code>
     */
    public String[] getReturnAttributes() {
        return returnAttributes;
    }

    /**
     * This sets the attributes that all searches will request from the ldap. This method will remove any cached
     * results.
     * 
     * @see {@link #clearCache()}.
     * 
     * @param s <code>String[]</code>
     */
    public void setReturnAttributes(String[] s) {
        returnAttributes = s;
        clearCache();
    }

    /**
     * This sets the attributes that all searches will request from the ldap. s should be a comma delimited string.
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
     * This returns the time in milliseconds that the ldap will wait for search results. A value of 0 means to wait
     * indefinitely.
     * 
     * @return <code>int</code> milliseconds
     */
    public int getSearchTimeLimit() {
        return ldapConfig.getTimeLimit();
    }

    /**
     * This sets the time in milliseconds that the ldap will wait for search results. A value of 0 means to wait
     * indefinitely. This method will remove any cached results.
     * 
     * @see {@link #clearCache()}.
     * 
     * @param i <code>int</code> milliseconds
     */
    public void setSearchTimeLimit(int i) {
        ldapConfig.setTimeLimit(i);
        clearCache();
    }

    /**
     * This returns the maximum number of search results the ldap will return. A value of 0 all entries will be
     * returned.
     * 
     * @return <code>long</code> maximum number of search results
     */
    public long getMaxResultSize() {
        return ldapConfig.getCountLimit();
    }

    /**
     * This sets the maximum number of search results the ldap will return. A value of 0 all entries will be returned.
     * This method will remove any cached results.
     * 
     * @see {@link #clearCache()}.
     * 
     * @param l <code>long</code> maximum number of search results
     */
    public void setMaxResultSize(long l) {
        ldapConfig.setCountLimit(l);
        clearCache();
    }

    /**
     * This returns whether objects will be returned in the search results. The default is false.
     * 
     * @return <code>boolean</code>
     */
    public boolean isReturningObjects() {
        return ldapConfig.getReturningObjFlag();
    }

    /**
     * This sets whether objects will be returned in the search results. This method will remove any cached results.
     * 
     * @see {@link #clearCache()}.
     * 
     * @param b <code>boolean</code>
     */
    public void setReturningObjects(boolean b) {
        ldapConfig.setReturningObjFlag(b);
        clearCache();
    }

    /**
     * This returns whether link dereferencing will be used during the search. The default is false.
     * 
     * @return <code>boolean</code>
     */
    public boolean isLinkDereferencing() {
        return ldapConfig.getDerefLinkFlag();
    }

    /**
     * This sets whether link dereferencing will be used during the search. This method will remove any cached results.
     * 
     * @see {@link #clearCache()}.
     * 
     * @param b <code>boolean</code>
     */
    public void setLinkDereferencing(boolean b) {
        ldapConfig.setDerefLinkFlag(b);
        clearCache();
    }

    /**
     * This returns the principal dn used to bind to the ldap for all searches.
     * 
     * @return <code>String</code> principal dn
     */
    public String getPrincipal() {
        return ldapConfig.getServiceUser();
    }

    /**
     * This sets the principal dn used to bind to the ldap for all searches. This method will remove any cached results
     * and initialize the ldap pool.
     * 
     * @see {@link #clearCache()} and {@link #initializeLdapPool()}.
     * 
     * @param s <code>String</code> principal dn
     */
    public void setPrincipal(String s) {
        ldapConfig.setServiceUser(s);
        clearCache();
        initializeLdapPool();
    }

    /**
     * This returns the principal credential used to bind to the ldap for all searches.
     * 
     * @return <code>String</code> principal credential
     */
    public String getPrincipalCredential() {
        return (String) ldapConfig.getServiceCredential();
    }

    /**
     * This sets the principal credential used to bind to the ldap for all searches. This method will remove any cached
     * results and initialize the ldap pool.
     * 
     * @see {@link #clearCache()} and {@link #initializeLdapPool()}.
     * 
     * @param s <code>String</code> principal credential
     */
    public void setPrincipalCredential(String s) {
        ldapConfig.setServiceCredential(s);
        clearCache();
        initializeLdapPool();
    }

    /**
     * This sets additional ldap context environment properties. This method will remove any cached results and
     * initialize the ldap pool.
     * 
     * @see {@link #clearCache()} and {@link #initializeLdapPool()}.
     * 
     * @param ldapProperties <code>Map</code> of name/value pairs
     */
    public void setLdapProperties(Map<String, String> ldapProperties) {
        for (Map.Entry<String, String> entry : ldapProperties.entrySet()) {
            ldapConfig.setEnvironmentProperties(entry.getKey(), entry.getValue());
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
            cache.remove(logoutEvent.getUserSession().getPrincipalName());
        }
    }

    /** {@inheritDoc} */
    public Map<String, BaseAttribute> resolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        log.debug("Begin resolve for {}", resolutionContext.getAttributeRequestContext().getPrincipalName());

        String searchFilter = filterCreator.createStatement(filterTemplateName, resolutionContext, getDependencyIds(),
                escapingStrategy);
        log.debug("Search filter: {}", searchFilter);

        // create Attribute objects to return
        Map<String, BaseAttribute> attributes = null;

        // check for cached data
        if (cacheResults) {
            attributes = getCachedAttributes(resolutionContext, searchFilter);
            if (attributes != null && log.isDebugEnabled()) {
                log.debug("Returning attributes from cache");
            }
        }

        // results not found in the cache
        if (attributes == null) {
            log.debug("Retrieving attributes from LDAP");
            Iterator<SearchResult> results = searchLdap(searchFilter);
            // check for empty result set
            if (noResultsIsError && !results.hasNext()) {
                throw new AttributeResolutionException("No LDAP entry found for "
                        + resolutionContext.getAttributeRequestContext().getPrincipalName());
            }
            // build resolved attributes from LDAP attributes
            attributes = buildBaseAttributes(results);
            if (cacheResults && attributes != null) {
                setCachedAttributes(resolutionContext, searchFilter, attributes);
                log.debug("Stored results in the cache");
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
    protected Map<String, BaseAttribute> buildBaseAttributes(Iterator<SearchResult> results)
            throws AttributeResolutionException {

        Map<String, BaseAttribute> attributes = new HashMap<String, BaseAttribute>();

        if (!results.hasNext()) {
            //
            // Nothing to add, return the empty set
            //
            return attributes;
        }

        SearchResult sr = results.next();
        Map<String, List<String>> attrs = mergeAttributes(new HashMap<String, List<String>>(), sr.getAttributes());
        // merge additional results if requested
        while (mergeMultipleResults && results.hasNext()) {
            SearchResult additionalResult = results.next();
            attrs = mergeAttributes(attrs, additionalResult.getAttributes());
        }
        // populate list of attributes
        for (Map.Entry<String, List<String>> entry : attrs.entrySet()) {
            log.debug("Found the following attribute: {}", entry);
            BasicAttribute<String> attribute = new BasicAttribute<String>();
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
    protected void setCachedAttributes(ShibbolethResolutionContext resolutionContext, String searchFiler,
            Map<String, BaseAttribute> attributes) {
        Map<String, Map<String, BaseAttribute>> results = null;
        String principal = resolutionContext.getAttributeRequestContext().getPrincipalName();
        if (cache.containsKey(principal)) {
            results = cache.get(principal);
        } else {
            results = new HashMap<String, Map<String, BaseAttribute>>();
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
    protected Map<String, BaseAttribute> getCachedAttributes(ShibbolethResolutionContext resolutionContext,
            String searchFilter) {
        Map<String, BaseAttribute> attributes = null;
        if (cacheResults) {
            String principal = resolutionContext.getAttributeRequestContext().getPrincipalName();
            if (cache.containsKey(principal)) {
                Map<String, Map<String, BaseAttribute>> results = cache.get(principal);
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

    /**
     * Escapes values that will be included within an LDAP filter.
     */
    protected class LDAPValueEscapingStrategy implements CharacterEscapingStrategy {

        /** {@inheritDoc} */
        public String escape(String value) {
            return value.replace("*", "\\*").replace("(", "\\(").replace(")", "\\)").replace("\\", "\\");
        }
    }
}
