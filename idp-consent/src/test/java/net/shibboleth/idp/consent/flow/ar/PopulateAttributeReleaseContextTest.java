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

import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.consent.ConsentTestingSupport;
import net.shibboleth.idp.consent.context.AttributeReleaseContext;
import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/** {@link PopulateAttributeReleaseContext} unit test. */
public class PopulateAttributeReleaseContextTest extends AbstractAttributeReleaseActionTest {

    @Test(expectedExceptions = ComponentInitializationException.class) public void testMissingPredicate()
            throws Exception {
        action = new PopulateAttributeReleaseContext();
        action.initialize();
    }

    @Test public void testObtainConsentForAllAttributes() throws Exception {
        action = new PopulateAttributeReleaseContext();
        ((PopulateAttributeReleaseContext) action).setAttributePredicate(Predicates.<IdPAttribute> alwaysTrue());
        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        final AttributeReleaseContext arc = prc.getSubcontext(AttributeReleaseContext.class, false);
        Assert.assertNotNull(arc);
        Assert.assertEquals(arc.getConsentableAttributes(), ConsentTestingSupport.newAttributeMap());
    }

    @Test public void testObtainConsentForSomeAttributes() throws Exception {
        action = new PopulateAttributeReleaseContext();
        ((PopulateAttributeReleaseContext) action).setAttributePredicate(new MockIdPAttributePredicate());
        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        final AttributeReleaseContext arc = prc.getSubcontext(AttributeReleaseContext.class, false);
        Assert.assertNotNull(arc);
        Assert.assertNotEquals(arc.getConsentableAttributes(), ConsentTestingSupport.newAttributeMap());
        Assert.assertTrue(arc.getConsentableAttributes().containsKey("attribute1"));
        Assert.assertTrue(arc.getConsentableAttributes().containsKey("attribute2"));
        Assert.assertFalse(arc.getConsentableAttributes().containsKey("attribute3"));
    }

    /** Mock IdP attribute predicate. */
    private class MockIdPAttributePredicate implements Predicate<IdPAttribute> {

        /** {@inheritDoc} */
        public boolean apply(@Nullable final IdPAttribute input) {

            if (input.getId().equals("attribute1") || input.getId().equals("attribute2")) {
                return true;
            }

            return false;
        }
    }

}
