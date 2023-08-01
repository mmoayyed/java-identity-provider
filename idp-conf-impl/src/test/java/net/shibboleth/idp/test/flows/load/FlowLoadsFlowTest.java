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

package net.shibboleth.idp.test.flows.load;

import javax.annotation.Nonnull;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.idp.test.flows.AbstractFlowTest;

/**
 * Ensure a flow loads correctly. Uses the custom IdP 
 * net.shibboleth.idp.profile.spring.factory.FlowDefinitionRegistryFactoryBean.
 */
@ContextConfiguration(locations = {"classpath:/flow-load-test/test-webflow-config.xml",})
public class FlowLoadsFlowTest extends AbstractFlowTest {
    
    /** Test flow ID. */
    @Nonnull public final static String TEST_FLOW_LOADS_ID = "test-flow-loads";
    
    /**
     * Ensure the flow beans configuration can import another resource using the wildcard classpath
     * prefix. See IDP-1833. 
     * <p>Unlike many of the other idp-conf tests, this loads the custom IdP 
     * net.shibboleth.idp.profile.spring.factory.FlowDefinitionRegistryFactoryBean, see 
     * flow-load-test/test-webflow-config.xml.</p>
     */
    @Test
    public void testFlowLoads_IDP1833() {
        
        final FlowExecutionResult result = flowExecutor.launchExecution(TEST_FLOW_LOADS_ID, null, externalContext);
        
        final Flow loadsFlow = getFlow(TEST_FLOW_LOADS_ID);     
        final LoadThisBean loadedBean = loadsFlow.getApplicationContext().getBean(LoadThisBean.class);
        
        Assert.assertNotNull(loadedBean);
        Assert.assertEquals(loadedBean.getMessage(), "loaded");
        assertFlowExecutionResult(result, TEST_FLOW_LOADS_ID);
        Assert.assertEquals(result.getOutcome().getId(), "end");
        
    }

}
