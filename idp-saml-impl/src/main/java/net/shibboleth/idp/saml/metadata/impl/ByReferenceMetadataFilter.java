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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.metadata.resolver.filter.FilterException;
import org.opensaml.saml.metadata.resolver.filter.MetadataFilter;
import org.opensaml.saml.metadata.resolver.filter.MetadataFilterContext;
import org.opensaml.saml.metadata.resolver.filter.data.impl.MetadataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * A Spring-aware {@link MetadataFilter} that associates other filters with specific
 * {@link MetadataResolver} instances by ID.
 * 
 * <p>The {@link MetadataFilterContext} is used to identify which resolver is actually
 * running, to properly identify which filters to apply.</p>
 * 
 * @since 4.0.0
 */
public class ByReferenceMetadataFilter implements MetadataFilter {

    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(ByReferenceMetadataFilter.class);
    
    /** Map of resolver names to filters. */
    @Nonnull @NonnullElements private Map<String,MetadataFilter> filterMap;
    
    /** Constructor. */
    public ByReferenceMetadataFilter() {
        filterMap = Collections.emptyMap();
    }
    
    /**
     * Mapping of resolver names to filters to run.
     * 
     * @param map filter mappings
     */
    public void setFilterMappings(@Nonnull @NonnullElements final Map<String,MetadataFilter> map) {
        Constraint.isNotNull(map, "Filter mappings cannot be null");
        
        filterMap = new HashMap<>(map.size());
        for (final Map.Entry<String,MetadataFilter> entry : map.entrySet()) {
            final String trimmed = StringSupport.trimOrNull(entry.getKey());
            if (trimmed != null && entry.getValue() != null) {
                filterMap.put(trimmed, entry.getValue());
            }
        }
    }
    
    /** {@inheritDoc} */
    public XMLObject filter(@Nullable final XMLObject metadata, @Nonnull final MetadataFilterContext context)
            throws FilterException {
        
        final MetadataSource source = context.get(MetadataSource.class);
        if (source == null || source.getSourceId() == null) {
            log.debug("No metadata source ID found in MetadataFilterContext");
            return metadata;
        }
        
        final MetadataFilter filter = filterMap.get(source.getSourceId());
        if (filter == null) {
            log.debug("No filters defined for resolver '{}', by-reference filter inactive", source.getSourceId());
            return metadata;
        }
        
        log.debug("Applying by-reference filter to metadata resolver '{}'", source.getSourceId());
        return filter.filter(metadata, context);
    }

}