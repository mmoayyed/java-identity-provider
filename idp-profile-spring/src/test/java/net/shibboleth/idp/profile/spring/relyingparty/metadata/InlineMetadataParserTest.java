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

package net.shibboleth.idp.profile.spring.relyingparty.metadata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import net.shibboleth.ext.spring.resource.PreferFileSystemResourceLoader;
import net.shibboleth.ext.spring.service.ReloadableSpringService;
import net.shibboleth.ext.spring.util.ApplicationContextBuilder;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.DOMMetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Collections2;

public class InlineMetadataParserTest extends AbstractMetadataParserTest {

    @Test public void entity() throws ResolverException, IOException {
        final DOMMetadataResolver resolver = getBean(DOMMetadataResolver.class, "inLineEntity.xml", "beans.xml");

        Assert.assertEquals(resolver.getId(), "inLineEntity");

        Assert.assertTrue(resolver.isFailFastInitialization());
        Assert.assertTrue(resolver.isRequireValidMetadata());
        
        Assert.assertTrue(resolver.isResolveViaPredicatesOnly());
        Assert.assertNotNull(resolver.getIndexes());
        Assert.assertFalse(resolver.getIndexes().isEmpty());

        final Iterator<EntityDescriptor> entities = resolver.resolve(criteriaFor(IDP_ID)).iterator();
        Assert.assertEquals(entities.next().getEntityID(), IDP_ID);
        Assert.assertFalse(entities.hasNext());

    }

    @Test public void entities() throws ResolverException, IOException {
        final DOMMetadataResolver resolver = getBean(DOMMetadataResolver.class, "inLineEntities.xml", "beans.xml");

        Assert.assertEquals(resolver.getId(), "inLineEntities");

        Assert.assertFalse(resolver.isFailFastInitialization());
        Assert.assertFalse(resolver.isRequireValidMetadata());
        
        Assert.assertTrue(resolver.isResolveViaPredicatesOnly());
        Assert.assertNotNull(resolver.getIndexes());
        Assert.assertFalse(resolver.getIndexes().isEmpty());

        Assert.assertNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
        Assert.assertNotNull(resolver.resolveSingle(criteriaFor(SP_ID)));

    }

    @Test public void multiple() throws ResolverException, IOException {

        final ResourceLoader loader = new PreferFileSystemResourceLoader();
        
        final ApplicationContextBuilder builder = new ApplicationContextBuilder();
        builder.setName("ApplicationContext: " + InlineMetadataParserTest.class);

        final Collection<String> defs = new ArrayList<>();
        defs.add("net/shibboleth/idp/profile/spring/relyingparty/metadata/beans.xml");
        defs.add("net/shibboleth/idp/profile/spring/relyingparty/metadata/multipleResolvers.xml");

        builder.setServiceConfigurations(Collections2.transform(defs, s -> loader.getResource(s)));

        final GenericApplicationContext context = builder.build();
        
        registerContext(context);
        
        final ReloadableSpringService<MetadataResolver> ms =
                context.getBean("shibboleth.MetadataResolverService", ReloadableSpringService.class);

        final ServiceableComponent<MetadataResolver> msc = ms.getServiceableComponent();

        try {
            final MetadataResolver resolver = msc.getComponent();
            Assert.assertNotNull(resolver.resolveSingle(criteriaFor(IDP_ID)));
            Assert.assertNotNull(resolver.resolveSingle(criteriaFor(SP_ID)));
        } finally {
            msc.unpinComponent();
        }
        msc.unloadComponent();
    }
    
}