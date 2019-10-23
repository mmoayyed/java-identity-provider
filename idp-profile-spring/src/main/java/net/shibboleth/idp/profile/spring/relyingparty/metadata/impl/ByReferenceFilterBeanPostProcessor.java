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

import java.util.Arrays;

import javax.annotation.Nullable;

import org.opensaml.saml.metadata.resolver.ChainingMetadataResolver;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.filter.MetadataFilter;
import org.opensaml.saml.metadata.resolver.filter.MetadataFilterChain;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;

import net.shibboleth.idp.saml.metadata.impl.ByReferenceMetadataFilterBridge;

/**
 * A {@link BeanPostProcessor} for {@link MetadataResolver} beans that ensures a {@link ByReferenceMetadataFilterBridge}
 * is attached.
 * 
 * @since 4.0.0
 */
public class ByReferenceFilterBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware, Ordered {

    /** Whether to enable the processor. */
    private boolean enabled;
    
    /** Spring context. */
    @Nullable private ApplicationContext applicationContext;

    /** Constructor. */
    public ByReferenceFilterBeanPostProcessor() {
        enabled = true;
    }
    
    /**
     * Set whether to enable the processor.
     * 
     * @param flag flag to set
     */
    public void setEnabled(final boolean flag) {
        enabled = flag;
    }
    
    /** {@inheritDoc} */
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

    /** {@inheritDoc} */
    public void setApplicationContext(@Nullable final ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    /** {@inheritDoc} */
    @Override public Object postProcessBeforeInitialization(final Object bean, final String beanName) {
        
        // Do not attach to beans which just include other ones.
        if (!enabled || !(bean instanceof MetadataResolver) || bean instanceof ChainingMetadataResolver) {
            return bean;
        }

        final MetadataResolver resolver = (MetadataResolver) bean;

        boolean filterAttached = false;

        final MetadataFilter filter = resolver.getMetadataFilter();
        if (filter instanceof ByReferenceMetadataFilterBridge) {
            filterAttached = true;
        } else if (filter instanceof MetadataFilterChain) {
            filterAttached = ((MetadataFilterChain) filter).getFilters().stream().anyMatch(
                    f -> f instanceof ByReferenceMetadataFilterBridge);
        }

        if (!filterAttached) {
            final ByReferenceMetadataFilterBridge filterToAttach = new ByReferenceMetadataFilterBridge();
            filterToAttach.setApplicationContext(applicationContext);

            if (filter == null) {
                resolver.setMetadataFilter(filterToAttach);
            } else if (filter instanceof MetadataFilterChain) {
                ((MetadataFilterChain) filter).getFilters().add(filterToAttach);
            } else {
                final MetadataFilterChain chain = new MetadataFilterChain();
                chain.setFilters(Arrays.asList(filter, filterToAttach));
                resolver.setMetadataFilter(chain);
            }
        }

        return resolver;
    }

    /** {@inheritDoc} */
    @Override public Object postProcessAfterInitialization(final Object bean, final String beanName) {
        return bean;
    }

}