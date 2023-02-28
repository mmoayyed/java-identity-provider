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

package net.shibboleth.idp.consent.flow.storage.impl;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.flow.impl.AbstractConsentActionTest;
import net.shibboleth.idp.consent.storage.impl.ConsentSerializer;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.interceptor.ProfileInterceptorFlowDescriptor;
import net.shibboleth.shared.component.UnmodifiableComponentException;
import net.shibboleth.shared.logic.FunctionSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageSerializer;
import org.opensaml.storage.impl.MemoryStorageService;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link AbstractConsentStorageAction} unit test. */
@SuppressWarnings("javadoc")
public abstract class AbstractConsentStorageActionTest extends AbstractConsentActionTest {

    private Object nullObj;

    protected void populateAction() throws Exception {
        ((AbstractConsentStorageAction) action).setStorageContextLookupStrategy(FunctionSupport
                .<ProfileRequestContext, String> constant("context"));
    
        ((AbstractConsentStorageAction) action).setStorageKeyLookupStrategy(FunctionSupport
                .<ProfileRequestContext, String> constant("key"));
    }

    @BeforeMethod protected void setUpMemoryStorageService() throws Exception {
        final MemoryStorageService storageService = new MemoryStorageService();
        storageService.setId("test");
        storageService.initialize();

        final ProfileInterceptorContext pic = prc.getSubcontext(ProfileInterceptorContext.class);
        assert pic!=null;
        final ProfileInterceptorFlowDescriptor flow = pic.getAttemptedFlow();
        assert flow!=null;
        flow.setStorageService(storageService);
    }

    protected MemoryStorageService getMemoryStorageService() {
        final ProfileInterceptorContext pic = prc.getSubcontext(ProfileInterceptorContext.class);
        assert pic!=null;
        final ProfileInterceptorFlowDescriptor flow = pic.getAttemptedFlow();
        assert flow!=null;
        Assert.assertNotNull(flow.getStorageService());
        Assert.assertTrue(flow.getStorageService() instanceof MemoryStorageService);
        return (MemoryStorageService) flow.getStorageService();
    }

    protected Map<String, Consent> readConsentsFromStorage() throws IOException {
        final StorageRecord<?> record = getMemoryStorageService().read("context", "key");
        assert record!=null;

        final ConsentSerializer consentSerializer =
                (ConsentSerializer) ((AbstractConsentStorageAction) action).getStorageSerializer();
        Assert.assertNotNull(consentSerializer);

        return consentSerializer.deserialize(0, "context", "key", record.getValue(), record.getExpiration());
    }

    @SuppressWarnings({ "null", "unchecked" })
    @Test(expectedExceptions = UnmodifiableComponentException.class)
    public void testUnmodifiableInterceptorContextStrategy() throws Exception {
        action.initialize();
        ((AbstractConsentStorageAction) action).setStorageContextLookupStrategy((Function<ProfileRequestContext, String>) nullObj);
    }
    
    @SuppressWarnings({ "null", "unchecked" })
    @Test(expectedExceptions = UnmodifiableComponentException.class)
    public void testUnmodifiableStorageKeyStrategy() throws Exception {
        action.initialize();
        ((AbstractConsentStorageAction) action).setStorageKeyLookupStrategy((Function<ProfileRequestContext, String>) nullObj);
    }
    
    @SuppressWarnings({ "null", "unchecked" })
    @Test(expectedExceptions = UnmodifiableComponentException.class)
    public void testUnmodifiableStorageSerializerStrategy() throws Exception {
        action.initialize();
        ((AbstractConsentStorageAction) action).setStorageSerializer((StorageSerializer<Map<String, Consent>>) nullObj);
    }

}
