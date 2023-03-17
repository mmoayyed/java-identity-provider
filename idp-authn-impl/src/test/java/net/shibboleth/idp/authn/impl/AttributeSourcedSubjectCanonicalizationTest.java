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

import javax.security.auth.Subject;

import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.idp.authn.principal.IdPAttributePrincipal;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;

/** {@link AttributeSourcedSubjectCanonicalization} unit test. */
public class AttributeSourcedSubjectCanonicalizationTest extends BaseAuthenticationContextTest {
    
    private AttributeSourcedSubjectCanonicalization action; 
    
    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        super.setUp();
        
        action = new AttributeSourcedSubjectCanonicalization();
        action.setAttributeSourceIds(CollectionSupport.listOf("attr1", "attr2"));
    }
    
    @Test(expectedExceptions=ComponentInitializationException.class)
    public void testNoSources() throws ComponentInitializationException {
        action = new AttributeSourcedSubjectCanonicalization();
        action.initialize();
    }
    
    @Test public void testNoContext() throws ComponentInitializationException {
        action.initialize();
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_SUBJECT_C14N_CTX);
    }

    @Test public void testNoAttributes() throws ComponentInitializationException {
        action.initialize();

        Subject subject = new Subject();
        prc.ensureSubcontext(SubjectCanonicalizationContext.class).setSubject(subject);
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_SUBJECT);
        SubjectCanonicalizationContext scc = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert scc != null && scc.getException()!=null;
    }

    @Test public void testNoSubjectSourcedAttributes() throws ComponentInitializationException {
        action.setResolveFromSubject(true);
        action.initialize();
        
        Subject subject = new Subject();
        prc.ensureSubcontext(SubjectCanonicalizationContext.class).setSubject(subject);
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_SUBJECT);
        SubjectCanonicalizationContext scc = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert scc != null && scc.getException()!=null;
    }

    @Test public void testSuccess() throws ComponentInitializationException {
        action.initialize();
        
        final IdPAttribute inputAttribute = new IdPAttribute("attr2");
        inputAttribute.setValues(Collections.singletonList(new StringAttributeValue("foo")));
        
        final SubjectCanonicalizationContext sc = prc.ensureSubcontext(SubjectCanonicalizationContext.class);
        sc.setSubject(new Subject());
        
        sc.ensureSubcontext(AttributeContext.class).setIdPAttributes(Collections.singleton(inputAttribute));
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(sc.getPrincipalName(), "foo");
    }

    @Test public void testSubjectSourcedSuccess() throws ComponentInitializationException {
        action.setResolveFromSubject(true);
        action.initialize();
        
        final IdPAttribute inputAttribute = new IdPAttribute("attr2");
        inputAttribute.setValues(Collections.singletonList(new StringAttributeValue("foo")));
        final SubjectCanonicalizationContext sc = prc.ensureSubcontext(SubjectCanonicalizationContext.class);
        final Subject subject = new Subject();
        sc.setSubject(subject);
        subject.getPrincipals().add(new IdPAttributePrincipal(inputAttribute));

        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(sc.getPrincipalName(), "foo");
    }

    @Test public void testSubjectSourcedScopedSuccess() throws ComponentInitializationException {
        action.setResolveFromSubject(true);
        action.initialize();
        
        final IdPAttribute inputAttribute = new IdPAttribute("attr2");
        inputAttribute.setValues(Collections.singletonList(new ScopedStringAttributeValue("foo", "scope")));
        final SubjectCanonicalizationContext sc = prc.ensureSubcontext(SubjectCanonicalizationContext.class);
        final Subject subject = new Subject();
        sc.setSubject(subject);
        subject.getPrincipals().add(new IdPAttributePrincipal(inputAttribute));

        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(sc.getPrincipalName(), "foo@scope");
    }
    
    @Test public void testDualSubjectSourcedSuccess() throws ComponentInitializationException {
        action.setResolveFromSubject(true);
        action.initialize();

        final IdPAttribute attr2 = new IdPAttribute("attr2");
        attr2.setValues(Collections.singletonList(new StringAttributeValue("foo")));

        final IdPAttribute attr2bar = new IdPAttribute("attr2");
        attr2bar.setValues(Collections.singletonList(new StringAttributeValue("bar")));

        final SubjectCanonicalizationContext sc = prc.ensureSubcontext(SubjectCanonicalizationContext.class);
        final Subject subject = new Subject();
        sc.setSubject(subject);
        subject.getPrincipals().add(new IdPAttributePrincipal(attr2));
        
        sc.ensureSubcontext(AttributeContext.class).setIdPAttributes(Collections.singleton(attr2bar));
        
        Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(sc.getPrincipalName(), "foo");

        subject.getPrincipals().clear();
        subject.getPrincipals().add(new IdPAttributePrincipal(attr2bar));
        sc.ensureSubcontext(AttributeContext.class).setIdPAttributes(Collections.singleton(attr2));

        event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(sc.getPrincipalName(), "bar");
    }

    @Test public void testDualSubjectSourcedSuccess2() throws ComponentInitializationException {
        action.setResolveFromSubject(true);
        action.initialize();

        final IdPAttribute attr2 = new IdPAttribute("attr2");
        attr2.setValues(Collections.singletonList(new StringAttributeValue("bar")));

        final IdPAttribute attr1 = new IdPAttribute("attr1");
        attr1.setValues(Collections.singletonList(new StringAttributeValue("foo")));

        final SubjectCanonicalizationContext sc = prc.ensureSubcontext(SubjectCanonicalizationContext.class);
        final Subject subject = new Subject();
        sc.setSubject(subject);
        subject.getPrincipals().add(new IdPAttributePrincipal(attr2));
        
        sc.ensureSubcontext(AttributeContext.class).setIdPAttributes(Collections.singleton(attr1));
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(sc.getPrincipalName(), "foo");
    }
    
}