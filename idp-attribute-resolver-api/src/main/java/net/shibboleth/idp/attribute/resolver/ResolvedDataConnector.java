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

package net.shibboleth.idp.attribute.resolver;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * A proxy which wraps a resolved data connector and always returns the same attributes. The goal being that once a data
 * connector is resolved this can be used in its place and calls to
 * {@link DataConnector#resolve(AttributeResolutionContext)} are "free".
 * 
 * This proxy is immutable so all setter methods simply return.
 */
@ThreadSafe
public final class ResolvedDataConnector extends AbstractDataConnector {

    /** The data connector that was resolved to produce the attributes. */
    @Nonnull private final DataConnector resolvedConnector;

    /** The attributes produced by the resolved data connector. */
    @Nullable private final Map<String, IdPAttribute> resolvedAttributes;

    /**
     * Constructor.
     * 
     * @param connector data connector that was resolved to produce the attributes
     * @param attributes attributes produced by the resolved data connector
     */
    public ResolvedDataConnector(@Nonnull final DataConnector connector,
            @Nullable final Map<String, IdPAttribute> attributes) {
        resolvedConnector = Constraint.isNotNull(connector, "Resolved data connector cannot be null");
        resolvedAttributes = attributes;
        Constraint.isTrue(connector.isInitialized(), "Provided connector should be initialized");
        Constraint.isFalse(connector.isDestroyed(), "Provided connector must not be destroyed");
    }

    /** {@inheritDoc} */
    @Override @Nullable protected Map<String, IdPAttribute> doDataConnectorResolve(
            @Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException {
        return resolvedAttributes;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(final Object obj) {
        return resolvedConnector.equals(obj);
    }

    /** {@inheritDoc} */
    @Override @Nonnull @NonnullElements public Set<ResolverAttributeDefinitionDependency> getAttributeDependencies() {
        return resolvedConnector.getAttributeDependencies();
    }

    /** {@inheritDoc} */
    @Override @Nonnull @NonnullElements public Set<ResolverDataConnectorDependency> getDataConnectorDependencies() {
        return resolvedConnector.getDataConnectorDependencies();
    }

    /** {@inheritDoc} */
    @Override @Nullable public Predicate<ProfileRequestContext> getActivationCondition() {
        return null;
    }

    /** {@inheritDoc} */
    @Override @Nullable public String getFailoverDataConnectorId() {
        return null;
    }

    /** {@inheritDoc} */
    @Override @Nonnull public String getId() {
        return resolvedConnector.getId();
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return resolvedConnector.hashCode();
    }

    /** {@inheritDoc} */
    @Override public boolean isPropagateResolutionExceptions() {
        return resolvedConnector.isPropagateResolutionExceptions();
    }

    /** {@inheritDoc} */
    @Override public void setFailoverDataConnectorId(final String id) {
        return;
    }

    /** {@inheritDoc} */
    @Override public void setPropagateResolutionExceptions(final boolean propagate) {
        return;
    }

    /** {@inheritDoc} */
    @Override @Nonnull public String toString() {
        return resolvedConnector.toString();
    }

    /** {@inheritDoc} */
    @Override public void setExportAllAttributes(@Nullable final boolean what) {
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
    }

    /** {@inheritDoc} */
    @Override public boolean isExportAllAttributes() {
        return resolvedConnector.isExportAllAttributes();
    }

    /** {@inheritDoc} */
    @Override public void setExportAttributes(@Nonnull final Collection<String> what) {
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
    }

    /** {@inheritDoc} */
    @Override @Nonnull @NonnullElements @Unmodifiable public Collection<String> getExportAttributes() {
        return resolvedConnector.getExportAttributes();
    }


    /**
     * Gets the resolved attributes.
     * 
     * @return the resolved attributes
     */
    @Nullable public Map<String, IdPAttribute> getResolvedAttributes() {
        return resolvedAttributes;
    }

    /**
     * Gets the wrapped data connector that was resolved.
     * 
     * @return the resolved data connector
     */
    @Nonnull public DataConnector getResolvedConnector() {
        return resolvedConnector;
    }

    /** {@inheritDoc} */
    @Override public boolean isInitialized() {
        return true;
    }

}