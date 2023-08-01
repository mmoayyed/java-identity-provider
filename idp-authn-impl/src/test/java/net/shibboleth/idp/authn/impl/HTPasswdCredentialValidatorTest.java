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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.AuthenticationErrorContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.authn.principal.impl.ExactPrincipalEvalPredicateFactory;
import net.shibboleth.idp.authn.testing.TestPrincipal;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.spring.resource.ResourceHelper;
import net.shibboleth.shared.testing.ConstantSupplier;

/** Unit test for htpasswd file validation. */
@SuppressWarnings("javadoc")
public class HTPasswdCredentialValidatorTest extends BaseAuthenticationContextTest {

    private static final String DATA_PATH = "src/test/resources/net/shibboleth/idp/authn/impl/";
    
    private HTPasswdCredentialValidator validator;
    
    private ValidateCredentials action;

    @BeforeMethod public void setUp() throws ComponentInitializationException {
        super.setUp();

        validator = new HTPasswdCredentialValidator();
        validator.setResource(ResourceHelper.of(new ClassPathResource("net/shibboleth/idp/authn/impl/htpasswd.txt")));
        validator.setId("htpasswdtest");
        
        action = new ValidateCredentials();
        action.setValidators(CollectionSupport.singletonList(validator));
        
        final Map<String,Collection<String>> mappings = new HashMap<>();
        mappings.put("InvalidPassword", CollectionSupport.singleton(AuthnEventIds.INVALID_CREDENTIALS));
        mappings.put(AuthnEventIds.UNKNOWN_USERNAME, CollectionSupport.singleton(AuthnEventIds.UNKNOWN_USERNAME));
        action.setClassifiedMessages(mappings);
        
        final MockHttpServletRequest request = new MockHttpServletRequest();
        action.setHttpServletRequestSupplier(new ConstantSupplier<>(request));
    }

