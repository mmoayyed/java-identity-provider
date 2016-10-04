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
import net.shibboleth.idp.profile.context.MetricContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;


/**
 * An action that populates a {@link MetricContext} child of the {@link ProfileRequestContext} with
 * a set of rules for activating timer measurements and counters on associated objects during the execution
 * of a profile request.
 * 
 * <p>Unlike a more typical "lookup strategy" design used in most other places, the strategy function
 * supplied is free, and indeed expected, to directly manipulate the created child context directly
 * rather than returning the data to use. The function may return false to indicate a lack of success,
 * but this value is merely logged.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 */
public class PopulateMetricContext extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateMetricContext.class);
    
    /** Strategy function for establishing metric mappings to apply. */
    @NonnullAfterInit private Function<ProfileRequestContext,Boolean> metricStrategy;
    
    /**
     * Set strategy to establish the metric mappings to use.
     * 
     * @param strategy  timer mapping strategy
     */
    public void setMetricStrategy(@Nullable final Function<ProfileRequestContext,Boolean> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        metricStrategy = strategy;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (metricStrategy == null) {
            metricStrategy = new NullFunction();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final MetricContext metricCtx = new MetricContext();
        profileRequestContext.addSubcontext(metricCtx, true);
        if (!metricStrategy.apply(profileRequestContext)) {
            log.warn("{} Configuration of metric mappings by supplied strategy function failed", getLogPrefix());
        }
    }

    /**
     * Default function to remove the context from the tree when no metrics are installed.
     */
    private class NullFunction implements Function<ProfileRequestContext,Boolean> {

        /** {@inheritDoc} */
        public Boolean apply(final ProfileRequestContext input) {
            input.removeSubcontext(MetricContext.class);
            return true;
        }
        
    }
    
}