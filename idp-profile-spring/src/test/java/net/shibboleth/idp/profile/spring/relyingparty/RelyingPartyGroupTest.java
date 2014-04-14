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

package net.shibboleth.idp.profile.spring.relyingparty;

import java.util.Collection;
import java.util.List;

import net.shibboleth.ext.spring.config.DurationToLongConverter;
import net.shibboleth.ext.spring.config.StringToIPRangeConverter;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.relyingparty.impl.DefaultRelyingPartyConfigurationResolver;
import net.shibboleth.idp.saml.metadata.impl.RelyingPartyMetadataProvider;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Sets;

/**
 * Test for a complete example RelyingParty file
 */
public class RelyingPartyGroupTest extends OpenSAMLInitBaseTestCase {

    private static final String PATH = "/net/shibboleth/idp/profile/spring/relyingparty/";

    private GenericApplicationContext getContext(String... files) {
        final Resource[] resources = new Resource[files.length];

        for (int i = 0; i < files.length; i++) {
            resources[i] = new ClassPathResource(PATH + files[i]);
        }

        final GenericApplicationContext context = new GenericApplicationContext();
        ConversionServiceFactoryBean service = new ConversionServiceFactoryBean();
        context.setDisplayName("ApplicationContext");
        service.setConverters(Sets.newHashSet(new DurationToLongConverter(), new StringToIPRangeConverter()));
        service.afterPropertiesSet();

        context.getBeanFactory().setConversionService(service.getObject());

        final XmlBeanDefinitionReader configReader = new XmlBeanDefinitionReader(context);

        configReader.setValidating(true);

        configReader.loadBeanDefinitions(resources);
        context.refresh();

        return context;
    }

    // TODO re-enable when all parsers are complete
    @Test(enabled=false) public void relyingParty() {
        GenericApplicationContext context = getContext("beans.xml", "relying-party-group.xml");

        DefaultRelyingPartyConfigurationResolver resolver = context.getBean(DefaultRelyingPartyConfigurationResolver.class);
        Assert.assertTrue(resolver.getRelyingPartyConfigurations().isEmpty());

        RelyingPartyConfiguration anon = resolver.getAnonymousConfiguration();
        Assert.assertFalse(anon.isDetailedErrors());
        Assert.assertTrue(anon.getProfileConfigurations().isEmpty());

        RelyingPartyConfiguration def = resolver.getDefaultConfiguration();
        Assert.assertEquals(def.getProfileConfigurations().size(), 8);
        
        final Collection<RelyingPartyMetadataProvider> metadataProviders = context.getBeansOfType(RelyingPartyMetadataProvider.class).values();
        
        Assert.assertEquals(metadataProviders.size(), 1);
    }

    @Test public void relyingParty2() {
        GenericApplicationContext context = getContext("relying-party-group2.xml");
        DefaultRelyingPartyConfigurationResolver resolver = context.getBean(DefaultRelyingPartyConfigurationResolver.class);

        Assert.assertEquals(resolver.getId(), "RelyingPartyGroup");

        final List<RelyingPartyConfiguration> rps = resolver.getRelyingPartyConfigurations();
        Assert.assertEquals(rps.size(), 2);

        RelyingPartyConfiguration rp = rps.get(0);
        Assert.assertEquals(rp.getId(), "the id1");
        Assert.assertEquals(rp.getResponderId(), "IdP1");

        rp = rps.get(1);
        Assert.assertEquals(rp.getId(), "the id2");
        Assert.assertEquals(rp.getResponderId(), "IdP2");

        rp = resolver.getAnonymousConfiguration();
        Assert.assertEquals(rp.getResponderId(), "AnonIdP");
        Assert.assertEquals(rp.getId(), "AnonymousRelyingParty");

        rp = resolver.getDefaultConfiguration();
        Assert.assertEquals(rp.getResponderId(), "DefaultIdP");
        Assert.assertEquals(rp.getId(), "DefaultRelyingParty");
    }

}
