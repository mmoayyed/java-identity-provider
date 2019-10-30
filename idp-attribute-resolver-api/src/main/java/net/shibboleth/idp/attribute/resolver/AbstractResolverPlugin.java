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

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.opensaml.messaging.context.navigate.ParentContextLookup;
import org.opensaml.profile.context.MetricContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import com.google.common.base.Predicates;

import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Base class for all {@link ResolverPlugin}s.
 * 
 * @param <ResolvedType> object type this plug-in resolves to
 */
@ThreadSafe
public abstract class AbstractResolverPlugin<ResolvedType> extends AbstractIdentifiableInitializableComponent implements
        ResolverPlugin<ResolvedType>, DisposableBean {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractResolverPlugin.class);

    /** Whether an {@link AttributeResolutionContext} that occurred resolving attributes will be re-thrown. */
    private boolean propagateResolutionExceptions = true;

    /** Strategy to get the {@link ProfileRequestContext}. */
    @Nonnull private Function<AttributeResolutionContext, ProfileRequestContext> profileContextStrategy;

    /** Criterion that must be met for this plugin to be active for the given request. */
    @Nullable private Predicate<ProfileRequestContext> activationCondition;

    /** The {@link ResolverAttributeDefinitionDependency}s this plug-in depends on. */
    @Nonnull @NonnullElements private Set<ResolverAttributeDefinitionDependency> attributeDependencies;
    
    /** The {@link ResolverDataConnectorDependency}s this plug-in depends on. */
    @Nonnull @NonnullElements private Set<ResolverDataConnectorDependency> dataConnectorDependencies;

    /** Constructor. */
    public AbstractResolverPlugin() {
        profileContextStrategy = new ParentContextLookup<>(ProfileRequestContext.class);
        attributeDependencies = Collections.emptySet();
        dataConnectorDependencies = Collections.emptySet(); 
    }

    /** {@inheritDoc} */
    @Override public boolean isPropagateResolutionExceptions() {
        return propagateResolutionExceptions;
    }

    /**
     * Set whether an {@link AttributeResolutionContext} that occurred resolving attributes will be re-thrown. Doing so
     * will cause the entire attribute resolution request to fail.
     * 
     * @param propagate true if {@link ResolutionException}s are propagated, false if not
     */
    public void setPropagateResolutionExceptions(final boolean propagate) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        propagateResolutionExceptions = propagate;
    }

    /**
     * Gets the mechanism to find out the {@link ProfileRequestContext}.
     * 
     * @return the mechanism
     */
    public Function<AttributeResolutionContext, ProfileRequestContext> getProfileContextStrategy() {
        return profileContextStrategy;
    }

    /**
     * Sets the mechanism to find out the {@link ProfileRequestContext}.
     * 
     * @param strategy the mechanism
     */
    public void setProfileContextStrategy(final Function<AttributeResolutionContext, ProfileRequestContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        
        profileContextStrategy = Constraint.isNotNull(strategy, "Profile Context Strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override @Nullable public Predicate<ProfileRequestContext> getActivationCondition() {
        return activationCondition;
    }

    /**
     * Sets the predicate which defines whether this plugin is active for a given request.
     * 
     * @param pred what to set
     */
    public void setActivationCondition(@Nonnull final Predicate<ProfileRequestContext> pred) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        
        activationCondition = Constraint.isNotNull(pred, "Activation condition cannot be null");
    }

    /**
     * Gets the unmodifiable list of attributeDependencies for this plugin.
     * 
     * @return unmodifiable list of dependencies for this plugin, never null
     */
    @Override @NonnullAfterInit @NonnullElements @Unmodifiable public Set<ResolverAttributeDefinitionDependency>
            getAttributeDependencies() {
        return attributeDependencies;
    }
    
    /**
     * Gets the unmodifiable list of dependencies for this plugin.
     * 
     * @return unmodifiable list of dependencies for this plugin, never null
     */
    @Override @NonnullAfterInit @NonnullElements @Unmodifiable public Set<ResolverDataConnectorDependency>
            getDataConnectorDependencies() {
        return dataConnectorDependencies;
    }


    /**
     * Sets the list of dependencies for this plugin.
     * 
     * @param dependencies unmodifiable list of dependencies for this plugin
     */
    public void setAttributeDependencies(@Nonnull @NonnullElements
            final Set<ResolverAttributeDefinitionDependency> dependencies) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        Constraint.isNotNull(dependencies, "Attribute Dependencies cannot be null");

        attributeDependencies = Set.copyOf(dependencies);
    }
    
    /**
     * Sets the list of dependencies for this plugin.
     * 
     * @param dependencies unmodifiable list of dependencies for this plugin
     */
    public void setDataConnectorDependencies(@Nonnull @NonnullElements
            final Set<ResolverDataConnectorDependency> dependencies) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        Constraint.isNotNull(dependencies, "DataConnector Dependencies cannot be null");

        dataConnectorDependencies = Set.copyOf(dependencies);
    }


    /**
     * Performs the attribute resolution for this plugin.
     * 
     * <p>
     * This method first checks to see if this plugin has been initialized and has not be destroyed. Then it checks if
     * the plugins activation criterion has been met. Finally it delegates to
     * {@link #doResolve(AttributeResolutionContext, AttributeResolverWorkContext)}. If an exception is thrown and
     * {@link #isPropagateResolutionExceptions()} is false the exception is logged but not re-thrown, otherwise it is
     * re-thrown.
     * </p>
     * 
     * @param resolutionContext current attribute resolution context
     * 
     * @return the attributes made available by the resolution, or null if no attributes were resolved
     * 
     * @throws ResolutionException thrown if there was a problem resolving the attributes
     */
    @Override @Nullable public final ResolvedType resolve(@Nonnull final AttributeResolutionContext resolutionContext)
            throws ResolutionException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        Constraint.isNotNull(resolutionContext, "AttributeResolutionContext cannot be null");

        final boolean timerStarted = startTimer(resolutionContext);
        
        try {
            if (null != activationCondition) {
                final ProfileRequestContext profileRequestContext = profileContextStrategy.apply(resolutionContext);
                if (!activationCondition.test(profileRequestContext)) {
                    log.debug("Resolver plugin '{}': activation criteria not met, nothing to do", getId());
                    return null;
                }
            }
    
            final AttributeResolverWorkContext workContext =
                    resolutionContext.getSubcontext(AttributeResolverWorkContext.class, false);
            Constraint.isNotNull(workContext, "AttributeResolverWorkContext cannot be null");
    
            try {
                final ResolvedType result = doResolve(resolutionContext, workContext);
                if (null == result) {
                    log.debug("Resolver plugin '{}' produced no value.", getId());
                }
                return result;
            } catch (final ResolutionException e) {
                //
                // NOTE - if you change this logic you MUST make changes in any derived classes that
                // depend on our handling of propagateResolutionExceptions.
                //
                if (propagateResolutionExceptions) {
                    throw e;
                }
                if (e instanceof NoResultAnErrorResolutionException
                        || e instanceof MultipleResultAnErrorResolutionException) {
                    log.debug(
                            "Resolver plugin '{}' produced the following error but was configured not to propagate",
                            getId(), e);
                } else {
                    log.error(
                            "Resolver plugin '{}' produced the following error but was configured not to propagate",
                            getId(), e);
                }
                return null;
            }
        } finally {
            if (timerStarted) {
                stopTimer(resolutionContext);
            }
        }
    }

    /** {@inheritDoc} */
    @Override protected void doDestroy() {
        activationCondition = Predicates.alwaysFalse();
        attributeDependencies = Collections.emptySet();
        dataConnectorDependencies = Collections.emptySet();
        super.doDestroy();
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return Objects.hash(getId());
    }

    /** {@inheritDoc} */
    @Override public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof AbstractResolverPlugin)) {
            return false;
        }

        final AbstractResolverPlugin<?> other = (AbstractResolverPlugin<?>) obj;
        return java.util.Objects.equals(getId(), other.getId());
    }

    /**
     * Perform the actual resolution. The resolved attribute(s) should not be recorded in the work context.
     * 
     * @param resolutionContext current resolution context
     * @param workContext child context where intermediate results are tracked
     * 
     * @return the resolved attributes or null if no attributes were resolved
     * @throws ResolutionException thrown if there is a problem resolving the attributes
     * 
     * @see AbstractResolverPlugin#resolve(AttributeResolutionContext)
     */
    @Nullable protected abstract ResolvedType doResolve(@Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException;

    
    /**
     * Conditionally start a timer at the beginning of the resolution process.
     * 
     * @param resolutionContext attribute resolution context
     * 
     * @return true iff the {@link #stopTimer(AttributeResolutionContext)} method needs to be called
     */
    private boolean startTimer(@Nonnull final AttributeResolutionContext resolutionContext) {
        final ProfileRequestContext prc = profileContextStrategy.apply(resolutionContext);
        if (prc != null) {
            final MetricContext timerCtx = prc.getSubcontext(MetricContext.class);
            if (timerCtx != null) {
                timerCtx.start(getId());
                return true;
            }
        }
        return false;
    }
    
    /**
     * Conditionally stop a timer at the end of the resolution process.
     * 
     * @param resolutionContext attribute resolution context
     */
    private void stopTimer(@Nonnull final AttributeResolutionContext resolutionContext) {
        final ProfileRequestContext prc = profileContextStrategy.apply(resolutionContext);
        if (prc != null) {
            final MetricContext timerCtx = prc.getSubcontext(MetricContext.class);
            if (timerCtx != null) {
                timerCtx.stop(getId());
            }
        }
    }

}