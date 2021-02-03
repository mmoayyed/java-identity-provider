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

package net.shibboleth.idp.attribute.resolver.ad.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.UnsupportedAttributeTypeException;
import net.shibboleth.idp.attribute.resolver.AbstractAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.ResolvedAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolverAttributeDefinitionDependency;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * An attribute definition that creates {@link ScopedStringAttributeValue}s by taking a source attribute value and
 * applying a static scope to each.
 */
@ThreadSafe
public class ScopedAttributeDefinition extends AbstractAttributeDefinition {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ScopedAttributeDefinition.class);

    /** Scope value. Mutually exclusive with {@link #scopeSource} */
    @Nullable private String scope;

    /** Scope source. Mutually exclusive with {@link #scope} */
    @Nullable private String scopeSource;

    /** The attribute dependencies mine the scopeSource attribute (if there is one). */
    @NonnullAfterInit private Collection<ResolverAttributeDefinitionDependency> nonScopeAttributeDependencies;

    /**
     * Get scope value.
     *
     * @return Returns the scope.
     */
    @Nullable public String getScope() {
        return scope;
    }

    /**
     * Set the scope for this definition.
     *
     * @param newScope what to set.
     */
    public void setScope(@Nonnull @NotEmpty final String newScope) {
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        scope = Constraint.isNotNull(StringSupport.trimOrNull(newScope), "Scope can not be null or empty");
    }

    /**
     * Get scope source (attribute id).
     *
     * @return Returns the scope.
     */
    @Nullable public String getScopeSource() {
        return scopeSource;
    }

    /**
     * Set the source of the scope for this definition.
     *
     * @param attributeId what to set.
     */
    public void setScopeSource(@Nonnull @NotEmpty final String attributeId) {
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        scopeSource = Constraint.isNotNull(
                StringSupport.trimOrNull(attributeId), "ScopeSource can not be null or empty");
    }

    /** Lookup the source attribute in the resolved list.
     * @param workContext where to look
     * @return the single string value contained in the attribute
     * @throws ResolutionException if the attribute was not there or if it didn't have only one
     * string value
     */
    private String getScopeFromSource(@Nonnull final AttributeResolverWorkContext workContext)
            throws ResolutionException{
        final ResolvedAttributeDefinition resolved =
                workContext.getResolvedIdPAttributeDefinitions().get(getScopeSource());
        if (resolved == null) {
            log.error("{} Scope source '{}' not found in resolved dependencies", getLogPrefix(), getScopeSource());
            log.debug("{} Attributes available {}", getLogPrefix(),
                    workContext.getResolvedIdPAttributeDefinitions().entrySet());
            throw new ResolutionException("Scope source not found in resolved dependencies");
        }
        final List<IdPAttributeValue> values = resolved.getResolvedAttribute().getValues();
        if (values.size() != 1) {
            log.error("{} Exactly one value required for {}, {} found", getLogPrefix(),
                    getScopeSource(), values.size());
            log.debug("{} Values returned {}",  getLogPrefix(), values);
            throw new ResolutionException("Exactly one value for scope source required");
        }
        final IdPAttributeValue value = values.get(0);
        if ((value instanceof StringAttributeValue) && !(value instanceof ScopedStringAttributeValue)) {
            return ((StringAttributeValue) value).getValue();
        }
        log.error("{} Attribute {} must return a StringAttributeValue returned a {}", getLogPrefix(),
                getScopeSource(), value.getClass());
        throw new ResolutionException("SourceAttribute must only return a StringAttributeValue");
    }

    /** {@inheritDoc} */
    @Override @Nonnull protected IdPAttribute doAttributeDefinitionResolve(
            @Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException {

        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        final IdPAttribute resultantAttribute = new IdPAttribute(getId());
        final String scopeValue;
        if (scopeSource == null) {
            scopeValue = scope;
        } else {
            scopeValue = getScopeFromSource(workContext);
        }

        final List<IdPAttributeValue> dependencyValues =
                PluginDependencySupport.getMergedAttributeValues(workContext,
                        nonScopeAttributeDependencies,
                        getDataConnectorDependencies(), 
                        getId());

        final List<IdPAttributeValue> valueList = new ArrayList<>(dependencyValues.size());

        for (final IdPAttributeValue dependencyValue : dependencyValues) {
            if (dependencyValue instanceof EmptyAttributeValue) {
                final EmptyAttributeValue emptyVal = (EmptyAttributeValue) dependencyValue;
                log.debug("{} ignored empty value of type {}", getLogPrefix(), emptyVal.getDisplayValue());
                continue;
            }
            if (!(dependencyValue instanceof StringAttributeValue)) {
                throw new ResolutionException(new UnsupportedAttributeTypeException(getLogPrefix()
                        + "This attribute definition only supports attribute value types of "
                        + StringAttributeValue.class.getName() + " not values of type "
                        + dependencyValue.getClass().getName()));
            }

            valueList.add(new ScopedStringAttributeValue(((StringAttributeValue) dependencyValue).getValue(),
                    scopeValue));
        }
        resultantAttribute.setValues(valueList);
        return resultantAttribute;
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (scope != null) {
            if (scopeSource != null) {
                throw new ComponentInitializationException(getLogPrefix() +
                        "': cannot specify scope and scopeSource");
            }
            nonScopeAttributeDependencies = getAttributeDependencies();
        } else if (scopeSource != null) {
            final ResolverAttributeDefinitionDependency source = new ResolverAttributeDefinitionDependency(scopeSource);
            final HashSet<ResolverAttributeDefinitionDependency> nonScope = new HashSet<>(getAttributeDependencies());
            if (!nonScope.remove(source)) {
                throw new ComponentInitializationException(getLogPrefix() +
                        "': AttributeDependencies did not contain scope source '" +
                        scopeSource + "'");
            }
            nonScopeAttributeDependencies = nonScope;
        } else {
            throw new ComponentInitializationException(getLogPrefix() + "': neither scope now scopeSource configured");
        }

        if (getDataConnectorDependencies().isEmpty() && nonScopeAttributeDependencies.isEmpty()) {
            throw new ComponentInitializationException(getLogPrefix() + "': no actual dependencies were configured");
        }
    }
    
}
