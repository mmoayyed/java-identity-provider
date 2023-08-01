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

package net.shibboleth.idp.test.flows.c14n;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.annotations.Test;

import net.shibboleth.idp.test.flows.AbstractFlowTest;

/**
 * Tests for C14N.
 */
@ContextConfiguration({"classpath:/c14n/test-webflow-config.xml", "classpath:/c14n/locate-resolver.xml"})
@SuppressWarnings("javadoc")
public class TestC14N extends AbstractFlowTest {

    @Test public void testTransientNameID() {

        final FlowExecutionResult result = flowExecutor.launchExecution("transientNameID", null, externalContext);

        assertFlowExecutionResult(result, "transientNameID");
        assertFlowExecutionOutcome(result.getOutcome());
    }

    @Test public void testCryptoTransientNameID() {

        final FlowExecutionResult result = flowExecutor.launchExecution("cryptoTransientNameID", null, externalContext);

        assertFlowExecutionResult(result, "cryptoTransientNameID");
        assertFlowExecutionOutcome(result.getOutcome());
    }

    @Test public void testTransformNameID() {

        final FlowExecutionResult result = flowExecutor.launchExecution("transformNameID", null, externalContext);

        assertFlowExecutionResult(result, "transformNameID");
        assertFlowExecutionOutcome(result.getOutcome());
    }

    @Test public void testTransientNameIdentifier() {

        final FlowExecutionResult result =
                flowExecutor.launchExecution("transientNameIdentifier", null, externalContext);

        assertFlowExecutionResult(result, "transientNameIdentifier");
        assertFlowExecutionOutcome(result.getOutcome());
    }

    @Test public void testCryptoTransientNameIdentifier() {

        final FlowExecutionResult result =
                flowExecutor.launchExecution("cryptoTransientNameIdentifier", null, externalContext);

        assertFlowExecutionResult(result, "cryptoTransientNameIdentifier");
        assertFlowExecutionOutcome(result.getOutcome());
    }

    @Test public void testTransformNameIdentifier() {

        final FlowExecutionResult result = flowExecutor.launchExecution("transformNameIdentifier", null, externalContext);

        assertFlowExecutionResult(result, "transformNameIdentifier");
        assertFlowExecutionOutcome(result.getOutcome());
    }
    
}