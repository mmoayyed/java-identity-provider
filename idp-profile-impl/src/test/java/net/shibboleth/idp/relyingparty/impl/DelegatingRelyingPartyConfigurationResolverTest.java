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

package net.shibboleth.idp.relyingparty.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.criterion.RoleDescriptorCriterion;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.saml.relyingparty.impl.RelyingPartyConfigurationSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/** Unit tests for {@link DelegatingCriteriaRelyingPartyConfigurationResolver. */
public class DelegatingRelyingPartyConfigurationResolverTest extends XMLObjectBaseTestCase {
    
    @Test
    public void testResolve() throws ComponentInitializationException, ResolverException {
        final RelyingPartyConfiguration anonRP = new RelyingPartyConfiguration();
        anonRP.setId("anonRPId");
        anonRP.setResponderId("anonRPResp");
        anonRP.setDetailedErrors(true);
        anonRP.initialize();
        
        final RelyingPartyConfiguration defaultRP = new RelyingPartyConfiguration();
        defaultRP.setId("defaultRPId");
        defaultRP.setResponderId("defaultRPResp");
        defaultRP.setDetailedErrors(true);
        defaultRP.initialize();
        
        final RelyingPartyConfiguration one = RelyingPartyConfigurationSupport.byName(Collections.singleton("rp1"));
        one.setResponderId("foo");
        one.setDetailedErrors(true);
        one.initialize();

        final RelyingPartyConfiguration two = RelyingPartyConfigurationSupport.byName(Collections.singleton("rp2"));
        two.setResponderId("foo");
        two.setDetailedErrors(true);
        two.initialize();

        final RelyingPartyConfiguration three = RelyingPartyConfigurationSupport.byName(Collections.singleton("rp3"));
        three.setResponderId("foo");
        three.setDetailedErrors(true);
        three.initialize();
        
        final List<RelyingPartyConfiguration> rpConfigs = Arrays.asList(one, two, three);

        final DefaultRelyingPartyConfigurationResolver delegate = new DefaultRelyingPartyConfigurationResolver();
        delegate.setId("delegate");
        delegate.setRelyingPartyConfigurations(rpConfigs);
        delegate.setUnverifiedConfiguration(anonRP);
        delegate.setDefaultConfiguration(defaultRP);
        delegate.initialize();
        
        final DelegatingCriteriaRelyingPartyConfigurationResolver resolver = new DelegatingCriteriaRelyingPartyConfigurationResolver();
        resolver.setId("resolver");
        resolver.setDelegate(delegate);
        resolver.initialize();
        
        Iterable<RelyingPartyConfiguration> results = resolver.resolve(new CriteriaSet(new EntityIdCriterion("rp1")));
        Assert.assertNotNull(results);

        Iterator<RelyingPartyConfiguration> resultItr = results.iterator();
        Assert.assertTrue(resultItr.hasNext());
        Assert.assertSame(resultItr.next(), one);
        Assert.assertFalse(resultItr.hasNext());

        RelyingPartyConfiguration result = resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion("rp2")));
        Assert.assertSame(result, two);
        
        EntityDescriptor ed = (EntityDescriptor) XMLObjectSupport.buildXMLObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        ed.setEntityID("rp3");
        RoleDescriptor rd = (RoleDescriptor) XMLObjectSupport.buildXMLObject(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        ed.getRoleDescriptors().add(rd);
        result = resolver.resolveSingle(new CriteriaSet(new RoleDescriptorCriterion(rd)));
        Assert.assertSame(result, three);
        
        result = resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion("doesNotExist")));
        Assert.assertSame(result, defaultRP);
        
        results = resolver.resolve(null);
        Assert.assertNotNull(results);
        Assert.assertFalse(results.iterator().hasNext());

        result = resolver.resolveSingle(null);
        Assert.assertNull(result);
    }

}
