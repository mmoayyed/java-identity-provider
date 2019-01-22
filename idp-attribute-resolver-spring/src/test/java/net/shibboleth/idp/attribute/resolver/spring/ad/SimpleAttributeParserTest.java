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

package net.shibboleth.idp.attribute.resolver.spring.ad;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockPropertySource;
import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.ext.spring.context.FilesystemGenericApplicationContext;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolverAttributeDefinitionDependency;
import net.shibboleth.idp.attribute.resolver.ResolverDataConnectorDependency;
import net.shibboleth.idp.attribute.resolver.ad.impl.SimpleAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.spring.BaseAttributeDefinitionParserTest;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.SimpleAttributeDefinitionParser;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.logic.RelyingPartyIdPredicate;
import net.shibboleth.idp.saml.attribute.encoding.impl.SAML1StringAttributeEncoder;
import net.shibboleth.idp.saml.attribute.encoding.impl.SAML2StringAttributeEncoder;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * Test for {@link SimpleAttributeDefinitionParser} and by extension {@link BaseAttributeDefinitionParser}.
 */
public class SimpleAttributeParserTest extends BaseAttributeDefinitionParserTest {

    @Test public void simple() {
        AttributeDefinition attrDef =
                getAttributeDefn("resolver/simpleAttributeUnpopulated.xml", SimpleAttributeDefinition.class);

        Assert.assertEquals(attrDef.getId(), "simpleUnpopulated");
        Assert.assertFalse(attrDef.isDependencyOnly(), "isDependencyOnly");
        Assert.assertTrue(attrDef.getDisplayDescriptions().isEmpty(), "getDisplayDescriptions().isEmpty()");
        Assert.assertTrue(attrDef.getDisplayNames().isEmpty(), "getDisplayNames().isEmpty()");
        Assert.assertEquals(attrDef.getAttributeDependencies().size(), 1);
        Assert.assertTrue(attrDef.getAttributeEncoders().isEmpty(), "getgetAttributeEncoders().isEmpty()");
    }

    @Test public void simplePopulated() throws ComponentInitializationException {
        final AttributeDefinition attrDef =
                getAttributeDefn("resolver/simpleAttributePopulated.xml", SimpleAttributeDefinition.class);

        attrDef.initialize();

        Assert.assertEquals(attrDef.getId(), "simplePopulated");
        Assert.assertTrue(attrDef.isDependencyOnly(), "isDependencyOnly");

        final Map<Locale, String> descriptions = attrDef.getDisplayDescriptions();
        Assert.assertEquals(descriptions.size(), 3, "getDisplayDescriptions");
        Assert.assertEquals(descriptions.get(new Locale("en")), "DescInEnglish");
        Assert.assertEquals(descriptions.get(new Locale("fr")), "DescEnFrancais");
        Assert.assertEquals(descriptions.get(new Locale("ca")), "DescInCanadian");

        final Map<Locale, String> names = attrDef.getDisplayNames();
        Assert.assertEquals(names.size(), 2, "getDisplayNames");
        Assert.assertEquals(names.get(new Locale("en")), "NameInEnglish");
        Assert.assertEquals(names.get(new Locale("fr")), "NameEnFrancais");

        Set<ResolverAttributeDefinitionDependency> adDeps = attrDef.getAttributeDependencies();
        Assert.assertEquals(adDeps.size(), 2, "getAttributeDependencies");
        Assert.assertTrue(adDeps.contains(TestSources.makeResolverPluginDependency("dep2")));
        Assert.assertTrue(adDeps.contains(TestSources.makeResolverPluginDependency("dep3")));

        Set<ResolverDataConnectorDependency> dcDeps = attrDef.getDataConnectorDependencies();
        Assert.assertEquals(dcDeps.size(), 1, "getDataConnectorDependencies");
        final ResolverDataConnectorDependency dcDep = dcDeps.iterator().next();
        Assert.assertEquals(dcDep.getDependencyPluginId(), "con1");
        Assert.assertTrue(dcDep.getAttributeNames().contains("dep1"));

        Assert.assertEquals(attrDef.getAttributeEncoders().size(), 1);
        final SAML2StringAttributeEncoder e1 =
                (SAML2StringAttributeEncoder) attrDef.getAttributeEncoders().iterator().next();
        Assert.assertEquals(e1.getName(), "urn:oid:0.9.2342.19200300.100.1.3");
        Assert.assertEquals(e1.getFriendlyName(), "mail");

    }

