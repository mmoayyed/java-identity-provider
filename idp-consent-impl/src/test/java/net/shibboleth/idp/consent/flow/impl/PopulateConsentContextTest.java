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

package net.shibboleth.idp.consent.flow.impl;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.context.ConsentContext;
import net.shibboleth.idp.consent.impl.ConsentTestingSupport;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.shared.logic.ConstraintViolationException;
import net.shibboleth.shared.logic.FunctionSupport;

import java.util.Map;
import java.util.function.Function;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link PopulateConsentContext} unit test. */
@SuppressWarnings("javadoc")
public class PopulateConsentContextTest extends AbstractConsentActionTest {

    private Object nullObj;
    
    @SuppressWarnings({ "null", "unchecked" })
    @Test(expectedExceptions = ConstraintViolationException.class) public void testNullCurrentConsentsFunction()
            throws Exception {
        action = new PopulateConsentContext((Function<ProfileRequestContext, Map<String, Consent>>) nullObj);
        action.initialize();
    }

    @Test public void testCurrentConsentsFunction() throws Exception {
        action = new PopulateConsentContext(FunctionSupport.constant(ConsentTestingSupport.newConsentMap()));
        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        final ConsentContext consentContext = prc.getSubcontext(ConsentContext.class, false);
        assert consentContext!= null;
        Assert.assertEquals(consentContext.getCurrentConsents(), ConsentTestingSupport.newConsentMap());
    }

}