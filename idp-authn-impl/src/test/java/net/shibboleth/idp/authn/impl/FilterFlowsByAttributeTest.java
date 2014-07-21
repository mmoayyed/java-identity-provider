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

package net.shibboleth.idp.authn.impl;

import java.util.Collections;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.principal.TestPrincipal;
import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link FilterFlowsByAttribute} unit test. */
public class FilterFlowsByAttributeTest extends PopulateAuthenticationContextTest {
    
    private FilterFlowsByAttribute action; 
    
    @BeforeMethod public void setUp() throws Exception {
        super.setUp();
        
        action = new FilterFlowsByAttribute();
        action.setAttributeId("foo");
        action.initialize();
    }

    @Test public void testNoAttributeID() throws ComponentInitializationException {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        
        action = new FilterFlowsByAttribute();
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(authCtx.getPotentialFlows().size(), 3);
    }

    @Test public void testNoAttribute() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        
        Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(authCtx.getPotentialFlows().size(), 3);
        
        authCtx.getSubcontext(AttributeContext.class, true).setIdPAttributes(
                Collections.singletonList(new IdPAttribute("foo")));
        event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(authCtx.getPotentialFlows().size(), 3);
    }
    
    @Test public void testNoMatch() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        final IdPAttribute attr = new IdPAttribute("foo");
        authCtx.getSubcontext(AttributeContext.class, true).setIdPAttributes(Collections.singletonList(attr));
        attr.setValues(Collections.singleton(new StringAttributeValue("bar")));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(authCtx.getPotentialFlows().size(), 0);
    }

    @Test public void testMatch() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        final IdPAttribute attr = new IdPAttribute("foo");
        authCtx.getSubcontext(AttributeContext.class, true).setIdPAttributes(Collections.singletonList(attr));
        attr.setValues(Collections.singleton(new StringAttributeValue("bar")));
        
        authCtx.getPotentialFlows().get("test1").getSupportedPrincipals().add(new TestPrincipal("baz"));
        authCtx.getPotentialFlows().get("test2").getSupportedPrincipals().add(new TestPrincipal("bar"));
        authCtx.getPotentialFlows().get("test3").getSupportedPrincipals().add(new TestPrincipal("bay"));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(authCtx.getPotentialFlows().size(), 1);
        Assert.assertEquals(authCtx.getPotentialFlows().entrySet().iterator().next().getValue().getId(), "test2");
    }
    
}