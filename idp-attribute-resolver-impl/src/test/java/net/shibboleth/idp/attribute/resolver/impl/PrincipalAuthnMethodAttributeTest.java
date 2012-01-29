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

package net.shibboleth.idp.attribute.resolver.impl;

import java.util.Collection;

import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

/** test for {@link net.shibboleth.idp.attribute.resolver.impl.SimpleAttribute}. */
public class PrincipalAuthnMethodAttributeTest {

    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "simple";

    /**
     * Test when dependent on a data connector.
     * 
     * @throws ComponentInitializationException if initialization fails (which it shouldn't).
     * @throws InterruptedException never.
     */
    @Test public void testSinglePrincipalAttributeNameDefinition() throws ComponentInitializationException,
            InterruptedException {

        final BaseAttributeDefinition attrDefn = new PrincipalAuthenticationMethodAttributeDefinition();
        attrDefn.setId(TEST_ATTRIBUTE_NAME);
        attrDefn.initialize();

        final AttributeResolver resolver = new AttributeResolver();
        resolver.setId("foo");
        resolver.setAttributeDefinition(Lists.newArrayList(attrDefn));
        resolver.initialize();

        AttributeResolutionContext context = new AttributeResolutionContext();

        try {
            resolver.resolveAttributes(context);
        } catch (AttributeResolutionException e) {
            Assert.fail("resolution failed", e);
        }

        Collection values = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME).getValues();
        Assert.assertEquals(values.size(), 1);
        Assert.assertTrue(values.contains(TestSources.TEST_AUTHN_METHOD), "looking for "
                + TestSources.TEST_AUTHN_METHOD);

    }

}
