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

import java.util.Objects;

import javax.annotation.Nonnull;

import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.ext.spring.service.AbstractServiceableComponent;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;


/**
 * This class is a sortable container of {@link MetadataResolver}s, wrapped into a serviceable component.
 */
public class MetadataProviderContainer extends AbstractServiceableComponent<MetadataResolver>
                                       implements Comparable<MetadataProviderContainer> {

    /** If we autogenerate a sort key it comes from this count. */
    private static int sortKeyValue;

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(MetadataProviderContainer.class);

    /** The embedded resolver. */
    @NonnullAfterInit private MetadataResolver resolver;

    /** The key by which we sort the provider. */
    @NonnullAfterInit private Integer sortKey;


    /**
     * Set the sort key.
     * 
     * @param key what to set
     */
    public void setSortKey(final int key) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        sortKey = key;
    }
    
    /**
     * Set the {@link MetadataResolver} to embed.
     * 
     * @param theResolver The {@link MetadataResolver} to embed.
     */
    @Nonnull public void setEmbeddedResolver(@Nonnull final MetadataResolver theResolver) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        resolver = Constraint.isNotNull(theResolver, "MetadataResolver cannot be null");
    }

    /**
     * Return what we are build around. Used for testing.
     * 
     * @return the contained resolver.
     */
    @Nonnull public MetadataResolver getEmbeddedResolver() {
        return resolver;
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        setId(resolver.getId());
        super.doInitialize();
        if (null == resolver) {
            throw new ComponentInitializationException("MetadataResolver cannot be null");
        }

        if (null == sortKey) {
            synchronized (MetadataProviderContainer.class) {
                sortKeyValue++;
                sortKey = sortKeyValue;
            }
            log.info("Top level Metadata Provider '{}' did not have a sort key; giving it value '{}'",
                    getId(), sortKey);
        }
    }
    
    /** {@inheritDoc} */
    @Override public int compareTo(final MetadataProviderContainer other) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        final int result = sortKey.compareTo(other.sortKey);
        if (result != 0) {
            return result;
        }
        if (equals(other)) {
            return 0;
        }
        return getId().compareTo(other.getId());
    }

    /**
     * {@inheritDoc}. We are within a spring context and so equality can be determined by ID, however we also test by
     * sortKey just in case.
     */
    @Override public boolean equals(final Object other) {
        if (null == other) {
            return false;
        }
        if (!(other instanceof MetadataProviderContainer)) {
            return false;
        }
        final MetadataProviderContainer otherRp = (MetadataProviderContainer) other;
        
        return Objects.equals(otherRp.sortKey, sortKey) && Objects.equals(getId(), otherRp.getId());
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return com.google.common.base.Objects.hashCode(sortKey, getId());
    }

    /** {@inheritDoc} */
    public MetadataResolver getComponent() {
        return getEmbeddedResolver();
    }
}
