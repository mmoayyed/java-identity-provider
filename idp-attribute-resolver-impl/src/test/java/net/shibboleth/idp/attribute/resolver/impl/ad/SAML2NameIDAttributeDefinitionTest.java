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
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.idp.attribute.resolver.impl.TestSources;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.saml.saml2.core.NameID;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Optional;

/**
 * Test for {@link SAML2NameIDAttributeDefinition}.
 */
public class SAML2NameIDAttributeDefinitionTest extends OpenSAMLInitBaseTestCase {

    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "simple";

    private static final String SECOND_ATTRIBUTE_NAME = "second";

    private static final String ALTERNATIVE_FORMAT = "ALTERNATE_FORMAT";

    private static final String ALTERNATE_QUALIFIER = "ALTERNATE_QUALIFIER";

    private static final String ALTERNATE_SP_QUALIFIER = "ALTERNATE_SP_QUAL";

    @Test public void testEmpty() throws AttributeResolutionException, ComponentInitializationException {
        final SAML2NameIDAttributeDefinition defn = new SAML2NameIDAttributeDefinition();
        defn.setId(TEST_ATTRIBUTE_NAME);
        defn.setDependencies(Collections.singleton(new ResolverPluginDependency("foo", "bar")));
        defn.initialize();

        final Optional<Attribute> result = defn.doAttributeDefinitionResolve(new AttributeResolutionContext());

        Assert.assertTrue(result.get().getValues().isEmpty());
    }

