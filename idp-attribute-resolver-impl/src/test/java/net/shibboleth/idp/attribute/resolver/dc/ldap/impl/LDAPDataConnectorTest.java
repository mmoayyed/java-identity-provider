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

package net.shibboleth.idp.attribute.resolver.dc.ldap.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;

import org.ldaptive.ConnectionFactory;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.SearchExecutor;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;

import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.EmptyAttributeValue.EmptyType;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.DataConnector;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.dc.ExecutableSearchBuilder;
import net.shibboleth.idp.attribute.resolver.dc.impl.TestCache;
import net.shibboleth.idp.attribute.resolver.dc.ldap.ExecutableSearchFilter;
import net.shibboleth.idp.attribute.resolver.dc.ldap.ParameterizedExecutableSearchFilterBuilder;
import net.shibboleth.idp.attribute.resolver.dc.ldap.SearchResultMappingStrategy;
import net.shibboleth.idp.attribute.resolver.dc.ldap.StringAttributeValueMappingStrategy;
import net.shibboleth.idp.attribute.resolver.dc.ldap.TemplatedExecutableSearchFilterBuilder;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;
import net.shibboleth.utilities.java.support.velocity.VelocityEngine;

/**
 * Tests for {@link LDAPDataConnector}
 */
public class LDAPDataConnectorTest {

    /** The connector name. */
    private static final String TEST_CONNECTOR_NAME = "ldapAttributeConnector";

    /** Base DN defined in LDIF. */
    private static final String TEST_BASE_DN = "ou=people,dc=shibboleth,dc=net";

    /** Default search attributes for entry in LDIF. */
    private static final String[] TEST_RETURN_ATTRIBUTES = new String[] {"cn", "sn", "uid", "mail"};

    /** In-memory directory server. */
    private InMemoryDirectoryServer directoryServer;

    /**
     * Creates an UnboundID in-memory directory server. Leverages LDIF found in test resources.
     * 
     * @throws LDAPException if the in-memory directory server cannot be created
     */
    @BeforeClass public void setupDirectoryServer() throws LDAPException {

        final InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=shibboleth,dc=net");
        config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", 10389));
        config.addAdditionalBindCredentials("cn=Directory Manager", "password");
        directoryServer = new InMemoryDirectoryServer(config);
        directoryServer
                .importFromLDIF(true,
                        "src/test/resources/net/shibboleth/idp/attribute/resolver/impl/dc/ldap/ldapDataConnectorTest.ldif");
        directoryServer.startListening();
    }

    /**
     * Shutdown the in-memory directory server.
     */
    @AfterClass public void teardownDirectoryServer() {
        directoryServer.shutDown(true);
    }

    /**
     * Creates an LDAP data connector using the supplied builder and strategy. Sets defaults values if the parameters
     * are null.
     * 
     * @param builder to build search requests
     * @param strategy to map search results
     * 
     * @return ldap data connector
     * 
     * @throws ComponentInitializationException  ...
     */
    protected LDAPDataConnector createLdapDataConnector(final ExecutableSearchBuilder<ExecutableSearchFilter> builder,
            final SearchResultMappingStrategy strategy) throws ComponentInitializationException {
        final LDAPDataConnector connector = new LDAPDataConnector();
        connector.setId(TEST_CONNECTOR_NAME);
        final ConnectionFactory connectionFactory = new DefaultConnectionFactory("ldap://localhost:10389");
        connector.setConnectionFactory(connectionFactory);
        final SearchExecutor searchExecutor = new SearchExecutor();
        searchExecutor.setBaseDn(TEST_BASE_DN);
        searchExecutor.setReturnAttributes(TEST_RETURN_ATTRIBUTES);
        connector.setSearchExecutor(searchExecutor);
        connector.setExecutableSearchBuilder(builder == null ? 
                                             newParameterizedExecutableSearchFilterBuilder("(uid={principalName})") : builder);
        connector.setValidator(newConnectionFactoryValidator(connectionFactory));
        connector.setMappingStrategy(strategy == null ? new StringAttributeValueMappingStrategy() : strategy);
        return connector;
    }

