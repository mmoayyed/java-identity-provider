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

package net.shibboleth.idp.profile.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

/**
 * TODO: replace this with extensible interceptor approach
 * 
 * Action that checks a {@link net.shibboleth.idp.profile.config.ProfileConfiguration} for an inbound
 * subflow ID and signals it as the action's event.
 * 
 * <p>The profile configuration is obtained from a {@link RelyingPartyContext} obtained from a lookup
 * function, by default a child of the profile request context.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event subflow ID
 */
public class CheckForProfileSubflow extends AbstractProfileAction {

    /** Used to indicate which type of subflow to check for. */
    public enum Direction {
        /** Check for an inbound subflow. */
        INBOUND, 
        
        /** Check for an outbound subflow. */
        OUTBOUND,
        
        };
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(CheckForProfileSubflow.class);

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** The direction of execution for this action instance. */
    @NonnullAfterInit private Direction direction;
    
    /** The RelyingPartyContext to operate on. */
    @Nullable private RelyingPartyContext rpCtx;
    
    /** Constructor. */
    public CheckForProfileSubflow() {
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
    }

    /**
     * Set the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link RelyingPartyContext} associated with a given
     *         {@link ProfileRequestContext}
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        relyingPartyContextLookupStrategy = Constraint.isNotNull(strategy,
                "RelyingPartyContext lookup strategy cannot be null");
    }

    /**
     * Set the subflow direction to check for.
     *
     * @param executionDirection the direction to check for
     */
    public void setDirection(@Nonnull final Direction executionDirection) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        direction = Constraint.isNotNull(executionDirection, "Execution direction cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (direction == null) {
            throw new ComponentInitializationException("Execution direction cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        rpCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpCtx == null) {
            log.debug("{} No relying party context associated with this profile request", getLogPrefix());
            return false;
        }

        if (rpCtx.getProfileConfig() == null) {
            log.debug("{} No profile configuration associated with this profile request", getLogPrefix());
            return false;
        }
        
        return super.doPreExecute(profileRequestContext);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (direction == Direction.INBOUND && rpCtx.getProfileConfig().getInboundSubflowId() != null) {
            log.debug("{} Found inbound subflow in profile configuration: {}",
                    getLogPrefix(), rpCtx.getProfileConfig().getInboundSubflowId());
            ActionSupport.buildEvent(profileRequestContext, rpCtx.getProfileConfig().getInboundSubflowId());
        }
    }
    
}