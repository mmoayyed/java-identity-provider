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

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.impl.TestSources;
import net.shibboleth.idp.attribute.resolver.impl.dc.ExecutableSearchBuilder;
import net.shibboleth.idp.attribute.resolver.impl.dc.MappingStrategy;
import net.shibboleth.idp.attribute.resolver.impl.dc.Validator;
import net.shibboleth.idp.attribute.resolver.impl.dc.ldap.LdapDataConnector;
import net.shibboleth.idp.service.ServiceException;
import net.shibboleth.idp.spring.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.ldaptive.BindConnectionInitializer;
import org.ldaptive.ConnectionConfig;
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
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.cache.Cache;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.ssl.KeyStoreKeyManager;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustStoreTrustManager;

/** Test for {@link LdapDataConnectorParser}. */
public class LdapDataConnectorParserTest {

    /** In-memory directory server. */
    private InMemoryDirectoryServer directoryServer;

    /**
     * Creates an UnboundID in-memory directory server. Leverages LDIF found in test resources.
     * 
     * @throws LDAPException if the in-memory directory server cannot be created
     * @throws GeneralSecurityException if the startTLS keystore or truststore cannot be loaded
     */
    @BeforeTest public void setupDirectoryServer() throws LDAPException, GeneralSecurityException {

        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=shibboleth,dc=net");
        SSLUtil sslUtil =
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

    @Test public void v2Config() throws ComponentInitializationException, ServiceException, ResolutionException {
        LdapDataConnector dataConnector =
                getLdapDataConnector("net/shibboleth/idp/attribute/resolver/spring/dc/ldap/ldap-attribute-resolver-v2.xml");
        Assert.assertNotNull(dataConnector);
        doTest(dataConnector);

        dataConnector.initialize();
        AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        Map<String, Attribute> attrs = dataConnector.doResolve(context);
        Assert.assertNotNull(attrs);
    }

    @Test public void springConfig() throws ComponentInitializationException, ServiceException, ResolutionException {
        LdapDataConnector dataConnector =
                getLdapDataConnector("net/shibboleth/idp/attribute/resolver/spring/dc/ldap/ldap-attribute-resolver-spring.xml");
        Assert.assertNotNull(dataConnector);
        doTest(dataConnector);

        dataConnector.initialize();
        AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        Map<String, Attribute> attrs = dataConnector.doResolve(context);
        Assert.assertNotNull(attrs);
    }

    protected LdapDataConnector getLdapDataConnector(final String springContext) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.setDisplayName("ApplicationContext: " + LdapDataConnectorParserTest.class);

        XmlBeanDefinitionReader configReader = new XmlBeanDefinitionReader(context);

        configReader.loadBeanDefinitions("net/shibboleth/idp/attribute/resolver/spring/velocity.xml");

        SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.setValidating(false);
        beanDefinitionReader.loadBeanDefinitions(springContext);

        return (LdapDataConnector) context.getBean("myLDAP");
    }

    protected void doTest(final LdapDataConnector dataConnector) throws ResolutionException {

        String id = dataConnector.getId();
        AssertJUnit.assertEquals("myLDAP", id);

        PooledConnectionFactory connFactory = (PooledConnectionFactory) dataConnector.getConnectionFactory();
        AssertJUnit.assertNotNull(connFactory);
        BlockingConnectionPool connPool = (BlockingConnectionPool) connFactory.getConnectionPool();
        AssertJUnit.assertNotNull(connPool);
        AssertJUnit.assertEquals(5000, connPool.getBlockWaitTime());
        PoolConfig poolConfig = connPool.getPoolConfig();
        AssertJUnit.assertNotNull(poolConfig);
        AssertJUnit.assertEquals(5, poolConfig.getMinPoolSize());
        AssertJUnit.assertEquals(10, poolConfig.getMaxPoolSize());
        AssertJUnit.assertEquals(true, poolConfig.isValidatePeriodically());
        AssertJUnit.assertEquals(900, poolConfig.getValidatePeriod());

        SearchValidator searchValidator = (SearchValidator) connPool.getValidator();
        AssertJUnit.assertNotNull(searchValidator);
        AssertJUnit.assertEquals("dc=shibboleth,dc=net", searchValidator.getSearchRequest().getBaseDn());
        AssertJUnit.assertEquals("(ou=people)", searchValidator.getSearchRequest().getSearchFilter().getFilter());

        IdlePruneStrategy pruneStrategy = (IdlePruneStrategy) connPool.getPruneStrategy();
        AssertJUnit.assertNotNull(pruneStrategy);
        AssertJUnit.assertEquals(300, pruneStrategy.getPrunePeriod());
        AssertJUnit.assertEquals(600, pruneStrategy.getIdleTime());

        ConnectionConfig connConfig = connPool.getConnectionFactory().getConnectionConfig();
        AssertJUnit.assertNotNull(connConfig);
        AssertJUnit.assertEquals("ldap://localhost:10389", connConfig.getLdapUrl());
        AssertJUnit.assertEquals(false, connConfig.getUseSSL());
        AssertJUnit.assertEquals(true, connConfig.getUseStartTLS());
        BindConnectionInitializer connInitializer = (BindConnectionInitializer) connConfig.getConnectionInitializer();
        AssertJUnit.assertEquals("cn=Directory Manager", connInitializer.getBindDn());
        AssertJUnit.assertEquals("password", connInitializer.getBindCredential().getString());

        SslConfig sslConfig = connPool.getConnectionFactory().getConnectionConfig().getSslConfig();
        AssertJUnit.assertNotNull(sslConfig);
        CredentialConfig credentialConfig = sslConfig.getCredentialConfig();
        AssertJUnit.assertNotNull(credentialConfig);

        final Map<String, Object> providerProps = new HashMap<String, Object>();
        providerProps.put("name1", "value1");
        providerProps.put("name2", "value2");
        ProviderConfig providerConfig = connPool.getConnectionFactory().getProvider().getProviderConfig();
        AssertJUnit.assertNotNull(providerConfig);
        AssertJUnit.assertEquals(providerProps, providerConfig.getProperties());

        SearchExecutor searchExecutor = dataConnector.getSearchExecutor();
        AssertJUnit.assertNotNull(searchExecutor);
        AssertJUnit.assertEquals("ou=people,dc=shibboleth,dc=net", searchExecutor.getBaseDn());
        AssertJUnit.assertNotNull(searchExecutor.getSearchFilter().getFilter());

        Validator validator = dataConnector.getValidator();
        AssertJUnit.assertNotNull(validator);

        ExecutableSearchBuilder searchBuilder = dataConnector.getExecutableSearchBuilder();
        AssertJUnit.assertNotNull(searchBuilder);

        MappingStrategy mappingStrategy = dataConnector.getMappingStrategy();
        AssertJUnit.assertNotNull(mappingStrategy);

        Cache<String, Map<String, Attribute>> resultCache = dataConnector.getResultCache();
        AssertJUnit.assertNull(resultCache);
    }
}
