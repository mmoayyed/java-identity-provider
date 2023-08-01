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

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.MultiFactorAuthenticationTransition;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.MultiFactorAuthenticationContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.FunctionSupport;

import java.util.HashMap;
import java.util.Map;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;

import com.google.common.collect.ImmutableMap;

/** Base class for further action tests. */
public class BaseMultiFactorAuthenticationContextTest {

    protected RequestContext src;
    protected ProfileRequestContext prc;
    protected AuthenticationContext ac;
    protected MultiFactorAuthenticationContext mfa;
    protected ImmutableMap<String,AuthenticationFlowDescriptor> authenticationFlows;

    protected void initializeMembers() throws ComponentInitializationException {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        ac = (AuthenticationContext) prc.addSubcontext(new AuthenticationContext(), true);

        ImmutableMap.Builder<String,AuthenticationFlowDescriptor> builder = ImmutableMap.builder();
        
        AuthenticationFlowDescriptor flow = new AuthenticationFlowDescriptor();
        flow.setId("authn/MFA");
        flow.setResultSerializer(new DefaultAuthenticationResultSerializer());
        flow.initialize();
        builder.put("authn/MFA", flow);
        ac.setAttemptedFlow(flow);
        
        flow = new AuthenticationFlowDescriptor();
        flow.setId("authn/test1");
        flow.setResultSerializer(new DefaultAuthenticationResultSerializer());
        flow.initialize();
        builder.put("authn/test1", flow);

        flow = new AuthenticationFlowDescriptor();
        flow.setId("authn/test2");
        flow.setResultSerializer(new DefaultAuthenticationResultSerializer());
        flow.initialize();
        builder.put("authn/test2", flow);

        flow = new AuthenticationFlowDescriptor();
        flow.setId("authn/test3");
        flow.setResultSerializer(new DefaultAuthenticationResultSerializer());
        flow.initialize();
        builder.put("authn/test3", flow);

        authenticationFlows = builder.build();
    }

    protected void setUp() throws ComponentInitializationException {        
        initializeMembers();
        
        final PopulateAuthenticationContext action = new PopulateAuthenticationContext();
        action.setAvailableFlows(authenticationFlows.values());
        action.setPotentialFlowsLookupStrategy(FunctionSupport.constant(CollectionSupport.singletonList(authenticationFlows.get("authn/MFA"))));
        action.initialize();

        action.execute(src);
        
        final Map<String,MultiFactorAuthenticationTransition> transitionMap = new HashMap<>();
        MultiFactorAuthenticationTransition transition = new MultiFactorAuthenticationTransition();
        transition.setNextFlow("authn/test1");
        transitionMap.put(null, transition);

        transition = new MultiFactorAuthenticationTransition();
        transition.setNextFlow("interim");
        transitionMap.put("authn/test1", transition);

        transition = new MultiFactorAuthenticationTransition();
        transition.setNextFlow("authn/test2");
        transitionMap.put("interim", transition);

        final PopulateMultiFactorAuthenticationContext mfaaction = new PopulateMultiFactorAuthenticationContext();
        mfaaction.setTransitionMapLookupStrategy(
                FunctionSupport.<ProfileRequestContext,Map<String,MultiFactorAuthenticationTransition>>constant(transitionMap));
        mfaaction.initialize();
        ActionTestingSupport.assertProceedEvent(mfaaction.execute(src));
        
        mfa = ac.getSubcontext(MultiFactorAuthenticationContext.class);
        Assert.assertNotNull(mfa);
    }

}