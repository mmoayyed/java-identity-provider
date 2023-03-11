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

package net.shibboleth.idp.profile.impl;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.shared.component.UninitializedComponentException;

import javax.annotation.Nonnull;

import org.opensaml.profile.context.PreviousEventContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link AbstractProfileAction}. */
@SuppressWarnings("javadoc")
public class AbstractProfileActionTest {

    @Test public void testActionExecution() throws Exception {
        RequestContext springRequestContext = new RequestContextBuilder().buildRequestContext();

        MockProfileAction action = new MockProfileAction();
        action.initialize();

        Event result = action.execute(springRequestContext);
        Assert.assertTrue(action.isExecuted());
        ActionTestingSupport.assertProceedEvent(result);

        action = new MockProfileAction(new RuntimeException());
        action.initialize();

        try {
            action.execute(springRequestContext);
            Assert.fail();
        } catch (RuntimeException e) {
            // expected this
        }

        action = new MockProfileAction(new RuntimeException());

        try {
            action.execute(springRequestContext);
            Assert.fail();
        } catch (UninitializedComponentException e) {
            // expected this
        }
    }
    
    @Test public void testActionEvent() throws Exception {
        RequestContext springRequestContext = new RequestContextBuilder().buildRequestContext();

        MockProfileAction action = new MockProfileAction("Event1", "Event2");
        action.initialize();
        Event result = action.execute(springRequestContext);
        ActionTestingSupport.assertEvent(result, "InvalidPreviousEvent");
        
        action = new MockProfileAction("Event1", "InvalidPreviousEvent");
        action.initialize();
        result = action.execute(springRequestContext);
        ActionTestingSupport.assertEvent(result, "Event1");

        action = new MockProfileAction("Event2", "Event1");
        action.initialize();
        result = action.execute(springRequestContext);
        ActionTestingSupport.assertEvent(result, "Event2");

        action = new MockProfileAction("Event3", "Event1");
        action.initialize();
        result = action.execute(springRequestContext);
        ActionTestingSupport.assertEvent(result, "InvalidPreviousEvent");
    }

    /** Mock {@link AbstractProfileAction}. */
    private class MockProfileAction extends AbstractProfileAction {

        private final String newEvent;
        private final String prevEvent;
        private final RuntimeException thrownException;
        private boolean executed;

        public MockProfileAction() {
            thrownException = null;
            newEvent = null;
            prevEvent = null;
        }
        
        public MockProfileAction(RuntimeException exception) {
            thrownException = exception;
            newEvent = null;
            prevEvent = null;
        }

        public MockProfileAction(String newEv, String prev) {
            newEvent = newEv;
            prevEvent = prev;
            thrownException = null;
        }
        
        public boolean isExecuted() {
            return executed;
        }

        /** {@inheritDoc} */
        @Override
        protected void doExecute(@Nonnull ProfileRequestContext profileRequestContext) {

            executed = true;
            
            final PreviousEventContext<?> prevCtx = profileRequestContext.getSubcontext(PreviousEventContext.class);
            if (prevEvent != null) {
                if (prevCtx == null || !prevEvent.equals(prevCtx.getEvent())) {
                    org.opensaml.profile.action.ActionSupport.buildEvent(profileRequestContext, "InvalidPreviousEvent");
                    return;
                }
            } else if (prevCtx != null) {
                org.opensaml.profile.action.ActionSupport.buildEvent(profileRequestContext, "InvalidPreviousEvent");
                return;
            }

            if (thrownException != null) {
                throw thrownException;
            } else if (newEvent != null) {
                org.opensaml.profile.action.ActionSupport.buildEvent(profileRequestContext, newEvent);
            }
        }
    }
}