    private AttributeResolver setupResolver() throws ComponentInitializationException {
        final SAML2NameIDAttributeDefinition defn = new SAML2NameIDAttributeDefinition();
        defn.setId(TEST_ATTRIBUTE_NAME);

        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(new ResolverPluginDependency(TestSources.STATIC_ATTRIBUTE_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        defn.setDependencies(dependencySet);
        defn.initialize();

        // And resolve
        final Set<BaseAttributeDefinition> am = new LazySet<BaseAttributeDefinition>();
        am.add(defn);
        am.add(TestSources.populatedStaticAttribute());

        final AttributeResolver resolver = new AttributeResolver("foo", am, null);
        resolver.initialize();

        return resolver;
    }

    @Test public void testSimple() throws AttributeResolutionException, ComponentInitializationException {
        final AttributeResolver resolver = setupResolver();

        AttributeResolutionContext context =
                TestSources.createResolutionContext(null, TestSources.IDP_ENTITY_ID, TestSources.SP_ENTITY_ID);
        resolver.resolveAttributes(context);
        final Collection<AttributeValue> values = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        Assert.assertEquals(values.size(), 2);
        Collection<String> nameValues = new HashSet<String>(2);
        for (AttributeValue val : values) {
            NameID id = (NameID) val.getValue();
            Assert.assertNull(id.getFormat());
            Assert.assertNull(id.getSPProvidedID());
            Assert.assertEquals(id.getSPNameQualifier(), TestSources.SP_ENTITY_ID);
            Assert.assertEquals(id.getNameQualifier(), TestSources.IDP_ENTITY_ID);
            nameValues.add(id.getValue());
        }
        Assert.assertTrue(nameValues.contains(TestSources.COMMON_ATTRIBUTE_VALUE_STRING));
        Assert.assertTrue(nameValues.contains(TestSources.ATTRIBUTE_ATTRIBUTE_VALUE_STRING));
    }

    @Test public void testNulls() throws ComponentInitializationException {

        AttributeResolver resolver = setupResolver();

        AttributeResolutionContext context = TestSources.createResolutionContext(null, null, TestSources.SP_ENTITY_ID);
        try {
            resolver.resolveAttributes(context);
            Assert.fail("null IdP EntityId should throw");
        } catch (AttributeResolutionException e) {
            // OK
        }

        resolver = setupResolver();

        context = TestSources.createResolutionContext(null, TestSources.IDP_ENTITY_ID, null);
        try {
            resolver.resolveAttributes(context);
            Assert.fail("null IdP EntityId should throw");
        } catch (AttributeResolutionException e) {
            // OK
        }
    }

    @Test public void testBadValue() throws AttributeResolutionException, ComponentInitializationException {

        final BaseAttributeDefinition defn = TestSources.nonStringAttributeDefiniton(TEST_ATTRIBUTE_NAME);

        final SAML2NameIDAttributeDefinition defn2 = new SAML2NameIDAttributeDefinition();
        defn2.setId(SECOND_ATTRIBUTE_NAME);

        // Set the dependency on the data connector
        Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(new ResolverPluginDependency(TEST_ATTRIBUTE_NAME, null));
        defn2.setDependencies(dependencySet);

        // And resolve
        Set<BaseAttributeDefinition> am = new LazySet<BaseAttributeDefinition>();
        am.add(defn);
        am.add(TestSources.populatedStaticAttribute());
        am.add(defn2);

        AttributeResolver resolver = new AttributeResolver("foo", am, null);
        resolver.initialize();

        AttributeResolutionContext context =
                TestSources.createResolutionContext(null, TestSources.IDP_ENTITY_ID, TestSources.SP_ENTITY_ID);;
        try {
            resolver.resolveAttributes(context);
        } catch (AttributeResolutionException e) {
            Assert.fail("resolution failed");
        }
        Assert.assertNull(context.getResolvedAttributes().get(SECOND_ATTRIBUTE_NAME));
    }

    @Test public void testSingleValueWithOptions() throws AttributeResolutionException,
            ComponentInitializationException {
        final SAML2NameIDAttributeDefinition defn = new SAML2NameIDAttributeDefinition();
        defn.setId(TEST_ATTRIBUTE_NAME);
        defn.setDependencies(Collections.singleton(new ResolverPluginDependency("foo", "bar")));

        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(new ResolverPluginDependency(TestSources.STATIC_ATTRIBUTE_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        defn.setDependencies(dependencySet);
        defn.setNameIdFormat(ALTERNATIVE_FORMAT);
        defn.setNameIdQualifier(ALTERNATE_QUALIFIER);
        defn.setNameIdSPQualifier(ALTERNATE_SP_QUALIFIER);
        defn.initialize();

        // And resolve
        final Set<BaseAttributeDefinition> am = new LazySet<BaseAttributeDefinition>();
        am.add(defn);
        am.add(TestSources.populatedStaticAttribute(TestSources.STATIC_ATTRIBUTE_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR, 1));

        final AttributeResolver resolver = new AttributeResolver("foo", am, null);
        resolver.initialize();

        AttributeResolutionContext context = TestSources.createResolutionContext(null, TestSources.IDP_ENTITY_ID, TestSources.SP_ENTITY_ID);
        try {
            resolver.resolveAttributes(context);
        } catch (AttributeResolutionException e) {
            Assert.fail("resolution failed", e);
        }
        final Collection<AttributeValue> values = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        Assert.assertEquals(values.size(), 1);
        NameID id = (NameID) values.iterator().next().getValue();
        Assert.assertEquals(id.getFormat(), ALTERNATIVE_FORMAT);
        Assert.assertEquals(defn.getNameIdFormat(), id.getFormat());
        Assert.assertNull(id.getSPProvidedID());
        Assert.assertEquals(id.getSPNameQualifier(), ALTERNATE_SP_QUALIFIER);
        Assert.assertEquals(defn.getNameIdSPQualifier(), id.getSPNameQualifier());
        Assert.assertEquals(id.getNameQualifier(), ALTERNATE_QUALIFIER);
        Assert.assertEquals(defn.getNameIdQualifier(), id.getNameQualifier());
        Assert.assertEquals(id.getValue(), TestSources.COMMON_ATTRIBUTE_VALUE_STRING);

    }

}
