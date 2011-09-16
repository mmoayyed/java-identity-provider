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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.jcip.annotations.ThreadSafe;

import org.opensaml.util.collections.CollectionSupport;
import org.opensaml.util.component.AbstractIdentifiableInitializableComponent;
import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.component.ComponentSupport;
import org.opensaml.util.component.ComponentValidationException;
import org.opensaml.util.component.DestructableComponent;
import org.opensaml.util.component.UnmodifiableComponent;
import org.opensaml.util.component.UnmodifiableComponentException;
import org.opensaml.util.component.ValidatableComponent;
import org.opensaml.util.criteria.EvaluableCriterion;
import org.opensaml.util.criteria.EvaluationException;
import org.opensaml.util.criteria.StaticResponseEvaluableCriterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all {@link ResolutionPlugIn}s.
 * 
 * @param <ResolvedType> object type this plug-in resolves to
 */
@ThreadSafe
public abstract class BaseResolverPlugin<ResolvedType> extends AbstractIdentifiableInitializableComponent implements
        ValidatableComponent, UnmodifiableComponent, DestructableComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(BaseResolverPlugin.class);

    /** Whether an {@link AttributeResolutionContext} that occurred resolving attributes will be re-thrown. */
    private boolean propagateResolutionExceptions;

    /** Criterion that must be met for this plugin to be active for the given request. */
    private EvaluableCriterion<AttributeResolutionContext> activationCriteria =
            StaticResponseEvaluableCriterion.TRUE_RESPONSE;

    /** IDs of the {@link ResolutionPlugIn}s this plug-in depends on. */
    private Set<ResolverPluginDependency> dependencies = Collections.emptySet();

    /** {@inheritDoc} */
    public synchronized void setId(final String componentId) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Attribute resolver plugin " + getId()
                    + " has already been initialized, plugin ID can not be changed.");
        }
        
        super.setId(componentId);
    }

    /**
     * Gets whether an {@link AttributeResolutionContext} that occurred resolving attributes will be re-thrown. Doing so
     * will cause the entire attribute resolution request to fail.
     * 
     * @return true if {@link AttributeResolutionException}s are propagated, false if not
     */
    public boolean isPropagateResolutionExceptions() {
        return propagateResolutionExceptions;
    }

    /**
     * Sets whether an {@link AttributeResolutionContext} that occurred resolving attributes will be re-thrown. Doing so
     * will cause the entire attribute resolution request to fail.
     * 
     * @param propagate true if {@link AttributeResolutionException}s are propagated, false if not
     */
    public synchronized void setPropagateResolutionExceptions(final boolean propagate) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Attribute resolver plugin " + getId()
                    + " has already been initialized, resolution exception propagation can not be changed.");
        }

        propagateResolutionExceptions = propagate;
    }

    /**
     * Gets the criteria that must be met for this plugin to be active for a given request.
     * 
     * @return criteria that must be met for this plugin to be active for a given request, never null
     */
    public EvaluableCriterion<AttributeResolutionContext> getActivationCriteria() {
        return activationCriteria;
    }

    /**
     * Sets the criteria that must be met for this plugin to be active for a given request.
     * 
     * @param criteria criteria that must be met for this plugin to be active for a given request
     */
    public synchronized void setActivationCriteria(final EvaluableCriterion<AttributeResolutionContext> criteria) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Attribute resolver plugin " + getId()
                    + " has already been initialized, resolution exception propagation can not be changed.");
        }

        if (criteria == null) {
            activationCriteria = StaticResponseEvaluableCriterion.TRUE_RESPONSE;
        } else {
            activationCriteria = criteria;
        }
    }

    /**
     * Gets the unmodifiable list of dependencies for this plugin.
     * 
     * @return unmodifiable list of dependencies for this plugin, never null
     */
    public Set<ResolverPluginDependency> getDependencies() {
        return dependencies;
    }

    /**
     * Sets the list of dependencies for this plugin.
     * 
     * @param pluginDependencies unmodifiable list of dependencies for this plugin
     */
    public synchronized void setDependencies(final Collection<ResolverPluginDependency> pluginDependencies) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Attribute resolver plugin " + getId()
                    + " has already been initialized, resolution exception propagation can not be changed.");
        }

        HashSet<ResolverPluginDependency> checkedDependencies =
                CollectionSupport.addNonNull(pluginDependencies, new HashSet<ResolverPluginDependency>());
        if (checkedDependencies.isEmpty()) {
            dependencies = Collections.emptySet();
        } else {
            dependencies = Collections.unmodifiableSet(checkedDependencies);
        }
    }

    /**
     * Checks to see if the current resolution context meets the evaluation condition for this plugin. If it does the
     * plugin should be resolved for this request, if it does not the plugin should not be resolved for this request.
     * 
     * If no evaluation condition is set for this plugin this method return true.
     * 
     * @param resolutionContext current resolution context
     * 
     * @return true if the current resolution context meets the requirements for this plugin, false if not
     */
    public boolean isApplicable(final AttributeResolutionContext resolutionContext) {
        try {
            if (activationCriteria.evaluate(resolutionContext)) {
                return true;
            }
        } catch (EvaluationException e) {
            log.warn("Error evaluating plugin applicability criteria", e);
        }

        return false;
    }

    /**
     * Performs the attribute resolution for this plugin.
     * 
     * If {@link #isApplicable(AttributeResolutionContext)} returns false this method returns null. Otherwise
     * {@link #doResolve(AttributeResolutionContext)} is invoked. If an exception is thrown and
     * {@link #isPropagateResolutionExceptions()} is false the exception is logged but not re-thrown, otherwise it is
     * re-thrown.
     * 
     * @param resolutionContext the context for the resolution
     * 
     * @return the attributes made available by the resolution, or null if no attributes were resolved
     * 
     * @throws AttributeResolutionException thrown if there was an error checking the evaluation condition, if one
     *             exists, or if there was a problem resolving the attributes
     */
    public final ResolvedType resolve(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        if (!isInitialized()) {
            throw new AttributeResolutionException("Resolver plugin" + getId()
                    + " has not be initialized and can not yet be used");
        }

        if (!isApplicable(resolutionContext)) {
            return null;
        }

        try {
            return doResolve(resolutionContext);
        } catch (AttributeResolutionException e) {
            if (propagateResolutionExceptions) {
                throw e;
            } else {
                return null;
            }
        }
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        ComponentSupport.validate(activationCriteria);
    }

    /** {@inheritDoc} */
    public synchronized void destroy() {
        ComponentSupport.destroy(activationCriteria);
        activationCriteria = StaticResponseEvaluableCriterion.FALSE_RESPONSE;
        dependencies = Collections.emptySet();
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        ComponentSupport.initialize(activationCriteria);
    }

    /**
     * Perform the actual resolution. The resolved attribute(s) should not be recorded in the resolution context.
     * 
     * @param resolutionContext current resolution context
     * 
     * @return the resolved attributes or null if no attributes were resolved
     * 
     * @throws AttributeResolutionException thrown if there is a problem resolving the attributes
     * 
     * @see BaseResolverPlugin#resolve(AttributeResolutionContext)
     */
    protected abstract ResolvedType doResolve(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException;
}