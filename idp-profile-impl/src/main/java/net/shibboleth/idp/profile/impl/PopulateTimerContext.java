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
import net.shibboleth.idp.profile.context.TimerContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;

import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;


/**
 * An action that populates a {@link TimerContext} child of the {@link ProfileRequestContext} with
 * a set of rules for activating timer measurements of associated objects during the execution of
 * a profile request.
 * 
 * <p>Unlike a more typical "lookup strategy" design used in most other places, the strategy function
 * supplied is free, and indeed expected, to directly manipulate the created child context directly
 * rather than returning the data to use. The function may return false to indicate a lack of success,
 * but this value is merely logged.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 */
public class PopulateTimerContext extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateTimerContext.class);
    
    /** Strategy function for establishing timer mappings to apply. */
    @NonnullAfterInit private Function<ProfileRequestContext,Boolean> timerStrategy;
    
    /** Constructor. */
    public PopulateTimerContext() {
        timerStrategy = FunctionSupport.constant(Boolean.TRUE);
    }
    
    /**
     * Set strategy to establish the timer mappings to use.
     * 
     * @param strategy  timer mapping strategy
     */
    public void setTimerStrategy(@Nullable final Function<ProfileRequestContext,Boolean> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        timerStrategy = strategy != null ? strategy 
                : FunctionSupport.<ProfileRequestContext,Boolean>constant(Boolean.TRUE);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final TimerContext timerCtx = new TimerContext();
        profileRequestContext.addSubcontext(timerCtx, true);
        if (!timerStrategy.apply(profileRequestContext)) {
            log.warn("{} Configuration of timer mappings by supplied strategy function failed", getLogPrefix());
        }
    }

}