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

package net.shibboleth.idp.attribute.resolver.impl.dc;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Set;

import javax.sql.DataSource;

import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.saml.attribute.resolver.PersistentIdEntry;
import net.shibboleth.idp.saml.attribute.resolver.StoredIDException;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.idp.saml.impl.attribute.resolver.StoredIDDataConnector;
import net.shibboleth.idp.testing.DatabaseTestingSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.io.CharStreams;

/**
 * Tests for {@link StoredIDDataConnector} Placed here for convenience.
 */
public class StoredIDDataConnectorTest extends OpenSAMLInitBaseTestCase {

    /** The attribute name. */
    private static final String TEST_ATTRIBUTE_NAME = "storedAttribute";

    /** The connector name. */
    private static final String TEST_CONNECTOR_NAME = "storedAttributeConnector";

    private static final String INIT_FILE = "/data/net/shibboleth/idp/attribute/resolver/impl/dc/StoredIdStore.sql";

    private DataSource testSource;

    public static String convertStreamToString(java.io.InputStream is) throws IOException {
        return CharStreams.toString(new InputStreamReader(is));
    }

    @BeforeTest public void setupSource() throws SQLException, IOException {

        testSource = DatabaseTestingSupport.GetMockDataSource(INIT_FILE, "StoredIDDataConnectorStore");
    }

    private void tryInitialize(StoredIDDataConnector connector, String failMessage) {
        try {
            connector.initialize();
            Assert.fail(failMessage);
        } catch (ComponentInitializationException e) {
            // OK
        }

    }

    @Test public void initializeAndGetters() throws ComponentInitializationException, SQLException, ResolutionException {

        StoredIDDataConnector connector = new StoredIDDataConnector();
        connector.setId(TEST_CONNECTOR_NAME);
        connector.setSourceAttributeId(TestSources.STATIC_ATTRIBUTE_NAME);
        connector.setGeneratedAttributeId(TEST_ATTRIBUTE_NAME);

        tryInitialize(connector, "No DataSource");
        connector.setDataSource(testSource);

        connector.setSalt(ComputedIDDataConnectorTest.smallSalt);
        tryInitialize(connector, "salt too small");
        connector.setSalt(ComputedIDDataConnectorTest.salt);

        Assert.assertEquals(connector.getDataSource(), testSource);
        Assert.assertEquals(connector.getQueryTimeout(), 0);
        connector.setQueryTimeout(1);

        try {
            connector.resolve(null);
            Assert.fail("need to initialize first");
        } catch (UninitializedComponentException e) {
            // OK
        }

        connector.initialize();
        try {
            connector.setDataSource(null);
            Assert.fail("work after initialize");
        } catch (UnmodifiableComponentException e) {
            // OK
        }
        connector.initialize();
        try {
            connector.setQueryTimeout(0);
            Assert.fail("work after initialize");
        } catch (UnmodifiableComponentException e) {
            // OK
        }
        Assert.assertEquals(connector.getDataSource(), testSource);
        Assert.assertEquals(connector.getStoredIDStore().getDataSource(), testSource);
        Assert.assertEquals(connector.getQueryTimeout(), 1);
    }

    private AttributeResolver constructResolver(int values) throws ComponentInitializationException {
        StoredIDDataConnector connector = new StoredIDDataConnector();
        connector.setDataSource(testSource);

        return ComputedIDDataConnectorTest.constructResolver(connector, values);
    }

    /**
     * Test Trivial case. Starting with an empty store do a resolve - just like the computed ID one. Make sure that the
     * value *is* the computed ID.
     * 
     * @throws ComponentInitializationException if badness happens
     * @throws SQLException if badness happens
     * @throws ResolutionException if badness happens
     */
    @Test public void storeEntry() throws ComponentInitializationException, SQLException, ResolutionException {
        AttributeResolver resolver = constructResolver(1);

        ComponentSupport.initialize(resolver);
        ComputedIDDataConnectorTest.connectorFromResolver(resolver).initialize();

        AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        resolver.resolveAttributes(context);

        // Now test that we got exactly what we expected
        Set<IdPAttributeValue<?>> resultValues =
                context.getResolvedIdPAttributes().get(ComputedIDDataConnectorTest.OUTPUT_ATTRIBUTE_NAME).getValues();
        Assert.assertEquals(resultValues.size(), 1);
        Assert.assertEquals(((StringAttributeValue) resultValues.iterator().next()).getValue(),
                ComputedIDDataConnectorTest.RESULT);

    }

    /**
     * Do we look like a guid 01234567-9ABC-EFGH-JKLM-......
     * 
     * @param value what to check.
     */
    private void assertIsUUID(String value) {
        Assert.assertEquals(value.charAt(8), '-');
        Assert.assertEquals(value.charAt(13), '-');
        Assert.assertEquals(value.charAt(18), '-');
        Assert.assertEquals(value.charAt(23), '-');

    }

