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

package net.shibboleth.idp.profile.interceptor.impl;

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.interceptor.AbstractProfileInterceptorResult;
import net.shibboleth.idp.profile.interceptor.ProfileInterceptorFlowDescriptor;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.shared.annotation.constraint.NotEmpty;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.impl.MemoryStorageService;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link WriteProfileInterceptorResultToStorage} unit test. */
@SuppressWarnings("javadoc")
public class WriteProfileInterceptorResultToStorageTest {

    private RequestContext src;

    private ProfileRequestContext prc;

    private MemoryStorageService ss;

    private WriteProfileInterceptorResultToStorage action;

    @BeforeMethod public void setUp() throws Exception {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);

        ss = new MemoryStorageService();
        ss.setId("test");
        ss.initialize();

        final ProfileInterceptorFlowDescriptor descriptor = new ProfileInterceptorFlowDescriptor();
        descriptor.setStorageService(ss);

        final ProfileInterceptorContext pic = new ProfileInterceptorContext();
        pic.setAttemptedFlow(descriptor);
        prc.addSubcontext(pic);

        action = new WriteProfileInterceptorResultToStorage();
        action.initialize();
    }

    @Test public void testNoResults() {
        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);
    }

    @Test public void testNoAttemptedFlow() {
        prc.addSubcontext(new ProfileInterceptorContext(), true);
        final ProfileInterceptorContext ctx = prc.getSubcontext(ProfileInterceptorContext.class);
        assert ctx != null && ctx.getAttemptedFlow() == null;

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);
    }

    @Test public void testNoStorageService() {
        final ProfileInterceptorContext ctx = prc.getSubcontext(ProfileInterceptorContext.class);
        assert ctx != null;
        ctx.setAttemptedFlow(new ProfileInterceptorFlowDescriptor());
        final ProfileInterceptorFlowDescriptor flow = ctx.getAttemptedFlow();
        assert flow != null && flow.getStorageService()==null;

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);
    }

    @Test public void testCreateStorageRecord() throws Exception {
        final MockProfileInterceptorResult result = new MockProfileInterceptorResult("context", "key", "value", null);
        final ProfileInterceptorContext ctx = prc.getSubcontext(ProfileInterceptorContext.class);
        assert ctx != null;
        ctx. getResults().add(result);

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        final StorageRecord<?> storageRecord = ss.read("context", "key");
        assert storageRecord != null;
        Assert.assertEquals(storageRecord.getValue(), "value");
        Assert.assertEquals(storageRecord.getExpiration(), null);
    }

    @Test public void testCreateStorageRecordWithExpiration() throws Exception {
        final Instant expiration = Instant.now().plusSeconds(60);
        final MockProfileInterceptorResult result =
                new MockProfileInterceptorResult("context", "key", "value", expiration);
        final ProfileInterceptorContext ctx = prc.getSubcontext(ProfileInterceptorContext.class);
        assert ctx != null;
        ctx.getResults().add(result);

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        final StorageRecord<?> storageRecord = ss.read("context", "key");
        assert storageRecord != null;
        Assert.assertEquals(storageRecord.getValue(), "value");
        Assert.assertEquals(storageRecord.getExpiration(), Long.valueOf(expiration.toEpochMilli()));
    }

    @Test public void testUpdateStorageRecord() throws Exception {
        final Instant expiration = Instant.now().plusSeconds(60);
        MockProfileInterceptorResult result = new MockProfileInterceptorResult("context", "key", "value", null);
        final ProfileInterceptorContext ctx = prc.getSubcontext(ProfileInterceptorContext.class);
        assert ctx != null;
        ctx.getResults().add(result);

        Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        StorageRecord<?> storageRecord = ss.read("context", "key");
        assert storageRecord != null;
        Assert.assertEquals(storageRecord.getValue(), "value");
        Assert.assertEquals(storageRecord.getExpiration(), null);

        result = new MockProfileInterceptorResult("context", "key", "value2", expiration);
        ctx.getResults().clear();
        ctx.getResults().add(result);

        event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        storageRecord = ss.read("context", "key");
        assert storageRecord != null;
        Assert.assertEquals(storageRecord.getValue(), "value2");
        Assert.assertEquals(storageRecord.getExpiration(), Long.valueOf(expiration.toEpochMilli()));
    }

    private class MockProfileInterceptorResult extends AbstractProfileInterceptorResult {

        public MockProfileInterceptorResult(@Nonnull @NotEmpty final String context,
                @Nonnull @NotEmpty final String key, @Nonnull @NotEmpty final String value,
                @Nullable final Instant expiration) {
            super(context, key, value, expiration);
        }
    }

}
