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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.CollectionSupport;
import net.shibboleth.utilities.java.support.component.AbstractDestructableIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;
import net.shibboleth.utilities.java.support.logic.Assert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;

/**
 * Base class for all {@link ResolutionPlugIn}s.
 * 
 * @param <ResolvedType> object type this plug-in resolves to
 */
@ThreadSafe
public abstract class BaseResolverPlugin<ResolvedType> extends AbstractDestructableIdentifiableInitializableComponent
        implements ValidatableComponent, UnmodifiableComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(BaseResolverPlugin.class);

    /** Whether an {@link AttributeResolutionContext} that occurred resolving attributes will be re-thrown. */
    private boolean propagateResolutionExceptions = true;

    /** Criterion that must be met for this plugin to be active for the given request. */
    private Predicate<AttributeResolutionContext> activationCriteria = Predicates.alwaysTrue();

    /** IDs of the {@link ResolutionPlugIn}s this plug-in depends on. */
    private Set<ResolverPluginDependency> dependencies = Collections.emptySet();

    /** {@inheritDoc} */
    public synchronized void setId(final String componentId) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

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
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        propagateResolutionExceptions = propagate;
    }

    /**
     * Gets the criteria that must be met for this plugin to be active for a given request.
     * 
     * @return criteria that must be met for this plugin to be active for a given request, never null
     */
    @Nonnull public Predicate<AttributeResolutionContext> getActivationCriteria() {
        return activationCriteria;
    }

    /**
     * Sets the criteria that must be met for this plugin to be active for a given request.
     * 
     * @param criteria criteria that must be met for this plugin to be active for a given request
     */
    public synchronized void setActivationCriteria(@Nonnull final Predicate<AttributeResolutionContext> criteria) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        activationCriteria = Assert.isNotNull(criteria, "Activiation criteria can not be null");
    }

    /**
     * Gets the unmodifiable list of dependencies for this plugin.
     * 
     * @return unmodifiable list of dependencies for this plugin, never null
     */
    @Nonnull @NonnullElements @Unmodifiable public Set<ResolverPluginDependency> getDependencies() {
        return dependencies;
    }

    /**
     * Sets the list of dependencies for this plugin.
     * 
     * @param pluginDependencies unmodifiable list of dependencies for this plugin
     */
    public synchronized void setDependencies(
            @Nullable @NullableElements final Collection<ResolverPluginDependency> pluginDependencies) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        HashSet<ResolverPluginDependency> checkedDeps = new HashSet<ResolverPluginDependency>();
        CollectionSupport.addIf(checkedDeps, pluginDependencies, Predicates.notNull());
        dependencies = ImmutableSet.copyOf(checkedDeps);
    }

    /**
     * Performs the attribute resolution for this plugin.
     * 
     * <p>
     * This method first checks to see if this plugin has been initialized and has not be destroyed. Then it checks if
     * the plugins activation criterion has been met. Finally it delegates to
     * {@link #doResolve(AttributeResolutionContext)}. If an exception is thrown and
     * {@link #isPropagateResolutionExceptions()} is false the exception is logged but not re-thrown, otherwise it is
     * re-thrown.
     * </p>
     * 
     * @param resolutionContext current attribute resolution context
     * 
     * @return the attributes made available by the resolution, or {@link Optional#absent()} if no attributes were
     *         resolved
     * 
     * @throws AttributeResolutionException thrown if there was a problem resolving the attributes
     */
    @Nonnull public final Optional<ResolvedType> resolve(@Nonnull final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        assert resolutionContext != null : "Attribute resolution context can not be null";

        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        if (!activationCriteria.apply(resolutionContext)) {
            log.debug("Resolver plugin '{}': activation criteria not met, nothing to do", getId());
            return Optional.absent();
        }

        try {
            Optional<ResolvedType> resolvedData = doResolve(resolutionContext);
            assert resolvedData != null : "Result of doResolve for resolver plugin " + getId() + " was null";

            return resolvedData;
        } catch (AttributeResolutionException e) {
            //
            // NOTE - if you change this logic you MUST make changes in any derived classes that
            // depend on our handling of propagateResolutionExceptions.
            //
            if (propagateResolutionExceptions) {
                throw e;
            } else {
                log.debug("Resolver {} produced the following"
                        + " error but was configured not to propogate it.", new Object[] {getId(), e,});
                return Optional.absent();
            }
        }
    }

    /** {@inheritDoc} */
    public final synchronized void validate() throws ComponentValidationException {
        ComponentSupport.validate(activationCriteria);

        doValidate();
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        ComponentSupport.destroy(activationCriteria);
        activationCriteria = Predicates.alwaysFalse();
        dependencies = Collections.emptySet();

        super.doDestroy();
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        ComponentSupport.initialize(activationCriteria);
    }

    /**
     * Performs implementation specific validation. Default implementation of this method is a no-op.
     * 
     * @throws ComponentValidationException thrown if the component is not valid
     */
    protected void doValidate() throws ComponentValidationException {

    }
    
    /** {@inheritDoc} */
    public int hashCode() {
        return Objects.hashCode(getId());
    }
    
    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof BaseResolverPlugin)) {
            return false;
        }
        
        BaseResolverPlugin<ResolvedType> other = (BaseResolverPlugin<ResolvedType>) obj;
        return Objects.equal(getId(), other.getId());        
    }

    /**
     * Perform the actual resolution. The resolved attribute(s) should not be recorded in the resolution context.
     * 
     * @param resolutionContext current resolution context, guaranteed not to be bull
     * 
     * @return the resolved attributes or null if no attributes were resolved
     * 
     * @throws AttributeResolutionException thrown if there is a problem resolving the attributes
     * 
     * @see BaseResolverPlugin#resolve(AttributeResolutionContext)
     */
    @Nonnull protected abstract Optional<ResolvedType> doResolve(
            @Nonnull final AttributeResolutionContext resolutionContext) throws AttributeResolutionException;
}