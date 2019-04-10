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

package net.shibboleth.idp.attribute.resolver.dc.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.DurablePairwiseIdStore;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.PairwiseId;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.impl.JDBCPairwiseIdStore;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.idp.testing.DatabaseTestingSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

/**
 * Test for {@link PairwiseIdDataConnector} with JDBC store.
 */
public class StoredIDDataConnectorTest extends OpenSAMLInitBaseTestCase {

    private static final String INIT_FILE = "/net/shibboleth/idp/attribute/resolver/impl/dc/StoredIdStore.sql";

    private static final String DELETE_FILE = "/net/shibboleth/idp/attribute/resolver/impl/dc/DeleteStore.sql";

    private DataSource testSource;

    @BeforeTest public void setupSource() throws SQLException, IOException {

        testSource = DatabaseTestingSupport.GetMockDataSource(INIT_FILE, "StoredIDDataConnectorStore");
    }

    @AfterClass public void teardown() {
        DatabaseTestingSupport.InitializeDataSource(DELETE_FILE, testSource);
    }

    private AttributeResolver constructResolver(final int values) throws ComponentInitializationException {
        return constructResolver(values, false);
    }
    
    private AttributeResolver constructResolver(final int values, final boolean noSalt) throws ComponentInitializationException {
        
        final JDBCPairwiseIdStore store = new JDBCPairwiseIdStore();
        store.setDataSource(testSource);
        
        final PairwiseIdDataConnector connector = new PairwiseIdDataConnector();
        connector.setPairwiseIdStore(store);

        return ComputedIDDataConnectorTest.constructResolver(connector, values, noSalt);
    }

    /**
     * Test Trivial case. Starting with an empty store do a resolve - just like the computed ID one. Make sure that the
     * value *is* the computed ID.
     * 
     * @throws ComponentInitializationException if badness happens
     * @throws SQLException if badness happens
     * @throws ResolutionException if badness happens
     */
    @Test(dependsOnMethods={"noSalt",}) public void storeEntry() throws ComponentInitializationException, SQLException, ResolutionException {
        final AttributeResolver resolver = constructResolver(1);

        ComponentSupport.initialize(resolver);
        
        ComputedIDDataConnectorTest.connectorFromResolver(resolver).initialize();

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        resolver.resolveAttributes(context);

        // Now test that we got exactly what we expected
        final List<IdPAttributeValue<?>> resultValues =
                context.getResolvedIdPAttributes().get(ComputedIDDataConnectorTest.OUTPUT_ATTRIBUTE_NAME).getValues();
        assertEquals(resultValues.size(), 1);
        assertEquals(((StringAttributeValue) resultValues.iterator().next()).getValue(),
                ComputedIDDataConnectorTest.RESULT);

    }
    
    /**
     * Test Trivial case. Starting with an empty store do a resolve. Make sure that the
     * value *is* a UID.
     * 
     * @throws ComponentInitializationException if badness happens
     * @throws SQLException if badness happens
     * @throws ResolutionException if badness happens
     */
    @Test public void noSalt() throws ComponentInitializationException, SQLException, ResolutionException {
        final AttributeResolver resolver = constructResolver(1, true);

        ComponentSupport.initialize(resolver);
        ComputedIDDataConnectorTest.connectorFromResolver(resolver).initialize();

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID+1,
                        TestSources.SP_ENTITY_ID);
        resolver.resolveAttributes(context);

