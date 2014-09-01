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

package net.shibboleth.idp.session.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.MultiRelyingPartyContext;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.idp.session.context.LogoutContext;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

/**
 * Profile action that processes a {@link LogoutContext} and {@link MultiRelyingPartyContext} and
 * routes to a flow to further populate a {@RelyingPartyContext} for each distinct relying party.
 * 
 * <p>The {@link MultiRelyingPartyContext} is used to track the process of the iteration over each
 * relying party. The flow to dispatch to is determined using an injected map based on
 * the type of {@link SPSession}. It is not an error if a type is unmapped; they are ignored.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event Selected flow ID to execute
 */
public class ProcessLogoutRelyingParties extends AbstractProfileAction {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ProcessLogoutRelyingParties.class);
    
    /** Map of {@link SPSession} subtypes to elaboration flow IDs. */
    @Nonnull private Map<Class<? extends SPSession>,String> elaborationFlowMap;
    
    /** Lookup function for {@link LogoutContext}. */
    @Nonnull private Function<ProfileRequestContext,LogoutContext> logoutContextLookupStrategy;
    
    /** Lookup function for {@link MultiRelyingPartyContext}. */
    @Nonnull private Function<ProfileRequestContext,MultiRelyingPartyContext> multiCtxLookupStrategy;
    
    /** {@link LogoutContext} to process. */
    @Nullable private LogoutContext logoutCtx;

    /** {@link MultiRelyingPartyContext} to process. */
    @Nullable private MultiRelyingPartyContext multiCtx;
    
    /** Constructor. */
    public ProcessLogoutRelyingParties() {
        elaborationFlowMap = Collections.emptyMap();
        logoutContextLookupStrategy = new ChildContextLookup<>(LogoutContext.class);
        multiCtxLookupStrategy = new ChildContextLookup<>(MultiRelyingPartyContext.class);
    }
    
    /**
     * Set the map of {@link SPSession} subtypes to elaboration flow IDs.
     * 
     * @param map map to set
     */
    public void setElaborationFlowMap(@Nonnull final Map<Class<? extends SPSession>,String> map) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(map, "Elaboration flow map cannot be null");
        
        elaborationFlowMap = Maps.newHashMapWithExpectedSize(map.size());
        
        for (final Map.Entry<Class<? extends SPSession>,String> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                final String trimmed = StringSupport.trimOrNull(entry.getValue());
                if (trimmed != null) {
                    elaborationFlowMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }
    
    /**
     * Set the lookup strategy for the LogoutContext to process.
     * 
     * @param strategy  lookup strategy
     */
    public void setLogoutContextLookupStrategy(@Nonnull final Function<ProfileRequestContext,LogoutContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        logoutContextLookupStrategy = Constraint.isNotNull(strategy, "LogoutContext lookup strategy cannot be null");
    }

    /**
     * Set the lookup strategy for the MultiRelyingPartyContext to process.
     * 
     * @param strategy  lookup strategy
     */
    public void setMultiRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,MultiRelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        multiCtxLookupStrategy = Constraint.isNotNull(strategy,
                "MultiRelyingPartyContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        logoutCtx = logoutContextLookupStrategy.apply(profileRequestContext);
        if (logoutCtx == null) {
            log.debug("{} No LogoutContext found, nothing to do", getLogPrefix());
            return false;
        }
        
        multiCtx = multiCtxLookupStrategy.apply(profileRequestContext);
        if (multiCtx == null) {
            log.error("{} No MultiRelyingPartyContext found", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        } else if (!multiCtx.getRelyingPartyContextIterator().hasNext()) {
            log.debug("{} No relying parties remaining to process, nothing to do", getLogPrefix());
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        while (multiCtx.getRelyingPartyContextIterator().hasNext()) {
            final RelyingPartyContext rpCtx = multiCtx.getRelyingPartyContextIterator().next();
            final Collection<SPSession> sessions = logoutCtx.getSessions(rpCtx.getRelyingPartyId());
            if (!sessions.isEmpty()) {
                final Class clazz = sessions.iterator().next().getClass();
                final String flowId = elaborationFlowMap.get(clazz);
                if (flowId == null) {
                    log.debug("{} Skipping SPSession of type {}", getLogPrefix(), clazz.getName());
                    continue;
                }
                
                log.debug("{} Dispatching to subflow {} to process relying party '{}'", getLogPrefix(), flowId,
                        rpCtx.getRelyingPartyId());
                ActionSupport.buildEvent(profileRequestContext, flowId);
                return;
            }
        }
    }
    
}