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

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.DestructableComponent;
import net.shibboleth.utilities.java.support.component.IdentifiableComponent;
import net.shibboleth.utilities.java.support.component.InitializableComponent;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;

import com.google.common.base.Predicate;

/**
 * Base class for all {@link ResolutionPlugIn}s.
 * 
 * @param <ResolvedType> object type this plug-in resolves to
 */
@ThreadSafe
public interface ResolverPlugin<ResolvedType> extends ValidatableComponent, UnmodifiableComponent,
        InitializableComponent, DestructableComponent, IdentifiableComponent {

    /**
     * Gets whether an {@link AttributeResolutionContext} that occurred resolving attributes will be re-thrown. Doing so
     * will cause the entire attribute resolution request to fail.
     * 
     * @return true if {@link ResolutionException}s are propagated, false if not
     */
    public boolean isPropagateResolutionExceptions();

    /**
     * Gets the criteria that must be met for this plugin to be active for a given request.
     * 
     * @return criteria that must be met for this plugin to be active for a given request, never null
     */
    @NonnullAfterInit public Predicate<AttributeResolutionContext> getActivationCriteria();

    /**
     * Gets the unmodifiable list of dependencies for this plugin.
     * 
     * @return unmodifiable list of dependencies for this plugin, never null
     */
    @NonnullAfterInit @NonnullElements @Unmodifiable public Set<ResolverPluginDependency> getDependencies();

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
     * @return the attributes made available by the resolution, or null if no attributes were resolved
     * 
     * @throws ResolutionException thrown if there was a problem resolving the attributes
     */
    @Nullable public ResolvedType resolve(@Nonnull final AttributeResolutionContext resolutionContext)
            throws ResolutionException;

}