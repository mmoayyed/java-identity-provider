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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.ldaptive.ConnectionFactory;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.LdapException;
import org.ldaptive.Response;
import org.ldaptive.SearchExecutor;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchResult;
import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeRecipientContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.impl.TestSources;
import net.shibboleth.idp.attribute.resolver.impl.dc.ExecutableSearch;
import net.shibboleth.idp.attribute.resolver.impl.dc.ExecutableSearchBuilder;
import net.shibboleth.idp.attribute.resolver.impl.dc.TestCache;
import net.shibboleth.idp.attribute.resolver.impl.dc.ldap.LdapDataConnector;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;

/**
 * Tests for {@link LdapDataConnector}
 */
public class LdapDataConnectorTest extends OpenSAMLInitBaseTestCase {

    /** The connector name. */
    private static final String TEST_CONNECTOR_NAME = "ldapAttributeConnector";

    /** Base DN defined in LDIF. */
    private static final String TEST_BASE_DN = "ou=people,dc=shibboleth,dc=net";

    /** Default search attributes for entry in LDIF. */
    private static final String[] TEST_RETURN_ATTRIBUTES = new String[] {"cn", "sn", "uid", "mail"};

    /** In-memory directory server. */
    private InMemoryDirectoryServer directoryServer;

    /** Simple search filter builder that leverages a custom filter. */
    private class TestSearchFilterBuilder implements ExecutableSearchBuilder<ExecutableSearchFilter> {

        /** {@inheritDoc} */
        @Nonnull public ExecutableSearchFilter build(@Nonnull AttributeResolutionContext resolutionContext)
                throws ResolutionException {
            final AttributeRecipientContext subContext =
                    resolutionContext.getSubcontext(AttributeRecipientContext.class);
            return new ExecutableSearchFilter() {

                private final SearchFilter sf = new SearchFilter(String.format("(uid=%s)", subContext.getPrincipal()));

                /** {@inheritDoc} */
                @Nonnull public String getResultCacheKey() {
                    return String.valueOf(sf.hashCode());
                }

                /** {@inheritDoc} */
                @Nonnull public SearchResult execute(@Nonnull final SearchExecutor executor,
                        @Nonnull final ConnectionFactory factory) throws LdapException {
                    final Response<SearchResult> response = executor.search(factory, sf);
                    return response.getResult();
                }

                /** {@inheritDoc} */
                public String toString() {
                    return sf.toString();
                }
            };
        }
    }

    /**
     * Creates an UnboundID in-memory directory server. Leverages LDIF found in test resources.
     * 
     * @throws LDAPException if the in-memory directory server cannot be created
     */
    @BeforeTest public void setupDirectoryServer() throws LDAPException {

        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=shibboleth,dc=net");
        config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", 10389));
        config.addAdditionalBindCredentials("cn=Directory Manager", "password");
        directoryServer = new InMemoryDirectoryServer(config);
        directoryServer
                .importFromLDIF(true,
                        "src/test/resources/data/net/shibboleth/idp/attribute/resolver/impl/dc/ldap/ldapDataConnectorTest.ldif");
        directoryServer.startListening();
    }

    /**
     * Shutdown the in-memory directory server.
     */
    @AfterTest public void teardownDirectoryServer() {
        directoryServer.shutDown(true);
    }

    /**
     * Creates an LDAP data connector using the supplied builder and strategy. Sets defaults values if the parameters
     * are null.
     * 
     * @param builder to build search requests
     * @param strategy to map search results
     * @return ldap data connector
     */
    protected LdapDataConnector createLdapDataConnector(ExecutableSearchBuilder builder,
            SearchResultMappingStrategy strategy) {
        LdapDataConnector connector = new LdapDataConnector();
        connector.setId(TEST_CONNECTOR_NAME);
        ConnectionFactory connectionFactory = new DefaultConnectionFactory("ldap://localhost:10389");
        connector.setConnectionFactory(connectionFactory);
        SearchExecutor searchExecutor = new SearchExecutor();
        searchExecutor.setBaseDn(TEST_BASE_DN);
        searchExecutor.setReturnAttributes(TEST_RETURN_ATTRIBUTES);
        connector.setSearchExecutor(searchExecutor);
        connector.setExecutableSearchBuilder(builder == null ? new TestSearchFilterBuilder() : builder);
        connector.setValidator(connector.new SearchValidator(new SearchFilter("(ou=people)")));
        connector.setMappingStrategy(strategy == null ? new StringAttributeValueMappingStrategy() : strategy);
        return connector;
    }

