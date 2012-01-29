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

import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.logic.Assert;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * A proxy which wraps a resolved data connector and always returns the same attributes. The goal being that once a data
 * connector is resolved this can be used in its place and calls to
 * {@link BaseDataConnector#resolve(AttributeResolutionContext)} are "free".
 * 
 * This proxy is immutable so all setter methods simply return.
 */
@ThreadSafe
public final class ResolvedDataConnector extends BaseDataConnector {

    /** The data connector that was resolved to produce the attributes. */
    private final BaseDataConnector resolvedConnector;

    /** The attributes produced by the resolved data connector. */
    private final Optional<Map<String, Attribute>> resolvedAttributes;

    /**
     * Constructor.
     * 
     * @param connector data connector that was resolved to produce the attributes
     * @param attributes attributes produced by the resolved data connector
     */
    public ResolvedDataConnector(@Nonnull BaseDataConnector connector,
            @Nonnull Optional<Map<String, Attribute>> attributes) {
        resolvedConnector = Assert.isNotNull(connector, "Resolved data connector can not be null");
        resolvedAttributes = Assert.isNotNull(attributes, "Resolved attributes can not be null");
    }

    /** {@inheritDoc} */
    @Nonnull protected Optional<Map<String, Attribute>> doDataConnectorResolve(
            AttributeResolutionContext resolutionContext) throws AttributeResolutionException {
        return resolvedAttributes;
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        return resolvedConnector.equals(obj);
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements public Set<ResolverPluginDependency> getDependencies() {
        return resolvedConnector.getDependencies();
    }

    /** {@inheritDoc} */
    @Nonnull public Predicate<AttributeResolutionContext> getActivationCriteria() {
        return Predicates.alwaysTrue();
    }

    /** {@inheritDoc} */
    @Nonnull public Optional<String> getFailoverDataConnectorId() {
        return Optional.absent();
    }

    /** {@inheritDoc} */
    @Nonnull public String getId() {
        return resolvedConnector.getId();
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return resolvedConnector.hashCode();
    }

    /** {@inheritDoc} */
    public boolean isPropagateResolutionExceptions() {
        return resolvedConnector.isPropagateResolutionExceptions();
    }

    /** {@inheritDoc} */
    public void setFailoverDataConnectorId(String id) {
        return;
    }

    /** {@inheritDoc} */
    public void setPropagateResolutionExceptions(boolean propagate) {
        return;
    }

    /** {@inheritDoc} */
    @Nonnull public String toString() {
        return resolvedConnector.toString();
    }

    /**
     * Gets the wrapped data connector that was resolved.
     * 
     * @return the resolved data connector
     */
    @Nonnull public BaseDataConnector unwrap() {
        return resolvedConnector;
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        return;
    }

    /** {@inheritDoc} */
    public boolean isInitialized() {
        return true;
    }
}