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

package net.shibboleth.idp.attribute.resolver.spring.dc.ldap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.ldaptive.BindConnectionInitializer;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.DerefAliases;
import org.ldaptive.SearchExecutor;
import org.ldaptive.SearchScope;
import org.ldaptive.pool.BlockingConnectionPool;
import org.ldaptive.pool.IdlePruneStrategy;
import org.ldaptive.pool.PoolConfig;
import org.ldaptive.pool.PooledConnectionFactory;
import org.ldaptive.pool.SearchValidator;
import org.ldaptive.provider.ProviderConfig;
import org.ldaptive.ssl.CredentialConfig;
import org.ldaptive.ssl.SslConfig;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePropertySource;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Collections2;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.ssl.KeyStoreKeyManager;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustStoreTrustManager;

import net.shibboleth.ext.spring.context.FilesystemGenericApplicationContext;
import net.shibboleth.ext.spring.resource.PreferFileSystemResourceLoader;
import net.shibboleth.ext.spring.util.ApplicationContextBuilder;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.dc.impl.ExecutableSearchBuilder;
import net.shibboleth.idp.attribute.resolver.dc.ldap.impl.ConnectionFactoryValidator;
import net.shibboleth.idp.attribute.resolver.dc.ldap.impl.LDAPDataConnector;
import net.shibboleth.idp.attribute.resolver.dc.ldap.impl.StringAttributeValueMappingStrategy;
import net.shibboleth.idp.attribute.resolver.spring.dc.ldap.impl.LDAPDataConnectorParser;
import net.shibboleth.idp.saml.impl.TestSources;


/** Test for {@link LDAPDataConnectorParser}. */
public class LDAPDataConnectorParserTest {

    /** In-memory directory server. */
    private InMemoryDirectoryServer directoryServer;
    
    private GenericApplicationContext pendingTeardownContext = null;
    
    @AfterMethod public void tearDownTestContext() {
        if (null == pendingTeardownContext ) {
            return;
        }
        pendingTeardownContext.close();
        pendingTeardownContext = null;
    }
    
    protected void setTestContext(final GenericApplicationContext context) {
        tearDownTestContext();
        pendingTeardownContext = context;
    }

