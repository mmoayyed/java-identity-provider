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

package net.shibboleth.idp.test.flows.interceptor;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.context.SpringRequestContext;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.interceptor.ProfileInterceptorFlowDescriptor;
import net.shibboleth.idp.test.flows.AbstractFlowTest;

import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Predicate;

/** Tests for the profile interceptor flow. */
@ContextConfiguration(locations = {"classpath:/intercept/test-webflow-config.xml",})
public class InterceptFlowTest extends AbstractFlowTest {

    /** Flow id. */
    @Nonnull public final static String TEST_PROFILE_FLOW_ID = "test-intercept-flow";

    /** Bean ID of user configured intercept flows. */
    @Nonnull public final static String INTERCEPT_FLOWS_BEAN_ID = "shibboleth.AvailableInterceptFlows";

    @Nonnull public final static String TEST_FLOW_REGISTRY_ID = "testFlowRegistry";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InterceptFlowTest.class);

    /**
     * Clear the list of user configured flows defined in bean with ID {@link #INTERCEPT_FLOWS_BEAN_ID}.
     */
    @BeforeMethod public void clearUserConfiguredInterceptFlows() {
        final Flow flow = getFlow(TEST_PROFILE_FLOW_ID);
        final List interceptFlows = flow.getApplicationContext().getBean(INTERCEPT_FLOWS_BEAN_ID, List.class);
        interceptFlows.clear();
    }

    /**
     * Register test flows in parent registry so they can be called by the 'intercept' flow in the parent registry.
     */
    @BeforeMethod public void registerFlowsInParentRegistry() {
        registerFlowsInParentRegistry("intercept/test-error-flow", TEST_FLOW_REGISTRY_ID);
        registerFlowsInParentRegistry("intercept/test-proceed-1-flow", TEST_FLOW_REGISTRY_ID);
        registerFlowsInParentRegistry("intercept/test-proceed-2-flow", TEST_FLOW_REGISTRY_ID);
    }

    @Test public void testNoAvailableFlows() {

        final FlowExecutionResult result = flowExecutor.launchExecution(TEST_PROFILE_FLOW_ID, null, externalContext);

        assertFlowExecutionResult(result, TEST_PROFILE_FLOW_ID);
        assertFlowExecutionOutcome(result.getOutcome());

        final ProfileRequestContext prc = retrieveProfileRequestContext(result);
        Assert.assertNotNull(prc);

        final ProfileInterceptorContext interceptorCtx = prc.getSubcontext(ProfileInterceptorContext.class);
        Assert.assertTrue(interceptorCtx.getAvailableFlows().isEmpty());
        Assert.assertEquals(result.getOutcome().getOutput().get("testProceed1"), false);
        Assert.assertEquals(result.getOutcome().getOutput().get("testProceed2"), false);
    }

    @Test public void testOneAvailableFlow() {

        final ProfileInterceptorFlowDescriptor flowDescriptor = new ProfileInterceptorFlowDescriptor();
        flowDescriptor.setId("intercept/test-proceed-1-flow");

        final Flow flow = getFlow(TEST_PROFILE_FLOW_ID);

        final List interceptFlows = flow.getApplicationContext().getBean(INTERCEPT_FLOWS_BEAN_ID, List.class);
        interceptFlows.add(flowDescriptor);

        final FlowExecutionResult result = flowExecutor.launchExecution(TEST_PROFILE_FLOW_ID, null, externalContext);

        assertFlowExecutionResult(result, TEST_PROFILE_FLOW_ID);
        assertFlowExecutionOutcome(result.getOutcome());

        final ProfileRequestContext prc = retrieveProfileRequestContext(result);
        Assert.assertNotNull(prc);

        final ProfileInterceptorContext interceptorCtx = prc.getSubcontext(ProfileInterceptorContext.class);
        Assert.assertEquals(interceptorCtx.getAvailableFlows().size(), 0);
        Assert.assertEquals(result.getOutcome().getOutput().get("testProceed1"), true);
        Assert.assertEquals(result.getOutcome().getOutput().get("testProceed2"), false);
    }

    @Test public void testTwoAvailableFlows() {

        final ProfileInterceptorFlowDescriptor flowDescriptor1 = new ProfileInterceptorFlowDescriptor();
        flowDescriptor1.setId("intercept/test-proceed-1-flow");

        final ProfileInterceptorFlowDescriptor flowDescriptor2 = new ProfileInterceptorFlowDescriptor();
        flowDescriptor2.setId("intercept/test-proceed-2-flow");

        final Flow flow = getFlow(TEST_PROFILE_FLOW_ID);

        final List interceptFlows = flow.getApplicationContext().getBean(INTERCEPT_FLOWS_BEAN_ID, List.class);
        interceptFlows.add(flowDescriptor1);
        interceptFlows.add(flowDescriptor2);

        final FlowExecutionResult result = flowExecutor.launchExecution(TEST_PROFILE_FLOW_ID, null, externalContext);

        assertFlowExecutionResult(result, TEST_PROFILE_FLOW_ID);
        assertFlowExecutionOutcome(result.getOutcome());

        final ProfileRequestContext prc = retrieveProfileRequestContext(result);
        Assert.assertNotNull(prc);

        final ProfileInterceptorContext interceptorCtx = prc.getSubcontext(ProfileInterceptorContext.class);
        Assert.assertEquals(interceptorCtx.getAvailableFlows().size(), 0);
        Assert.assertEquals(result.getOutcome().getOutput().get("testProceed1"), true);
        Assert.assertEquals(result.getOutcome().getOutput().get("testProceed2"), true);
    }

    @Test public void testErrorFlow() {

        final ProfileInterceptorFlowDescriptor flowDescriptor1 = new ProfileInterceptorFlowDescriptor();
        flowDescriptor1.setId("intercept/test-error-flow");

        final Flow flow = getFlow(TEST_PROFILE_FLOW_ID);

        final List interceptFlows = flow.getApplicationContext().getBean(INTERCEPT_FLOWS_BEAN_ID, List.class);
        interceptFlows.add(flowDescriptor1);

        final FlowExecutionResult result = flowExecutor.launchExecution(TEST_PROFILE_FLOW_ID, null, externalContext);

        assertFlowExecutionResult(result, TEST_PROFILE_FLOW_ID);
        assertFlowExecutionOutcome(result.getOutcome(), EventIds.INVALID_PROFILE_CTX);

        final ProfileRequestContext prc = retrieveProfileRequestContext(result);
        Assert.assertNotNull(prc);

        final ProfileInterceptorContext interceptorCtx = prc.getSubcontext(ProfileInterceptorContext.class);
        Assert.assertEquals(interceptorCtx.getAvailableFlows().size(), 1);
        Assert.assertEquals(result.getOutcome().getOutput().get("testProceed1"), false);
        Assert.assertEquals(result.getOutcome().getOutput().get("testProceed2"), false);
    }

    @Test public void testProceedThenErrorFlow() {

        final ProfileInterceptorFlowDescriptor flowDescriptor1 = new ProfileInterceptorFlowDescriptor();
        flowDescriptor1.setId("intercept/test-proceed-1-flow");

        final ProfileInterceptorFlowDescriptor flowDescriptor2 = new ProfileInterceptorFlowDescriptor();
        flowDescriptor2.setId("intercept/test-error-flow");

        final Flow flow = getFlow(TEST_PROFILE_FLOW_ID);

        final List interceptFlows = flow.getApplicationContext().getBean(INTERCEPT_FLOWS_BEAN_ID, List.class);
        interceptFlows.add(flowDescriptor1);
        interceptFlows.add(flowDescriptor2);

        final FlowExecutionResult result = flowExecutor.launchExecution(TEST_PROFILE_FLOW_ID, null, externalContext);

        assertFlowExecutionResult(result, TEST_PROFILE_FLOW_ID);
        assertFlowExecutionOutcome(result.getOutcome(), EventIds.INVALID_PROFILE_CTX);

        final ProfileRequestContext prc = retrieveProfileRequestContext(result);
        Assert.assertNotNull(prc);

        final ProfileInterceptorContext interceptorCtx = prc.getSubcontext(ProfileInterceptorContext.class);
        Assert.assertEquals(interceptorCtx.getAvailableFlows().size(), 1);
        Assert.assertEquals(result.getOutcome().getOutput().get("testProceed1"), true);
        Assert.assertEquals(result.getOutcome().getOutput().get("testProceed2"), false);
    }

    @Test public void testAttemptedFlow() {

        final ProfileInterceptorFlowDescriptor flowDescriptor1 = new ProfileInterceptorFlowDescriptor();
        flowDescriptor1.setId("intercept/test-proceed-1-flow");
        flowDescriptor1.setActivationCondition(new ConversationScopeAttributeCondition("testProceed2"));

        final ProfileInterceptorFlowDescriptor flowDescriptor2 = new ProfileInterceptorFlowDescriptor();
        flowDescriptor2.setId("intercept/test-proceed-2-flow");

        final Flow flow = getFlow(TEST_PROFILE_FLOW_ID);

        final List interceptFlows = flow.getApplicationContext().getBean(INTERCEPT_FLOWS_BEAN_ID, List.class);
        interceptFlows.add(flowDescriptor1);
        interceptFlows.add(flowDescriptor2);

        final FlowExecutionResult result = flowExecutor.launchExecution(TEST_PROFILE_FLOW_ID, null, externalContext);

        assertFlowExecutionResult(result, TEST_PROFILE_FLOW_ID);
        assertFlowExecutionOutcome(result.getOutcome());

        final ProfileRequestContext prc = retrieveProfileRequestContext(result);
        Assert.assertNotNull(prc);

        final ProfileInterceptorContext interceptorCtx = prc.getSubcontext(ProfileInterceptorContext.class);
        Assert.assertEquals(interceptorCtx.getAvailableFlows().size(), 0);
        Assert.assertEquals(result.getOutcome().getOutput().get("testProceed1"), true);
        Assert.assertEquals(result.getOutcome().getOutput().get("testProceed2"), true);
    }

    /**
     * Return true if a boolean attribute exists in the Web Flow conversation scope.
     */
    public class ConversationScopeAttributeCondition implements Predicate<ProfileRequestContext> {

        /** Attribute name. */
        @Nullable final String name;

        public ConversationScopeAttributeCondition(@Nullable final String attributeName) {
            name = attributeName;
        }

        /** {@inheritDoc} */
        public boolean apply(ProfileRequestContext input) {
            final SpringRequestContext springSubcontext = input.getSubcontext(SpringRequestContext.class, false);
            if (springSubcontext != null) {
                final Object object = springSubcontext.getRequestContext().getConversationScope().get(name);
                if (object != null && object instanceof Boolean) {
                    return (Boolean) object;
                }
            }
            return false;
        }
    }
}
