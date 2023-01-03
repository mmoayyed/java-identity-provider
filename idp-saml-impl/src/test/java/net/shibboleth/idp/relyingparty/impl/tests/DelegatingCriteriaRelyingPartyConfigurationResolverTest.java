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

package net.shibboleth.idp.relyingparty.impl.tests;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.testing.XMLObjectBaseTestCase;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.profile.logic.EntityAttributesPredicate;
import org.opensaml.saml.common.profile.logic.EntityAttributesPredicate.Candidate;
import org.opensaml.saml.criterion.RoleDescriptorCriterion;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.metadata.EntityGroupName;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.relyingparty.impl.DefaultRelyingPartyConfigurationResolver;
import net.shibboleth.idp.relyingparty.impl.DelegatingCriteriaRelyingPartyConfigurationResolver;
import net.shibboleth.idp.saml.relyingparty.impl.RelyingPartyConfigurationSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.ResolverException;

/** Unit tests for {@link DelegatingCriteriaRelyingPartyConfigurationResolver}. */
public class DelegatingCriteriaRelyingPartyConfigurationResolverTest extends XMLObjectBaseTestCase {
    
    private RelyingPartyConfiguration anonRP, defaultRP; 
    
    private RelyingPartyConfiguration oneByName, twoByName, threeByName;
    private RelyingPartyConfiguration oneByGroup, twoByGroup;
    private RelyingPartyConfiguration oneByTag, twoByTag;
    
    private DefaultRelyingPartyConfigurationResolver delegate;
    
    private DelegatingCriteriaRelyingPartyConfigurationResolver resolver;
    
    @BeforeMethod
    public void setup() throws ComponentInitializationException {
        anonRP = new RelyingPartyConfiguration();
        anonRP.setId("anonRPId");
        anonRP.setResponderId("anonRPResp");
        anonRP.setDetailedErrors(true);
        anonRP.initialize();
        
        defaultRP = new RelyingPartyConfiguration();
        defaultRP.setId("defaultRPId");
        defaultRP.setResponderId("defaultRPResp");
        defaultRP.setDetailedErrors(true);
        defaultRP.initialize();
        
        oneByName = RelyingPartyConfigurationSupport.byName(Collections.singleton("rp1"));
        oneByName.setResponderId("foo");
        oneByName.setDetailedErrors(true);
        oneByName.initialize();

        twoByName = RelyingPartyConfigurationSupport.byName(Collections.singleton("rp2"));
        twoByName.setResponderId("foo");
        twoByName.setDetailedErrors(true);
        twoByName.initialize();

        threeByName = RelyingPartyConfigurationSupport.byName(Collections.singleton("rp3"));
        threeByName.setResponderId("foo");
        threeByName.setDetailedErrors(true);
        threeByName.initialize();
        
        oneByGroup = RelyingPartyConfigurationSupport.byGroup(Collections.singleton("group1"), null);
        oneByGroup.setResponderId("foo");
        oneByGroup.setDetailedErrors(true);
        oneByGroup.initialize();
        
        twoByGroup = RelyingPartyConfigurationSupport.byGroup(Collections.singleton("group2"), null);
        twoByGroup.setResponderId("foo");
        twoByGroup.setDetailedErrors(true);
        twoByGroup.initialize();
        
        Candidate candidate1 = new EntityAttributesPredicate.Candidate("urn:test:attr:tag", Attribute.URI_REFERENCE);
        candidate1.setValues(Collections.singleton("tag1"));
        oneByTag = RelyingPartyConfigurationSupport.byTag(Collections.singleton(candidate1), true, true);
        oneByTag.setId("byTag1");
        oneByTag.setResponderId("foo");
        oneByTag.setDetailedErrors(true);
        oneByTag.initialize();
        
        Candidate candidate2 = new EntityAttributesPredicate.Candidate("urn:test:attr:tag", Attribute.URI_REFERENCE);
        candidate2.setValues(Collections.singleton("tag2"));
        twoByTag = RelyingPartyConfigurationSupport.byTag(Collections.singleton(candidate2), true, true);
        twoByTag.setId("byTag2");
        twoByTag.setResponderId("foo");
        twoByTag.setDetailedErrors(true);
        twoByTag.initialize();
        
        // Complete the population and init this in the individual tests.
        delegate = new DefaultRelyingPartyConfigurationResolver();
        delegate.setId("delegate");
        delegate.setUnverifiedConfiguration(anonRP);
        delegate.setDefaultConfiguration(defaultRP);
        
        resolver = new DelegatingCriteriaRelyingPartyConfigurationResolver();
        resolver.setId("resolver");
        resolver.setDelegate(delegate);
        resolver.initialize();
    }
    
