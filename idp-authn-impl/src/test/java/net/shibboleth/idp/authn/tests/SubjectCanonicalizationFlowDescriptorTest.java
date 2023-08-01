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

package net.shibboleth.idp.authn.tests;

import net.shibboleth.idp.authn.SubjectCanonicalizationFlowDescriptor;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.component.UnmodifiableComponentException;
import net.shibboleth.shared.logic.PredicateSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link SubjectCanonicalizationFlowDescriptor} unit test. */
@SuppressWarnings("javadoc")
public class SubjectCanonicalizationFlowDescriptorTest {

    private SubjectCanonicalizationFlowDescriptor descriptor;

    private RequestContext src;

    private ProfileRequestContext prc;

    @BeforeMethod public void setUp() throws ComponentInitializationException {
        descriptor = new SubjectCanonicalizationFlowDescriptor();
        descriptor.setId("test");
        descriptor.initialize();

        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
    }

    @Test public void testInstantation() throws ComponentInitializationException {
        Assert.assertEquals(descriptor.getId(), "test");
        Assert.assertTrue(descriptor.test(prc));
    }

    @Test public void testSetters() {
        try {
            descriptor.setActivationCondition(PredicateSupport.alwaysFalse());
            Assert.fail();
        } catch (UnmodifiableComponentException e) {
            // OK
        }
    }

    @Test public void testEquality() {
        final SubjectCanonicalizationFlowDescriptor sameId = new SubjectCanonicalizationFlowDescriptor();
        sameId.setId("test");
        Assert.assertTrue(descriptor.equals(sameId));

        final SubjectCanonicalizationFlowDescriptor differentId = new SubjectCanonicalizationFlowDescriptor();
        differentId.setId("differentId");
        Assert.assertFalse(descriptor.equals(differentId));
    }

    @Test public void testPredicate() throws ComponentInitializationException {
        descriptor = new SubjectCanonicalizationFlowDescriptor();
        descriptor.setId("test");
        descriptor.setActivationCondition(PredicateSupport.alwaysFalse());
        descriptor.initialize();

        Assert.assertFalse(descriptor.test(prc));
    }

}
