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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.shibboleth.ext.spring.config.DurationToLongConverter;
import net.shibboleth.ext.spring.config.StringToIPRangeConverter;
import net.shibboleth.ext.spring.config.StringToResourceConverter;
import net.shibboleth.ext.spring.context.FilesystemGenericApplicationContext;
import net.shibboleth.ext.spring.util.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.dc.impl.ExecutableSearchBuilder;
import net.shibboleth.idp.attribute.resolver.dc.ldap.impl.ConnectionFactoryValidator;
import net.shibboleth.idp.attribute.resolver.dc.ldap.impl.LDAPDataConnector;
import net.shibboleth.idp.attribute.resolver.dc.ldap.impl.StringAttributeValueMappingStrategy;
import net.shibboleth.idp.attribute.resolver.spring.dc.ldap.impl.LDAPDataConnectorParser;
import net.shibboleth.idp.saml.impl.TestSources;

import org.ldaptive.BindConnectionInitializer;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.SearchExecutor;
import org.ldaptive.pool.BlockingConnectionPool;
import org.ldaptive.pool.IdlePruneStrategy;
import org.ldaptive.pool.PoolConfig;
import org.ldaptive.pool.PooledConnectionFactory;
import org.ldaptive.pool.SearchValidator;
import org.ldaptive.provider.ProviderConfig;
import org.ldaptive.ssl.CredentialConfig;
import org.ldaptive.ssl.SslConfig;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.ssl.KeyStoreKeyManager;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustStoreTrustManager;

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
                getLdapDataConnector(new String[] {"net/shibboleth/idp/attribute/resolver/spring/dc/ldap/ldap-attribute-resolver-v2.xml"});
        Assert.assertNotNull(dataConnector);
        doTest(dataConnector);
        final StringAttributeValueMappingStrategy mappingStrategy =
                (StringAttributeValueMappingStrategy) dataConnector.getMappingStrategy();
        Assert.assertEquals(mappingStrategy.getResultRenamingMap().size(), 1);
        Assert.assertEquals(mappingStrategy.getResultRenamingMap().get("homephone"), "phonenumber");

        dataConnector.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, IdPAttribute> attrs = dataConnector.resolve(context);
        Assert.assertNotNull(attrs);
        Assert.assertNotNull(attrs.get("entryDN"));
    }
    
    @Test public void v2Resolver() throws Exception {
        final LDAPDataConnector dataConnector =
                getLdapDataConnector(new String[] {"net/shibboleth/idp/attribute/resolver/spring/dc/ldap/resolver/ldap-attribute-resolver-v2.xml"});
        Assert.assertNotNull(dataConnector);
        doTest(dataConnector);
        final StringAttributeValueMappingStrategy mappingStrategy =
                (StringAttributeValueMappingStrategy) dataConnector.getMappingStrategy();
        Assert.assertEquals(mappingStrategy.getResultRenamingMap().size(), 1);
        Assert.assertEquals(mappingStrategy.getResultRenamingMap().get("homephone"), "phonenumber");

        dataConnector.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, IdPAttribute> attrs = dataConnector.resolve(context);
        Assert.assertNotNull(attrs);
        Assert.assertNotNull(attrs.get("entryDN"));
    }


    @Test public void v2MinimalConfig() throws Exception {
        final LDAPDataConnector dataConnector =
                getLdapDataConnector(new String[] {"net/shibboleth/idp/attribute/resolver/spring/dc/ldap/ldap-attribute-resolver-v2-minimal.xml"});
        Assert.assertNotNull(dataConnector);
        AssertJUnit.assertEquals(0, dataConnector.getNoRetryDelay());
        final DefaultConnectionFactory connFactory = (DefaultConnectionFactory) dataConnector.getConnectionFactory();
        AssertJUnit.assertNotNull(connFactory);

        final ConnectionConfig connConfig = connFactory.getConnectionConfig();
        AssertJUnit.assertNotNull(connConfig);
        AssertJUnit.assertEquals("ldap://localhost:10389", connConfig.getLdapUrl());
        AssertJUnit.assertEquals(false, connConfig.getUseSSL());
        AssertJUnit.assertEquals(false, connConfig.getUseStartTLS());
        final BindConnectionInitializer connInitializer = (BindConnectionInitializer) connConfig.getConnectionInitializer();
        AssertJUnit.assertEquals("cn=Directory Manager", connInitializer.getBindDn());
        AssertJUnit.assertEquals("password", connInitializer.getBindCredential().getString());
        AssertJUnit.assertEquals(3000, connConfig.getConnectTimeout());
        AssertJUnit.assertEquals(3000, connConfig.getResponseTimeout());

        final SslConfig sslConfig = connFactory.getConnectionConfig().getSslConfig();
        AssertJUnit.assertNotNull(sslConfig);
        final CredentialConfig credentialConfig = sslConfig.getCredentialConfig();
        AssertJUnit.assertNotNull(credentialConfig);

        final ProviderConfig providerConfig = connFactory.getProvider().getProviderConfig();
        AssertJUnit.assertNotNull(providerConfig);
        AssertJUnit.assertTrue(providerConfig.getProperties().isEmpty());

        final SearchExecutor searchExecutor = dataConnector.getSearchExecutor();
        AssertJUnit.assertNotNull(searchExecutor);
        AssertJUnit.assertEquals("", searchExecutor.getBaseDn());
        AssertJUnit.assertNotNull(searchExecutor.getSearchFilter().getFilter());
        AssertJUnit.assertEquals(3000, searchExecutor.getTimeLimit());

        final ConnectionFactoryValidator validator = (ConnectionFactoryValidator) dataConnector.getValidator();
        AssertJUnit.assertNotNull(validator);
        AssertJUnit.assertTrue(validator.isThrowValidateError());
        AssertJUnit.assertNotNull(validator.getConnectionFactory());

        final ExecutableSearchBuilder searchBuilder = dataConnector.getExecutableSearchBuilder();
        AssertJUnit.assertNotNull(searchBuilder);

        final StringAttributeValueMappingStrategy mappingStrategy = (StringAttributeValueMappingStrategy) dataConnector.getMappingStrategy();
        AssertJUnit.assertNotNull(mappingStrategy);
        AssertJUnit.assertFalse(mappingStrategy.isNoResultAnError());
        AssertJUnit.assertFalse(mappingStrategy.isMultipleResultsAnError());

        AssertJUnit.assertNull(dataConnector.getResultsCache());
        
        dataConnector.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, IdPAttribute> attrs = dataConnector.resolve(context);
        Assert.assertNotNull(attrs);
        Assert.assertNotNull(attrs.get("entryDN"));
    }

    @Test public void v2MinimalPoolConfig() throws Exception {
        final LDAPDataConnector dataConnector =
                getLdapDataConnector(new String[] {"net/shibboleth/idp/attribute/resolver/spring/dc/ldap/ldap-attribute-resolver-v2-minimal-pool.xml"});
        Assert.assertNotNull(dataConnector);
        AssertJUnit.assertEquals(0, dataConnector.getNoRetryDelay());
        final PooledConnectionFactory connFactory = (PooledConnectionFactory) dataConnector.getConnectionFactory();
        AssertJUnit.assertNotNull(connFactory);
        final BlockingConnectionPool connPool = (BlockingConnectionPool) connFactory.getConnectionPool();
        AssertJUnit.assertNotNull(connPool);
        AssertJUnit.assertEquals(0, connPool.getBlockWaitTime());
        AssertJUnit.assertEquals("resolver-pool", connPool.getName());
        final PoolConfig poolConfig = connPool.getPoolConfig();
        AssertJUnit.assertNotNull(poolConfig);
        AssertJUnit.assertEquals(0, poolConfig.getMinPoolSize());
        AssertJUnit.assertEquals(3, poolConfig.getMaxPoolSize());
        AssertJUnit.assertFalse(poolConfig.isValidatePeriodically());
        AssertJUnit.assertEquals(1800, poolConfig.getValidatePeriod());
        AssertJUnit.assertTrue(connPool.getFailFastInitialize());
        AssertJUnit.assertNull(connPool.getValidator());

        final IdlePruneStrategy pruneStrategy = (IdlePruneStrategy) connPool.getPruneStrategy();
        AssertJUnit.assertNotNull(pruneStrategy);
        AssertJUnit.assertEquals(300, pruneStrategy.getPrunePeriod());
        AssertJUnit.assertEquals(600, pruneStrategy.getIdleTime());

        final ConnectionConfig connConfig = connPool.getConnectionFactory().getConnectionConfig();
        AssertJUnit.assertNotNull(connConfig);
        AssertJUnit.assertEquals("ldap://localhost:10389", connConfig.getLdapUrl());
        AssertJUnit.assertEquals(false, connConfig.getUseSSL());
        AssertJUnit.assertEquals(false, connConfig.getUseStartTLS());
        final BindConnectionInitializer connInitializer = (BindConnectionInitializer) connConfig.getConnectionInitializer();
        AssertJUnit.assertEquals("cn=Directory Manager", connInitializer.getBindDn());
        AssertJUnit.assertEquals("password", connInitializer.getBindCredential().getString());
        AssertJUnit.assertEquals(3000, connConfig.getConnectTimeout());
        AssertJUnit.assertEquals(3000, connConfig.getResponseTimeout());

        final SslConfig sslConfig = connPool.getConnectionFactory().getConnectionConfig().getSslConfig();
        AssertJUnit.assertNotNull(sslConfig);
        final CredentialConfig credentialConfig = sslConfig.getCredentialConfig();
        AssertJUnit.assertNotNull(credentialConfig);

        final ProviderConfig providerConfig = connPool.getConnectionFactory().getProvider().getProviderConfig();
        AssertJUnit.assertNotNull(providerConfig);
        AssertJUnit.assertTrue(providerConfig.getProperties().isEmpty());

        final SearchExecutor searchExecutor = dataConnector.getSearchExecutor();
        AssertJUnit.assertNotNull(searchExecutor);
        AssertJUnit.assertEquals("", searchExecutor.getBaseDn());
        AssertJUnit.assertNotNull(searchExecutor.getSearchFilter().getFilter());
        AssertJUnit.assertEquals(3000, searchExecutor.getTimeLimit());

        final ConnectionFactoryValidator validator = (ConnectionFactoryValidator) dataConnector.getValidator();
        AssertJUnit.assertNotNull(validator);
        AssertJUnit.assertTrue(validator.isThrowValidateError());
        AssertJUnit.assertNotNull(validator.getConnectionFactory());

        final ExecutableSearchBuilder searchBuilder = dataConnector.getExecutableSearchBuilder();
        AssertJUnit.assertNotNull(searchBuilder);

        final StringAttributeValueMappingStrategy mappingStrategy = (StringAttributeValueMappingStrategy) dataConnector.getMappingStrategy();
        AssertJUnit.assertNotNull(mappingStrategy);
        AssertJUnit.assertFalse(mappingStrategy.isNoResultAnError());
        AssertJUnit.assertFalse(mappingStrategy.isMultipleResultsAnError());

        AssertJUnit.assertNull(dataConnector.getResultsCache());
        
        dataConnector.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, IdPAttribute> attrs = dataConnector.resolve(context);
        Assert.assertNotNull(attrs);
        Assert.assertNotNull(attrs.get("entryDN"));
    }

    @Test public void v2PropsConfig() throws Exception {
        final Resource props = new ClassPathResource("net/shibboleth/idp/attribute/resolver/spring/dc/ldap/ldap-v2.properties");
        final LDAPDataConnector dataConnector =
                getLdapDataConnector(props, new String[] {
                        "net/shibboleth/idp/attribute/resolver/spring/dc/ldap/ldap-attribute-resolver-v2-props.xml",});
        Assert.assertNotNull(dataConnector);
        doTest(dataConnector);

        dataConnector.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, IdPAttribute> attrs = dataConnector.resolve(context);
        Assert.assertNotNull(attrs);
        Assert.assertEquals(attrs.size(), 4);
        Assert.assertNotNull(attrs.get("uid"));
        Assert.assertNotNull(attrs.get("homephone"));
        Assert.assertNotNull(attrs.get("mail"));
        Assert.assertNotNull(attrs.get("entryDN"));
    }

    @Test public void springConfig() throws Exception {
        final LDAPDataConnector dataConnector =
                getLdapDataConnector(new String[] {"net/shibboleth/idp/attribute/resolver/spring/dc/ldap/ldap-attribute-resolver-spring.xml"});
        Assert.assertNotNull(dataConnector);
        doTest(dataConnector);

        dataConnector.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, IdPAttribute> attrs = dataConnector.resolve(context);
        Assert.assertNotNull(attrs);
    }

    @Test public void springPropsConfig() throws Exception,
            ResolutionException {
        final Resource props = new ClassPathResource("net/shibboleth/idp/attribute/resolver/spring/dc/ldap/ldap-v3.properties");
        final LDAPDataConnector dataConnector =
                getLdapDataConnector(props, new String[] {"net/shibboleth/idp/attribute/resolver/spring/dc/ldap/ldap-attribute-resolver-spring-props.xml"});
        Assert.assertNotNull(dataConnector);
        doTest(dataConnector);

        dataConnector.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, IdPAttribute> attrs = dataConnector.resolve(context);
        Assert.assertNotNull(attrs);
        Assert.assertEquals(attrs.size(), 3);
        Assert.assertNotNull(attrs.get("uid"));
        Assert.assertNotNull(attrs.get("phonenumber"));
        Assert.assertNotNull(attrs.get("mail"));
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

        Assert.assertNotNull(cb);
        Assert.assertNotNull(c);
        Assert.assertNotNull(cbc);
        Assert.assertNotNull(ccc);
        Assert.assertNotNull(cc,
                "The Spring bug described in https://issues.shibboleth.net/jira/browse/IDP-338 has come back");

    }

    @Test public void hybridConfig() throws Exception {
        final LDAPDataConnector dataConnector =
                getLdapDataConnector(new String[] {
                        "net/shibboleth/idp/attribute/resolver/spring/dc/ldap/ldap-attribute-resolver-v2-hybrid.xml",
                        "net/shibboleth/idp/attribute/resolver/spring/dc/ldap/ldap-attribute-resolver-spring-context.xml"});
        Assert.assertNotNull(dataConnector);
        doTest(dataConnector);

        dataConnector.initialize();
        final StringAttributeValueMappingStrategy mappingStrategy =
                (StringAttributeValueMappingStrategy) dataConnector.getMappingStrategy();
        Assert.assertEquals(mappingStrategy.getResultRenamingMap().size(), 1);
        Assert.assertEquals(mappingStrategy.getResultRenamingMap().get("homephone"), "phonenumber");
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, IdPAttribute> attrs = dataConnector.resolve(context);
        Assert.assertNotNull(attrs);
        Assert.assertNull(attrs.get("homephone"));
        Assert.assertNotNull(attrs.get("phonenumber"));
        Assert.assertNotNull(attrs.get("entryDN"));
    }

    protected LDAPDataConnector getLdapDataConnector(final Resource properties, final String[] beanDefinitions) throws IOException {
        final GenericApplicationContext context = new FilesystemGenericApplicationContext() ;
        setTestContext(context);
        context.setDisplayName("ApplicationContext: " + LDAPDataConnectorParserTest.class);
        
        
        final ConversionServiceFactoryBean service = new ConversionServiceFactoryBean();
        service.setConverters(new HashSet<>(Arrays.asList(new DurationToLongConverter(), new StringToIPRangeConverter(),
                new StringToResourceConverter())));
        service.afterPropertiesSet();

        context.getBeanFactory().setConversionService(service.getObject());
        
        if (null != properties) {
            final ConfigurableEnvironment env = context.getEnvironment();
            env.getPropertySources().replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, new ResourcePropertySource(properties));
            
           env.setPlaceholderPrefix("%{");
           env.setPlaceholderSuffix("}");
        }

        final XmlBeanDefinitionReader configReader = new SchemaTypeAwareXMLBeanDefinitionReader(context);

        configReader.loadBeanDefinitions("net/shibboleth/idp/attribute/resolver/spring/externalBeans.xml");

        final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.setValidating(true);

        beanDefinitionReader.loadBeanDefinitions(beanDefinitions);
        context.refresh();

        return (LDAPDataConnector) context.getBean("myLDAP");
    }
    
    protected LDAPDataConnector getLdapDataConnector(final String[] beanDefinitions) throws IOException {
        return getLdapDataConnector(null, beanDefinitions);
    }

    protected void doTest(final LDAPDataConnector dataConnector) throws ResolutionException {

        final String id = dataConnector.getId();
        AssertJUnit.assertEquals("myLDAP", id);
        AssertJUnit.assertEquals(300000, dataConnector.getNoRetryDelay());

        final PooledConnectionFactory connFactory = (PooledConnectionFactory) dataConnector.getConnectionFactory();
        AssertJUnit.assertNotNull(connFactory);
        final BlockingConnectionPool connPool = (BlockingConnectionPool) connFactory.getConnectionPool();
        AssertJUnit.assertNotNull(connPool);
        AssertJUnit.assertEquals(5000, connPool.getBlockWaitTime());
        AssertJUnit.assertEquals("resolver-pool", connPool.getName());
        final PoolConfig poolConfig = connPool.getPoolConfig();
        AssertJUnit.assertNotNull(poolConfig);
        AssertJUnit.assertEquals(5, poolConfig.getMinPoolSize());
        AssertJUnit.assertEquals(10, poolConfig.getMaxPoolSize());
        AssertJUnit.assertTrue(poolConfig.isValidatePeriodically());
        AssertJUnit.assertEquals(900, poolConfig.getValidatePeriod());
        AssertJUnit.assertFalse(connPool.getFailFastInitialize());

        final SearchValidator searchValidator = (SearchValidator) connPool.getValidator();
        AssertJUnit.assertNotNull(searchValidator);
        AssertJUnit.assertEquals("dc=shibboleth,dc=net", searchValidator.getSearchRequest().getBaseDn());
        AssertJUnit.assertEquals("(ou=people)", searchValidator.getSearchRequest().getSearchFilter().getFilter());

        final IdlePruneStrategy pruneStrategy = (IdlePruneStrategy) connPool.getPruneStrategy();
        AssertJUnit.assertNotNull(pruneStrategy);
        AssertJUnit.assertEquals(300, pruneStrategy.getPrunePeriod());
        AssertJUnit.assertEquals(600, pruneStrategy.getIdleTime());

        final ConnectionConfig connConfig = connPool.getConnectionFactory().getConnectionConfig();
        AssertJUnit.assertNotNull(connConfig);
        AssertJUnit.assertEquals("ldap://localhost:10389", connConfig.getLdapUrl());
        AssertJUnit.assertEquals(false, connConfig.getUseSSL());
        AssertJUnit.assertEquals(true, connConfig.getUseStartTLS());
        final BindConnectionInitializer connInitializer = (BindConnectionInitializer) connConfig.getConnectionInitializer();
        AssertJUnit.assertEquals("cn=Directory Manager", connInitializer.getBindDn());
        AssertJUnit.assertEquals("password", connInitializer.getBindCredential().getString());
        AssertJUnit.assertEquals(2000, connConfig.getConnectTimeout());
        AssertJUnit.assertEquals(4000, connConfig.getResponseTimeout());

        final SslConfig sslConfig = connPool.getConnectionFactory().getConnectionConfig().getSslConfig();
        AssertJUnit.assertNotNull(sslConfig);
        final CredentialConfig credentialConfig = sslConfig.getCredentialConfig();
        AssertJUnit.assertNotNull(credentialConfig);

        final Map<String, Object> providerProps = new HashMap<>();
        providerProps.put("name1", "value1");
        providerProps.put("name2", "value2");
        final ProviderConfig providerConfig = connPool.getConnectionFactory().getProvider().getProviderConfig();
        AssertJUnit.assertNotNull(providerConfig);
        AssertJUnit.assertEquals(providerProps, providerConfig.getProperties());

        final SearchExecutor searchExecutor = dataConnector.getSearchExecutor();
        AssertJUnit.assertNotNull(searchExecutor);
        AssertJUnit.assertEquals("ou=people,dc=shibboleth,dc=net", searchExecutor.getBaseDn());
        AssertJUnit.assertNotNull(searchExecutor.getSearchFilter().getFilter());
        AssertJUnit.assertEquals(7000, searchExecutor.getTimeLimit());

        final ConnectionFactoryValidator validator = (ConnectionFactoryValidator) dataConnector.getValidator();
        AssertJUnit.assertNotNull(validator);
        AssertJUnit.assertTrue(validator.isThrowValidateError());
        AssertJUnit.assertNotNull(validator.getConnectionFactory());

        final ExecutableSearchBuilder searchBuilder = dataConnector.getExecutableSearchBuilder();
        AssertJUnit.assertNotNull(searchBuilder);

        final StringAttributeValueMappingStrategy mappingStrategy = (StringAttributeValueMappingStrategy) dataConnector.getMappingStrategy();
        AssertJUnit.assertNotNull(mappingStrategy);
        AssertJUnit.assertTrue(mappingStrategy.isNoResultAnError());
        AssertJUnit.assertTrue(mappingStrategy.isMultipleResultsAnError());

        final Cache<String, Map<String, IdPAttribute>> resultCache = dataConnector.getResultsCache();
        AssertJUnit.assertNotNull(resultCache);
    }
}
