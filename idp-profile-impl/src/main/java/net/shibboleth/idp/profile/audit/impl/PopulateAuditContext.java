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

package net.shibboleth.idp.profile.audit.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.AuditExtractorFunction;
import net.shibboleth.idp.profile.context.AuditContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
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
 * Action that populates fields in an {@link AuditContext} using injected functions.
 * 
 *  <p>Each function is registered against a field specifier, and the result of the function
 *  is added to that field in the context's map. This reduces the code footprint required to
 *  implement, and extend, the fields logged, instead of requiring a dedicated action for
 *  a particular field or set of fields.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 */
public class PopulateAuditContext extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateAuditContext.class);
    
    /** Strategy used to locate or create the {@link AuditContext} to populate. */
    @Nonnull private Function<ProfileRequestContext,AuditContext> auditContextCreationStrategy;
    
    /** Map of fields to extract and the corresponding extraction functions. */
    @Nonnull @NonnullElements private Map<String,AuditExtractorFunction> fieldExtractors;
    
    /** {@link AuditContext} to populate. */
    @Nullable private AuditContext auditCtx;
    
    /** Constructor. */
    public PopulateAuditContext() {
        auditContextCreationStrategy = new ChildContextLookup<>(AuditContext.class, true);
        fieldExtractors = Collections.emptyMap();
    }

    /**
     * Set the strategy used to locate the {@link AuditContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setAuditContextCreationStrategy(@Nonnull final Function<ProfileRequestContext,AuditContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        auditContextCreationStrategy = Constraint.isNotNull(strategy, "AuditContext lookup strategy cannot be null");
    }
    
    /**
     * Set the map of fields and extraction functions to run.
     * 
     * @param map   map from field name to extraction function
     */
    public void setFieldExtractors(
            @Nonnull @NonnullElements final Map<String,AuditExtractorFunction> map) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(map, "Field extractor map cannot be null");
        
        fieldExtractors = Maps.newHashMapWithExpectedSize(map.size());
        for (final Map.Entry<String,AuditExtractorFunction> entry : map.entrySet()) {
            final String field = StringSupport.trimOrNull(entry.getKey());
            if (entry.getValue() != null) {
                fieldExtractors.put(field, entry.getValue());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext) || fieldExtractors.isEmpty()) {
            return false;
        }
        
        auditCtx = auditContextCreationStrategy.apply(profileRequestContext);
        if (auditCtx == null) {
            log.error("{} Unable to create AuditContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        for (final Map.Entry<String,AuditExtractorFunction> entry : fieldExtractors.entrySet()) {
            
            final Collection<String> values = entry.getValue().apply(profileRequestContext);
            if (values != null && !values.isEmpty()) {
                log.debug("{} Adding {} value(s) for field '{}'", getLogPrefix(), values.size(), entry.getKey());
                auditCtx.getFieldValues(entry.getKey()).addAll(values);
            }
        }
    }
    
}