    @Test public void testMissingFlow() throws ComponentInitializationException {
        validator.initialize();
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_AUTHN_CTX);
    }

    @Test public void testMissingUser() throws ComponentInitializationException {
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        validator.initialize();
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testMissingUser2() throws ComponentInitializationException {
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        ac.getSubcontext(UsernamePasswordContext.class);
        
        validator.initialize();
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }
    
    @Test public void testUnsupported() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "foo");
        getMockHttpServletRequest(action).addParameter("password", "bar");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        final RequestedPrincipalContext rpc = ac.ensureSubcontext(RequestedPrincipalContext.class);
        rpc.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "exact", new ExactPrincipalEvalPredicateFactory());
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(CollectionSupport.singletonList(new TestPrincipal("test1")));

        validator.setSupportedPrincipals(CollectionSupport.singletonList(new TestPrincipal("test2")));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.REQUEST_UNSUPPORTED);
    }
    
    @Test public void testUnmatchedUser() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "foo");
        getMockHttpServletRequest(action).addParameter("password", "bar");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        ac.getSubcontext(UsernamePasswordContext.class);
        
        validator.setMatchExpression(Pattern.compile("foo.+"));
        validator.initialize();
        
        action.initialize();
        
        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.REQUEST_UNSUPPORTED);
    }

    @Test public void testBadUsername() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "foo");
        getMockHttpServletRequest(action).addParameter("password", "bar");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.UNKNOWN_USERNAME);
        AuthenticationErrorContext errorCtx = ac.getSubcontext(AuthenticationErrorContext.class);
        assert errorCtx!= null;
        Assert.assertTrue(errorCtx.getExceptions().get(0) instanceof LoginException);
        Assert.assertTrue(errorCtx.isClassifiedError(AuthnEventIds.UNKNOWN_USERNAME));
        Assert.assertFalse(errorCtx.isClassifiedError("InvalidPassword"));
    }

    @Test public void testBadPassword() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "PETER_THE_PRINCIPAL");
        getMockHttpServletRequest(action).addParameter("password", "bar");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "InvalidPassword");
        AuthenticationErrorContext errorCtx = ac.getSubcontext(AuthenticationErrorContext.class);
        assert errorCtx != null;
        Assert.assertTrue(errorCtx.getExceptions().get(0) instanceof LoginException);
        Assert.assertFalse(errorCtx.isClassifiedError("UnknownUsername"));
        Assert.assertTrue(errorCtx.isClassifiedError("InvalidPassword"));
    }

    @Test public void testAuthorizedMD5() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "PETER_THE_PRINCIPAL");
        getMockHttpServletRequest(action).addParameter("password", "changeit");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));

        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationResult result = ac.getAuthenticationResult();
        assert result != null;
        Assert.assertEquals(result.getSubject().getPrincipals(UsernamePrincipal.class).iterator()
                .next().getName(), "PETER_THE_PRINCIPAL");
    }

    @Test public void testAuthorizedMD5WithFile() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "PETER_THE_PRINCIPAL");
        getMockHttpServletRequest(action).addParameter("password", "changeit");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));

        validator.setResource(ResourceHelper.of(new FileSystemResource(DATA_PATH + "htpasswd.txt")));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationResult result = ac.getAuthenticationResult();
        assert result != null;
        Assert.assertEquals(result.getSubject().getPrincipals(UsernamePrincipal.class).iterator()
                .next().getName(), "PETER_THE_PRINCIPAL");
    }

    @Test public void testAuthorizedSHA() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "PETER_THE_PRINCIPAL2");
        getMockHttpServletRequest(action).addParameter("password", "changeit");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));

        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationResult result = ac.getAuthenticationResult();
        assert result != null;
        Assert.assertEquals(result.getSubject().getPrincipals(UsernamePrincipal.class).iterator()
                .next().getName(), "PETER_THE_PRINCIPAL2");
    }

    @Test public void testAuthorizedCrypt() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "PETER_THE_PRINCIPAL3");
        getMockHttpServletRequest(action).addParameter("password", "changeit");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));

        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationResult result = ac.getAuthenticationResult();
        assert result != null;
        Assert.assertEquals(result.getSubject().getPrincipals(UsernamePrincipal.class).iterator()
                .next().getName(), "PETER_THE_PRINCIPAL3");
    }
    
    @Test public void testAuthorizedAndKeep() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "PETER_THE_PRINCIPAL");
        getMockHttpServletRequest(action).addParameter("password", "changeit");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));

        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationResult result = ac.getAuthenticationResult();
        assert result != null;
        Assert.assertEquals(result.getSubject().getPrincipals(UsernamePrincipal.class).iterator()
                .next().getName(), "PETER_THE_PRINCIPAL");
    }

    @Test public void testSupported() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "PETER_THE_PRINCIPAL");
        getMockHttpServletRequest(action).addParameter("password", "changeit");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        final RequestedPrincipalContext rpc = ac.ensureSubcontext(RequestedPrincipalContext.class);
        rpc.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "exact", new ExactPrincipalEvalPredicateFactory());
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(CollectionSupport.singletonList(new TestPrincipal("test1")));

        validator.setSupportedPrincipals(CollectionSupport.singletonList(new TestPrincipal("test1")));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationResult result = ac.getAuthenticationResult();
        assert result != null;
        Assert.assertEquals(result.getSubject().getPrincipals(UsernamePrincipal.class).iterator()
                .next().getName(), "PETER_THE_PRINCIPAL");
        Assert.assertEquals(result.getSubject().getPrincipals(TestPrincipal.class).iterator()
                .next().getName(), "test1");
    }
    
    private void doExtract() throws ComponentInitializationException {
        final ExtractUsernamePasswordFromFormRequest extract = new ExtractUsernamePasswordFromFormRequest();
        extract.setHttpServletRequestSupplier(action.getHttpServletRequestSupplier());
        extract.initialize();
        extract.execute(src);
    }
    
}