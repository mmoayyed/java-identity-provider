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

package net.shibboleth.idp.saml.metadata;

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

import net.shibboleth.ext.spring.service.AbstractServiceableComponent;
import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.saml.metadata.resolver.ClearableMetadataResolver;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.RefreshableMetadataResolver;
import org.opensaml.saml.metadata.resolver.filter.MetadataFilter;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class exists primarily to allow the parsing of relying-party.xml to create a serviceable implementation of
 * {@link MetadataResolver}.
 */
public class RelyingPartyMetadataProvider extends AbstractServiceableComponent<MetadataResolver> implements
        RefreshableMetadataResolver, ClearableMetadataResolver, Comparable<RelyingPartyMetadataProvider> {

    /** If we autogenerate a sort key it comes from this count. */
    private static int sortKeyValue;

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(RelyingPartyMetadataProvider.class);

    /** The embedded resolver. */
    @NonnullAfterInit private MetadataResolver resolver;

    /** The key by which we sort the provider. */
    @NonnullAfterInit private Integer sortKey;

    /**
     * Constructor.
     * 
     * @param child The {@link MetadataResolver} to embed.
     * @deprecated use properties and {@link #RelyingPartyMetadataProvider()}.
     */
    @Deprecated public RelyingPartyMetadataProvider(
            @Nonnull @ParameterName(name="child") final MetadataResolver child) {
        DeprecationSupport.warnOnce(ObjectType.METHOD, "RelyingPartyMetadataProvider(MetadataResolver)", null, null);
        resolver = Constraint.isNotNull(child, "MetadataResolver cannot be null");
    }
    
    /** Constructor. */
    public RelyingPartyMetadataProvider() {
    }

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
     * @return the parameter we got as a constructor
     */
    @Nonnull public MetadataResolver getEmbeddedResolver() {
        return resolver;
    }

    /** {@inheritDoc} */
    @Override @Nonnull public Iterable<EntityDescriptor> resolve(@Nullable final CriteriaSet criteria)
            throws ResolverException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        return resolver.resolve(criteria);
    }

    /** {@inheritDoc} */
    @Override @Nullable public EntityDescriptor resolveSingle(@Nullable final CriteriaSet criteria)
            throws ResolverException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        return resolver.resolveSingle(criteria);
    }

    /** {@inheritDoc} */
    @Override public boolean isRequireValidMetadata() {
        return resolver.isRequireValidMetadata();
    }

    /** {@inheritDoc} */
    @Override public void setRequireValidMetadata(final boolean requireValidMetadata) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        resolver.setRequireValidMetadata(requireValidMetadata);

    }

    /** {@inheritDoc} */
    @Override @Nullable public MetadataFilter getMetadataFilter() {
        return resolver.getMetadataFilter();
    }

    /** {@inheritDoc} */
    @Override public void setMetadataFilter(@Nullable final MetadataFilter newFilter) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        resolver.setMetadataFilter(newFilter);
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        setId(resolver.getId());
        super.doInitialize();
        if (null == resolver) {
            throw new ComponentInitializationException("MetadataResolver cannot be null");
        }

        if (null == sortKey) {
            synchronized (this) {
                sortKeyValue++;
                sortKey = sortKeyValue;
            }
            log.info("Top level Metadata Provider '{}' did not have a sort key; giving it value '{}'",
                    getId(), sortKey);
        }
    }

    /** {@inheritDoc} */
    @Override @Nonnull public MetadataResolver getComponent() {
        return this;
    }

    /** {@inheritDoc} */
    public void clear() throws ResolverException {
        if (resolver instanceof ClearableMetadataResolver) {
            ((ClearableMetadataResolver) resolver).clear();
        }
    }

    /** {@inheritDoc} */
    public void clear(final String entityID) throws ResolverException {
        if (resolver instanceof ClearableMetadataResolver) {
            ((ClearableMetadataResolver) resolver).clear(entityID);
        }
    }

    /** {@inheritDoc} */
    @Override public void refresh() throws ResolverException {
        if (resolver instanceof RefreshableMetadataResolver) {
            ((RefreshableMetadataResolver) resolver).refresh();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Instant getLastRefresh() {
        if (resolver instanceof RefreshableMetadataResolver) {
            return ((RefreshableMetadataResolver) resolver).getLastRefresh();
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Instant getLastUpdate() {
        if (resolver instanceof RefreshableMetadataResolver) {
            return ((RefreshableMetadataResolver) resolver).getLastUpdate();
        } else {
            return null;
        }
    }
    
    /** {@inheritDoc} */
    @Override public int compareTo(final RelyingPartyMetadataProvider other) {
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
        if (!(other instanceof RelyingPartyMetadataProvider)) {
            return false;
        }
        final RelyingPartyMetadataProvider otherRp = (RelyingPartyMetadataProvider) other;
        
        return Objects.equals(otherRp.sortKey, sortKey) && Objects.equals(getId(), otherRp.getId());
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return com.google.common.base.Objects.hashCode(sortKey, getId());
    }

}
