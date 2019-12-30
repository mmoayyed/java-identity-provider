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

package net.shibboleth.idp.attribute.resolver;

import java.util.Collections;
import java.util.HashSet;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.opensaml.messaging.context.navigate.ParentContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;

/** Unit test for {@link ResolverPlugin}. */
public class AbstractResolverPluginTest {

    /** Test an instantiated object has the proper state. */
    @Test public void instantiation() {
        MockBaseAttributeResolver plugin = new MockBaseAttributeResolver(" foo ", "bar");

        Assert.assertEquals(plugin.getId(), "foo");
        Assert.assertTrue(plugin.isPropagateResolutionExceptions());
        Assert.assertNull(plugin.getActivationCondition());
        Assert.assertNotNull(plugin.getDataConnectorDependencies());
        Assert.assertTrue(plugin.getDataConnectorDependencies().isEmpty());
        Assert.assertNotNull(plugin.getAttributeDependencies());
        Assert.assertTrue(plugin.getAttributeDependencies().isEmpty());
    }

    /** Test get/set activation criteria. */
    @Test public void activationCriteria() {
        MockBaseAttributeResolver plugin = new MockBaseAttributeResolver(" foo ", "bar");

        plugin.setActivationCondition(Predicates.<ProfileRequestContext> alwaysFalse());
        Assert.assertEquals(plugin.getActivationCondition(), Predicates.alwaysFalse());
        try {
            plugin.setActivationCondition(null);
            Assert.fail("Able to set a null activiation criteria");
        } catch (ConstraintViolationException e) {
            // expected this
        }
    }
    
    @Test public void nativation() {
        MockBaseAttributeResolver plugin = new MockBaseAttributeResolver(" foo ", "bar");
        Assert.assertEquals(plugin.getProfileContextStrategy().getClass(), ParentContextLookup.class);
        plugin.setProfileContextStrategy(new TestFunc());
        Assert.assertEquals(plugin.getProfileContextStrategy().getClass(), TestFunc.class);
    }

    /** Test setters to {@link AbstractResolverPlugin#setPropagateResolutionExceptions(boolean)}. */
    @Test public void propogateSetters() {
        MockBaseAttributeResolver plugin = new MockBaseAttributeResolver("foo", "bar");

        plugin.setPropagateResolutionExceptions(true);
        Assert.assertTrue(plugin.isPropagateResolutionExceptions());

        plugin.setPropagateResolutionExceptions(true);
        Assert.assertTrue(plugin.isPropagateResolutionExceptions());

        plugin.setPropagateResolutionExceptions(false);
        Assert.assertFalse(plugin.isPropagateResolutionExceptions());
    }

    /** Test add, removing, setting dependencies. */
    @Test public void dependencies() {
        MockBaseAttributeResolver plugin = new MockBaseAttributeResolver("foo", "bar");

        Assert.assertNotNull(plugin.getAttributeDependencies());
        Assert.assertTrue(plugin.getAttributeDependencies().isEmpty());
        Assert.assertNotNull(plugin.getDataConnectorDependencies());
        Assert.assertTrue(plugin.getDataConnectorDependencies().isEmpty());

        plugin.setAttributeDependencies(new HashSet<ResolverAttributeDefinitionDependency>());
        plugin.setDataConnectorDependencies(new HashSet<ResolverDataConnectorDependency>());
        Assert.assertNotNull(plugin.getAttributeDependencies());
        Assert.assertTrue(plugin.getAttributeDependencies().isEmpty());
        Assert.assertNotNull(plugin.getDataConnectorDependencies());
        Assert.assertTrue(plugin.getDataConnectorDependencies().isEmpty());

        final ResolverAttributeDefinitionDependency dep1 = new ResolverAttributeDefinitionDependency("foo");
        final ResolverAttributeDefinitionDependency dep2 = new ResolverAttributeDefinitionDependency("foo2");

        final ResolverDataConnectorDependency depd1 = new ResolverDataConnectorDependency("food");
        final ResolverDataConnectorDependency depd2 = new ResolverDataConnectorDependency("food2");

        final HashSet<ResolverAttributeDefinitionDependency> adeps = new HashSet<>();
        final HashSet<ResolverDataConnectorDependency> ddeps = new HashSet<>();
        

        adeps.add(dep1);
        adeps.add(dep1);
        adeps.add(dep2);

        plugin.setAttributeDependencies(adeps);
        Assert.assertNotNull(plugin.getAttributeDependencies());
        Assert.assertEquals(plugin.getAttributeDependencies().size(), 2);

        ddeps.add(depd1);
        Assert.assertEquals(ddeps.size(),1);
        ddeps.add(depd1);
        Assert.assertEquals(ddeps.size(),1);
        ddeps.add(depd2);
        Assert.assertEquals(ddeps.size(),2);


        plugin.setDataConnectorDependencies(ddeps);        
        Assert.assertNotNull(plugin.getDataConnectorDependencies());
        Assert.assertEquals(plugin.getDataConnectorDependencies().size(), 2);

        try {
            plugin.getAttributeDependencies().add(dep1);
            Assert.fail("able to add entry to supossedly unmodifiable collection");
        } catch (UnsupportedOperationException e) {
            // expected this
        }
        try {
            plugin.getDataConnectorDependencies().add(depd1);
            Assert.fail("able to add entry to supossedly unmodifiable collection");
        } catch (UnsupportedOperationException e) {
            // expected this
        }
    }

    /**
     * Test {@link ResolverPlugin#resolve(AttributeResolutionContext)}.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void resolver() throws Exception {
        AttributeResolutionContext context = new AttributeResolutionContext();
        context.getSubcontext(AttributeResolverWorkContext.class, true);
        MockBaseAttributeResolver plugin = new MockBaseAttributeResolver("foo", "bar");
        plugin.initialize();

        Assert.assertEquals(plugin.resolve(context).getId(), "foo");

        context = new AttributeResolutionContext();
        plugin = new MockBaseAttributeResolver(" foo ", "bar");
        plugin.setActivationCondition(Predicates.<ProfileRequestContext> alwaysFalse());

        plugin.initialize();
        Assert.assertNull(plugin.resolve(context));

    }

    /**
     * This class implements the minimal level of functionality and is meant only as a means of testing the abstract
     * {@link ResolverPlugin}.
     */
    private static final class MockBaseAttributeResolver extends AbstractAttributeDefinition  {

        /** Static value return by resolution. */
        private final IdPAttribute resolverValue;

        /**
         * Constructor.
         * 
         * @param id id of this plugin
         * @param value value returned by resolution
         */
        public MockBaseAttributeResolver(String id, String value) {
            resolverValue = new IdPAttribute(id);
            setId(id);
            resolverValue.setValues(Collections.singletonList(new StringAttributeValue(value)));
        }

        /** {@inheritDoc} */
        @Override @Nullable protected IdPAttribute doResolve(@Nonnull final AttributeResolutionContext resolutionContext,
                @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException {
            return resolverValue;
        }

        /** {@inheritDoc} */
        protected IdPAttribute doAttributeDefinitionResolve(AttributeResolutionContext resolutionContext,
                AttributeResolverWorkContext workContext) throws ResolutionException {
            return resolverValue;
        }
    }

    static class TestFunc implements Function<AttributeResolutionContext, ProfileRequestContext> {

        /** {@inheritDoc} */
        @Nullable public ProfileRequestContext apply(@Nullable AttributeResolutionContext input) {
            return null;
        }

    }
}