        // Now test that we got exactly what we expected
        final List<IdPAttributeValue<?>> resultValues =
                context.getResolvedIdPAttributes().get(ComputedIDDataConnectorTest.OUTPUT_ATTRIBUTE_NAME).getValues();
        assertEquals(resultValues.size(), 1);
        assertIsUUID(((StringAttributeValue) resultValues.iterator().next()).getValue());

    }


    /**
     * Do we look like a guid 01234567-9ABC-EFGH-JKLM-......
     * 
     * @param value what to check.
     */
    private void assertIsUUID(final String value) {
        assertEquals(value.charAt(8), '-');
        assertEquals(value.charAt(13), '-');
        assertEquals(value.charAt(18), '-');
        assertEquals(value.charAt(23), '-');

    }

    /**
     * Test deactivated case. We exist in the database because of the dependency. Check this then mark the ID as
     * deactivated. The resolve again and hey presto a new value.
     * 
     * @throws ComponentInitializationException if badness happens
     * @throws IOException if badness happens
     * @throws ResolutionException if badness happens
     */
    @Test(dependsOnMethods = {"storeEntry"}) void retrieveEntry() throws ComponentInitializationException,
            IOException, ResolutionException {
        AttributeResolver resolver = constructResolver(1);

        ComputedIDDataConnectorTest.connectorFromResolver(resolver).initialize();
        ComponentSupport.initialize(resolver);

        AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);

        resolver.resolveAttributes(context);

        // Now test that we got exactly what we expected
        List<IdPAttributeValue<?>> resultValues =
                context.getResolvedIdPAttributes().get(ComputedIDDataConnectorTest.OUTPUT_ATTRIBUTE_NAME).getValues();
        assertEquals(resultValues.size(), 1);
        assertEquals(((StringAttributeValue) resultValues.iterator().next()).getValue(),
                ComputedIDDataConnectorTest.RESULT);

        // Now void it and try again

        resolver = constructResolver(1);

        
        final PairwiseIdDataConnector connector =
                (PairwiseIdDataConnector) ComputedIDDataConnectorTest.connectorFromResolver(resolver);
        ComponentSupport.initialize(resolver);
        connector.initialize();
        
        final PairwiseId pid = new PairwiseId();
        pid.setIssuerEntityID(TestSources.IDP_ENTITY_ID);
        pid.setRecipientEntityID(TestSources.SP_ENTITY_ID);
        pid.setPairwiseId(ComputedIDDataConnectorTest.RESULT);
        ((DurablePairwiseIdStore) connector.getPairwiseIdStore()).deactivate(pid);

        context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);

        resolver.resolveAttributes(context);

        // Now test that we got exactly what we expected
        resultValues =
                context.getResolvedIdPAttributes().get(ComputedIDDataConnectorTest.OUTPUT_ATTRIBUTE_NAME).getValues();
        assertEquals(resultValues.size(), 1);
        final String val = ((StringAttributeValue) resultValues.iterator().next()).getValue();
        assertNotEquals(val, ComputedIDDataConnectorTest.RESULT);
        assertIsUUID(val);
    }

    @Test(dependsOnMethods = {"retrieveEntry"}) void badEntry() throws ComponentInitializationException,
            IOException, ResolutionException {
        
        final JDBCPairwiseIdStore store = new JDBCPairwiseIdStore();
        store.setDataSource(testSource);
        
        final PairwiseIdDataConnector connector = new PairwiseIdDataConnector();
        connector.setPairwiseIdStore(store);

        final AttributeResolver resolver = ComputedIDDataConnectorTest.constructResolverWithNonString(connector, "nonString");

        ComponentSupport.initialize(resolver);
        ComputedIDDataConnectorTest.connectorFromResolver(resolver).initialize();

        final PairwiseId pid = new PairwiseId();
        pid.setIssuerEntityID(TestSources.IDP_ENTITY_ID);
        pid.setRecipientEntityID(TestSources.SP_ENTITY_ID);
        pid.setPairwiseId(ComputedIDDataConnectorTest.RESULT);
        ((DurablePairwiseIdStore) connector.getPairwiseIdStore()).deactivate(pid);

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(" ", TestSources.IDP_ENTITY_ID, TestSources.SP_ENTITY_ID);

        resolver.resolveAttributes(context);

        // Now test that we got exactly what we expected - nothing
        assertNull(context.getResolvedIdPAttributes().get(ComputedIDDataConnectorTest.OUTPUT_ATTRIBUTE_NAME));
    }

}
