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

import java.util.HashMap;
import java.util.Map;

import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.context.ConsentContext;
import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.context.RelyingPartyContext;

import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link ReleaseAttributes} unit test. */
// TODO incomplete
public class ReleaseAttributesTest extends AbstractAttributeReleaseActionTest {

    @Test public void testReleaseAttributes() throws Exception {
        final Consent consentToAttribute1 = new Consent();
        consentToAttribute1.setId("attribute1");
        consentToAttribute1.setApproved(true);

        final Consent consentToAttribute2 = new Consent();
        consentToAttribute2.setId("attribute2");
        consentToAttribute2.setApproved(false);

        final Map<String, Consent> consent = new HashMap<>();
        consent.put(consentToAttribute1.getId(), consentToAttribute1);
        consent.put(consentToAttribute2.getId(), consentToAttribute2);

        final ConsentContext consentCtx = prc.getSubcontext(ConsentContext.class);
        consentCtx.getPreviousConsents().putAll(consent);

        action = new ReleaseAttributes();
        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        final AttributeContext attrCtx =
                prc.getSubcontext(RelyingPartyContext.class).getSubcontext(AttributeContext.class);
        Assert.assertNotNull(attrCtx);
        Assert.assertEquals(attrCtx.getIdPAttributes().size(), 1);
        Assert.assertTrue(attrCtx.getIdPAttributes().containsKey("attribute1"));
        Assert.assertFalse(attrCtx.getIdPAttributes().containsKey("attribute2"));
        Assert.assertFalse(attrCtx.getIdPAttributes().containsKey("attribute3"));
    }

}
