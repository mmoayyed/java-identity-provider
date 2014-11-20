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

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.ConsentTestingSupport;
import net.shibboleth.idp.consent.context.ConsentContext;
import net.shibboleth.idp.consent.storage.ConsentResult;
import net.shibboleth.idp.consent.storage.ConsentSerializer;
import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.interceptor.ProfileInterceptorResult;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link CreateResult} unit test. */
public class CreateResultTest extends AbstractConsentStorageActionTest {

    @BeforeMethod public void setUpAction() throws Exception {
        action = new CreateResult();

        ((AbstractConsentStorageAction) action).setStorageContextLookupStrategy(FunctionSupport
                .<ProfileRequestContext, String> constant("context"));

        ((AbstractConsentStorageAction) action).setStorageKeyLookupStrategy(FunctionSupport
                .<ProfileRequestContext, String> constant("key"));
    }

    @Test public void testCreateResultNoCurrentConsents() throws Exception {
        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        final ProfileInterceptorContext pic = prc.getSubcontext(ProfileInterceptorContext.class, false);
        Assert.assertNotNull(pic);
        Assert.assertEquals(pic.getResults().size(), 0);
    }

    @Test public void testCreateResult() throws Exception {

        final ConsentContext consentCtx = prc.getSubcontext(ConsentContext.class);
        consentCtx.getCurrentConsents().putAll(ConsentTestingSupport.newConsentMap());

        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        final ProfileInterceptorContext pic = prc.getSubcontext(ProfileInterceptorContext.class, false);
        Assert.assertNotNull(pic);
        Assert.assertEquals(pic.getResults().size(), 1);

        final ProfileInterceptorResult result = pic.getResults().get(0);
        Assert.assertTrue(result instanceof ConsentResult);
        Assert.assertEquals(result.getStorageContext(), "context");
        Assert.assertEquals(result.getStorageKey(), "key");
        Assert.assertNull(result.getStorageExpiration());

        final ConsentSerializer consentSerializer =
                (ConsentSerializer) ((AbstractConsentStorageAction) action).getStorageSerializer();

        final Map<String, Consent> consents =
                consentSerializer.deserialize(0, result.getStorageContext(), result.getStorageKey(),
                        result.getStorageValue(), result.getStorageExpiration());
        Assert.assertEquals(consents.size(), 2);
        Assert.assertEquals(consents, ConsentTestingSupport.newConsentMap());
    }

}