    @Test
    public void testResolveByEntityIDViaEntityIDCriterion() throws ComponentInitializationException, ResolverException {
        Iterable<RelyingPartyConfiguration> results = null;
        RelyingPartyConfiguration result = null;
        
        final List<RelyingPartyConfiguration> rpConfigs = Arrays.asList(oneByName, twoByName, threeByName);

        delegate.setId("delegate");
        delegate.setRelyingPartyConfigurations(rpConfigs);
        delegate.initialize();
        
        results = resolver.resolve(new CriteriaSet(new EntityIdCriterion("rp1")));
        Assert.assertNotNull(results);

        Iterator<RelyingPartyConfiguration> resultItr = results.iterator();
        Assert.assertTrue(resultItr.hasNext());
        Assert.assertSame(resultItr.next(), oneByName);
        Assert.assertFalse(resultItr.hasNext());

        result = resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion("rp2")));
        Assert.assertSame(result, twoByName);
        
        result = resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion("doesNotExist")));
        Assert.assertSame(result, defaultRP);
        
        results = resolver.resolve(null);
        Assert.assertNotNull(results);
        Assert.assertFalse(results.iterator().hasNext());

        result = resolver.resolveSingle(null);
        Assert.assertNull(result);
    }
    
    @Test
    public void testResolveByEntityIDViaRoleDescriptor() throws ComponentInitializationException, ResolverException {
        Iterable<RelyingPartyConfiguration> results = null;
        RelyingPartyConfiguration result = null;
        
        final List<RelyingPartyConfiguration> rpConfigs = Arrays.asList(oneByName, twoByName, threeByName);

        delegate.setId("delegate");
        delegate.setRelyingPartyConfigurations(rpConfigs);
        delegate.initialize();
        
        EntityDescriptor ed = (EntityDescriptor) XMLObjectSupport.buildXMLObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        ed.setEntityID("rp3");
        RoleDescriptor rd = (RoleDescriptor) XMLObjectSupport.buildXMLObject(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        ed.getRoleDescriptors().add(rd);
        
        results = resolver.resolve(new CriteriaSet(new RoleDescriptorCriterion(rd)));
        Assert.assertNotNull(results);
        
        Iterator<RelyingPartyConfiguration> resultItr = results.iterator();
        Assert.assertTrue(resultItr.hasNext());
        Assert.assertSame(resultItr.next(), threeByName);
        Assert.assertFalse(resultItr.hasNext());
        
        result = resolver.resolveSingle(new CriteriaSet(new RoleDescriptorCriterion(rd)));
        Assert.assertSame(result, threeByName);
    }
    
    @Test
    public void testResolveByEntityGroupName() throws ComponentInitializationException, ResolverException {
        Iterable<RelyingPartyConfiguration> results = null;
        RelyingPartyConfiguration result = null;
        
        final List<RelyingPartyConfiguration> rpConfigs = Arrays.asList(oneByGroup, twoByGroup);

        delegate.setId("delegate");
        delegate.setRelyingPartyConfigurations(rpConfigs);
        delegate.initialize();
        
        EntityDescriptor ed = (EntityDescriptor) XMLObjectSupport.buildXMLObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        ed.setEntityID("rp3");
        ed.getObjectMetadata().put(new EntityGroupName("group1"));
        RoleDescriptor rd = (RoleDescriptor) XMLObjectSupport.buildXMLObject(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        ed.getRoleDescriptors().add(rd);
        
        results = resolver.resolve(new CriteriaSet(new RoleDescriptorCriterion(rd)));
        Assert.assertNotNull(results);
        
        Iterator<RelyingPartyConfiguration> resultItr = results.iterator();
        Assert.assertTrue(resultItr.hasNext());
        Assert.assertSame(resultItr.next(), oneByGroup);
        Assert.assertFalse(resultItr.hasNext());
        
        result = resolver.resolveSingle(new CriteriaSet(new RoleDescriptorCriterion(rd)));
        Assert.assertSame(result, oneByGroup);
        
        // With 2 known group names and 1 unknown, should resolve 2 by group
        ed.getObjectMetadata().put(new EntityGroupName("group2"));
        ed.getObjectMetadata().put(new EntityGroupName("unknown"));
        
        results = resolver.resolve(new CriteriaSet(new RoleDescriptorCriterion(rd)));
        Assert.assertNotNull(results);
        
        resultItr = results.iterator();
        Assert.assertTrue(resultItr.hasNext());
        Assert.assertSame(resultItr.next(), oneByGroup);
        Assert.assertTrue(resultItr.hasNext());
        Assert.assertSame(resultItr.next(), twoByGroup);
        Assert.assertFalse(resultItr.hasNext());
    }
    
    @Test
    public void testResolveByEntityTag() throws ComponentInitializationException, ResolverException, MarshallingException {
        Iterable<RelyingPartyConfiguration> results = null;
        RelyingPartyConfiguration result = null;
        
        final List<RelyingPartyConfiguration> rpConfigs = Arrays.asList(oneByTag, twoByTag);

        delegate.setId("delegate");
        delegate.setRelyingPartyConfigurations(rpConfigs);
        delegate.initialize();
        
        EntityDescriptor ed = (EntityDescriptor) XMLObjectSupport.buildXMLObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        ed.setEntityID("rp3");
        RoleDescriptor rd = (RoleDescriptor) XMLObjectSupport.buildXMLObject(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        ed.getRoleDescriptors().add(rd);
        
        addTag(ed, "tag1");
        
        results = resolver.resolve(new CriteriaSet(new RoleDescriptorCriterion(rd)));
        Assert.assertNotNull(results);
        
        Iterator<RelyingPartyConfiguration> resultItr = results.iterator();
        Assert.assertTrue(resultItr.hasNext());
        Assert.assertSame(resultItr.next(), oneByTag);
        Assert.assertFalse(resultItr.hasNext());
        
        result = resolver.resolveSingle(new CriteriaSet(new RoleDescriptorCriterion(rd)));
        Assert.assertSame(result, oneByTag);
        
        // With 2 known tags names and 1 unknown, should resolve 2 by tag
        addTag(ed, "tag2");
        addTag(ed, "unknown");
        
        results = resolver.resolve(new CriteriaSet(new RoleDescriptorCriterion(rd)));
        Assert.assertNotNull(results);
        
        resultItr = results.iterator();
        Assert.assertTrue(resultItr.hasNext());
        Assert.assertSame(resultItr.next(), oneByTag);
        Assert.assertTrue(resultItr.hasNext());
        Assert.assertSame(resultItr.next(), twoByTag);
        Assert.assertFalse(resultItr.hasNext());
    }
    
    private void addTag(EntityDescriptor ed, String value) {
        Extensions extensions = ed.getExtensions();
        if (extensions == null) {
            extensions = (Extensions) XMLObjectSupport.buildXMLObject(Extensions.DEFAULT_ELEMENT_NAME);
            ed.setExtensions(extensions);
        }
        
        EntityAttributes entityAttributes = null;
        List<XMLObject> entityAttributesList = extensions.getUnknownXMLObjects(EntityAttributes.DEFAULT_ELEMENT_NAME);
        if (entityAttributesList.isEmpty()) {
            entityAttributes = (EntityAttributes) XMLObjectSupport.buildXMLObject(EntityAttributes.DEFAULT_ELEMENT_NAME);
            extensions.getUnknownXMLObjects().add(entityAttributes);
        } else {
            entityAttributes = (EntityAttributes) entityAttributesList.get(0);
        }
        
        Attribute attr = null;
        List<Attribute> attrs = entityAttributes.getAttributes();
        if (attrs.isEmpty()) {
            attr = (Attribute) XMLObjectSupport.buildXMLObject(Attribute.DEFAULT_ELEMENT_NAME);
            attr.setNameFormat(Attribute.URI_REFERENCE);
            attr.setName("urn:test:attr:tag");
        } else {
            attr = attrs.get(0);
        }
        
        XSString val = (XSString) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(XSString.TYPE_NAME)
                .buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        val.setValue(value);
        attr.getAttributeValues().add(val);
        
        attrs.add(attr);
    }

}