    @Test public void initializeAndGetters() throws ComponentInitializationException, ResolutionException {

        final LDAPDataConnector connector = new LDAPDataConnector();
        connector.setId(TEST_CONNECTOR_NAME);

        try {
            connector.initialize();
            fail("No connection factory");
        } catch (final ComponentInitializationException e) {
            // OK
        }

        connector.setConnectionFactory(new DefaultConnectionFactory("ldap://localhost:55555"));
        try {
            connector.initialize();
            fail("No search executor");
        } catch (final ComponentInitializationException e) {
            // OK
        }
        connector.setFailFastInitialize(true);

        final SearchExecutor searchExecutor = new SearchExecutor();
        searchExecutor.setBaseDn(TEST_BASE_DN);
        searchExecutor.setReturnAttributes(TEST_RETURN_ATTRIBUTES);
        connector.setSearchExecutor(searchExecutor);
        try {
            connector.initialize();
            fail("No filter builder");
        } catch (final ComponentInitializationException e) {
            // OK
        }

        final ExecutableSearchBuilder<ExecutableSearchFilter> requestBuilder =
                newParameterizedExecutableSearchFilterBuilder("(uid={principalName})");
        connector.setExecutableSearchBuilder(requestBuilder);
        try {
            connector.initialize();
            fail("Invalid Connection Factory");
        } catch (final ComponentInitializationException e) {
            // OK
        }

        final ConnectionFactory connectionFactory = new DefaultConnectionFactory("ldap://localhost:10389");
        connector.setConnectionFactory(connectionFactory);

        final SearchResultMappingStrategy mappingStrategy = new StringAttributeValueMappingStrategy();
        connector.setMappingStrategy(mappingStrategy);

        try {
            connector.resolve(null);
            fail("Need to initialize first");
        } catch (final UninitializedComponentException e) {
            // OK
        }

        connector.initialize();
        try {
            connector.setConnectionFactory(null);
            fail("Setter after initialize");
        } catch (final UnmodifiableComponentException e) {
            // OK
        }
        assertEquals(connector.getConnectionFactory(), connectionFactory);
        assertEquals(connector.getSearchExecutor(), searchExecutor);
        assertEquals(connector.getExecutableSearchBuilder(), requestBuilder);
        assertEquals(connector.getMappingStrategy(), mappingStrategy);
    }

    @Test public void failFastInitialize() throws ComponentInitializationException {
        final LDAPDataConnector connector = new LDAPDataConnector();
        connector.setId(TEST_CONNECTOR_NAME);

        final ConnectionFactory connectionFactory = new DefaultConnectionFactory("ldap://localhost:55555");
        connector.setConnectionFactory(connectionFactory);
        connector.setSearchExecutor(new SearchExecutor());
        connector.setExecutableSearchBuilder(newParameterizedExecutableSearchFilterBuilder("(uid={principalName})"));
        connector.setFailFastInitialize(true);

        try {
            connector.initialize();
            fail("No failfast");
        } catch (final ComponentInitializationException e) {
            // OK
        }

        connector.setFailFastInitialize(false);
        connector.initialize();
    }

    @Test public void resolve() throws ComponentInitializationException, ResolutionException {
        final ParameterizedExecutableSearchFilterBuilder builder =
                newParameterizedExecutableSearchFilterBuilder("(uid={principalName})");
        resolve(builder);
    }

    @Test public void resolveMulti() throws ComponentInitializationException, ResolutionException {
        final ParameterizedExecutableSearchFilterBuilder builder =
                newParameterizedExecutableSearchFilterBuilder("(uid=P*)");
        resolveMulti(builder);
    }

    @Test public void resolveWithDepends() throws ComponentInitializationException, ResolutionException {
        final ParameterizedExecutableSearchFilterBuilder builder =
                newParameterizedExecutableSearchFilterBuilder("(&(cn={principalName})(eduPersonAffiliation={affiliation[0]}))");
        builder.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, List<IdPAttributeValue>> dependsAttributes = new HashMap<>();
        final List<IdPAttributeValue> attributeValues = new ArrayList<>();
        attributeValues.add(new StringAttributeValue("student"));
        dependsAttributes.put("affiliation", attributeValues);
        final ExecutableSearchFilter filter = builder.build(context, dependsAttributes);
        assertEquals(filter.getSearchFilter().format(), "(&(cn=PETER_THE_PRINCIPAL)(eduPersonAffiliation=student))");
        assertEquals(filter.getResultCacheKey(), "(&(cn=PETER_THE_PRINCIPAL)(eduPersonAffiliation=student))");
    }

