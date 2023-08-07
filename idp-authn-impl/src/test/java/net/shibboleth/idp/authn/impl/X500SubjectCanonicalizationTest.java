/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.util.Arrays;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.component.ComponentInitializationException;

import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link X500SubjectCanonicalization} unit test. */
@SuppressWarnings("javadoc")
public class X500SubjectCanonicalizationTest extends BaseAuthenticationContextTest {
    
    private X500SubjectCanonicalization action; 
    
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        super.setUp();
        
        action = new X500SubjectCanonicalization();
        action.setTransforms(Arrays.asList(new Pair<>("^(.+)@osu\\.edu$", "$1")));
        action.setObjectIds(Arrays.asList("1.2.840.113549.1.9.1", "0.9.2342.19200300.100.1.1"));
        action.initialize();
    }
    
    @Test public void testNoContext() {
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_SUBJECT_C14N_CTX);
    }

    @Test public void testNoPrincipal() {
        final Subject subject = new Subject();
        prc.ensureSubcontext(SubjectCanonicalizationContext.class).setSubject(subject);
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_SUBJECT);
        final SubjectCanonicalizationContext scc = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert scc != null;
        Assert.assertNotNull(scc.getException());
    }

    @Test public void testMultiPrincipals() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new X500Principal("CN=foo"));
        subject.getPrincipals().add(new X500Principal("CN=bar"));
        prc.ensureSubcontext(SubjectCanonicalizationContext.class).setSubject(subject);
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_SUBJECT);
        final SubjectCanonicalizationContext scc = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert scc != null;
        Assert.assertNotNull(scc.getException());
    }

    @Test public void testNone() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new X500Principal("CN=foo@example.edu"));
        prc.ensureSubcontext(SubjectCanonicalizationContext.class).setSubject(subject);
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_SUBJECT);
        final SubjectCanonicalizationContext sc = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert sc!= null && sc.getPrincipalName() ==null;
    }
    
    @Test public void testSuccess() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new X500Principal("EMAILADDRESS=foo@example.edu"));
        prc.ensureSubcontext(SubjectCanonicalizationContext.class).setSubject(subject);
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        final SubjectCanonicalizationContext sc = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert sc != null;
        Assert.assertEquals(sc.getPrincipalName(), "foo@example.edu");
    }

    @Test public void testComplex() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new X500Principal("EMAILADDRESS=foo@example.edu\\, EMAILADDRESS=bar@example.edu"));
        prc.ensureSubcontext(SubjectCanonicalizationContext.class).setSubject(subject);
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        final SubjectCanonicalizationContext sc = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert sc != null;
        Assert.assertEquals(sc.getPrincipalName(), "foo@example.edu, EMAILADDRESS=bar@example.edu");
    }
    
    @Test public void testTransform() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new X500Principal("EMAILADDRESS=foo@osu.edu"));
        prc.ensureSubcontext(SubjectCanonicalizationContext.class).setSubject(subject);
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        final SubjectCanonicalizationContext sc = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert sc != null;
        Assert.assertEquals(sc.getPrincipalName(), "foo");
    }
    
    @Test public void testMultipleTypes() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new X500Principal("EMAILADDRESS=foo@example.edu, 0.9.2342.19200300.100.1.1=bar@example.edu"));
        prc.ensureSubcontext(SubjectCanonicalizationContext.class).setSubject(subject);
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        final SubjectCanonicalizationContext sc = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert sc != null;
        Assert.assertEquals(sc.getPrincipalName(), "foo@example.edu");
    }

    @Test public void testMultipleValues() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new X500Principal("EMAILADDRESS=foo@example.edu, EMAILADDRESS=bar@example.edu"));
        prc.ensureSubcontext(SubjectCanonicalizationContext.class).setSubject(subject);
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        final SubjectCanonicalizationContext sc = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert sc != null;
        Assert.assertEquals(sc.getPrincipalName(), "foo@example.edu");
    }

    @Test public void testSecondary() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new X500Principal("0.9.2342.19200300.100.1.1=bar@example.edu"));
        prc.ensureSubcontext(SubjectCanonicalizationContext.class).setSubject(subject);
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        final SubjectCanonicalizationContext scc = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert scc != null;
        Assert.assertEquals(scc.getPrincipalName(), "bar@example.edu");
    }
}