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

package net.shibboleth.idp.consent.flow.storage;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.consent.ConsentTestingSupport;
import net.shibboleth.idp.consent.storage.ConsentResult;
import net.shibboleth.idp.consent.storage.StorageIndex;
import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.interceptor.impl.WriteProfileInterceptorResultToStorage;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageRecord;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link UpdateStorageIndex} unit test. */
public class UpdateStorageIndexTest extends AbstractConsentStorageActionTest {

    @BeforeMethod public void setUpAction() throws Exception {
        action = new UpdateStorageIndex();

        ((AbstractConsentStorageAction) action).setStorageKeyLookupStrategy(FunctionSupport
                .<ProfileRequestContext, String> constant("key"));
    }

    @Nonnull private Map<String, StorageIndex> deserializeStorageIndex(@Nonnull final StorageRecord storageRecord)
            throws Exception {
        return (Map<String, StorageIndex>) storageRecord.getValue(((UpdateStorageIndex) action).getStorageSerializer(),
                UpdateStorageIndex.STORAGE_INDEX_CONTEXT, "key");
    }

    @Nullable private StorageRecord readStorageIndexRecord() throws Exception {
        return getMemoryStorageService().read(UpdateStorageIndex.STORAGE_INDEX_CONTEXT, "key");
    }

    private void writeResultToStorage() throws Exception {
        final WriteProfileInterceptorResultToStorage writeAction = new WriteProfileInterceptorResultToStorage();
        writeAction.initialize();
        final Event writeEvent = writeAction.execute(src);
        ActionTestingSupport.assertProceedEvent(writeEvent);
    }

    @Test public void testCreateStorageIndex() throws Exception {

        Assert.assertNull(readStorageIndexRecord());

        final ProfileInterceptorContext pic = prc.getSubcontext(ProfileInterceptorContext.class);
        pic.getResults().addAll(ConsentTestingSupport.newConsentResults());

        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        writeResultToStorage();

        final StorageRecord storageRecord = readStorageIndexRecord();
        Assert.assertNotNull(readStorageIndexRecord());
        final Map<String, StorageIndex> storageIndexes = deserializeStorageIndex(storageRecord);
        Assert.assertEquals(storageIndexes, ConsentTestingSupport.newStorageIndexMap());
    }

    @Test public void testUpdateStorageIndex() throws Exception {

        testCreateStorageIndex();
        
        final ProfileInterceptorContext pic = prc.getSubcontext(ProfileInterceptorContext.class);
        pic.getResults().add(new ConsentResult("context3", "key3", "value3", null));
        
        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        writeResultToStorage();
        
        final StorageRecord storageRecord = readStorageIndexRecord();
        Assert.assertNotNull(readStorageIndexRecord());
        final Map<String, StorageIndex> storageIndexes = deserializeStorageIndex(storageRecord);
        
        final Map<String, StorageIndex> expectedStorageIndexes = ConsentTestingSupport.newStorageIndexMap();
        final StorageIndex storageIndex3 = new StorageIndex();
        storageIndex3.setContext("context3");
        storageIndex3.getKeys().add("key3");
        expectedStorageIndexes.put(storageIndex3.getContext(), storageIndex3);
        Assert.assertEquals(storageIndexes, expectedStorageIndexes);
    }
    
    @Test public void testUpdateStorageIndexNoChanges() throws Exception {
        
        testCreateStorageIndex();
        
        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        writeResultToStorage();
        
        final StorageRecord storageRecord = readStorageIndexRecord();
        Assert.assertNotNull(readStorageIndexRecord());
        final Map<String, StorageIndex> storageIndexes = deserializeStorageIndex(storageRecord);
        Assert.assertEquals(storageIndexes, ConsentTestingSupport.newStorageIndexMap());
    }

}
