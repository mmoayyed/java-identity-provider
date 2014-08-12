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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * Action that produces an audit log entry based on an {@link AuditContext} and a formatting string. 
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 */
public class WriteAuditLog extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(WriteAuditLog.class);
    
    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    @Nonnull private Function<ProfileRequestContext,AuditContext> auditContextLookupStrategy;
    
    /** Base of category for log entries. */
    @Nonnull @NotEmpty private String categoryBase;
    
    /** Sequence of formatting tokens and literals to output. */
    @Nonnull @NonnullElements private List<String> format;

    /** The AuditContext to operate on. */
    @Nullable private AuditContext auditCtx;
    
    /** Constructor. */
    public WriteAuditLog() {
        auditContextLookupStrategy = new ChildContextLookup<>(AuditContext.class);
        categoryBase = "Shibboleth-Audit.";
        format = Collections.emptyList();
    }

    /**
     * Set the strategy used to locate the {@link AuditContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setAuditContextLookupStrategy(@Nonnull final Function<ProfileRequestContext,AuditContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        auditContextLookupStrategy = Constraint.isNotNull(strategy, "AuditContext lookup strategy cannot be null");
    }
    
    /**
     * Set the base of the logging category to use.
     * 
     * @param base  base prefix for logging category
     */
    public void setCategoryBase(@Nonnull @NotEmpty final String base) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        categoryBase = Constraint.isNotNull(StringSupport.trimOrNull(base),
                "Logging category base cannot be null or empty");
    }
    
    /**
     * Set the formatting string to use.
     * 
     * <p>A formatting string consists of tokens prefixed by '%' separated by any non-alphanumeric or whitespace.
     * Tokens can contain any letter or number or a hypen. Anything other than a token, including whitespace, is
     * a literal.</p>
     * 
     * @param s formatting string
     */
    public void setFormat(@Nonnull @NotEmpty final String s) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        if (Strings.isNullOrEmpty(s)) {
            throw new ConstraintViolationException("Formatting string cannot be null or empty");
        }
        
        format = Lists.newArrayListWithExpectedSize(10);
        
        int len = s.length();
        boolean inToken = false;
        StringBuilder field = new StringBuilder();
        for (int pos = 0; pos < len; ++pos) {
            char ch = s.charAt(pos);
            if (inToken) {
                if (!Character.isLetterOrDigit(ch) && ch != '-' && ch != '%') {
                    format.add(field.toString());
                    field.setLength(0);
                    inToken = false;
                }
                
            } else if (ch == '%') {
                if (field.length() > 0) {
                    format.add(field.toString());
                    field.setLength(0);
                }
                inToken = true;
            }
            
            field.append(ch);
        }
        
        if (field.length() > 0) {
            format.add(field.toString());
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        auditCtx = auditContextLookupStrategy.apply(profileRequestContext);
        if (auditCtx == null) {
            log.debug("{} No audit context associated with this profile request, nothing to do", getLogPrefix());
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        final StringBuilder entry = new StringBuilder();

        for (final String token : format) {
            if (token.startsWith("%")) {
                final Collection<String> values = auditCtx.getFieldValues(token.substring(1));
                for (final String val : values) {
                    entry.append(val).append(',');
                }
            } else {
                entry.append(token);
            }
        }
        
        LoggerFactory.getLogger(categoryBase + profileRequestContext.getLoggingId()).info(entry.toString());
    }
    
}