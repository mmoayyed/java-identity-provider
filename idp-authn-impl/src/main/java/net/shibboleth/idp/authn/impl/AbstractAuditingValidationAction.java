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

package net.shibboleth.idp.authn.impl;

import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.EventContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import net.shibboleth.idp.authn.AbstractValidationAction;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.audit.impl.PopulateAuditContext;
import net.shibboleth.idp.profile.audit.impl.WriteAuditLog;
import net.shibboleth.idp.profile.context.AuditContext;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.logic.Constraint;

/**
 * Base class for validation actions that includes new audit logging support.
 * 
 * <p>This is not great design, but embedding the existing audit action classes
 * as fields is by far the simplest way to reuse that logic without getting caught up
 * in the vagaries of the individual validator's logic.</p>
 * 
 * @since 4.3.0
 */
public abstract class AbstractAuditingValidationAction extends AbstractValidationAction {

    /** Strategy used to locate or create the {@link AuditContext} to populate. */
    @Nonnull private Function<ProfileRequestContext,AuditContext> auditContextCreationStrategy;

    /** Optional audit extraction action. */
    @Nullable private PopulateAuditContext populateAuditContextAction;

    /** Optional audit output action. */
    @Nullable private WriteAuditLog writeAuditLogAction;
    
    /** The Spring RequestContext to operate on. */
    @Nullable private RequestContext requestContext;
    
    /** Constructor. */
    public AbstractAuditingValidationAction() {
        auditContextCreationStrategy =
                new ChildContextLookup<>(AuditContext.class, true).compose(
                        new ChildContextLookup<>(AuthenticationContext.class));
    }

    /**
     * Set the strategy used to locate the {@link AuditContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setAuditContextCreationStrategy(@Nonnull final Function<ProfileRequestContext,AuditContext> strategy) {
        checkSetterPreconditions();
        auditContextCreationStrategy = Constraint.isNotNull(strategy, "AuditContext creation strategy cannot be null");
    }
    
    /**
     * Sets an audit context population action to run.
     * 
     * @param action optional action to use to populate audit context
     * 
     * @since 4.3.0
     */
    public void setPopulateAuditContextAction(@Nullable final PopulateAuditContext action) {
        checkSetterPreconditions();
        populateAuditContextAction = action;
    }

    /**
     * Sets an audit output action to run.
     * 
     * @param action optional action to use to write to audit log
     * 
     * @since 4.3.0
     */
    public void setWriteAuditLogAction(@Nullable final WriteAuditLog action) {
        checkSetterPreconditions();
        writeAuditLogAction = action;
    }

    /** {@inheritDoc} */
    @Override
    protected Event doExecute(@Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext) {
        
        requestContext = springRequestContext;
        return super.doExecute(springRequestContext, profileRequestContext);
    }

    /** {@inheritDoc} */
    @Override
    protected void recordSuccess(@Nonnull final ProfileRequestContext profileRequestContext) {
        doAudit(profileRequestContext);
        super.recordSuccess(profileRequestContext);
    }

    /** {@inheritDoc} */
    @Override
    protected void recordFailure(@Nonnull final ProfileRequestContext profileRequestContext) {
        doAudit(profileRequestContext);
        super.recordFailure(profileRequestContext);
    }
    
    /**
     * Create or locate the {@link AuditContext} via the defined strategy.
     * 
     * @param profileRequestContext profile request context
     * 
     * @return the audit context
     */
    @Nullable protected AuditContext getAuditContext(@Nonnull final ProfileRequestContext profileRequestContext) {
        return auditContextCreationStrategy.apply(profileRequestContext);
    }
    
    /**
     * Do audit extraction and output.
     * 
     * @param profileRequestContext profile request context
     */
    protected void doAudit(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (populateAuditContextAction != null && writeAuditLogAction != null) {
            final EventContext existingEvent = profileRequestContext.getSubcontext(EventContext.class);
            
            try {
                populateAuditContextAction.execute(requestContext);
                
                final Map<String,String> fields = getAuditFields(profileRequestContext);
                if (fields != null) {
                    final AuditContext ac = getAuditContext(profileRequestContext);
                    if (ac != null) {
                        for (final Map.Entry<String,String> field : fields.entrySet()) {
                            ac.getFieldValues(field.getKey()).add(field.getValue());
                        }
                    }
                }
            } finally {
                if (existingEvent != null) {
                    profileRequestContext.addSubcontext(existingEvent);
                }
            }
            
            try {
                writeAuditLogAction.execute(requestContext);
            } finally {
                if (existingEvent != null) {
                    profileRequestContext.addSubcontext(existingEvent);
                }
            }
        }
    }
    
    /**
     * Subclasses can override this method to supply additional audit fields to store.
     * 
     * @param profileRequestContext profile request context
     * @return audit fields
     */
    @Nullable @NonnullElements protected Map<String,String> getAuditFields(
            @Nonnull final ProfileRequestContext profileRequestContext) {
        return null;
    }
    
}