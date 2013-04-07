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

package net.shibboleth.idp.attribute.resolver.impl.ad;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.idp.attribute.resolver.impl.TestSources;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Optional;

/**
 * Test for {@link SAML1NameIdentifierAttributeDefinition}.
 */
public class SAML1NameIdentifierAttributeDefinitionTest extends OpenSAMLInitBaseTestCase {

    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "simple";

    private static final String SECOND_ATTRIBUTE_NAME = "second";

    private static final String IDP_ENTITY_ID = "https://idp.example.org/idp";

    private static final String ALTERNATIVE_FORMAT = "ALTERNATE_FORMAT";

    private static final String ALTERNATE_QUALIFIER = "ALTERNATE_QUALIFIER";

    @Test public void testEmpty() throws ResolutionException, ComponentInitializationException {
        final SAML1NameIdentifierAttributeDefinition defn = new SAML1NameIdentifierAttributeDefinition();
        defn.setId(TEST_ATTRIBUTE_NAME);
        defn.setDependencies(Collections.singleton(TestSources.makeResolverPluginDependency("foo", "bar")));
        defn.initialize();

        final Optional<Attribute> result =
                defn.doAttributeDefinitionResolve(TestSources.createResolutionContext(null, TestSources.IDP_ENTITY_ID,
                        null));

        Assert.assertTrue(result.get().getValues().isEmpty());
    }

    @Test public void testSimple() throws ResolutionException, ComponentInitializationException {
        final SAML1NameIdentifierAttributeDefinition defn = new SAML1NameIdentifierAttributeDefinition();
        defn.setId(TEST_ATTRIBUTE_NAME);
        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(TestSources.makeResolverPluginDependency(TestSources.STATIC_ATTRIBUTE_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        defn.setDependencies(dependencySet);
        defn.initialize();

        // And resolve
        final Set<BaseAttributeDefinition> am = new LazySet<BaseAttributeDefinition>();
        am.add(defn);
        am.add(TestSources.populatedStaticAttribute());

        final AttributeResolver resolver = new AttributeResolver("foo", am, null);
        resolver.initialize();

        AttributeResolutionContext context = TestSources.createResolutionContext(null, TestSources.IDP_ENTITY_ID, null);
        try {
            resolver.resolveAttributes(context);
        } catch (ResolutionException e) {
            Assert.fail("resolution failed", e);
        }
        final Collection<AttributeValue> values = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        Assert.assertEquals(values.size(), 2);
        Collection<String> nameValues = new HashSet<String>(2);
        for (AttributeValue val : values) {
            NameIdentifier id = (NameIdentifier) val.getValue();
            Assert.assertNull(id.getFormat());
            Assert.assertEquals(id.getNameQualifier(), IDP_ENTITY_ID);
            nameValues.add(id.getNameIdentifier());
        }
        Assert.assertTrue(nameValues.contains(TestSources.COMMON_ATTRIBUTE_VALUE_STRING));
        Assert.assertTrue(nameValues.contains(TestSources.ATTRIBUTE_ATTRIBUTE_VALUE_STRING));
    }

    @Test public void testNulls() throws ComponentInitializationException {
        final SAML1NameIdentifierAttributeDefinition defn = new SAML1NameIdentifierAttributeDefinition();
        defn.setId(TEST_ATTRIBUTE_NAME);
        defn.setDependencies(Collections.singleton(TestSources.makeResolverPluginDependency("foo", "bar")));

        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(TestSources.makeResolverPluginDependency(TestSources.STATIC_ATTRIBUTE_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        defn.setDependencies(dependencySet);
        defn.initialize();
        final Set<BaseAttributeDefinition> am = new LazySet<BaseAttributeDefinition>();
        am.add(defn);
        am.add(TestSources.populatedStaticAttribute());
        final AttributeResolver resolver = new AttributeResolver("foo", am, null);
        resolver.initialize();

        AttributeResolutionContext context = new AttributeResolutionContext();
        try {
            resolver.resolveAttributes(context);
            Assert.fail("resolution Should have failed");
        } catch (ResolutionException e) {
            // OK
        }
    }

    @Test public void testBadValue() throws ResolutionException, ComponentInitializationException {
        final BaseAttributeDefinition defn = TestSources.nonStringAttributeDefiniton(TEST_ATTRIBUTE_NAME);

        final SAML1NameIdentifierAttributeDefinition defn2 = new SAML1NameIdentifierAttributeDefinition();
        defn2.setId(SECOND_ATTRIBUTE_NAME);
        defn2.setDependencies(Collections.singleton(TestSources.makeResolverPluginDependency("foo", "bar")));

        // Set the dependency on the data connector
        Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(new ResolverPluginDependency(TEST_ATTRIBUTE_NAME));
        defn2.setDependencies(dependencySet);

        // And resolve
        Set<BaseAttributeDefinition> am = new LazySet<BaseAttributeDefinition>();
        am.add(defn);
        am.add(TestSources.populatedStaticAttribute());
        am.add(defn2);

        AttributeResolver resolver = new AttributeResolver("foo", am, null);
        resolver.initialize();

        //
        // Attribute recipient needed because the nonStringAttributeDefinition needs it
        //
        AttributeResolutionContext context =
                TestSources.createResolutionContext(null, TestSources.IDP_ENTITY_ID, TestSources.SP_ENTITY_ID);
        resolver.resolveAttributes(context);
        Assert.assertNull(context.getResolvedAttributes().get(SECOND_ATTRIBUTE_NAME));
    }

    @Test public void testSingleValueWithOptions() throws ResolutionException, ComponentInitializationException {
        final SAML1NameIdentifierAttributeDefinition defn = new SAML1NameIdentifierAttributeDefinition();
        defn.setId(TEST_ATTRIBUTE_NAME);
        defn.setDependencies(Collections.singleton(TestSources.makeResolverPluginDependency("foo", "bar")));

        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(TestSources.makeResolverPluginDependency(TestSources.STATIC_ATTRIBUTE_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        defn.setDependencies(dependencySet);
        defn.setNameIdFormat(ALTERNATIVE_FORMAT);
        defn.setNameIdQualifier(ALTERNATE_QUALIFIER);
        defn.initialize();

        // And resolve
        final Set<BaseAttributeDefinition> am = new LazySet<BaseAttributeDefinition>();
        am.add(defn);
        am.add(TestSources.populatedStaticAttribute(TestSources.STATIC_ATTRIBUTE_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR, 1));

        final AttributeResolver resolver = new AttributeResolver("foo", am, null);
        resolver.initialize();

        AttributeResolutionContext context = TestSources.createResolutionContext(null, TestSources.IDP_ENTITY_ID, null);
        try {
            resolver.resolveAttributes(context);
        } catch (ResolutionException e) {
            Assert.fail("resolution failed", e);
        }
        final Collection<AttributeValue> values = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        Assert.assertEquals(values.size(), 1);
        NameIdentifier id = (NameIdentifier) values.iterator().next().getValue();
        Assert.assertEquals(id.getFormat(), ALTERNATIVE_FORMAT);
        Assert.assertEquals(defn.getNameIdFormat(), id.getFormat());
        Assert.assertEquals(id.getNameQualifier(), ALTERNATE_QUALIFIER);
        Assert.assertEquals(defn.getNameIdQualifier(), id.getNameQualifier());
        Assert.assertEquals(id.getNameIdentifier(), TestSources.COMMON_ATTRIBUTE_VALUE_STRING);

    }
}
