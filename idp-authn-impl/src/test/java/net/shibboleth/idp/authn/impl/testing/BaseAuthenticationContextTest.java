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

package net.shibboleth.idp.authn.impl.testing;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.impl.PopulateAuthenticationContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.FunctionSupport;

import java.util.List;

import javax.annotation.Nonnull;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.execution.RequestContext;

import jakarta.servlet.http.HttpServletRequest;

/** Base class for further action tests. */
public class BaseAuthenticationContextTest extends OpenSAMLInitBaseTestCase {

    protected RequestContext src;
    protected ProfileRequestContext prc;
    protected List<AuthenticationFlowDescriptor> authenticationFlows;

    protected void initializeMembers() throws ComponentInitializationException {        
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        prc.addSubcontext(new AuthenticationContext(), true);

        authenticationFlows = List.of(new AuthenticationFlowDescriptor(),
                new AuthenticationFlowDescriptor(), new AuthenticationFlowDescriptor());
        authenticationFlows.get(0).setId("test1");
        authenticationFlows.get(1).setId("test2");
        authenticationFlows.get(1).setPassiveAuthenticationSupported(true);
        authenticationFlows.get(2).setId("test3");
    }

    protected void setUp() throws ComponentInitializationException {        
        initializeMembers();
        
        final PopulateAuthenticationContext action = new PopulateAuthenticationContext();
        assert authenticationFlows!= null;
        action.setAvailableFlows(authenticationFlows);
        action.setPotentialFlowsLookupStrategy(FunctionSupport.constant(authenticationFlows));
        action.initialize();

        action.execute(src);
    }

    @Nonnull protected final MockHttpServletRequest getMockHttpServletRequest(final AbstractAuthenticationAction action) {
        assert action != null;
        final HttpServletRequest req = action.getHttpServletRequest();
        assert req != null;
        return (MockHttpServletRequest)req;
    }

}