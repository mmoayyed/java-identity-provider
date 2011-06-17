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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.ComponentValidationException;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeEncoder;

import org.opensaml.util.Assert;
import org.opensaml.util.collections.LazyList;
import org.springframework.expression.Expression;

/**
 * A proxy which wraps a resolved attribute definition and always returns the same attribute. The goal being that once
 * an attribute definition is resolved once this can be used in its place and calls to
 * {@link BaseAttributeDefinition#resolve(AttributeResolutionContext)} are "free".
 * 
 * This proxy is immutable so all setter methods simply return.
 */
@ThreadSafe
public class ResolvedAttributeDefinition extends BaseAttributeDefinition {

    /** The attribute definition that was resolved to produce the attribute. */
    private final BaseAttributeDefinition resolvedDefinition;

    /** The attribute produced by the resolved attribute definition. */
    private final Attribute<?> resolvedAttribute;

    /**
     * Constructor.
     * 
     * @param definition attribute definition that was resolved to produce the given attribute, may not be null
     * @param attribute attribute produced by the given attribute definition, may be null
     */
    public ResolvedAttributeDefinition(BaseAttributeDefinition definition, Attribute<?> attribute) {
        super(definition.getId());

        Assert.isNotNull(definition, "Wrapped attribute definition may not be null");
        resolvedDefinition = definition;

        resolvedAttribute = attribute;
    }

    /** {@inheritDoc} */
    protected Attribute<?> doAttributeResolution(AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        return resolvedAttribute;
    }

    /** {@inheritDoc} */
    protected Attribute<?> doResolve(AttributeResolutionContext resolutionContext) {
        return resolvedAttribute;
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        return resolvedDefinition.equals(obj);
    }

    /** {@inheritDoc} */
    public Set<AttributeEncoder> getAttributeEncoders() {
        return resolvedDefinition.getAttributeEncoders();
    }

    /** {@inheritDoc} */
    public Set<ResolverPluginDependency> getDependencies() {
        return resolvedDefinition.getDependencies();
    }

    /** {@inheritDoc} */
    public Map<Locale, String> getDisplayDescriptions() {
        return resolvedAttribute.getDisplayDescriptions();
    }

    /** {@inheritDoc} */
    public Map<Locale, String> getDisplayNames() {
        return resolvedDefinition.getDisplayNames();
    }

    /** {@inheritDoc} */
    public Expression getEvaluationCondition() {
        return null;
    }

    /** {@inheritDoc} */
    public String getId() {
        return resolvedDefinition.getId();
    }

    /**
     * Gets the resolved attribute.
     * 
     * @return resolved attribute, or null
     */
    public Attribute<?> getResolvedAttribute() {
        return resolvedAttribute;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return resolvedDefinition.hashCode();
    }

    /** {@inheritDoc} */
    public boolean isApplicable(AttributeResolutionContext resolutionContext) throws AttributeResolutionException {
        return true;
    }

    /** {@inheritDoc} */
    public boolean isDependencyOnly() {
        return resolvedDefinition.isDependencyOnly();
    }

    /** {@inheritDoc} */
    public boolean isPropagateEvaluationConditionExceptions() {
        return resolvedDefinition.isPropagateEvaluationConditionExceptions();
    }

    /** {@inheritDoc} */
    public boolean isPropagateResolutionExceptions() {
        return resolvedDefinition.isPropagateResolutionExceptions();
    }

    /** {@inheritDoc} */
    public void setAttributeEncoders(LazyList<AttributeEncoder> attributeEncoders) {
        return;
    }

    /** {@inheritDoc} */
    public void setDependencies(List<ResolverPluginDependency> pluginDependencies) {
        return;
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
    public void setEvaluationCondition(Expression condition) {
        return;
    }

    /** {@inheritDoc} */
    public void setPropagateEvaluationConditionExceptions(boolean propagate) {
        return;
    }

    /** {@inheritDoc} */
    public void setPropagateResolutionExceptions(boolean propagate) {
        return;
    }

    /** {@inheritDoc} */
    public String toString() {
        return resolvedDefinition.toString();
    }

    /**
     * Gets the wrapped attribute definition that was resolved.
     * 
     * @return the resolved attribute definition
     */
    public BaseAttributeDefinition unwrap() {
        return resolvedDefinition;
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        return;
    }
}