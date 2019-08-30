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

package net.shibboleth.idp.profile.spring.relyingparty.metadata.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.opensaml.saml.metadata.resolver.ChainingMetadataResolver;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;

import net.shibboleth.idp.profile.spring.relyingparty.metadata.MetadataProviderContainer;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import net.shibboleth.utilities.java.support.service.ServiceException;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

/**
 * Strategy for summoning up a {@link MetadataResolver} from a populated {@link ApplicationContext}. <br/>
 * This is made somewhat complex by the need to chain multiple, top level Metadata Resolvers, but to not combine non
 * top level resolvers. The parser will create a {@link MetadataProviderContainer} for each top level resolver. If we
 * encounter but one we are done (it is a {@link ServiceableComponent} already), otherwise we need to chain all the
 * children together and wrap them into a Serviceable Component.
 * 
 */
public class MetadataResolverServiceStrategy extends AbstractIdentifiableInitializableComponent implements
        Function<ApplicationContext, ServiceableComponent<MetadataResolver>> {

    /** {@inheritDoc} */
    @Nullable public ServiceableComponent<MetadataResolver> apply(@Nullable final ApplicationContext appContext) {
        final Collection<MetadataProviderContainer> containers =
                appContext.getBeansOfType(MetadataProviderContainer.class).values();

        if (containers.isEmpty()) {
            throw new ServiceException("Reload did not produce any bean of type"
                    + MetadataProviderContainer.class.getName());
        }
        if (1 == containers.size()) {
            // done
            return containers.iterator().next();
        }
        // initialize so we can sort
        for (final MetadataProviderContainer resolver:containers) {
            try {
                resolver.initialize();
            } catch (final ComponentInitializationException e) {
                throw new BeanCreationException("could not preinitialize , metadata provider " + resolver.getId(), e);
            }
        }
        
        final List<MetadataProviderContainer> containerList = new ArrayList<>(containers.size());
        containerList.addAll(containers);
        Collections.sort(containerList);
        final ChainingMetadataResolver chain = new ChainingMetadataResolver();
        try {
            chain.setResolvers(containerList.stream().
                               map(MetadataProviderContainer::getEmbeddedResolver).
                               collect(Collectors.toList()));
            chain.setId("MultiFileResolverFor:"+containers.size()+":Resources");
            chain.initialize();
            final MetadataProviderContainer result = new MetadataProviderContainer();
            result.setEmbeddedResolver(chain);
            result.initialize();
            return result;
        } catch (final ResolverException | ComponentInitializationException e) {
           throw new ServiceException("Chaining constructor create failed", e);
        }
    }
}
