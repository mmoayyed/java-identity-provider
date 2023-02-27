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

package net.shibboleth.idp.cas.service.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.time.Duration;
import java.util.Collections;
import java.util.Timer;

import javax.annotation.Nonnull;

import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.metadata.resolver.impl.PredicateRoleDescriptorResolver;
import org.opensaml.saml.metadata.resolver.impl.ResourceBackedMetadataResolver;
import org.opensaml.saml.metadata.resolver.index.MetadataIndex;
import org.opensaml.saml.metadata.resolver.index.impl.EndpointMetadataIndex;
import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import net.shibboleth.idp.cas.service.Service;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.resource.Resource;
import net.shibboleth.shared.spring.resource.ResourceHelper;
import net.shibboleth.shared.xml.ParserPool;

/**
 * Unit test for {@link MetadataServiceRegistry}.
 *
 * @author Marvin S. Addison
 */
@SuppressWarnings("javadoc")
public class MetadataServiceRegistryTest {

    private ResourceBackedMetadataResolver metadataResolver;

    @DataProvider(name = "parameters")
    public Object[][] parameters() {
        final String group = "urn:mace:example.org";
        return new Object[][] {
            {"https://alpha.example.org/", new Service("https://alpha.example.org/", group, true, true)},
            {"https://alpha.example.org/a/b/", new Service("https://alpha.example.org/a/b/", group, true, true)},
            {"https://alpha.dev.example.org/", new Service("https://alpha.dev.example.org/", group, true, true)},
            {"https://alpha.dev.example.org/#1", new Service("https://alpha.dev.example.org/#1", group, true, true)},
            {"https://alpha.dev.example.org", null},
            {"https://beta.example.org/", new Service("https://beta.example.org/", group, false)},
            {
                "https://betatest.example.org:8443/a?b=2",
                new Service("https://betatest.example.org:8443/a?b=2", group, false),
            },
        };
    }

    /**
     *  Initialize OpenSAML.
     *
     * @throws InitializationException ...
     */
    @BeforeSuite
    public void initOpenSAML() throws InitializationException {
        InitializationService.initialize();
    }

    @BeforeClass
    public void setUp() throws Exception {
        final Resource metadata = ResourceHelper.of(new ClassPathResource("/metadata/cas-test-metadata.xml"));
        metadataResolver = new ResourceBackedMetadataResolver(new Timer(true), metadata);
        final ParserPool pool = XMLObjectProviderRegistrySupport.getParserPool();
        assert pool != null;
        metadataResolver.setParserPool(pool);
        metadataResolver.setMaxRefreshDelay(Duration.ofSeconds(500));
        metadataResolver.setId("cas");
        metadataResolver.setIndexes(Collections.<MetadataIndex>singleton(new EndpointMetadataIndex(
                new MetadataServiceRegistry.LoginEndpointPredicate())));
        metadataResolver.initialize();
    }

    @AfterClass
    public void tearDown() {
        metadataResolver.destroy();
    }

    @Test(dataProvider = "parameters")
    public void testLookup(@Nonnull final String serviceURL, final Service expected) throws ComponentInitializationException {
        assert metadataResolver!=null;
        final PredicateRoleDescriptorResolver wrapper = new PredicateRoleDescriptorResolver(metadataResolver);
        wrapper.initialize();
        final MetadataServiceRegistry registry = new MetadataServiceRegistry(wrapper);
        final Service actual = registry.lookup(serviceURL);
        if (expected == null) {
            assertNull(actual);
        } else {
            assert actual != null;
            assertEquals(actual.getName(), expected.getName());
            assertEquals(actual.getGroup(), expected.getGroup());
            assertEquals(actual.isAuthorizedToProxy(), expected.isAuthorizedToProxy());
            assertEquals(actual.isSingleLogoutParticipant(), expected.isSingleLogoutParticipant());
        }
    }
}