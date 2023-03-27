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

package net.shibboleth.idp.saml.profile.impl;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.saml.authn.principal.NameIDPrincipal;
import net.shibboleth.idp.saml.authn.principal.NameIdentifierPrincipal;
import net.shibboleth.idp.saml.profile.impl.ExtractSubjectFromRequest.SubjectNameLookupFunction;
import net.shibboleth.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.profile.context.navigate.IssuerLookupFunction;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;

import javax.security.auth.Subject;

import org.opensaml.core.testing.XMLObjectBaseTestCase;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.profile.logic.DefaultNameIDPolicyPredicate;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.opensaml.saml.saml1.core.Request;
import org.opensaml.saml.saml1.testing.SAML1ActionTestingSupport;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.testing.SAML2ActionTestingSupport;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit test for {@link ExtractSubjectFromRequest}. */
@SuppressWarnings("javadoc")
public class ExtractSubjectFromRequestTest extends XMLObjectBaseTestCase {

    private RequestContext rc;
    
    private ProfileRequestContext prc;
    
    private ExtractSubjectFromRequest action;
    
    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        rc = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(rc);

        final DefaultNameIDPolicyPredicate nameIDPolicyPredicate = new DefaultNameIDPolicyPredicate();
        nameIDPolicyPredicate.setRequesterIdLookupStrategy(new RelyingPartyIdLookupFunction());
        nameIDPolicyPredicate.setResponderIdLookupStrategy(new IssuerLookupFunction());
        nameIDPolicyPredicate.setObjectLookupStrategy(new SubjectNameLookupFunction());
        nameIDPolicyPredicate.initialize();
        
        action = new ExtractSubjectFromRequest();
        action.setNameIDPolicyPredicate(nameIDPolicyPredicate);
        action.initialize();
    }

    @Test
    public void testNoInboundContext() {
        prc.setInboundMessageContext(null);
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, ExtractSubjectFromRequest.NO_SUBJECT);
    }
   
    @Test
    public void testNoMessage() {
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(null);
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, ExtractSubjectFromRequest.NO_SUBJECT);
    }

    @Test
    public void testNoSubject() {
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(SAML2ActionTestingSupport.buildAuthnRequest());
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, ExtractSubjectFromRequest.NO_SUBJECT);
    }

    @Test
    public void testSAML2Subject() {
        final AuthnRequest request = SAML2ActionTestingSupport.buildAuthnRequest();
        request.setSubject(SAML2ActionTestingSupport.buildSubject("foo"));
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(request);
        
        final NameID nameID = Constraint.isNotNull(request.getSubject(), "Subject null").getNameID();
        assert nameID != null;
        nameID.setFormat(NameID.TRANSIENT);
        nameID.setNameQualifier("foo");
        Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_SUBJECT);
        
        nameID.setNameQualifier(ActionTestingSupport.OUTBOUND_MSG_ISSUER);
        nameID.setSPNameQualifier("foo");
        event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_SUBJECT);
        
        nameID.setSPNameQualifier(ActionTestingSupport.INBOUND_MSG_ISSUER);
        event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        final SubjectCanonicalizationContext scc = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert scc!=null;
        final Subject subject = scc.getSubject();
        assert subject!=null;
        Assert.assertEquals(subject.getPrincipals(NameIDPrincipal.class).size(), 1);
        
        final NameIDPrincipal princ = subject.getPrincipals(NameIDPrincipal.class).iterator().next();
        Assert.assertEquals(princ.getNameID().getValue(), "foo");
    }
    
    @Test
    public void testSAML1Subject() {
        final Request request = SAML1ActionTestingSupport.buildAttributeQueryRequest(
                SAML1ActionTestingSupport.buildSubject("foo"));
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(request);
        
        final org.opensaml.saml.saml1.core.Subject s = Constraint.isNotNull(request.getAttributeQuery(), "Query was null").getSubject();
        assert s != null;
        final NameIdentifier nameID = s.getNameIdentifier();
                
        nameID.setFormat(NameID.TRANSIENT);
        nameID.setNameQualifier("foo");
        Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_SUBJECT);
        
        nameID.setNameQualifier(ActionTestingSupport.OUTBOUND_MSG_ISSUER);
        event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        final SubjectCanonicalizationContext scc = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert scc!=null;
        final Subject subject = scc.getSubject();
        assert subject!=null;
        Assert.assertEquals(subject.getPrincipals(NameIdentifierPrincipal.class).size(), 1);
        
        final NameIdentifierPrincipal princ =
                subject.getPrincipals(NameIdentifierPrincipal.class).iterator().next();
        Assert.assertEquals(princ.getNameIdentifier().getValue(), "foo");
    }

}