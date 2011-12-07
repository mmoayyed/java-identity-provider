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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.impl.CheckSpringRequestContext.InvalidSpringRequestContextException;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
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
        final RequestContext springRequestContext = mock(RequestContext.class);

        CheckSpringRequestContext action = new CheckSpringRequestContext();
        action.setId("mock");
        action.initialize();

        try {
            action.execute(springRequestContext);
            Assert.fail();
        } catch (InvalidSpringRequestContextException e) {
            // expected this
        }
    }

    /**
     * Checks that the action fails if the there is not request associated with the context or if it's not an
     * {@link HttpServletRequest}.
     */
    @Test
    public void checkHttpSerlvetRequest() throws Exception {
        final RequestContext springRequestContext = mock(RequestContext.class);
        final ServletExternalContext externalContext = mock(ServletExternalContext.class);
        when(springRequestContext.getExternalContext()).thenReturn(externalContext);

        CheckSpringRequestContext action = new CheckSpringRequestContext();
        action.setId("mock");
        action.initialize();

        try {
            action.execute(springRequestContext);
            Assert.fail();
        } catch (InvalidSpringRequestContextException e) {
            // expected this
        }
    }

    /**
     * Checks that the action fails if the there is not request associated with the context or if it's not an
     * {@link HttpServletResponse}.
     */
    @Test
    public void checkHttpSerlvetResponse() throws Exception {
        final RequestContext springRequestContext = mock(RequestContext.class);
        final ServletExternalContext externalContext = mock(ServletExternalContext.class);
        when(externalContext.getNativeRequest()).thenReturn(new MockHttpServletRequest());
        when(springRequestContext.getExternalContext()).thenReturn(externalContext);

        CheckSpringRequestContext action = new CheckSpringRequestContext();
        action.setId("mock");
        action.initialize();

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
        RequestContext springRequestContext = ActionTestingSupport.buildMockSpringRequestContext(null);

        CheckSpringRequestContext action = new CheckSpringRequestContext();
        action.setId("mock");
        action.initialize();

        Event result = action.execute(springRequestContext);
        ActionTestingSupport.assertProceedEvent(result);
    }
}