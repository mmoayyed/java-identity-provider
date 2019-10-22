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

package net.shibboleth.idp.saml.metadata.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.metadata.resolver.filter.FilterException;
import org.opensaml.saml.metadata.resolver.filter.MetadataFilter;
import org.opensaml.saml.metadata.resolver.filter.MetadataFilterChain;
import org.opensaml.saml.metadata.resolver.filter.MetadataFilterContext;
import org.opensaml.saml.metadata.resolver.filter.impl.ByReferenceMetadataFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

/**
 * This is a bridge filter that uses Spring to locate extant {@link ByReferenceMetadataFilter}
 * objects to run.
 */
public class ByReferenceMetadataFilterBridge implements MetadataFilter {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ByReferenceMetadataFilterBridge.class);

    /** Application context. */
    @Nullable private ApplicationContext applicationContext;
    
    /** Chain to wrap the beans obtained from the context. */
    @Nullable private MetadataFilterChain filterChain;
    
    /**
     * Set the containing {@link ApplicationContext}.
     * 
     * @param context the context
     */
    public void setApplicationContext(@Nullable final ApplicationContext context) {
        applicationContext = context;
    }
    
    /** {@inheritDoc} */
    public XMLObject filter(@Nullable final XMLObject metadata, @Nonnull final MetadataFilterContext context)
            throws FilterException {

        MetadataFilterChain chain = null;
        
        synchronized(this) {
            if (filterChain != null) {
                chain = filterChain;
            } else if (applicationContext != null) {
                try {
                    final Map<String,ByReferenceMetadataFilter> beans =
                            applicationContext.getBeansOfType(ByReferenceMetadataFilter.class);
                    log.debug("Bridging to {} ByReference filters in Spring context", beans.size());
                    filterChain = new MetadataFilterChain();
                    filterChain.setFilters(List.copyOf(beans.values()));
                    chain = filterChain;
                } catch (final BeansException e) {
                    throw new FilterException(e);
                }
            } else {
                throw new FilterException("ApplicationContext is not set");
            }
        }
        
        return chain != null ? chain.filter(metadata, context) : metadata;
    }

}