    @Test public void resolveWithMultiValueDepends() throws ComponentInitializationException, ResolutionException {
        final ParameterizedExecutableSearchFilterBuilder builder =
                newParameterizedExecutableSearchFilterBuilder(
                        "(&(cn={principalName})(eduPersonEntitlement={entitlement[0]})(eduPersonEntitlement={entitlement[1]}))");
        builder.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, List<IdPAttributeValue>> dependsAttributes = new HashMap<>();
        final List<IdPAttributeValue> attributeValues = new ArrayList<>();
        attributeValues.add(new StringAttributeValue("entitlement1"));
        attributeValues.add(new StringAttributeValue("entitlement*"));
        dependsAttributes.put("entitlement", attributeValues);
        final ExecutableSearchFilter filter = builder.build(context, dependsAttributes);
        assertEquals(
                filter.getSearchFilter().format(),
                "(&(cn=PETER_THE_PRINCIPAL)(eduPersonEntitlement=entitlement1)(eduPersonEntitlement=entitlement\\2a))");
        assertEquals(
                filter.getResultCacheKey(),
                "(&(cn=PETER_THE_PRINCIPAL)(eduPersonEntitlement=entitlement1)(eduPersonEntitlement=entitlement\\2a))");
    }

    @Test public void escape() throws ComponentInitializationException, ResolutionException {
        final ParameterizedExecutableSearchFilterBuilder builder =
                newParameterizedExecutableSearchFilterBuilder("(cn={principalName})");
        builder.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext("domain\\user*", TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final ExecutableSearchFilter filter = builder.build(context, null);
        assertEquals(filter.getSearchFilter().format(), "(cn=domain\\5cuser\\2a)");
        assertEquals(filter.getResultCacheKey(), "(cn=domain\\5cuser\\2a)");
    }

    @Test public void resolveTemplate() throws ComponentInitializationException, ResolutionException {
        final TemplatedExecutableSearchFilterBuilder builder = new TemplatedExecutableSearchFilterBuilder();
        builder.setTemplateText("(uid=${resolutionContext.principal})");
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.initialize();
        resolve(builder);
    }
    
    @Test(expectedExceptions={ResolutionException.class}) public void resolveTemplateExcept() throws ComponentInitializationException, ResolutionException {
        final TemplatedExecutableSearchFilterBuilder builder = new TemplatedExecutableSearchFilterBuilder();
        builder.setTemplateText("(uid=${resolutionContext.AttributeRecipientID.toString().substring(99, 106)})");
        final Properties props = new Properties();
        // TODO, should expose a way to set the strict prop underneath
        props.setProperty("runtime.references.strict", "true");
        props.setProperty("string.resource.loader.class", "org.apache.velocity.runtime.resource.loader.StringResourceLoader");
        props.setProperty("resource.loader", "classpath, string");
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine(props));
        builder.initialize();
        resolve(builder);
    }


    @Test public void resolveTemplateWithDepends() throws ComponentInitializationException, ResolutionException {
        final TemplatedExecutableSearchFilterBuilder builder = new TemplatedExecutableSearchFilterBuilder();
        builder.setTemplateText("(&(cn=${resolutionContext.principal})(eduPersonAffiliation=${affiliation[0]}))");
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, List<IdPAttributeValue>> dependsAttributes = new HashMap<>();
        final List<IdPAttributeValue> attributeValues = new ArrayList<>();
        attributeValues.add(new StringAttributeValue("student"));
        dependsAttributes.put("affiliation", attributeValues);
        final ExecutableSearchFilter filter = builder.build(context, dependsAttributes);
        assertEquals(filter.getSearchFilter().format(), "(&(cn=PETER_THE_PRINCIPAL)(eduPersonAffiliation=student))");
        assertEquals(filter.getResultCacheKey(), "(&(cn=PETER_THE_PRINCIPAL)(eduPersonAffiliation=student))");
    }