    /**
     * Test deactivated case. We exist in the database because of the dependency. Check this then mark the ID as
     * deactivated. The resolve again and hey presto a new value.
     * 
     * @throws ComponentInitializationException if badness happens
     * @throws StoredIDException if badness happens
     * @throws ResolutionException if badness happens
     */
    @Test(dependsOnMethods = {"storeEntry"}) void retrieveEntry() throws ComponentInitializationException,
            StoredIDException, ResolutionException {
        AttributeResolver resolver = constructResolver(1);

        ComputedIDDataConnectorTest.connectorFromResolver(resolver).initialize();
        ComponentSupport.initialize(resolver);

        AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);

        resolver.resolveAttributes(context);

        // Now test that we got exactly what we expected
        Set<IdPAttributeValue<?>> resultValues =
                context.getResolvedIdPAttributes().get(ComputedIDDataConnectorTest.OUTPUT_ATTRIBUTE_NAME).getValues();
        Assert.assertEquals(resultValues.size(), 1);
        Assert.assertEquals(((StringAttributeValue) resultValues.iterator().next()).getValue(),
                ComputedIDDataConnectorTest.RESULT);

        // Now void it and try again

        resolver = constructResolver(1);

        
        StoredIDDataConnector connector =
                (StoredIDDataConnector) ComputedIDDataConnectorTest.connectorFromResolver(resolver);
        ComponentSupport.initialize(resolver);
        connector.initialize();
        connector.getStoredIDStore().deactivatePersistentId(ComputedIDDataConnectorTest.RESULT, null);

        context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);

        resolver.resolveAttributes(context);

        // Now test that we got exactly what we expected
        resultValues =
                context.getResolvedIdPAttributes().get(ComputedIDDataConnectorTest.OUTPUT_ATTRIBUTE_NAME).getValues();
        Assert.assertEquals(resultValues.size(), 1);
        String val = ((StringAttributeValue) resultValues.iterator().next()).getValue();
        Assert.assertNotEquals(val, ComputedIDDataConnectorTest.RESULT);
        assertIsUUID(val);
    }

    @Test(dependsOnMethods = {"retrieveEntry"}) void badEntry() throws ComponentInitializationException,
            StoredIDException, ResolutionException {
        StoredIDDataConnector connector = new StoredIDDataConnector();
        connector.setDataSource(testSource);

        AttributeResolver resolver = ComputedIDDataConnectorTest.constructResolverWithNonString(connector, "nonString");

        ComponentSupport.initialize(resolver);
        ComputedIDDataConnectorTest.connectorFromResolver(resolver).initialize();

        connector.getStoredIDStore().deactivatePersistentId(ComputedIDDataConnectorTest.RESULT, null);

        AttributeResolutionContext context =
                TestSources.createResolutionContext(" ", TestSources.IDP_ENTITY_ID, TestSources.SP_ENTITY_ID);

        resolver.resolveAttributes(context);

        // Now test that we got exactly what we expected - nothing
        Assert.assertNull(context.getResolvedIdPAttributes().get(ComputedIDDataConnectorTest.OUTPUT_ATTRIBUTE_NAME));
    }

    /**
     * Test a marginal case where the persistentID already exists. We need to generate another one.
     * 
     * @throws ComponentInitializationException if badness happens
     * @throws SQLException if badness happens
     * @throws ResolutionException if badness happens
     */
    @Test() void previousEntry() throws ComponentInitializationException, StoredIDException, ResolutionException {
        AttributeResolver resolver = constructResolver(1);
        StoredIDDataConnector connector =
                (StoredIDDataConnector) ComputedIDDataConnectorTest.connectorFromResolver(resolver);
        DataSource source = DatabaseTestingSupport.GetMockDataSource(INIT_FILE, "StoredIDDataConnectorStorePrevious");
        connector.setDataSource(source);
        //
        // Precharge the store with a duplicate persisent ID
        //

        connector.initialize();
        ComponentSupport.initialize(resolver);
        PersistentIdEntry idEntry = new PersistentIdEntry();
        idEntry.setAttributeIssuerId(TestSources.IDP_ENTITY_ID);
        idEntry.setLocalId("wibble");
        idEntry.setPeerEntityId(TestSources.SP_ENTITY_ID + "2");
        idEntry.setPrincipalName("princ");
        idEntry.setPersistentId(ComputedIDDataConnectorTest.RESULT);

        connector.getStoredIDStore().storePersistentIdEntry(idEntry);

        AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        resolver.resolveAttributes(context);

        // Now test that we got exactly what we expected
        Set<IdPAttributeValue<?>> resultValues =
                context.getResolvedIdPAttributes().get(ComputedIDDataConnectorTest.OUTPUT_ATTRIBUTE_NAME).getValues();
        Assert.assertEquals(resultValues.size(), 1);
        String val = ((StringAttributeValue) resultValues.iterator().next()).getValue();
        Assert.assertNotEquals(val, ComputedIDDataConnectorTest.RESULT);
        assertIsUUID(val);

    }
}