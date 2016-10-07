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

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.collect.ImmutableList;

/** Base class for further action tests. */
public class BaseAuthenticationContextTest extends OpenSAMLInitBaseTestCase {

    protected RequestContext src;
    protected ProfileRequestContext prc;
    protected ImmutableList<AuthenticationFlowDescriptor> authenticationFlows;

    protected void initializeMembers() throws Exception {        
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        prc.addSubcontext(new AuthenticationContext(), true);

        authenticationFlows = ImmutableList.of(new AuthenticationFlowDescriptor(),
                new AuthenticationFlowDescriptor(), new AuthenticationFlowDescriptor());
        authenticationFlows.get(0).setId("test1");
        authenticationFlows.get(1).setId("test2");
        authenticationFlows.get(2).setId("test3");
    }

    protected void setUp() throws Exception {        
        initializeMembers();
        
        final PopulateAuthenticationContext action = new PopulateAuthenticationContext();
        action.setAvailableFlows(authenticationFlows);
        action.setPotentialFlows(authenticationFlows);
        action.initialize();

        action.execute(src);
    }

}