    @Test public void resolveTemplateWithMultiValueDepends() throws ComponentInitializationException, ResolutionException {
        final TemplatedExecutableSearchFilterBuilder builder = new TemplatedExecutableSearchFilterBuilder();
        builder.setTemplateText(
                "(&(cn=${resolutionContext.principal})(eduPersonEntitlement=${entitlement[0]})(eduPersonEntitlement=${entitlement[1]}))");
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, List<IdPAttributeValue>> dependsAttributes = new HashMap<>();
        final List<IdPAttributeValue> attributeValues = new ArrayList<>();
        attributeValues.add(new StringAttributeValue("entitlement1"));
        attributeValues.add(new StringAttributeValue("entitlement*"));
        dependsAttributes.put("entitlement", attributeValues);
        final ExecutableSearchFilter filter = builder.build(context, dependsAttributes);
        assertEquals(
                filter.getSearchFilter().format(),
                "(&(cn=PETER_THE_PRINCIPAL)(eduPersonEntitlement=entitlement1)(eduPersonEntitlement=entitlement\\2a))");
        assertEquals(
                filter.getResultCacheKey(),
                "(&(cn=PETER_THE_PRINCIPAL)(eduPersonEntitlement=entitlement1)(eduPersonEntitlement=entitlement\\2a))");
    }

