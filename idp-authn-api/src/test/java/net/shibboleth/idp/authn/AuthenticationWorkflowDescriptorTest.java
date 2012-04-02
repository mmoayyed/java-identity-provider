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

package net.shibboleth.idp.authn;

import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link AuthenticationWorkflowDescriptor} unit test. */
public class AuthenticationWorkflowDescriptorTest {

    /** Tests that everything is properly initialized during object construction. */
    @Test public void testInstantation() {
        AuthenticationWorkflowDescriptor descriptor = new AuthenticationWorkflowDescriptor("test");
        Assert.assertEquals(descriptor.getWorkflowId(), "test");
        Assert.assertFalse(descriptor.isForcedAuthenticationSupported());
        Assert.assertFalse(descriptor.isPassiveAuthenticationSupported());

        try {
            new AuthenticationWorkflowDescriptor(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {

        }

        try {
            new AuthenticationWorkflowDescriptor("");
            Assert.fail();
        } catch (ConstraintViolationException e) {

        }

        try {
            new AuthenticationWorkflowDescriptor("  ");
            Assert.fail();
        } catch (ConstraintViolationException e) {

        }
    }

    /** Tests mutating inactivity timeout. */
    @Test public void testInactivityTimeout() {
        AuthenticationWorkflowDescriptor descriptor = new AuthenticationWorkflowDescriptor("test");

        descriptor.setInactivityTimeout(10);
        Assert.assertEquals(descriptor.getInactivityTimeout(), 10);

        try {
            descriptor.setInactivityTimeout(-10);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            Assert.assertEquals(descriptor.getInactivityTimeout(), 10);
        }
    }

    /** Tests mutating lifetime. */
    @Test public void testLifetime() {
        AuthenticationWorkflowDescriptor descriptor = new AuthenticationWorkflowDescriptor("test");

        descriptor.setLifetime(10);
        Assert.assertEquals(descriptor.getLifetime(), 10);

        try {
            descriptor.setLifetime(-10);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            Assert.assertEquals(descriptor.getLifetime(), 10);
        }
    }

    /** Tests mutating forced authentication support. */
    @Test public void testSupportedForcedAuthentication() {
        AuthenticationWorkflowDescriptor descriptor = new AuthenticationWorkflowDescriptor("test");

        descriptor.setForcedAuthenticationSupported(true);
        Assert.assertTrue(descriptor.isForcedAuthenticationSupported());

        descriptor.setForcedAuthenticationSupported(false);
        Assert.assertFalse(descriptor.isForcedAuthenticationSupported());
    }

    /** Tests mutating passive authentication support. */
    @Test public void testSupportedPassiveAuthentication() {
        AuthenticationWorkflowDescriptor descriptor = new AuthenticationWorkflowDescriptor("test");

        descriptor.setPassiveAuthenticationSupported(true);
        Assert.assertTrue(descriptor.isPassiveAuthenticationSupported());

        descriptor.setPassiveAuthenticationSupported(false);
        Assert.assertFalse(descriptor.isPassiveAuthenticationSupported());
    }
}