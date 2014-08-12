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

import java.util.List;

import javax.annotation.Nonnull;

import net.shibboleth.idp.profile.ActionTestingSupport;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.profile.context.AuditContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link WriteAuditLog} unit test. */
public class WriteAuditLogTest {

    private RequestContext src;
    
    private ProfileRequestContext prc;

    private FilteringAction action;
    
    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);

        action = new FilteringAction();
    }
    
    @Test public void testNoContext() throws Exception {
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
    }

    @Test public void testFormat() {
        action.setFormat("Foo");
        List<String> format = action.getFormat();
        Assert.assertEquals(format.size(), 1);
        Assert.assertEquals(format.get(0), "Foo");

        action.setFormat("%Foo");
        format = action.getFormat();
        Assert.assertEquals(format.size(), 1);
        Assert.assertEquals(format.get(0), "%Foo");

        action.setFormat("%Foo|%Bar %Baz%Bat");
        format = action.getFormat();
        Assert.assertEquals(format.size(), 5);
        Assert.assertEquals(format.toArray(), new String[]{"%Foo", "|", "%Bar", " ", "%Baz%Bat"});

        action.setFormat("%Foo|%Bar %Baz-Bat");
        format = action.getFormat();
        Assert.assertEquals(format.size(), 5);
        Assert.assertEquals(format.toArray(), new String[]{"%Foo", "|", "%Bar", " ", "%Baz-Bat"});

        action.setFormat("%Foo|%Bar %%%");
        format = action.getFormat();
        Assert.assertEquals(format.size(), 5);
        Assert.assertEquals(format.toArray(), new String[]{"%Foo", "|", "%Bar", " ", "%%%"});

        action.setFormat("%Foo|%Bar % %%");
        format = action.getFormat();
        Assert.assertEquals(format.size(), 7);
        Assert.assertEquals(format.toArray(), new String[]{"%Foo", "|", "%Bar", " ", "%", " ", "%%"});
    }
    
    @Test public void testTwo() throws ComponentInitializationException {
        final AuditContext ac = prc.getSubcontext(AuditContext.class, true);
        ac.getFieldValues("a").add("foo");
        ac.getFieldValues("b").add("bar");
        ac.getFieldValues("b").add("baz");
        
        action.setFormat("%a %b");
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(action.getResult(), "foo bar,baz");
    }

    @Test public void testMissing() throws ComponentInitializationException {
        final AuditContext ac = prc.getSubcontext(AuditContext.class, true);
        ac.getFieldValues("a").add("foo");
        ac.getFieldValues("b").add("bar");
        ac.getFieldValues("b").add("baz");
        
        action.setFormat("%a - %c|%b");
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(action.getResult(), "foo - |bar,baz");
    }

    /**
     * Subclass for testing purposes that grants access to the built log entry.
     */
    class FilteringAction extends WriteAuditLog {
        
        private String result;
        
        /**
         * Get the log entry being written.
         * 
         * @return  log entry
         */
        public String getResult() {
            return result;
        }
        
        /** {@inheritDoc} */
        @Override
        protected void filter(@Nonnull StringBuilder entry) {
            result = entry.toString();
        }
    }
}