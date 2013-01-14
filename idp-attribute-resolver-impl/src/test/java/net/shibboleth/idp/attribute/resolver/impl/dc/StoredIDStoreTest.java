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
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import net.shibboleth.idp.attribute.resolver.impl.ConstantStringStrategy;
import net.shibboleth.idp.attribute.resolver.impl.DatabaseTestingSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.base.Objects;
import com.google.common.io.CharStreams;

/**
 * Tests for {@link StoredIDStore}
 */
public class StoredIDStoreTest {
    
    private final Logger log = LoggerFactory.getLogger(StoredIDStoreTest.class);

    private DataSource testSource;
    
    public static String convertStreamToString(java.io.InputStream is) throws IOException {
        return CharStreams.toString(new InputStreamReader(is));
    }
    
    @BeforeTest
    public void setupSource() throws SQLException, IOException  {
        
        testSource = DatabaseTestingSupport.GetMockDataSource("/data/net/shibboleth/idp/attribute/resolver/impl/dc/StoredIdStore.sql", "StoredIDStore");
    }
    
    @Test public void testInitializeAndGetters() throws ComponentInitializationException, SQLException {

        StoredIDStore store = new StoredIDStore();
        try {
            store.initialize();
            Assert.fail("Need to initialize the source");
        } catch (ComponentInitializationException e) {
            // OK 
        }
        store.setDataSource(testSource);
        
        Assert.assertEquals(store.getDataSource(), testSource);
        Assert.assertEquals(store.getQueryTimeout(), 0);
        store.setQueryTimeout(1);
        
        try {
            store.getActivePersistentIdEntry("fii");
            Assert.fail("need to initialize first");
        } catch (UninitializedComponentException e) {
            // OK
        }
        
        store.initialize();
        try {
            store.setDataSource(null);
            Assert.fail("work after initialize");
        } catch (UnmodifiableComponentException e) {
            // OK
        }
        store.initialize();
        try {
            store.setQueryTimeout(0);
            Assert.fail("work after initialize");
        } catch (UnmodifiableComponentException e) {
            // OK
        }
        Assert.assertEquals(store.getDataSource(), testSource);
        Assert.assertEquals(store.getQueryTimeout(), 1);
    }
    
    private boolean comparePersistentIdEntrys(@Nonnull PersistentIdEntry one, @Nonnull PersistentIdEntry other)
    {
        //
        // Do not compare times
        //
        boolean result = Objects.equal(one.getPersistentId(), other.getPersistentId()) &&
                Objects.equal(one.getLocalEntityId(), other.getLocalEntityId()) &&
                Objects.equal(one.getPeerEntityId(), other.getPeerEntityId()) &&
                Objects.equal(one.getLocalId(), other.getLocalId()) &&
                Objects.equal(one.getPrincipalName(), other.getPrincipalName()) &&
                Objects.equal(one.getPeerProvidedId(), other.getPeerProvidedId()) &&
                Objects.equal(one.getDeactivationTime(), other.getDeactivationTime());
        if (!result) {
            log.warn("Not equals: {} and {}", one, other);
        }
        return result;
    }
    
    private void testValidate(StoredIDStore store, PersistentIdEntry id, String errorMessage) {

        try {
            store.validatePersistentIdEntry(id);
            Assert.fail(errorMessage);
        } catch (SQLException e) {
            // OK
        }

    }
   
    @Test public void testStoreEntry() throws ComponentInitializationException, SQLException {
        StoredIDStore store = new StoredIDStore();
        store.setDataSource(testSource);
        store.initialize();
        
        final PersistentIdEntry id = new PersistentIdEntry();
        String persistentId = UUID.randomUUID().toString();
        
        testValidate(store, id, "No Local Id");
        id.setLocalEntityId(ConstantStringStrategy.IDP_ENTITY_ID);
        
        testValidate(store, id, "No Peer Entity Id");
        id.setPeerEntityId(ConstantStringStrategy.SP_ENTITY_ID);
        
        testValidate(store, id, "No Principal Name");
        id.setPrincipalName(ConstantStringStrategy.PRINCIPAL_ID);
        
        testValidate(store, id, "No Local Id");
        id.setLocalId("localID");
        
        id.setPeerProvidedId("PeerprovidedId");
        
        testValidate(store, id, "No PersistentId");
        id.setPersistentId(persistentId);
        
        store.storePersistentIdEntry(id);
        
        PersistentIdEntry gotback = store.getActivePersistentIdEntry(persistentId);
        
        Assert.assertNull(gotback.getDeactivationTime());
        
        Assert.assertTrue(comparePersistentIdEntrys(gotback, id));
        List<PersistentIdEntry> list = store.getPersistentIdEntries(ConstantStringStrategy.IDP_ENTITY_ID, ConstantStringStrategy.SP_ENTITY_ID, "localID");
        
        Assert.assertEquals(list.size(), 1);
        Assert.assertTrue(comparePersistentIdEntrys(list.get(0), gotback));
        
        store.deactivatePersistentId(persistentId, null);
        
        Assert.assertNull(store.getActivePersistentIdEntry(persistentId));
        
        Assert.assertEquals(store.getNumberOfPersistentIdEntries(ConstantStringStrategy.IDP_ENTITY_ID, ConstantStringStrategy.SP_ENTITY_ID, "localID"), 1);
     
        persistentId = UUID.randomUUID().toString();
        id.setPersistentId(persistentId);
        id.setPeerProvidedId(null);
        id.setPeerEntityId(ConstantStringStrategy.SP_ENTITY_ID + "2");
        store.storePersistentIdEntry(id);
        
        gotback = store.getActivePersistentIdEntry(persistentId);
        
    }
}
