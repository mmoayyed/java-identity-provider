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

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeEncoder;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.logic.Assert;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * A proxy which wraps a resolved attribute definition and always returns the same attribute. The goal being that once
 * an attribute definition is resolved once this can be used in its place and calls to
 * {@link BaseAttributeDefinition#resolve(AttributeResolutionContext)} are "free".
 * 
 * This proxy is immutable so all setter methods simply return.
 */
@ThreadSafe
public final class ResolvedAttributeDefinition extends BaseAttributeDefinition {

    /** The attribute definition that was resolved to produce the attribute. */
    private final BaseAttributeDefinition resolvedDefinition;

    /** The attribute produced by the resolved attribute definition. */
    private final Optional<Attribute> resolvedAttribute;

    /**
     * Constructor.
     * 
     * @param definition attribute definition that was resolved to produce the given attribute
     * @param attribute attribute produced by the given attribute definition
     */
    public ResolvedAttributeDefinition(@Nonnull BaseAttributeDefinition definition,
            @Nonnull Optional<Attribute> attribute) {
        resolvedDefinition = Assert.isNotNull(definition, "Resolved attribute definition can not be null");
        resolvedAttribute = Assert.isNotNull(attribute, "Resolved attribute can not be null");
        Assert.isTrue(definition.isInitialized());
        Assert.isFalse(definition.isDestroyed());
    }

    /** {@inheritDoc} */
    @Nonnull protected Optional<Attribute> doAttributeDefinitionResolve(
            @Nonnull AttributeResolutionContext resolutionContext) throws AttributeResolutionException {
        return resolvedAttribute;
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        return resolvedDefinition.equals(obj);
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements public Set<AttributeEncoder<?>> getAttributeEncoders() {
        return resolvedDefinition.getAttributeEncoders();
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements public Set<ResolverPluginDependency> getDependencies() {
        return resolvedDefinition.getDependencies();
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements public Map<Locale, String> getDisplayDescriptions() {
        return resolvedDefinition.getDisplayDescriptions();
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements public Map<Locale, String> getDisplayNames() {
        return resolvedDefinition.getDisplayNames();
    }

    /** {@inheritDoc} */
    @Nonnull public Predicate<AttributeResolutionContext> getActivationCriteria() {
        return Predicates.alwaysTrue();
    }

    /** {@inheritDoc} */
    @Nonnull public String getId() {
        return resolvedDefinition.getId();
    }

    /**
     * Gets the resolved attribute.
     * 
     * @return resolved attribute, or null
     */
    @Nonnull public Optional<Attribute> getResolvedAttribute() {
        return resolvedAttribute;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return resolvedDefinition.hashCode();
    }

    /** {@inheritDoc} */
    public boolean isDependencyOnly() {
        return resolvedDefinition.isDependencyOnly();
    }

    /** {@inheritDoc} */
    public boolean isPropagateResolutionExceptions() {
        return resolvedDefinition.isPropagateResolutionExceptions();
    }

    /** {@inheritDoc} */
    public void setDependencyOnly(boolean isDependencyOnly) {
        return;
    }

    /** {@inheritDoc} */
    public void setDisplayDescriptions(Map<Locale, String> descriptions) {
        return;
    }

    /** {@inheritDoc} */
    public void setDisplayNames(Map<Locale, String> names) {
        return;
    }

    /** {@inheritDoc} */
    public void setPropagateResolutionExceptions(boolean propagate) {
        return;
    }

    /** {@inheritDoc} */
    @Nonnull public String toString() {
        return resolvedDefinition.toString();
    }

    /**
     * Gets the wrapped attribute definition that was resolved.
     * 
     * @return the resolved attribute definition
     */
    @Nonnull public BaseAttributeDefinition getResolvedDefinition() {
        return resolvedDefinition;
    }

    /** {@inheritDoc} */
    public void doValidate() throws ComponentValidationException {
        super.doValidate();
        return;
    }

    /** {@inheritDoc} */
    public boolean isInitialized() {
        return true;
    }
}