    @Test public void initializeAndGetters() throws ComponentInitializationException, ResolutionException {

        LdapDataConnector connector = new LdapDataConnector();
        connector.setId(TEST_CONNECTOR_NAME);

        try {
            connector.initialize();
            Assert.fail("No connection factory");
        } catch (ComponentInitializationException e) {
            // OK
        }

        connector.setConnectionFactory(new DefaultConnectionFactory("ldap://localhost:55555"));
        try {
            connector.initialize();
            Assert.fail("No search executor");
        } catch (ComponentInitializationException e) {
            // OK
        }

        SearchExecutor searchExecutor = new SearchExecutor();
        searchExecutor.setBaseDn(TEST_BASE_DN);
        searchExecutor.setReturnAttributes(TEST_RETURN_ATTRIBUTES);
        connector.setSearchExecutor(searchExecutor);
        try {
            connector.initialize();
            Assert.fail("No filter builder");
        } catch (ComponentInitializationException e) {
            // OK
        }

        ExecutableSearchBuilder requestBuilder = new TestSearchFilterBuilder();
        connector.setExecutableSearchBuilder(requestBuilder);
        try {
            connector.initialize();
            Assert.fail("Invalid Connection Factory");
        } catch (ComponentInitializationException e) {
            // OK
        }

        ConnectionFactory connectionFactory = new DefaultConnectionFactory("ldap://localhost:10389");
        connector.setConnectionFactory(connectionFactory);

        SearchResultMappingStrategy mappingStrategy = new StringAttributeValueMappingStrategy();
        connector.setMappingStrategy(mappingStrategy);

        try {
            connector.doResolve(null);
            Assert.fail("Need to initialize first");
        } catch (UninitializedComponentException e) {
            // OK
        }

        connector.initialize();
        try {
            connector.setConnectionFactory(null);
            Assert.fail("Setter after initialize");
        } catch (UnmodifiableComponentException e) {
            // OK
        }
        Assert.assertEquals(connector.getConnectionFactory(), connectionFactory);
        Assert.assertEquals(connector.getSearchExecutor(), searchExecutor);
        Assert.assertEquals(connector.getExecutableSearchBuilder(), requestBuilder);
        Assert.assertEquals(connector.getMappingStrategy(), mappingStrategy);
    }

    @Test public void resolve() throws ComponentInitializationException, ResolutionException {
        LdapDataConnector connector = createLdapDataConnector(null, null);
        connector.initialize();

        AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        Map<String, Attribute> attrs = connector.doResolve(context);
        // check total attributes: uid, cn, sn, mail
        Assert.assertTrue(attrs.size() == 4);
        // check uid
        Assert.assertTrue(attrs.get("uid").getValues().size() == 1);
        Assert.assertEquals(new StringAttributeValue(TestSources.PRINCIPAL_ID), attrs.get("uid").getValues().iterator()
                .next());
        // check cn
        Assert.assertTrue(attrs.get("cn").getValues().size() == 3);
        Set<StringAttributeValue> cn = new HashSet<StringAttributeValue>();
        cn.add(new StringAttributeValue("Peter Principal"));
        cn.add(new StringAttributeValue("Peter J Principal"));
        cn.add(new StringAttributeValue("pete principal"));
        Assert.assertEquals(cn, attrs.get("cn").getValues());
        // check sn
        Assert.assertTrue(attrs.get("sn").getValues().size() == 1);
        Assert.assertEquals(new StringAttributeValue("Principal"), attrs.get("sn").getValues().iterator().next());
        // check mail
        Assert.assertTrue(attrs.get("mail").getValues().size() == 2);
        Set<StringAttributeValue> mail = new HashSet<StringAttributeValue>();
        mail.add(new StringAttributeValue("peter.principal@shibboleth.net"));
        mail.add(new StringAttributeValue("peterprincipal@shibboleth.net"));
        Assert.assertEquals(mail, attrs.get("mail").getValues());
    }

    @Test(expectedExceptions = ResolutionException.class) public void resolveNoFilter()
            throws ComponentInitializationException, ResolutionException {
        LdapDataConnector connector = createLdapDataConnector(new ExecutableSearchBuilder() {

            @Nonnull public ExecutableSearch build(@Nonnull AttributeResolutionContext resolutionContext)
                    throws ResolutionException {
                return null;
            }
        }, null);
        connector.initialize();

        AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        connector.doResolve(context);
    }

    @Test(expectedExceptions = ResolutionException.class) public void resolveNoResultIsError()
            throws ComponentInitializationException, ResolutionException {
        LdapDataConnector connector = createLdapDataConnector(null, null);
        connector.setNoResultAnError(true);
        connector.initialize();

        AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        try {
            Map<String, Attribute> res = connector.doResolve(context);
            Assert.assertNotNull(res);
        } catch (ResolutionException e) {
            Assert.fail("Resolution exception occurred", e);
        }

        context =
                TestSources.createResolutionContext("NOT_A_PRINCIPAL", TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        connector.doResolve(context);
    }

    @Test public void resolveWithCache() throws ComponentInitializationException, ResolutionException {
        LdapDataConnector connector = createLdapDataConnector(null, null);
        final TestCache cache = new TestCache();
        connector.setResultsCache(cache);
        connector.initialize();

        AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        Assert.assertTrue(cache.size() == 0);
        Map<String, Attribute> optional = connector.doResolve(context);
        Assert.assertTrue(cache.size() == 1);
        Assert.assertEquals(cache.iterator().next(), optional);
    }
}
