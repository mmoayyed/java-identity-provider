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

package net.shibboleth.idp.profile.audit.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.profile.audit.impl.PopulateAuditContext.FormattingMapParser;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.profile.context.AuditContext;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link PopulateAuditContext} unit test. */
@SuppressWarnings("javadoc")
public class PopulateAuditContextTest {

    private RequestContext src;
    
    private ProfileRequestContext prc;

    private PopulateAuditContext action;
    
    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);

        action = new PopulateAuditContext();
    }
    
    @Test public void testSingle() throws Exception {
        final Map<String,Function<ProfileRequestContext,Object>> map = new HashMap<>();
        map.put("a", new MockFunction(CollectionSupport.singletonList("foo")));
        
        action.setFieldExtractors(map);
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuditContext ac = prc.getSubcontext(AuditContext.class);
        assert ac != null;
        Assert.assertEquals(ac.getFieldValues("a").size(), 1);
        Assert.assertEquals(ac.getFieldValues("a").iterator().next(), "foo");
        Assert.assertTrue(ac.getFieldValues("b").isEmpty());
    }

    @Test public void testMultiple() throws Exception {
        final Map<String,Function<ProfileRequestContext,Object>> map = new HashMap<>();
        map.put("a", new MockFunction(CollectionSupport.singletonList("foo")));
        map.put("A", new MockFunction(CollectionSupport.listOf("bar", "baz")));
        
        action.setFieldExtractors(map);
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuditContext ac = prc.getSubcontext(AuditContext.class);
        assert ac != null;
        Assert.assertEquals(ac.getFieldValues("a").size(), 1);
        Assert.assertEquals(ac.getFieldValues("a").iterator().next(), "foo");
        Assert.assertTrue(ac.getFieldValues("b").isEmpty());
        Assert.assertEquals(ac.getFieldValues("A").size(), 2);
        Assert.assertEquals(ac.getFieldValues("A").toArray(), new String[]{"bar", "baz"});
    }

    @Test public void testSkipped() throws Exception {
        final Map<String,Function<ProfileRequestContext,Object>> map = new HashMap<>();
        map.put("a", new MockFunction(CollectionSupport.singletonList("foo")));
        map.put("A", new MockFunction(CollectionSupport.listOf("bar", "baz")));
        
        action.setFieldExtractors(map);
        action.setFormattingMapParser(new FormattingMapParser(CollectionSupport.singletonMap("foo", "%A - %b %%")));
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuditContext ac = prc.getSubcontext(AuditContext.class);
        assert ac != null;
        Assert.assertTrue(ac.getFieldValues("a").isEmpty());
        Assert.assertTrue(ac.getFieldValues("b").isEmpty());
        Assert.assertEquals(ac.getFieldValues("A").size(), 2);
        Assert.assertEquals(ac.getFieldValues("A").toArray(), new String[]{"bar", "baz"});
    }
    
    private class MockFunction implements Function<ProfileRequestContext,Object> {
        
        private Collection<String> result;
        
        /**
         * Constructor.
         * 
         * @param arg ...
         */
        public MockFunction(final Collection<String> arg) {
            result = arg;
        }

        /** {@inheritDoc} */
        @Override
        public Collection<String> apply(ProfileRequestContext input) {
            return result;
        }
    }
    
}
