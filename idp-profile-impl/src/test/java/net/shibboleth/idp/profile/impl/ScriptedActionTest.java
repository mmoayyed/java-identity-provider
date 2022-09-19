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

import net.shibboleth.idp.profile.ScriptedAction;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import javax.script.ScriptException;

import org.opensaml.profile.action.EventIds;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit test for {@link ScriptedAction}. */
public class ScriptedActionTest {

    private RequestContext rc;
    
    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        rc = new RequestContextBuilder().buildRequestContext();
    }
        
    @Test public void testProceed() throws ScriptException, ComponentInitializationException {

        final ScriptedAction action = ScriptedAction.inlineScript("null");
        action.initialize();
        
        final Event result = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(result);
    }

    @Test public void testEvent() throws ScriptException, ComponentInitializationException {

        final ScriptedAction action = ScriptedAction.inlineScript("'foo'");
        action.initialize();
        
        final Event result = action.execute(rc);
        ActionTestingSupport.assertEvent(result, "foo");
    }

    @Test public void testBadEvent() throws ScriptException, ComponentInitializationException {

        final ScriptedAction action = ScriptedAction.inlineScript("0");
        action.initialize();
        
        final Event result = action.execute(rc);
        ActionTestingSupport.assertEvent(result, EventIds.INVALID_PROFILE_CTX);
    }

    @Test public void testBadScript() throws ScriptException, ComponentInitializationException {

        final ScriptedAction action = ScriptedAction.inlineScript("foo");
        action.initialize();
        
        try {
            action.execute(rc);
            Assert.fail("Should have thrown");
        } catch (final RuntimeException e) {
            Assert.assertTrue(e.getCause() instanceof ScriptException);
        }
    }

    @Test public void testBadScriptHidden() throws ScriptException, ComponentInitializationException {

        final ScriptedAction action = ScriptedAction.inlineScript("foo");
        action.setHideExceptions(true);
        action.initialize();
        
        final Event result = action.execute(rc);
        ActionTestingSupport.assertEvent(result, EventIds.INVALID_PROFILE_CTX);
    }

}