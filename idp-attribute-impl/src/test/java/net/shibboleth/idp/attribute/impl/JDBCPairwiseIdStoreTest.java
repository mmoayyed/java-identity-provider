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

package net.shibboleth.idp.attribute.impl;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import net.shibboleth.idp.attribute.PairwiseId;
import net.shibboleth.idp.testing.DatabaseTestingSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link JDBCPairwiseIdStore}. */
public class JDBCPairwiseIdStoreTest {

    private DataSource testSource;
    
    public static final String INIT_FILE = "/net/shibboleth/idp/attribute/impl/StoredIdStore.sql";
    public static final String DELETE_FILE = "/net/shibboleth/idp/attribute/impl/DeleteStore.sql";
    
    @BeforeMethod
    public void setupSource() throws IOException, IOException  {
        testSource = DatabaseTestingSupport.GetMockDataSource(INIT_FILE, "PersistentIdStore");
    }
    
    @AfterMethod public void teardown() {
        DatabaseTestingSupport.InitializeDataSource(DELETE_FILE, testSource);
    }

    
    @Test public void initializeAndGetters() throws ComponentInitializationException, IOException {

        final JDBCPairwiseIdStore store = new JDBCPairwiseIdStore();
        try {
            store.initialize();
            Assert.fail("Need to initialize the source");
        } catch (final ComponentInitializationException e) {
            // OK
        }
        store.setDataSource(testSource);
        
        Assert.assertEquals(store.getDataSource(), testSource);
        Assert.assertEquals(store.getQueryTimeout(), Duration.ofSeconds(5));
        store.setQueryTimeout(Duration.ofMillis(1));
        
        try {
            store.getBySourceValue(new PairwiseId(), true);
            Assert.fail("need to initialize first");
        } catch (final UninitializedComponentException e) {
            // OK
        }
        
        store.initialize();
        try {
            store.setDataSource(null);
            Assert.fail("work after initialize");
        } catch (final UnmodifiableComponentException e) {
            // OK
        }
        store.initialize();
        try {
            store.setQueryTimeout(Duration.ZERO);
            Assert.fail("work after initialize");
        } catch (final UnmodifiableComponentException e) {
            // OK
        }
        Assert.assertEquals(store.getDataSource(), testSource);
        Assert.assertEquals(store.getQueryTimeout(), Duration.ofMillis(1));
    }
    
    private boolean comparePersistentIdEntrys(@Nonnull PairwiseId one, @Nonnull PairwiseId other)
    {
        //
        // Do not compare times
        //
        return Objects.equals(one.getPairwiseId(), other.getPairwiseId()) &&
                Objects.equals(one.getIssuerEntityID(), other.getIssuerEntityID()) &&
                Objects.equals(one.getRecipientEntityID(), other.getRecipientEntityID()) &&
                Objects.equals(one.getSourceSystemId(), other.getSourceSystemId()) &&
                Objects.equals(one.getPrincipalName(), other.getPrincipalName()) &&
                Objects.equals(one.getPeerProvidedId(), other.getPeerProvidedId());
    }
   
    @Test public void storeEntry() throws ComponentInitializationException, IOException, SQLException {
        final JDBCPairwiseIdStore store = new JDBCPairwiseIdStore();
        store.setDataSource(testSource);
        store.setVerifyDatabase(true);
        store.initialize();
        
        final PairwiseId id = new PairwiseId();
        String persistentId = UUID.randomUUID().toString();
        
        id.setIssuerEntityID(DatabaseTestingSupport.IDP_ENTITY_ID);
        id.setRecipientEntityID(DatabaseTestingSupport.SP_ENTITY_ID);
        id.setPrincipalName(DatabaseTestingSupport.PRINCIPAL_ID);
        id.setSourceSystemId("localID");
        id.setPeerProvidedId("PeerprovidedId");
        id.setPairwiseId(persistentId);
        id.setCreationTime(Instant.now());
        
        try (final Connection conn = testSource.getConnection()) {
            store.store(id, conn);
        }
        
        PairwiseId id2 = new PairwiseId();
        id2.setIssuerEntityID(DatabaseTestingSupport.IDP_ENTITY_ID);
        id2.setRecipientEntityID(DatabaseTestingSupport.SP_ENTITY_ID);
        id2.setPairwiseId(persistentId);
        id2 = store.getByIssuedValue(id2);
        
        Assert.assertNull(id2.getDeactivationTime());
        Assert.assertTrue(comparePersistentIdEntrys(id2, id));
        
        id.setDeactivationTime(Instant.now().plus(1, ChronoUnit.HOURS));
        store.deactivate(id);
        
        id2 = store.getByIssuedValue(id2);
        
        Assert.assertNotNull(id2.getDeactivationTime());
        Assert.assertTrue(comparePersistentIdEntrys(id2, id));
        
        id.setDeactivationTime(null);
        store.deactivate(id);
        
        Assert.assertNull(store.getByIssuedValue(id2));
     
        persistentId = UUID.randomUUID().toString();
        id.setPairwiseId(persistentId);
        id.setPeerProvidedId(null);
        id.setRecipientEntityID(DatabaseTestingSupport.SP_ENTITY_ID + "2");
        try (final Connection conn = testSource.getConnection()) {
            store.store(id, conn);
        }
        
        PairwiseId id3 = new PairwiseId();
        id3.setIssuerEntityID(DatabaseTestingSupport.IDP_ENTITY_ID);
        id3.setRecipientEntityID(DatabaseTestingSupport.SP_ENTITY_ID + "2");
        id3.setPairwiseId(persistentId);
        id3 = store.getByIssuedValue(id3);
        Assert.assertNull(id3.getDeactivationTime());
        Assert.assertTrue(comparePersistentIdEntrys(id3, id));
    }
    
}