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

package net.shibboleth.idp.profile.impl;

import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.profile.impl.CheckSpringRequestContext.InvalidSpringRequestContextException;

import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.test.MockExternalContext;
import org.springframework.webflow.test.MockRequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link CheckSpringRequestContext} */
public class CheckSpringRequestContextTest {

    /**
     * Checks that the action fails if the {@link RequestContext#getExternalContext()} is not a
     * {@link ServletExternalContext}.
     */
    @Test
    public void checkServletExternalContext() throws Exception {
        final MockRequestContext springRequestContext = new MockRequestContext();

        CheckSpringRequestContext action = new CheckSpringRequestContext();
        action.setId("mock");
        action.initialize();

        try {
            action.execute(springRequestContext);
            Assert.fail();
        } catch (InvalidSpringRequestContextException e) {
            // expected this
        }

        springRequestContext.setExternalContext(new MockExternalContext());

        try {
            action.execute(springRequestContext);
            Assert.fail();
        } catch (InvalidSpringRequestContextException e) {
            // expected this
        }
    }

    /** Checks that a properly set up {@link RequestContext} passes. */
    @Test
    public void checkProperRequestContext() throws Exception {
        RequestContext springRequestContext = new RequestContextBuilder().buildRequestContext();

        CheckSpringRequestContext action = new CheckSpringRequestContext();
        action.setId("mock");
        action.initialize();

        Event result = action.execute(springRequestContext);
        ActionTestingSupport.assertProceedEvent(result);
    }
}