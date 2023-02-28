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

package net.shibboleth.idp.consent.flow.ar.impl;

import java.util.Collection;
import java.util.function.Function;

import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.consent.logic.impl.AttributeValuesHashFunction;
import net.shibboleth.shared.component.UnmodifiableComponentException;
import net.shibboleth.shared.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link AttributeReleaseFlowDescriptor} unit test. */
@SuppressWarnings("javadoc")
public class AttributeReleaseFlowDescriptorTest {

    private AttributeReleaseFlowDescriptor descriptor;

    private Object nullObj;
    
    @BeforeMethod public void setUp() {
        descriptor = new AttributeReleaseFlowDescriptor();
        descriptor.setId("test");
    }

    @Test public void testInstantation() {
        Assert.assertEquals(descriptor.getId(), "test");
        Assert.assertFalse(descriptor.isDoNotRememberConsentAllowed());
        Assert.assertFalse(descriptor.isGlobalConsentAllowed());
        Assert.assertFalse(descriptor.isPerAttributeConsentEnabled());
        Assert.assertNotNull(descriptor.getAttributeValuesHashFunction());
    }

    @SuppressWarnings({ "null", "unchecked" })
    @Test(expectedExceptions = ConstraintViolationException.class) public void testNullAttributeValuesHashFunction() {
        descriptor.setAttributeValuesHashFunction((Function<Collection<IdPAttributeValue>, String>) nullObj);
    }

    @Test(expectedExceptions = UnmodifiableComponentException.class) public void
            testSettingAttributeValuesHashFunctionAfterInitialization() throws Exception {
        descriptor.initialize();
        descriptor.setAttributeValuesHashFunction(new AttributeValuesHashFunction());
    }

    @Test public void testSettingAttributeValuesHashFunction() throws Exception {
        final Function<Collection<IdPAttributeValue>, String> function = new AttributeValuesHashFunction();
        descriptor.setAttributeValuesHashFunction(function);
        Assert.assertEquals(descriptor.getAttributeValuesHashFunction(), function);
    }

    @Test public void testMutatingDoNotRememberConsent() throws Exception {
        descriptor.setDoNotRememberConsentAllowed(true);
        Assert.assertTrue(descriptor.isDoNotRememberConsentAllowed());

        descriptor.setDoNotRememberConsentAllowed(false);
        Assert.assertFalse(descriptor.isDoNotRememberConsentAllowed());
    }

    @Test(expectedExceptions = UnmodifiableComponentException.class) public void
            testSettingDoNotRememberConsentAfterInitialization() throws Exception {
        descriptor.initialize();
        descriptor.setDoNotRememberConsentAllowed(true);
    }

    @Test public void testMutatingGlobalConsent() {
        descriptor.setGlobalConsentAllowed(true);
        Assert.assertTrue(descriptor.isGlobalConsentAllowed());

        descriptor.setGlobalConsentAllowed(false);
        Assert.assertFalse(descriptor.isGlobalConsentAllowed());
    }

    @Test(expectedExceptions = UnmodifiableComponentException.class) public void
            testSettingGlobalConsentAfterInitialization() throws Exception {
        descriptor.initialize();
        descriptor.setGlobalConsentAllowed(true);
    }

    @Test public void testMutatingPerAttributeConsent() {
        descriptor.setPerAttributeConsentEnabled(true);
        Assert.assertTrue(descriptor.isPerAttributeConsentEnabled());

        descriptor.setPerAttributeConsentEnabled(false);
        Assert.assertFalse(descriptor.isPerAttributeConsentEnabled());
    }

    @Test(expectedExceptions = UnmodifiableComponentException.class) public void
            testSettingPerAttributeConsentAfterInitialization() throws Exception {
        descriptor.initialize();
        descriptor.setPerAttributeConsentEnabled(true);
    }

}
