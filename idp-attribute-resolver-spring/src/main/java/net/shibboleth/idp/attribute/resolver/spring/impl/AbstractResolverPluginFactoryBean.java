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

package net.shibboleth.idp.attribute.resolver.spring.impl;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.ext.spring.factory.AbstractComponentAwareFactoryBean;
import net.shibboleth.idp.attribute.resolver.AbstractResolverPlugin;
import net.shibboleth.idp.attribute.resolver.ResolverAttributeDefinitionDependency;
import net.shibboleth.idp.attribute.resolver.ResolverDataConnectorDependency;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;

/**
 * A factory bean to collect the parameterization that goes onto a {@link AbstractResolverPlugin}.
 * 
 * @param <T> The exact type being deployed.
 */
public abstract class AbstractResolverPluginFactoryBean<T extends AbstractResolverPlugin> extends
        AbstractComponentAwareFactoryBean<T> {

    /** The component Id. */
    @Nullable private String componentId;
    
    /** Plugin property "propagateResolutionExceptions". */
    @Nullable private Boolean propagateResolutionExceptions;

    /** Plugin property "profileContextStrategy". */
    @Nullable private Function<AttributeResolutionContext, ProfileRequestContext> profileContextStrategy;

    /** Plugin property "activationCondition". */
    @Nullable private Predicate<ProfileRequestContext> activationCondition;

    /** Plugin property "attributeDependencies". */
    @Nullable private Set<ResolverAttributeDefinitionDependency> attributeDependencies;

    /** Plugin property "dataConnectorDependencies". */
    @Nullable private Set<ResolverDataConnectorDependency> dataConnectorDependencies;

    /** Get the component Id.
     * @return the id.
     */
    @Nullable public String getId() {
        return componentId;
    }
    
    /** Set the component Id.
     * @param id the id.
     */
    @Nullable public void setId(@Nullable final String id) {
        componentId = id;
    }
    
    /**
     * Bean setter in support of {@link AbstractResolverPlugin#setPropagateResolutionExceptions(boolean)}.
     * 
     * @param propagate value to be set
     */
    public void setPropagateResolutionExceptions(final boolean propagate) {
        propagateResolutionExceptions = propagate;
    }

    /**
     * Bean getter in support of {@link AbstractResolverPlugin#setPropagateResolutionExceptions(boolean)}.
     * 
     * @return The value to be set
     */
    @Nullable public Boolean getPropagateResolutionExceptions() {
        return propagateResolutionExceptions;
    }

    /**
     * Bean setter in support of {@link AbstractResolverPlugin#setProfileContextStrategy(Function)}.
     * 
     * @param strategy value to be set
     */
    public void setProfileContextStrategy(
            @Nullable final Function<AttributeResolutionContext, ProfileRequestContext> strategy) {
        profileContextStrategy = strategy;
    }

    /**
     * Bean getter in support of {@link AbstractResolverPlugin#setProfileContextStrategy(Function)}.
     * 
     * @return The value to be set
     */
    public Function<AttributeResolutionContext, ProfileRequestContext> getProfileContextStrategy() {
        return profileContextStrategy;
    }

    /**
     * Bean setter in support of {@link AbstractResolverPlugin#setActivationCondition(Predicate)}.
     * 
     * @param pred what to set
     */
    public void setActivationCondition(@Nullable final Predicate<ProfileRequestContext> pred) {
        activationCondition = pred;
    }

    /**
     * Bean getter in support of {@link AbstractResolverPlugin#setActivationCondition(Predicate)}.
     * 
     * @return The value to be set
     */
    @Nullable public Predicate<ProfileRequestContext> getActivationCondition() {
        return activationCondition;
    }

    /**
     * Bean setter in support of {@link AbstractResolverPlugin#setAttributeDependencies(Set)}.
     * 
     * @param dependencies value to set
     */
    public void setAttributeDependencies(@Nullable final Set<ResolverAttributeDefinitionDependency> dependencies) {

        attributeDependencies = dependencies;
    }
    
    /**
     * Bean setter in support of {@link AbstractResolverPlugin#setDataConnectorDependencies(Set)}.
     * 
     * @param dependencies value to set
     */
    public void setDataConnectorDependencies(@Nullable final Set<ResolverDataConnectorDependency> dependencies) {

        dataConnectorDependencies = dependencies;
    }

    /**
     * Bean getter in support of {@link AbstractResolverPlugin#setAttributeDependencies(Set)}.
     * 
     * @return The value
     */
    @Nullable public Set<ResolverAttributeDefinitionDependency> getAttributeDependencies() {
        return attributeDependencies;
    }

    /**
     * Bean getter in support of {@link AbstractResolverPlugin#setDataConnectorDependencies(Set)}.
     * 
     * @return The value
     */
    @Nullable public Set<ResolverDataConnectorDependency> getDataConnectorDependencies() {
        return dataConnectorDependencies;
    }

    /** 
     * Set the locally define values into the object under construction.
     * @param what the object being built.
     */
    protected void setValues(@Nonnull final T what) {   
        if (null != getId()) {
            what.setId(getId());
        }
        if (null != getActivationCondition()) {
            what.setActivationCondition(getActivationCondition());
        }
        if (null != getAttributeDependencies()) {
            what.setAttributeDependencies(getAttributeDependencies());
        }
        if (null != getDataConnectorDependencies()) {
            what.setDataConnectorDependencies(getDataConnectorDependencies());
        }
        if (null != getProfileContextStrategy()) {
            what.setProfileContextStrategy(getProfileContextStrategy());
        }
        if (null != getPropagateResolutionExceptions()) {
            what.setPropagateResolutionExceptions(getPropagateResolutionExceptions());
        }
    }
    
}