    /**
     * Creates an UnboundID in-memory directory server. Leverages LDIF found in test resources.
     * 
     * @throws LDAPException if the in-memory directory server cannot be created
     * @throws GeneralSecurityException if the startTLS keystore or truststore cannot be loaded
     */
    @BeforeTest public void setupDirectoryServer() throws LDAPException, GeneralSecurityException {
        final InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=shibboleth,dc=net");
        final SSLUtil sslUtil =
                new SSLUtil(new KeyStoreKeyManager(
                        "src/test/resources/net/shibboleth/idp/attribute/resolver/spring/dc/ldap/server.keystore",
                        "changeit".toCharArray()), new TrustStoreTrustManager(
                        "src/test/resources/net/shibboleth/idp/attribute/resolver/spring/dc/ldap/client.keystore",
                        "changeit".toCharArray(), "JKS", false));
        config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", null, 10389,
                sslUtil.createSSLSocketFactory()));
        config.addAdditionalBindCredentials("cn=Directory Manager", "password");
        directoryServer = new InMemoryDirectoryServer(config);
        directoryServer.importFromLDIF(true,
                "src/test/resources/net/shibboleth/idp/attribute/resolver/spring/dc/ldap/ldapDataConnectorTest.ldif");
        directoryServer.startListening();
    }

    /**
     * Shutdown the in-memory directory server.
     */
    @AfterTest public void teardownDirectoryServer() {
        directoryServer.shutDown(true);
    }

    @Test public void v2Config() throws Exception {
        final LDAPDataConnector dataConnector =
                getLdapDataConnector(new String[] {"net/shibboleth/idp/attribute/resolver/spring/dc/ldap/resolver/ldap-attribute-resolver-v2.xml"});
        assertNotNull(dataConnector);
        doTest(dataConnector);
        final StringAttributeValueMappingStrategy mappingStrategy =
                (StringAttributeValueMappingStrategy) dataConnector.getMappingStrategy();
        assertEquals(mappingStrategy.getResultRenamingMap().size(), 1);
        assertEquals(mappingStrategy.getResultRenamingMap().get("homephone"), "phonenumber");

        dataConnector.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, IdPAttribute> attrs = dataConnector.resolve(context);
        assertNotNull(attrs);
        assertEquals(2, attrs.size());
        assertEquals(attrs.get("employeeNumber").getValues().get(0).getNativeValue(), "C2J20hMNp7NlUwQ+");
        assertNotNull(attrs.get("entryDN"));
    }
    
    @Test public void v2NoSec() throws Exception {
        final LDAPDataConnector dataConnector =
                getLdapDataConnector(new String[] {"net/shibboleth/idp/attribute/resolver/spring/dc/ldap/resolver/ldap-attribute-resolver-v2-nosec.xml"});
        assertNotNull(dataConnector);
        doTest(dataConnector);
        final StringAttributeValueMappingStrategy mappingStrategy =
                (StringAttributeValueMappingStrategy) dataConnector.getMappingStrategy();
        assertEquals(mappingStrategy.getResultRenamingMap().size(), 1);
        assertEquals(mappingStrategy.getResultRenamingMap().get("homephone"), "phonenumber");

        dataConnector.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, IdPAttribute> attrs = dataConnector.resolve(context);
        assertNotNull(attrs);
        assertNotNull(attrs.get("entryDN"));
    }

    @Test public void v2ResolverOtherDups() throws Exception {
        final LDAPDataConnector dataConnector =
                getLdapDataConnector(new String[] {"net/shibboleth/idp/attribute/resolver/spring/dc/ldap/resolver/ldap-attribute-resolver-v2-multi.xml",
                        "net/shibboleth/idp/attribute/resolver/spring/dc/ldap/ldap-attribute-resolver-spring-context.xml"});
        assertNotNull(dataConnector);
        doTest(dataConnector);
    }

    @Test public void v2MinimalConfig() throws Exception {
        final LDAPDataConnector dataConnector =
                getLdapDataConnector(new String[] {"net/shibboleth/idp/attribute/resolver/spring/dc/ldap/resolver/ldap-attribute-resolver-v2-minimal.xml"});
        assertNotNull(dataConnector);
        assertEquals(Duration.ZERO, dataConnector.getNoRetryDelay());
        final DefaultConnectionFactory connFactory = (DefaultConnectionFactory) dataConnector.getConnectionFactory();
        assertNotNull(connFactory);

        final ConnectionConfig connConfig = connFactory.getConnectionConfig();
        assertNotNull(connConfig);
        assertEquals("ldap://localhost:10389", connConfig.getLdapUrl());
        assertEquals(false, connConfig.getUseSSL());
        assertEquals(false, connConfig.getUseStartTLS());
        final BindConnectionInitializer connInitializer = (BindConnectionInitializer) connConfig.getConnectionInitializer();
        assertEquals("cn=Directory Manager", connInitializer.getBindDn());
        assertEquals("password", connInitializer.getBindCredential().getString());
        assertEquals(3000, connConfig.getConnectTimeout());
        assertEquals(3000, connConfig.getResponseTimeout());

        final SslConfig sslConfig = connFactory.getConnectionConfig().getSslConfig();
        assertNotNull(sslConfig);
        final CredentialConfig credentialConfig = sslConfig.getCredentialConfig();
        assertNotNull(credentialConfig);

        final ProviderConfig<?> providerConfig = connFactory.getProvider().getProviderConfig();
        assertNotNull(providerConfig);
        assertTrue(providerConfig.getProperties().isEmpty());

        final SearchExecutor searchExecutor = dataConnector.getSearchExecutor();
        assertNotNull(searchExecutor);
        assertEquals("", searchExecutor.getBaseDn());
        assertNull(searchExecutor.getSearchFilter());
        assertEquals(3000, searchExecutor.getTimeLimit());

        final ConnectionFactoryValidator validator = (ConnectionFactoryValidator) dataConnector.getValidator();
        assertNotNull(validator);
        assertTrue(validator.isThrowValidateError());
        assertNotNull(validator.getConnectionFactory());

        final ExecutableSearchBuilder<?> searchBuilder = dataConnector.getExecutableSearchBuilder();
        assertNotNull(searchBuilder);

        final StringAttributeValueMappingStrategy mappingStrategy = (StringAttributeValueMappingStrategy) dataConnector.getMappingStrategy();
        assertNotNull(mappingStrategy);
        assertFalse(mappingStrategy.isNoResultAnError());
        assertFalse(mappingStrategy.isMultipleResultsAnError());

        assertNull(dataConnector.getResultsCache());
        
        dataConnector.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, IdPAttribute> attrs = dataConnector.resolve(context);
        assertNotNull(attrs);
        assertNotNull(attrs.get("entryDN"));
    }

    @Test public void v2MinimalPoolConfig() throws Exception {
        final LDAPDataConnector dataConnector =
                getLdapDataConnector(new String[] {"net/shibboleth/idp/attribute/resolver/spring/dc/ldap/resolver/ldap-attribute-resolver-v2-minimal-pool.xml"});
        assertNotNull(dataConnector);
        assertEquals(Duration.ZERO, dataConnector.getNoRetryDelay());
        final PooledConnectionFactory connFactory = (PooledConnectionFactory) dataConnector.getConnectionFactory();
        assertNotNull(connFactory);
        final BlockingConnectionPool connPool = (BlockingConnectionPool) connFactory.getConnectionPool();
        assertNotNull(connPool);
        assertEquals(0, connPool.getBlockWaitTime());
        assertEquals("resolver-pool", connPool.getName());
        final PoolConfig poolConfig = connPool.getPoolConfig();
        assertNotNull(poolConfig);
        assertEquals(0, poolConfig.getMinPoolSize());
        assertEquals(3, poolConfig.getMaxPoolSize());
        assertFalse(poolConfig.isValidatePeriodically());
        assertEquals(1800, poolConfig.getValidatePeriod());
        assertTrue(connPool.getFailFastInitialize());
        assertNull(connPool.getValidator());

        final IdlePruneStrategy pruneStrategy = (IdlePruneStrategy) connPool.getPruneStrategy();
        assertNotNull(pruneStrategy);
        assertEquals(300, pruneStrategy.getPrunePeriod());
        assertEquals(600, pruneStrategy.getIdleTime());

        final ConnectionConfig connConfig = connPool.getConnectionFactory().getConnectionConfig();
        assertNotNull(connConfig);
        assertEquals("ldap://localhost:10389", connConfig.getLdapUrl());
        assertEquals(false, connConfig.getUseSSL());
        assertEquals(false, connConfig.getUseStartTLS());
        final BindConnectionInitializer connInitializer = (BindConnectionInitializer) connConfig.getConnectionInitializer();
        assertEquals("cn=Directory Manager", connInitializer.getBindDn());
        assertEquals("password", connInitializer.getBindCredential().getString());
        assertEquals(3000, connConfig.getConnectTimeout());
        assertEquals(3000, connConfig.getResponseTimeout());

        final SslConfig sslConfig = connPool.getConnectionFactory().getConnectionConfig().getSslConfig();
        assertNotNull(sslConfig);
        final CredentialConfig credentialConfig = sslConfig.getCredentialConfig();
        assertNotNull(credentialConfig);

        final ProviderConfig<?> providerConfig = connPool.getConnectionFactory().getProvider().getProviderConfig();
        assertNotNull(providerConfig);
        assertTrue(providerConfig.getProperties().isEmpty());

        final SearchExecutor searchExecutor = dataConnector.getSearchExecutor();
        assertNotNull(searchExecutor);
        assertEquals("", searchExecutor.getBaseDn());
        assertNull(searchExecutor.getSearchFilter());
        assertEquals(3000, searchExecutor.getTimeLimit());

        final ConnectionFactoryValidator validator = (ConnectionFactoryValidator) dataConnector.getValidator();
        assertNotNull(validator);
        assertTrue(validator.isThrowValidateError());
        assertNotNull(validator.getConnectionFactory());

        final ExecutableSearchBuilder<?> searchBuilder = dataConnector.getExecutableSearchBuilder();
        assertNotNull(searchBuilder);

        final StringAttributeValueMappingStrategy mappingStrategy = (StringAttributeValueMappingStrategy) dataConnector.getMappingStrategy();
        assertNotNull(mappingStrategy);
        assertFalse(mappingStrategy.isNoResultAnError());
        assertFalse(mappingStrategy.isMultipleResultsAnError());

        assertNull(dataConnector.getResultsCache());
        
        dataConnector.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, IdPAttribute> attrs = dataConnector.resolve(context);
        assertNotNull(attrs);
        assertNotNull(attrs.get("entryDN"));
    }

    @Test public void v2JndiConfig() throws Exception {
        final LDAPDataConnector dataConnector =
          getLdapDataConnector(new String[] {"net/shibboleth/idp/attribute/resolver/spring/dc/ldap/resolver/ldap-attribute-resolver-v2-jndi.xml"});
        assertNotNull(dataConnector);
        assertEquals(Duration.ofMinutes(5), dataConnector.getNoRetryDelay());
        final DefaultConnectionFactory connFactory = (DefaultConnectionFactory) dataConnector.getConnectionFactory();
        assertNotNull(connFactory);

        final ConnectionConfig connConfig = connFactory.getConnectionConfig();
        assertNotNull(connConfig);
        assertEquals("ldap://localhost:10389", connConfig.getLdapUrl());
        assertEquals(false, connConfig.getUseSSL());
        assertEquals(true, connConfig.getUseStartTLS());
        final BindConnectionInitializer connInitializer = (BindConnectionInitializer) connConfig.getConnectionInitializer();
        assertEquals("cn=Directory Manager", connInitializer.getBindDn());
        assertEquals("password", connInitializer.getBindCredential().getString());
        assertEquals(2000, connConfig.getConnectTimeout());
        assertEquals(4000, connConfig.getResponseTimeout());

        final SslConfig sslConfig = connFactory.getConnectionConfig().getSslConfig();
        assertNotNull(sslConfig);
        final CredentialConfig credentialConfig = sslConfig.getCredentialConfig();
        assertNotNull(credentialConfig);

        final ProviderConfig providerConfig = connFactory.getProvider().getProviderConfig();
        assertNotNull(providerConfig);
        assertEquals("value1", providerConfig.getProperties().get("name1"));
        assertEquals("finding", providerConfig.getProperties().get("java.naming.ldap.derefAliases"));
        assertEquals("jpegPhoto employeeNumber", providerConfig.getProperties().get("java.naming.ldap.attributes.binary"));

        final SearchExecutor searchExecutor = dataConnector.getSearchExecutor();
        assertNotNull(searchExecutor);
        assertEquals("ou=people,dc=shibboleth,dc=net", searchExecutor.getBaseDn());
        assertNull(searchExecutor.getSearchFilter());
        assertEquals(7000, searchExecutor.getTimeLimit());
        assertEquals(SearchScope.SUBTREE, searchExecutor.getSearchScope());
        assertEquals(DerefAliases.FINDING, searchExecutor.getDerefAliases());
        assertEquals(new String[] {"jpegPhoto", "employeeNumber"}, searchExecutor.getBinaryAttributes());

        final ConnectionFactoryValidator validator = (ConnectionFactoryValidator) dataConnector.getValidator();
        assertNotNull(validator);
        assertTrue(validator.isThrowValidateError());
        assertNotNull(validator.getConnectionFactory());

        final ExecutableSearchBuilder searchBuilder = dataConnector.getExecutableSearchBuilder();
        assertNotNull(searchBuilder);

        final StringAttributeValueMappingStrategy mappingStrategy =(StringAttributeValueMappingStrategy) dataConnector.getMappingStrategy();
        assertNotNull(mappingStrategy);
        assertTrue(mappingStrategy.isNoResultAnError());
        assertTrue(mappingStrategy.isMultipleResultsAnError());

        assertNull(dataConnector.getResultsCache());

        dataConnector.initialize();
        final AttributeResolutionContext context =
          TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
            TestSources.SP_ENTITY_ID);
        final Map<String, IdPAttribute> attrs = dataConnector.resolve(context);
        assertNotNull(attrs);
        assertEquals(5, attrs.size());
        assertNotNull(attrs.get("cn"));
        assertNotNull(attrs.get("sn"));
        assertNotNull(attrs.get("jpegPhoto"));
        assertEquals(attrs.get("employeeNumber").getValues().get(0).getNativeValue(), "C2J20hMNp7NlUwQ+");
        assertNotNull(attrs.get("entryDN"));
    }

    @Test public void v2PropsConfig() throws Exception {
        final Resource props = new ClassPathResource("net/shibboleth/idp/attribute/resolver/spring/dc/ldap/ldap-v2.properties");
        final LDAPDataConnector dataConnector =
                getLdapDataConnector(props, new String[] {
                        "net/shibboleth/idp/attribute/resolver/spring/dc/ldap/resolver/ldap-attribute-resolver-v2-props.xml",});
        assertNotNull(dataConnector);
        doTest(dataConnector);

        dataConnector.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, IdPAttribute> attrs = dataConnector.resolve(context);
        assertNotNull(attrs);
        assertEquals(attrs.size(), 4);
        assertNotNull(attrs.get("uid"));
        assertNotNull(attrs.get("homephone"));
        assertNotNull(attrs.get("mail"));
        assertNotNull(attrs.get("entryDN"));
    }

    @Test public void springConfig() throws Exception {
        final LDAPDataConnector dataConnector =
                getLdapDataConnector(new String[] {"net/shibboleth/idp/attribute/resolver/spring/dc/ldap/resolver/ldap-attribute-resolver-spring.xml"});
        assertNotNull(dataConnector);
        doTest(dataConnector);

        dataConnector.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, IdPAttribute> attrs = dataConnector.resolve(context);
        assertNotNull(attrs);
    }

    @Test public void springPropsConfig() throws Exception,
            ResolutionException {
        final Resource props = new ClassPathResource("net/shibboleth/idp/attribute/resolver/spring/dc/ldap/ldap-v3.properties");
        final LDAPDataConnector dataConnector =
                getLdapDataConnector(props, new String[] {"net/shibboleth/idp/attribute/resolver/spring/dc/ldap/resolver/ldap-attribute-resolver-spring-props.xml"});
        assertNotNull(dataConnector);
        doTest(dataConnector);

        dataConnector.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, IdPAttribute> attrs = dataConnector.resolve(context);
        assertNotNull(attrs);
        assertEquals(attrs.size(), 3);
        assertNotNull(attrs.get("uid"));
        assertNotNull(attrs.get("phonenumber"));
        assertNotNull(attrs.get("mail"));
    }

    /**
     * This test will fail when it is time to revert the fixes put in for
     * https://issues.shibboleth.net/jira/browse/IDP-338.
     */
    @Test public void IdP338Canary() {
        final GenericApplicationContext context = new FilesystemGenericApplicationContext();
        setTestContext(context);
        context.setDisplayName("ApplicationContext: " + LDAPDataConnectorParserTest.class);

        final XmlBeanDefinitionReader configReader = new XmlBeanDefinitionReader(context);

        configReader.loadBeanDefinitions("net/shibboleth/idp/attribute/resolver/spring/dc/IdP338.xml");
        context.refresh();

        Object cbc, cc = null, cb, c;

        cbc = context.getBean(CacheBuilder.class);
        cc = context.getBean(Cache.class);
        cb = context.getBean("cacheBuilder");
        c = context.getBean("cache");
        final Object ccc = context.getBean(Cache.class);

        assertNotNull(cb);
        assertNotNull(c);
        assertNotNull(cbc);
        assertNotNull(ccc);
        assertNotNull(cc,
                "The Spring bug described in https://issues.shibboleth.net/jira/browse/IDP-338 has come back");

    }

    @Test public void hybridConfig() throws Exception {
        final LDAPDataConnector dataConnector =
                getLdapDataConnector(new String[] {
                        "net/shibboleth/idp/attribute/resolver/spring/dc/ldap/resolver/ldap-attribute-resolver-v2-hybrid.xml",
                        "net/shibboleth/idp/attribute/resolver/spring/dc/ldap/ldap-attribute-resolver-spring-context.xml"});
        assertNotNull(dataConnector);
        doTest(dataConnector);

        dataConnector.initialize();
        final StringAttributeValueMappingStrategy mappingStrategy =
                (StringAttributeValueMappingStrategy) dataConnector.getMappingStrategy();
        assertEquals(mappingStrategy.getResultRenamingMap().size(), 1);
        assertEquals(mappingStrategy.getResultRenamingMap().get("homephone"), "phonenumber");
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, IdPAttribute> attrs = dataConnector.resolve(context);
        assertNotNull(attrs);
        assertNull(attrs.get("homephone"));
        assertNotNull(attrs.get("phonenumber"));
        assertNotNull(attrs.get("entryDN"));
    }

    protected LDAPDataConnector getLdapDataConnector(final Resource properties, final String[] beanDefinitions) throws IOException {

        final ResourceLoader loader = new PreferFileSystemResourceLoader();
        
        final ApplicationContextBuilder builder = new ApplicationContextBuilder();
        builder.setName("ApplicationContext: " + LDAPDataConnectorParserTest.class);

        final Collection<String> defs = new ArrayList<>(Arrays.asList(beanDefinitions));
        defs.add("net/shibboleth/idp/attribute/resolver/spring/externalBeans.xml");

        builder.setServiceConfigurations(Collections2.transform(defs, s -> loader.getResource(s)));

        if (properties != null) {
            builder.setPropertySources(Collections.singletonList(new ResourcePropertySource(properties)));
        }
        
        final GenericApplicationContext context = builder.build();
        
        setTestContext(context);

        return (LDAPDataConnector) context.getBean("myLDAP");
    }
    
    protected LDAPDataConnector getLdapDataConnector(final String[] beanDefinitions) throws IOException {
        return getLdapDataConnector(null, beanDefinitions);
    }

    protected void doTest(final LDAPDataConnector dataConnector) throws ResolutionException {

        final String id = dataConnector.getId();
        assertEquals("myLDAP", id);
        assertEquals(Duration.ofMinutes(5), dataConnector.getNoRetryDelay());

        final PooledConnectionFactory connFactory = (PooledConnectionFactory) dataConnector.getConnectionFactory();
        assertNotNull(connFactory);
        final BlockingConnectionPool connPool = (BlockingConnectionPool) connFactory.getConnectionPool();
        assertNotNull(connPool);
        assertEquals(5000, connPool.getBlockWaitTime());
        assertEquals("resolver-pool", connPool.getName());
        final PoolConfig poolConfig = connPool.getPoolConfig();
        assertNotNull(poolConfig);
        assertEquals(5, poolConfig.getMinPoolSize());
        assertEquals(10, poolConfig.getMaxPoolSize());
        assertTrue(poolConfig.isValidatePeriodically());
        assertEquals(900, poolConfig.getValidatePeriod());
        assertFalse(connPool.getFailFastInitialize());

        final SearchValidator searchValidator = (SearchValidator) connPool.getValidator();
        assertNotNull(searchValidator);
        assertEquals("dc=shibboleth,dc=net", searchValidator.getSearchRequest().getBaseDn());
        assertEquals("(ou=people)", searchValidator.getSearchRequest().getSearchFilter().getFilter());

        final IdlePruneStrategy pruneStrategy = (IdlePruneStrategy) connPool.getPruneStrategy();
        assertNotNull(pruneStrategy);
        assertEquals(300, pruneStrategy.getPrunePeriod());
        assertEquals(600, pruneStrategy.getIdleTime());

        final ConnectionConfig connConfig = connPool.getConnectionFactory().getConnectionConfig();
        assertNotNull(connConfig);
        assertEquals("ldap://localhost:10389", connConfig.getLdapUrl());
        assertEquals(false, connConfig.getUseSSL());
        assertEquals(true, connConfig.getUseStartTLS());
        final BindConnectionInitializer connInitializer = (BindConnectionInitializer) connConfig.getConnectionInitializer();
        assertEquals("cn=Directory Manager", connInitializer.getBindDn());
        assertEquals("password", connInitializer.getBindCredential().getString());
        assertEquals(2000, connConfig.getConnectTimeout());
        assertEquals(4000, connConfig.getResponseTimeout());

        final SslConfig sslConfig = connPool.getConnectionFactory().getConnectionConfig().getSslConfig();
        assertNotNull(sslConfig);
        final CredentialConfig credentialConfig = sslConfig.getCredentialConfig();
        assertNotNull(credentialConfig);

        final Map<String, Object> providerProps = new HashMap<>();
        providerProps.put("name1", "value1");
        providerProps.put("name2", "value2");
        final ProviderConfig<?> providerConfig = connPool.getConnectionFactory().getProvider().getProviderConfig();
        assertNotNull(providerConfig);
        assertEquals(providerProps, providerConfig.getProperties());

        final SearchExecutor searchExecutor = dataConnector.getSearchExecutor();
        assertNotNull(searchExecutor);
        assertEquals("ou=people,dc=shibboleth,dc=net", searchExecutor.getBaseDn());
        assertNull(searchExecutor.getSearchFilter());
        assertEquals(7000, searchExecutor.getTimeLimit());

        final ConnectionFactoryValidator validator = (ConnectionFactoryValidator) dataConnector.getValidator();
        assertNotNull(validator);
        assertTrue(validator.isThrowValidateError());
        assertNotNull(validator.getConnectionFactory());

        final ExecutableSearchBuilder<?> searchBuilder = dataConnector.getExecutableSearchBuilder();
        assertNotNull(searchBuilder);

        final StringAttributeValueMappingStrategy mappingStrategy = (StringAttributeValueMappingStrategy) dataConnector.getMappingStrategy();
        assertNotNull(mappingStrategy);
        assertTrue(mappingStrategy.isNoResultAnError());
        assertTrue(mappingStrategy.isMultipleResultsAnError());

        final Cache<String, Map<String, IdPAttribute>> resultCache = dataConnector.getResultsCache();
        assertNotNull(resultCache);
    }
}
