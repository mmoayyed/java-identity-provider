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

package net.shibboleth.idp.saml.impl.profile.saml1;

import net.shibboleth.idp.attribute.AttributeContext;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link AddAttributeStatementToAssertion} unit test. */
public class AddAttributeStatementToAssertionTest extends OpenSAMLInitBaseTestCase {

    @Test public void testNoRelyingPartyContext() throws Exception {
        ProfileRequestContext profileCtx = new ProfileRequestContext();

        AddAttributeStatementToAssertion action = new AddAttributeStatementToAssertion();
        Event event = action.doExecute(null, null, null, profileCtx);
        
        Assert.assertEquals(event.getId(), AddAttributeStatementToAssertion.NO_RPC_EVENT_ID);
    }
    
    @Test public void testNoAttributeContext() throws Exception {
        RelyingPartyContext rpCtx = new RelyingPartyContext("http://example.org");
        
        ProfileRequestContext profileCtx = new ProfileRequestContext();
        profileCtx.addSubcontext(rpCtx);
        
        AddAttributeStatementToAssertion action = new AddAttributeStatementToAssertion();
        Event event = action.doExecute(null, null, null, profileCtx);
        
        Assert.assertEquals(event.getId(), AddAttributeStatementToAssertion.NO_AC_EVENT_ID);
    }
    
    @Test public void testNoAttributes() throws Exception {
        AttributeContext attribCtx = new AttributeContext();
        
        RelyingPartyContext rpCtx = new RelyingPartyContext("http://example.org");
        rpCtx.addSubcontext(attribCtx);
        
        ProfileRequestContext profileCtx = new ProfileRequestContext();
        profileCtx.addSubcontext(rpCtx);
        
        AddAttributeStatementToAssertion action = new AddAttributeStatementToAssertion();
        Event event = action.doExecute(null, null, null, profileCtx);
        
        Assert.assertEquals(event.getId(), ActionSupport.PROCEED_EVENT_ID);
    }
    
    @Test public void testIgnoreAttributeEncodingErrors(){
      //TODO
    }
    
    @Test public void failOnAttributeEncodingErrors(){
      //TODO
    }
    
    @Test public void testNonResponseOutboundMessage(){
      //TODO
    }
    
    @Test public void testNoOutboundMessage(){
      //TODO
    }
    
    @Test public void testNoAssertionInResponse(){
      //TODO
    }
    
    @Test public void testAddedAttributeStatement(){
        //TODO
    }
}