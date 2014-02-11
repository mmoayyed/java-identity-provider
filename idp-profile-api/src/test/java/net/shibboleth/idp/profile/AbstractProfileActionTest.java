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

package net.shibboleth.idp.profile;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.context.PreviousEventContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link AbstractProfileAction}. */
public class AbstractProfileActionTest {

    @Test public void testActionId() {
        MockProfileAction action = new MockProfileAction();

        action.setId(" mock");
        Assert.assertEquals(action.getId(), "mock");

        try {
            action.setId(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }

        try {
            action.setId("   ");
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }
    }

    @Test public void testActionInitialization() {
        MockProfileAction action = new MockProfileAction();
        Assert.assertFalse(action.isInitialized());
        action.setId("mock");

        try {
            action.initialize();
        } catch (ComponentInitializationException e) {
            Assert.fail();
        }

        Assert.assertEquals(action.getId(), "mock");
        try {
            action.setId("foo");
        } catch (UnmodifiableComponentException e) {
            // ignore
        }
        Assert.assertEquals(action.getId(), "mock");
    }

    @Test public void testActionExecution() throws Exception {
        RequestContext springRequestContext = new RequestContextBuilder().buildRequestContext();

        MockProfileAction action = new MockProfileAction();
        action.initialize();

        Event result = action.execute(springRequestContext);
        Assert.assertTrue(action.isExecuted());
        ActionTestingSupport.assertProceedEvent(result);

        action = new MockProfileAction(new ProfileException());
        action.initialize();

        try {
            action.execute(springRequestContext);
            Assert.fail();
        } catch (ProfileException e) {
            // expected this
        }

        action = new MockProfileAction(new ProfileException());

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
        private final ProfileException thrownException;
        private boolean executed;

        public MockProfileAction() {
            thrownException = null;
            newEvent = null;
            prevEvent = null;
            setId("test");
        }
        
        public MockProfileAction(ProfileException exception) {
            thrownException = exception;
            newEvent = null;
            prevEvent = null;
            setId("test");
        }

        public MockProfileAction(String newEvent, String prevEvent) {
            this.newEvent = newEvent;
            this.prevEvent = prevEvent;
            thrownException = null;
            setId("test");
        }
        
        public boolean isExecuted() {
            return executed;
        }

        /** {@inheritDoc} */
        @Override
        protected void doExecute(ProfileRequestContext profileRequestContext) throws ProfileException {

            executed = true;
            
            final PreviousEventContext prevCtx = profileRequestContext.getSubcontext(PreviousEventContext.class, false);
            if (prevEvent != null) {
                if (prevCtx == null || !prevCtx.getEvent().equals(prevEvent)) {
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