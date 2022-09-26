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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.authn.AbstractSubjectCanonicalizationAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.SubjectCanonicalizationException;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.principal.IdPAttributePrincipal;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.primitive.StringSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * An action that extracts a resolved {@link IdPAttribute} value from an {@link AttributeContext} child obtained via
 * lookup function (by default a child of the {@link SubjectCanonicalizationContext}), and uses it as the result
 * of subject canonicalization.
 * 
 * <p>This action operates on a set of previously resolved attributes that are presumed to have been generated based
 * in some fashion on the content of the {@link SubjectCanonicalizationContext}.</p>
 * 
 * <p>String and scoped attribute values are supported.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link AuthnEventIds#INVALID_SUBJECT}
 * @pre <pre>ProfileRequestContext.getSubcontext(SubjectCanonicalizationContext.class) != null</pre>
 * @post <pre>SubjectCanonicalizationContext.getPrincipalName() != null
 *  || SubjectCanonicalizationContext.getException() != null</pre>
 */
public class AttributeSourcedSubjectCanonicalization extends AbstractSubjectCanonicalizationAction {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AttributeSourcedSubjectCanonicalization.class);

    /** Delimiter to use for scoped attribute serialization. */
    private char delimiter;
    
    /** Whether to also check the original Subject for {@link IdPAttributePrincipal}s. */
    private boolean resolveFromSubject;
    
    /** Indexed attributes pulled from subject. */
    @Nonnull @NonnullElements private Map<String,IdPAttribute> subjectSourcedAttributes;
    
    /** Ordered list of attributes to look for and read from. */
    @Nonnull @NonnullElements private List<String> attributeSourceIds;
        
    /** Lookup strategy for {@link AttributeContext} to read from. */
    @Nonnull private Function<ProfileRequestContext,AttributeContext> attributeContextLookupStrategy;
    
    /** The context to read from. */
    @Nullable private AttributeContext attributeCtx;
    
    /** Constructor. */
    public AttributeSourcedSubjectCanonicalization() {
        delimiter = '@';
        attributeSourceIds = Collections.emptyList();
        subjectSourcedAttributes = Collections.emptyMap();
        
        attributeContextLookupStrategy =
                new ChildContextLookup<>(AttributeContext.class).compose(
                        new ChildContextLookup<>(SubjectCanonicalizationContext.class));
    }
    
    /**
     * Set the delimiter to use for serializing scoped attribute values.
     * 
     * @param ch delimiter to use
     */
    public void setScopedDelimiter(final char ch) {
        checkSetterPreconditions();
        delimiter = ch;
    }
    
    /**
     * Whether to include any {@link IdPAttributePrincipal} objects found in the input {@link Subject}
     * when searching for a matching attribute ID.
     * 
     * @param flag flag to set
     * 
     * @since 4.1.0
     */
    public void setResolveFromSubject(final boolean flag) {
        checkSetterPreconditions();
        resolveFromSubject = flag;
    }
    
    /**
     * Set the attribute IDs to read from in order of preference.
     * 
     * @param ids   attribute IDs to read from
     */
    public void setAttributeSourceIds(@Nonnull @NonnullElements final List<String> ids) {
        checkSetterPreconditions();
        attributeSourceIds = new ArrayList<>(StringSupport.normalizeStringCollection(ids));
    }
    
    /**
     * Set the lookup strategy for the {@link AttributeContext} to read from.
     * 
     * @param strategy  lookup strategy
     */
    public void setAttributeContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,AttributeContext> strategy) {
        checkSetterPreconditions();
        attributeContextLookupStrategy = Constraint.isNotNull(strategy,
                "AttributeContext lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (attributeSourceIds.isEmpty()) {
            throw new ComponentInitializationException("Attribute source ID list cannot be empty");
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext, 
            @Nonnull final SubjectCanonicalizationContext c14nContext) {

        if (!super.doPreExecute(profileRequestContext, c14nContext)) {
            return false;
        }
        
        if (resolveFromSubject) {
            final Set<IdPAttributePrincipal> subjectSourced =
                    c14nContext.getSubject().getPrincipals(IdPAttributePrincipal.class);
            if (subjectSourced != null && !subjectSourced.isEmpty()) {
                subjectSourcedAttributes = new HashMap<>(subjectSourced.size());
                subjectSourced.forEach(a -> subjectSourcedAttributes.put(a.getAttribute().getId(), a.getAttribute()));
            }
        }
        
        attributeCtx = attributeContextLookupStrategy.apply(profileRequestContext);
        if (subjectSourcedAttributes.isEmpty() && (attributeCtx == null || attributeCtx.getIdPAttributes().isEmpty())) {
            log.warn("{} No attributes found, canonicalization not possible", getLogPrefix());
            c14nContext.setException(new SubjectCanonicalizationException("No attributes were found"));
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT);
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext, 
            @Nonnull final SubjectCanonicalizationContext c14nContext) {
        
        for (final String id : attributeSourceIds) {
            
            IdPAttribute attr = subjectSourcedAttributes.get(id);
            if (attr != null) {
                final String result = findValue(attr);
                if (result != null) {
                    c14nContext.setPrincipalName(result);
                    return;
                }
            } else if (attributeCtx != null) {
                attr = attributeCtx.getIdPAttributes().get(id);
                if (attr != null) {
                    final String result = findValue(attr);
                    if (result != null) {
                        c14nContext.setPrincipalName(result);
                        return;
                    }
                }
            }
        }
        
        log.info("{} Attribute sources {} did not produce a usable identifier", getLogPrefix(), attributeSourceIds);
        c14nContext.setException(new SubjectCanonicalizationException("No usable attribute values were found"));
        ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT);
    }

    /**
     * Check for a compatible value in the input attribute.
     * 
     * @param attribute input attribute
     * 
     * @return value to use for result, or null
     */
    @Nullable private String findValue(@Nonnull final IdPAttribute attribute) {
        
        for (final IdPAttributeValue val : attribute.getValues()) {
            if (val instanceof ScopedStringAttributeValue) {
                final ScopedStringAttributeValue scoped = (ScopedStringAttributeValue) val;
                final String withScope = scoped.getValue() + delimiter + scoped.getScope();
                log.debug("{} Using attribute {} scoped value {} as input to transforms", getLogPrefix(),
                        attribute.getId(), withScope);
                return applyTransforms(withScope);
            } else if (val instanceof StringAttributeValue) {
                final StringAttributeValue stringVal = (StringAttributeValue) val;
                if (stringVal.getValue() == null || stringVal.getValue().isEmpty()) {
                    log.debug("{} Ignoring null/empty string value", getLogPrefix());
                    continue;
                }
                log.debug("{} Using attribute {} string value {} as input to transforms", getLogPrefix(),
                        attribute.getId(), stringVal.getValue());
                return applyTransforms(stringVal.getValue());
            } else {
                log.warn("{} Unsupported attribute value type: {}", getLogPrefix(), val.getClass().getName());
            }
        }
        
        return null;
    }
    
}