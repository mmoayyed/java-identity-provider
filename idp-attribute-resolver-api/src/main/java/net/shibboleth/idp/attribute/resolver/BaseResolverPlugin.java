/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Set;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.AbstractComponent;

import org.opensaml.util.collections.LazySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;

/**
 * Base class for all {@link ResolutionPlugIn}s.
 * 
 * @param <ResolvedType> object type this plug-in resolves to
 */
@ThreadSafe
public abstract class BaseResolverPlugin<ResolvedType> extends AbstractComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(BaseResolverPlugin.class);

    /**
     * Whether an {@link EvaluationException} that occurred when checking this plugins evaluation condition is re-thrown
     * as a {@link AttributeResolutionException}.
     */
    private boolean propagateEvaluationConditionExceptions;

    /** Whether an {@link AttributeResolutionContext} that occurred resolving attributes will be re-thrown. */
    private boolean propagateResolutionExceptions;

    /** Condition, whose result must be a {@link Boolean}, under which this plugin will be resolved. */
    private Expression evaluationCondition;

    /** IDs of the {@link ResolutionPlugIn}s this plug-in depends on. */
    private Set<ResolverPluginDependency> dependencies;

    /**
     * Constructor.
     * 
     * @param pluginId unique identifier for the plugin, never null or empty
     */
    public BaseResolverPlugin(final String pluginId) {
        super(pluginId);

        propagateEvaluationConditionExceptions = false;
        propagateResolutionExceptions = false;
        evaluationCondition = null;
        dependencies = new LazySet<ResolverPluginDependency>();
    }

    /**
     * Gets whether an {@link EvaluationException} that occurred when checking this plugins evaluation condition is
     * re-thrown as a {@link AttributeResolutionException}. Doing so will cause the entire attribute resolution request
     * to fail.
     * 
     * @return true if {@link EvaluationException}s are propagated, false if not
     */
    public boolean isPropagateEvaluationConditionExceptions() {
        return propagateEvaluationConditionExceptions;
    }

    /**
     * Sets whether an {@link EvaluationException} that occurred when checking this plugins evaluation condition is
     * re-thrown as a {@link AttributeResolutionException}. Doing so will cause the entire attribute resolution request
     * to fail.
     * 
     * @param propagate true if {@link EvaluationException}s are propagated, false if not
     */
    public void setPropagateEvaluationConditionExceptions(final boolean propagate) {
        propagateEvaluationConditionExceptions = propagate;
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
    public void setPropagateResolutionExceptions(final boolean propagate) {
        propagateResolutionExceptions = propagate;
    }

    /**
     * Gets the condition which must be met for this plugin to be resolved for a given request.
     * 
     * @return condition which must be met for this plugin to be resolved for a given request
     */
    public Expression getEvaluationCondition() {
        return evaluationCondition;
    }

    /**
     * Sets the condition which must be met for this plugin to be resolved for a given request.
     * 
     * @param condition condition which must be met for this plugin to be resolved for a given request
     */
    public void setEvaluationCondition(final Expression condition) {
        evaluationCondition = condition;
    }

    /**
     * Gets the unmodifiable list of dependencies for this plugin.
     * 
     * @return unmodifiable list of dependencies for this plugin, never null
     */
    public Set<ResolverPluginDependency> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    /**
     * Sets the list of dependencies for this plugin.
     * 
     * @param pluginDependencies unmodifiable list of dependencies for this plugin
     */
    public void setDependencies(final Collection<ResolverPluginDependency> pluginDependencies) {
        dependencies.clear();

        if (pluginDependencies != null) {
            for (ResolverPluginDependency dependency : pluginDependencies) {
                addDependency(dependency);
            }
        }
    }

    /**
     * Adds a dependency to this plugin.
     * 
     * @param dependency dependency to added, may be null
     * 
     * @return true if the addition changed the dependencies for this plugin, false otherwise
     */
    public boolean addDependency(final ResolverPluginDependency dependency) {
        if (dependency == null) {
            return false;
        }

        return dependencies.add(dependency);
    }

    /**
     * Removes a dependency from this plugin.
     * 
     * @param dependency dependency to removed, may be null
     * 
     * @return true if the removal changed the dependencies for this plugin, false otherwise
     */
    public boolean removeDependency(final ResolverPluginDependency dependency) {
        if (dependency == null) {
            return false;
        }

        return dependencies.remove(dependency);
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
     * 
     * @throws AttributeResolutionException thrown if there is a problem evaluating the condition
     */
    public boolean isApplicable(final AttributeResolutionContext resolutionContext) throws AttributeResolutionException {
        if (evaluationCondition == null) {
            return true;
        }

        try {
            return evaluationCondition.getValue(resolutionContext, Boolean.class);
        } catch (EvaluationException e) {
            log.error("Error checking evaluation condition for resolver plugin '{}'", getId(), e);
            if (propagateEvaluationConditionExceptions) {
                throw new AttributeResolutionException(e);
            }
            return false;
        }
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