    @Test public void populated2() throws ComponentInitializationException {
        AttributeDefinition attrDef =
                getAttributeDefn("resolver/simpleAttributePopulated2.xml", SimpleAttributeDefinition.class);

        attrDef.initialize();

        Assert.assertEquals(attrDef.getId(), "simplePopulated2");
        Assert.assertFalse(attrDef.isDependencyOnly(), "isDependencyOnly");

        Assert.assertTrue(attrDef.getDisplayDescriptions().isEmpty(), "getDisplayDescriptions().isEmpty()");

        final Map<Locale, String> names = attrDef.getDisplayNames();
        Assert.assertEquals(names.size(), 1, "getDisplayNames");
        Assert.assertEquals(names.get(new Locale("en")), "NameInAmerican");

        final Set<ResolverAttributeDefinitionDependency> attrDeps = attrDef.getAttributeDependencies();
        Assert.assertEquals(attrDeps.size(), 1, "getAttributeDependencies");
        Assert.assertTrue(attrDeps.contains(TestSources.makeResolverPluginDependency("dep3")));

        final Set<ResolverDataConnectorDependency> dcDeps = attrDef.getDataConnectorDependencies();
        Assert.assertEquals(dcDeps.size(), 0, "getDataConnectorDependencies");

        
        Assert.assertEquals(attrDef.getAttributeEncoders().size(), 2);
        final List a = new ArrayList(attrDef.getAttributeEncoders());

        final SAML2StringAttributeEncoder saml2;
        final SAML1StringAttributeEncoder saml1;
        if (a.get(0) instanceof SAML2StringAttributeEncoder) {
            saml2 = (SAML2StringAttributeEncoder) a.get(0);
            saml1 = (SAML1StringAttributeEncoder) a.get(1);
        } else {
            saml2 = (SAML2StringAttributeEncoder) a.get(1);
            saml1 = (SAML1StringAttributeEncoder) a.get(0);
        }
        Assert.assertEquals(saml2.getName(), "urn:oid:0.9.2342.19200300.100.1.3");
        Assert.assertEquals(saml2.getFriendlyName(), "mail");

        Assert.assertEquals(saml1.getName(), "urn:mace:dir:attribute-def:mail");
    }

    @Test public void bad() throws ComponentInitializationException {
        getAttributeDefn("resolver/simpleAttributeBadValues.xml", SimpleAttributeDefinition.class);
    }

    @Test public void relyingParties() throws ComponentInitializationException {
        final GenericApplicationContext context = new FilesystemGenericApplicationContext();
        final MutablePropertySources propertySources = context.getEnvironment().getPropertySources();
        final MockPropertySource mockEnvVars = new MockPropertySource();
        mockEnvVars.setProperty("prop1", "p1");
        mockEnvVars.setProperty("prop2", "p2 p3");
        mockEnvVars.setProperty("prop3", "");
        propertySources.replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, mockEnvVars);

        final PropertySourcesPlaceholderConfigurer placeholderConfig = new PropertySourcesPlaceholderConfigurer();
        placeholderConfig.setPlaceholderPrefix("%{");
        placeholderConfig.setPlaceholderSuffix("}");
        placeholderConfig.setPropertySources(propertySources);
        context.addBeanFactoryPostProcessor(placeholderConfig);

        AttributeDefinition attr = getAttributeDefn("resolver/relyingParties.xml", SimpleAttributeDefinition.class, context);
        RelyingPartyIdPredicate pre = (RelyingPartyIdPredicate) attr.getActivationCondition();
        ProfileRequestContext prc = new ProfileRequestContext<>();
        RelyingPartyContext rpContext = prc.getSubcontext(RelyingPartyContext.class, true);
        rpContext.setRelyingPartyId("p1");
        Assert.assertTrue(pre.apply(prc));
        rpContext.setRelyingPartyId("p2 p3");
        Assert.assertFalse(pre.apply(prc));
        rpContext.setRelyingPartyId("p3");
        Assert.assertTrue(pre.apply(prc));
    }
}