    @Test public void escapeTemplate() throws ComponentInitializationException, ResolutionException {
        final TemplatedExecutableSearchFilterBuilder builder = new TemplatedExecutableSearchFilterBuilder();
        builder.setTemplateText("(cn=${resolutionContext.principal})");
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext("domain\\user*", TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final ExecutableSearchFilter filter = builder.build(context, null);
        assertEquals(filter.getSearchFilter().format(), "(cn=domain\\5cuser\\2a)");
        assertEquals(filter.getResultCacheKey(), "(cn=domain\\5cuser\\2a)");
    }

    protected void resolve(final ExecutableSearchBuilder<ExecutableSearchFilter> builder) throws ComponentInitializationException,
            ResolutionException {
        final LDAPDataConnector connector = createLdapDataConnector(builder, new StringAttributeValueMappingStrategy());
        connector.initialize();

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, IdPAttribute> attrs = connector.resolve(context);
        assertNotNull(attrs);
        // check total attributes: uid, cn, sn, mail
        assertEquals(attrs.size(), 4);
        // check uid
        assertEquals(attrs.get("uid").getValues().size(), 1);
        assertEquals(new StringAttributeValue(TestSources.PRINCIPAL_ID), attrs.get("uid").getValues().iterator()
                .next());
        // check cn
        assertEquals(attrs.get("cn").getValues().size(), 3);
        assertTrue(attrs.get("cn").getValues().contains(new StringAttributeValue("Peter Principal")));
        assertTrue(attrs.get("cn").getValues().contains(new StringAttributeValue("Peter J Principal")));
        assertTrue(attrs.get("cn").getValues().contains(new StringAttributeValue("pete principal")));
        // check sn
        assertEquals(attrs.get("sn").getValues().size(), 1);
        assertEquals(new StringAttributeValue("Principal"), attrs.get("sn").getValues().iterator().next());
        // check mail
        assertEquals(attrs.get("mail").getValues().size(), 2);
        assertTrue(attrs.get("mail").getValues().contains(new StringAttributeValue("peter.principal@shibboleth.net")));
        assertTrue(attrs.get("mail").getValues().contains(new StringAttributeValue("peterprincipal@shibboleth.net")));
    }

    protected void resolveMulti(final ExecutableSearchBuilder<ExecutableSearchFilter> builder) throws ComponentInitializationException,
            ResolutionException {
        final LDAPDataConnector connector = createLdapDataConnector(builder, new StringAttributeValueMappingStrategy());
        connector.initialize();
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, IdPAttribute> attrs = connector.resolve(context);
        assertNotNull(attrs);
        // check total attributes: uid, cn, sn, mail
        assertEquals(attrs.size(), 4);
        // check uid
        assertEquals(attrs.get("uid").getValues().size(), 3);
        assertTrue(attrs.get("uid").getValues().contains(new StringAttributeValue(TestSources.PRINCIPAL_ID)));
        assertTrue(attrs.get("uid").getValues().contains(new StringAttributeValue("PAUL_THE_PRINCIPAL")));
        assertTrue(attrs.get("uid").getValues().contains(new StringAttributeValue("PHILIP_THE_PRINCIPAL")));
        // check cn
        assertEquals(attrs.get("cn").getValues().size(), 5);
        assertTrue(attrs.get("cn").getValues().contains(new StringAttributeValue("Peter Principal")));
        assertTrue(attrs.get("cn").getValues().contains(new StringAttributeValue("Peter J Principal")));
        assertTrue(attrs.get("cn").getValues().contains(new StringAttributeValue("pete principal")));
        assertTrue(attrs.get("cn").getValues().contains(new StringAttributeValue("Paul Principal")));
        assertTrue(attrs.get("cn").getValues().contains(new StringAttributeValue("Philip Principal")));
        // check sn
        assertEquals(attrs.get("sn").getValues().size(), 3);
        assertTrue(attrs.get("sn").getValues().contains(new StringAttributeValue("Principal")));
        // check mail
        assertEquals(attrs.get("mail").getValues().size(), 8);
        assertTrue(attrs.get("mail").getValues().contains(new StringAttributeValue("peter.principal@shibboleth.net")));
        assertTrue(attrs.get("mail").getValues().contains(new StringAttributeValue("peterprincipal@shibboleth.net")));
        assertTrue(attrs.get("mail").getValues().contains(new StringAttributeValue("paul.principal@shibboleth.net")));
        assertTrue(attrs.get("mail").getValues().contains(new StringAttributeValue("paulprincipal@shibboleth.net")));
        assertTrue(attrs.get("mail").getValues().contains(EmptyAttributeValue.ZERO_LENGTH));
        assertTrue(attrs.get("mail").getValues().contains(new StringAttributeValue("\"\"")));
        assertTrue(attrs.get("mail").getValues().contains(new StringAttributeValue("  ")));
        assertTrue(attrs.get("mail").getValues().contains(new StringAttributeValue(" philip.principal@shibboleth.net ")));
    }
    
    @Test(expectedExceptions = ResolutionException.class) public void resolveNoFilter()
            throws ComponentInitializationException, ResolutionException {
        final LDAPDataConnector connector = createLdapDataConnector(new ExecutableSearchBuilder<ExecutableSearchFilter>() {

            @Override
            @Nonnull public ExecutableSearchFilter build(@Nonnull final AttributeResolutionContext resolutionContext,
                    @Nonnull final Map<String, List<IdPAttributeValue>> dependencyAttributes) throws ResolutionException {
                return null;
            }
        }, null);
        connector.initialize();

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        connector.resolve(context);
    }

    @Test(expectedExceptions = ResolutionException.class) public void resolveNoResultIsError()
            throws ComponentInitializationException, ResolutionException {
        final StringAttributeValueMappingStrategy mappingStrategy = new StringAttributeValueMappingStrategy();
        mappingStrategy.setNoResultAnError(true);
        final LDAPDataConnector connector = createLdapDataConnector(null, mappingStrategy);
        connector.initialize();

        AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        try {
            final Map<String, IdPAttribute> res = connector.resolve(context);
            assertNotNull(res);
        } catch (final ResolutionException e) {
            fail("Resolution exception occurred", e);
        }

        context =
                TestSources.createResolutionContext("NOT_A_PRINCIPAL", TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        connector.resolve(context);
    }

    @Test(expectedExceptions = ResolutionException.class) public void resolveMultipleResultsIsError()
            throws ComponentInitializationException, ResolutionException {
        final StringAttributeValueMappingStrategy mappingStrategy = new StringAttributeValueMappingStrategy();
        mappingStrategy.setMultipleResultsAnError(true);
        final LDAPDataConnector connector = createLdapDataConnector(newParameterizedExecutableSearchFilterBuilder("(sn={principalName})"), mappingStrategy);
        connector.initialize();

        AttributeResolutionContext context =
                TestSources.createResolutionContext("NOT_A_PRINCIPAL", TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        try {
            final Map<String, IdPAttribute> res = connector.resolve(context);
            assertNull(res);
        } catch (final ResolutionException e) {
            fail("Resolution exception occurred", e);
        }

        context =
                TestSources.createResolutionContext("PRINCIPAL", TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        connector.resolve(context);
    }

    @Test public void resolveWithCache() throws ComponentInitializationException, ResolutionException {
        final LDAPDataConnector connector = createLdapDataConnector(null, null);
        final TestCache cache = new TestCache();
        connector.setResultsCache(cache);
        connector.initialize();

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        assertEquals(cache.size(), 0);
        final Map<String, IdPAttribute> optional = connector.resolve(context);
        assertEquals(cache.size(), 1);
        assertEquals(cache.iterator().next(), optional);
    }
    
    /**
     * See IDP-1077.
     * 
     * @throws ComponentInitializationException ...
     * @throws ResolutionException ...
     */
    @Test public void resolveWithCacheCollison() throws ComponentInitializationException, ResolutionException {
        final LDAPDataConnector connector = createLdapDataConnector(null, null);
        final TestCache cache = new TestCache();
        connector.setResultsCache(cache);
        connector.initialize();

        assertEquals(cache.size(), 0);
        final AttributeResolutionContext context1 =
                TestSources.createResolutionContext("dlo1", TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        connector.resolve(context1);
        assertEquals(cache.size(), 1);

        final AttributeResolutionContext context2 =
                TestSources.createResolutionContext("dn11", TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        connector.resolve(context2);
        assertEquals(cache.size(), 2);
    }

    /**
     * See IDP-573.
     * 
     * @throws ComponentInitializationException ...
     * @throws ResolutionException ...
     */
    @Test public void resolveEmptyAttribute() throws ComponentInitializationException, ResolutionException {
        final ParameterizedExecutableSearchFilterBuilder builder =
                newParameterizedExecutableSearchFilterBuilder("(uid={principalName})");

        final DataConnector connector = createLdapDataConnector(builder, new StringAttributeValueMappingStrategy());
        connector.initialize();

        final AttributeResolutionContext context =
                TestSources.createResolutionContext("PHILIP_THE_PRINCIPAL", TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, IdPAttribute> attrs = connector.resolve(context);
        assertNotNull(attrs);
        // check total attributes: uid, cn, sn, mail
        assertEquals(attrs.size(), 4);
        // check uid
        assertEquals(attrs.get("uid").getValues().size(), 1);
        assertEquals(attrs.get("uid").getValues().iterator().next(), new StringAttributeValue(
                "PHILIP_THE_PRINCIPAL"));
        // check cn
        assertEquals(attrs.get("cn").getValues().size(), 1);
        assertTrue(attrs.get("cn").getValues().contains(new StringAttributeValue("Philip Principal")));
        // check sn
        assertEquals(attrs.get("sn").getValues().size(), 1);
        assertEquals(attrs.get("sn").getValues().iterator().next(), new StringAttributeValue("Principal"));
        // check mail
        assertEquals(attrs.get("mail").getValues().size(), 4);
        assertTrue(attrs.get("mail").getValues().contains(new EmptyAttributeValue(EmptyType.ZERO_LENGTH_VALUE)));
        assertTrue(attrs.get("mail").getValues().contains(new StringAttributeValue("\"\"")));
        assertTrue(attrs.get("mail").getValues().contains(new StringAttributeValue("  ")));
        assertTrue(attrs.get("mail").getValues().contains(new StringAttributeValue(" philip.principal@shibboleth.net ")));
    }
    
    public static ParameterizedExecutableSearchFilterBuilder newParameterizedExecutableSearchFilterBuilder(final String filter) throws ComponentInitializationException {
        final ParameterizedExecutableSearchFilterBuilder builder = new ParameterizedExecutableSearchFilterBuilder();
        builder.setSearchFilter(filter);
        builder.initialize();
        return builder;
    }
    
    public static ConnectionFactoryValidator newConnectionFactoryValidator(final ConnectionFactory connectionFactory, final boolean throwValidateError) throws ComponentInitializationException {
        final ConnectionFactoryValidator validator = new ConnectionFactoryValidator();
        
        validator.setConnectionFactory(connectionFactory);
        validator.setThrowValidateError(throwValidateError);
        return validator;
    }

    public static ConnectionFactoryValidator newConnectionFactoryValidator(final ConnectionFactory connectionFactory) throws ComponentInitializationException {
        final ConnectionFactoryValidator validator = new ConnectionFactoryValidator();
        validator.setConnectionFactory(connectionFactory);
        return validator;
    }
}
