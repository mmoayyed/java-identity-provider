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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.saml.metadata.resolver.ChainingMetadataResolver;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.filter.MetadataFilter;
import org.opensaml.saml.metadata.resolver.filter.MetadataFilterChain;
import org.opensaml.saml.metadata.resolver.filter.MetadataNodeProcessor;
import org.opensaml.saml.metadata.resolver.filter.impl.NodeProcessingMetadataFilter;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * A {@link BeanPostProcessor} for {@link MetadataResolver} beans that ensures a {@link NodeProcessingMetadataFilter}
 * containing a set of default {@link MetadataNodeProcessor} plugins is attached.
 * 
 * <p>
 * This is done to ensure that other components function correctly, such as the PKIX trust engine and predicates that
 * depend on group information.
 * </p>
 */
public class NodeProcessingAttachingBeanPostProcessor implements BeanPostProcessor, Ordered {

    /** The processors to install. */
    @Nonnull @NonnullElements private List<MetadataNodeProcessor> nodeProcessors;
    
    /** Constructor. */
    public NodeProcessingAttachingBeanPostProcessor() {
        nodeProcessors = Collections.emptyList();
    }
    
    /**
     * Set the {@link MetadataNodeProcessor} instances to auto-attach.
     * 
     * @param processors processors to auto-attach
     * 
     * @since 4.1.0
     */
    public void setNodeProcessors(@Nullable @NonnullElements final Collection<MetadataNodeProcessor> processors) {
        if (processors != null) {
            nodeProcessors = List.copyOf(processors);
        } else {
            nodeProcessors = Collections.emptyList();
        }
    }

    /** {@inheritDoc} */
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }
    
    /** {@inheritDoc} */
    @Override public Object postProcessBeforeInitialization(final Object bean, final String beanName) {
        
        // Do not attach to beans which just include other ones.
        if (!(bean instanceof MetadataResolver) || bean instanceof ChainingMetadataResolver) {
            return bean;
        }

        final MetadataResolver resolver = (MetadataResolver) bean;

        final NodeProcessingMetadataFilter filterToAttach = new NodeProcessingMetadataFilter();
        filterToAttach.setNodeProcessors(nodeProcessors);
        try {
            filterToAttach.initialize();
        } catch (final ComponentInitializationException e) {
            throw new BeanCreationException("Error initializing NodeProcessingMetadataFilter", e);
        }

        final MetadataFilter filter = resolver.getMetadataFilter();
        if (filter == null) {
            resolver.setMetadataFilter(filterToAttach);
        } else if (filter instanceof MetadataFilterChain) {
            ((MetadataFilterChain) filter).getFilters().add(filterToAttach);
        } else {
            final MetadataFilterChain chain = new MetadataFilterChain();
            chain.setFilters(List.of(filter, filterToAttach));
            resolver.setMetadataFilter(chain);
        }

        return resolver;
    }

    /** {@inheritDoc} */
    @Override public Object postProcessAfterInitialization(final Object bean, final String beanName) {
        return bean;
    }

}