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

package net.shibboleth.idp.consent.flow.ar;

import java.util.Map;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.flow.storage.AbstractConsentStorageAction;
import net.shibboleth.idp.consent.flow.storage.AbstractConsentStorageActionTest;
import net.shibboleth.idp.consent.storage.ConsentResult;
import net.shibboleth.idp.consent.storage.ConsentSerializer;
import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.interceptor.ProfileInterceptorResult;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link CreateGlobalAttributeReleaseResult} unit test. */
public class CreateGlobalAttributeReleaseResultTest extends AbstractConsentStorageActionTest {

    @Test public void setUpAction() throws Exception {
        action = new CreateGlobalAttributeReleaseResult();

        ((AbstractConsentStorageAction) action).setStorageContextLookupStrategy(FunctionSupport
                .<ProfileRequestContext, String> constant("context"));

        ((AbstractConsentStorageAction) action).setStorageKeyLookupStrategy(FunctionSupport
                .<ProfileRequestContext, String> constant("key"));
    }

    @Test public void testGlobalAttributeReleaseResult() throws Exception {
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
        Assert.assertEquals(consents.size(), 1);

        final Consent globalConsent = consents.values().iterator().next();
        Assert.assertNotNull(globalConsent);
        Assert.assertEquals(globalConsent.getId(), Consent.WILDCARD);
        Assert.assertNull(globalConsent.getValue());
        Assert.assertTrue(globalConsent.isApproved());
    }
}
