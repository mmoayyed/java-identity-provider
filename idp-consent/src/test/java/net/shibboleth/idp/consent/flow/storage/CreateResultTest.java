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

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.ConsentTestingSupport;
import net.shibboleth.idp.consent.context.ConsentContext;
import net.shibboleth.idp.consent.storage.ConsentSerializer;
import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageRecord;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link CreateResult} unit test. */
public class CreateResultTest extends AbstractConsentIndexedStorageActionTest {

    @BeforeMethod public void setUpAction() throws Exception {
        action = new CreateResult();
        populateAction();
    }

    @Test public void testNoCurrentConsents() throws Exception {
        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        final ProfileInterceptorContext pic = prc.getSubcontext(ProfileInterceptorContext.class, false);
        Assert.assertNotNull(pic);
        Assert.assertEquals(pic.getResults().size(), 0);

        final StorageRecord record = getMemoryStorageService().read("context", "key");
        Assert.assertNull(record);
    }

    @Test public void testCreateResult() throws Exception {
        action.initialize();

        final ConsentContext consentCtx = prc.getSubcontext(ConsentContext.class);
        consentCtx.getCurrentConsents().putAll(ConsentTestingSupport.newConsentMap());

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        final ProfileInterceptorContext pic = prc.getSubcontext(ProfileInterceptorContext.class, false);
        Assert.assertNotNull(pic);
        Assert.assertEquals(pic.getResults().size(), 0);

        final Map<String, Consent> consents = readConsentsFromStorage();
        Assert.assertEquals(consents.size(), 2);
        Assert.assertEquals(consents, ConsentTestingSupport.newConsentMap());

        final Collection<String> keys = readStorageKeysFromIndex();
        Assert.assertEquals(keys, Arrays.asList("key"));
    }

    @Test public void testUpdateResult() throws Exception {
        action.initialize();

        final ConsentContext consentCtx = prc.getSubcontext(ConsentContext.class);
        consentCtx.getCurrentConsents().putAll(ConsentTestingSupport.newConsentMap());

        ActionTestingSupport.assertProceedEvent(action.execute(src));
        ActionTestingSupport.assertProceedEvent(action.execute(src));

        final ProfileInterceptorContext pic = prc.getSubcontext(ProfileInterceptorContext.class, false);
        Assert.assertNotNull(pic);
        Assert.assertEquals(pic.getResults().size(), 0);

        final Map<String, Consent> consents = readConsentsFromStorage();
        Assert.assertEquals(consents.size(), 2);
        Assert.assertEquals(consents, ConsentTestingSupport.newConsentMap());

        final Collection<String> keys = readStorageKeysFromIndex();
        Assert.assertEquals(keys, Arrays.asList("key"));
    }

    @Test public void testMaxStoredRecords() throws Exception {
        descriptor.setMaximumNumberOfStoredRecords(2);

        final ConsentContext consentCtx = prc.getSubcontext(ConsentContext.class);
        consentCtx.getCurrentConsents().putAll(ConsentTestingSupport.newConsentMap());

        // key1

        final CreateResult action1 = new CreateResult();
        ((AbstractConsentStorageAction) action1).setStorageContextLookupStrategy(FunctionSupport
                .<ProfileRequestContext, String> constant("context"));
        ((AbstractConsentStorageAction) action1).setStorageKeyLookupStrategy(FunctionSupport
                .<ProfileRequestContext, String> constant("key1"));
        ((AbstractConsentIndexedStorageAction) action1).setStorageIndexKeyLookupStrategy(FunctionSupport
                .<ProfileRequestContext, String> constant("_index"));
        action1.initialize();

        ActionTestingSupport.assertProceedEvent(action1.execute(src));

        final StorageRecord record1 = getMemoryStorageService().read("context", "key1");
        Assert.assertNotNull(record1);
        final ConsentSerializer consentSerializer1 =
                (ConsentSerializer) ((AbstractConsentStorageAction) action1).getStorageSerializer();
        Assert.assertNotNull(consentSerializer1);

        final Map<String, Consent> consents1 =
                consentSerializer1.deserialize(0, "context", "key1", record1.getValue(), record1.getExpiration());
        Assert.assertEquals(consents1.size(), 2);
        Assert.assertEquals(consents1, ConsentTestingSupport.newConsentMap());

        Assert.assertEquals(readStorageKeysFromIndex(), Arrays.asList("key1"));

        // key2

        final CreateResult action2 = new CreateResult();
        ((AbstractConsentStorageAction) action2).setStorageContextLookupStrategy(FunctionSupport
                .<ProfileRequestContext, String> constant("context"));
        ((AbstractConsentStorageAction) action2).setStorageKeyLookupStrategy(FunctionSupport
                .<ProfileRequestContext, String> constant("key2"));
        ((AbstractConsentIndexedStorageAction) action2).setStorageIndexKeyLookupStrategy(FunctionSupport
                .<ProfileRequestContext, String> constant("_index"));
        action2.initialize();

        ActionTestingSupport.assertProceedEvent(action2.execute(src));

        final StorageRecord record2 = getMemoryStorageService().read("context", "key2");
        Assert.assertNotNull(record2);
        final ConsentSerializer consentSerializer2 =
                (ConsentSerializer) ((AbstractConsentStorageAction) action2).getStorageSerializer();
        Assert.assertNotNull(consentSerializer2);

        final Map<String, Consent> consents2 =
                consentSerializer2.deserialize(0, "context", "key2", record1.getValue(), record1.getExpiration());
        Assert.assertEquals(consents2.size(), 2);
        Assert.assertEquals(consents2, ConsentTestingSupport.newConsentMap());

        Assert.assertEquals(readStorageKeysFromIndex(), Arrays.asList("key1", "key2"));

        // key3

        final CreateResult action3 = new CreateResult();
        ((AbstractConsentStorageAction) action3).setStorageContextLookupStrategy(FunctionSupport
                .<ProfileRequestContext, String> constant("context"));
        ((AbstractConsentStorageAction) action3).setStorageKeyLookupStrategy(FunctionSupport
                .<ProfileRequestContext, String> constant("key3"));
        ((AbstractConsentIndexedStorageAction) action3).setStorageIndexKeyLookupStrategy(FunctionSupport
                .<ProfileRequestContext, String> constant("_index"));
        action3.initialize();

        ActionTestingSupport.assertProceedEvent(action3.execute(src));

        final StorageRecord record3 = getMemoryStorageService().read("context", "key2");
        Assert.assertNotNull(record3);
        final ConsentSerializer consentSerializer3 =
                (ConsentSerializer) ((AbstractConsentStorageAction) action2).getStorageSerializer();
        Assert.assertNotNull(consentSerializer3);

        final Map<String, Consent> consents3 =
                consentSerializer2.deserialize(0, "context", "key3", record1.getValue(), record1.getExpiration());
        Assert.assertEquals(consents3.size(), 2);
        Assert.assertEquals(consents3, ConsentTestingSupport.newConsentMap());

        Assert.assertEquals(readStorageKeysFromIndex(), Arrays.asList("key2", "key3"));
    }
}
