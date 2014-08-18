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
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.AuditContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

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
    @Nonnull @NonnullElements private Map<String,Function<ProfileRequestContext,Object>> fieldExtractors;
    
    /** Fields being audited, to optimize extraction.. */
    @Nonnull @NonnullElements private Set<String> fieldsToExtract;
    
    /** {@link AuditContext} to populate. */
    @Nullable private AuditContext auditCtx;
    
    /** Constructor. */
    public PopulateAuditContext() {
        auditContextCreationStrategy = new ChildContextLookup<>(AuditContext.class, true);
        fieldExtractors = Collections.emptyMap();
        fieldsToExtract = Collections.emptySet();
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
            @Nonnull @NonnullElements final Map<String,Function<ProfileRequestContext,Object>> map) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(map, "Field extractor map cannot be null");
        
        fieldExtractors = Maps.newHashMapWithExpectedSize(map.size());
        for (final Map.Entry<String,Function<ProfileRequestContext,Object>> entry : map.entrySet()) {
            final String field = StringSupport.trimOrNull(entry.getKey());
            if (entry.getValue() != null) {
                fieldExtractors.put(field, entry.getValue());
            }
        }
    }
    
    /**
     * Set the formatting string in use.
     * 
     * <p>A formatting string consists of tokens prefixed by '%' separated by any non-alphanumeric or whitespace.
     * Tokens can contain any letter or number or a hypen. Anything other than a token, including whitespace, is
     * a literal.</p>
     * 
     * <p>The string is parsed to determine what fields will eventually need to be audited, to skip extraction of
     * unused fields.</p>
     * 
     * @param s formatting string
     */
    public void setFormat(@Nonnull @NotEmpty final String s) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        if (Strings.isNullOrEmpty(s)) {
            throw new ConstraintViolationException("Formatting string cannot be null or empty");
        }
        
        fieldsToExtract = Sets.newHashSetWithExpectedSize(10);
        
        int len = s.length();
        boolean inToken = false;
        StringBuilder field = new StringBuilder();
        for (int pos = 0; pos < len; ++pos) {
            char ch = s.charAt(pos);
            if (inToken) {
                if (!Character.isLetterOrDigit(ch) && ch != '-' && ch != '%') {
                    fieldsToExtract.add(field.substring(1));
                    field.setLength(0);
                    inToken = false;
                }
            } else if (ch == '%') {
                field.setLength(0);
                inToken = true;
            }
            
            field.append(ch);
        }
        
        if (field.length() > 0 && inToken) {
            fieldsToExtract.add(field.substring(1));
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
        
        for (final Map.Entry<String,Function<ProfileRequestContext,Object>> entry : fieldExtractors.entrySet()) {
            
            if (!fieldsToExtract.isEmpty() && !fieldsToExtract.contains(entry.getKey())) {
                log.debug("{} Skipping field '{}' not included in audit format", getLogPrefix(), entry.getKey());
                continue;
            }
            
            final Object values = entry.getValue().apply(profileRequestContext);
            if (values != null) {
                if (values instanceof Collection && !((Collection) values).isEmpty()) {
                    log.debug("{} Adding {} value(s) for field '{}'", getLogPrefix(), ((Collection) values).size(),
                            entry.getKey());
                    for (final Object value : (Collection) values) {
                        auditCtx.getFieldValues(entry.getKey()).add(value.toString());
                    }
                } else {
                    log.debug("{} Adding 1 value for field '{}'", getLogPrefix(), entry.getKey());
                    auditCtx.getFieldValues(entry.getKey()).add(values.toString());
                }
            }
        }
    }
    
}