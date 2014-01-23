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

package net.shibboleth.idp.saml.impl.nameid;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.XMLObjectAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.ThreadSafeAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.opensaml.saml.saml1.profile.AbstractSAML1NameIdentifierGenerator;
import org.opensaml.saml.saml1.profile.SAML1ObjectSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * Default generator for {@link NameIdentifier} objects based on {@link IdPAttribute} data.
 */
@ThreadSafeAfterInit
public class DefaultSAML1NameIdentifierGenerator extends AbstractSAML1NameIdentifierGenerator {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(DefaultSAML1NameIdentifierGenerator.class);
    
    /** Strategy function to lookup RelyingPartyContext. */
    @Nonnull private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** Strategy function to lookup AttributeContext. */
    @Nonnull private Function<ProfileRequestContext, AttributeContext> attributeContextLookupStrategy;
    
    /** Attribute(s) to use as an identifier source. */
    @Nonnull @NonnullElements private List<String> attributeSourceIds;
    
    /** Constructor. */
    public DefaultSAML1NameIdentifierGenerator() {
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
        attributeContextLookupStrategy = new ChildContextLookup<>(AttributeContext.class);
        attributeSourceIds = Collections.emptyList();
    }
    
    /**
     * Set the lookup strategy to use to locate the {@link RelyingPartyContext}.
     * 
     * @param strategy lookup function to use
     */
    public synchronized void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        relyingPartyContextLookupStrategy = Constraint.isNotNull(strategy,
                "RelyingPartyContext lookup strategy cannot be null");
    }

    /**
     * Set the lookup strategy to use to locate the {@link AttributeContext}.
     * 
     * @param strategy lookup function to use
     */
    public synchronized void setAttributeContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, AttributeContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        attributeContextLookupStrategy = Constraint.isNotNull(strategy,
                "AttributeContext lookup strategy cannot be null");
    }
    
    /**
     * Set the relying parties to match against.
     * 
     * @param ids   relying party IDs to match against
     */
    public synchronized void setAttributeSourceIds(@Nonnull @NonnullElements final List<String> ids) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(ids, "Attribute ID collection cannot be null");
        
        attributeSourceIds = Lists.newArrayList(Collections2.filter(ids, Predicates.notNull()));
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
    @Nullable protected NameIdentifier doGenerate(@Nonnull final ProfileRequestContext profileRequestContext)
            throws ProfileException {
        
        // Check for a natively generated NameIdentifier attribute value.

        final AttributeContext attributeCtx = attributeContextLookupStrategy.apply(profileRequestContext);
        if (attributeCtx == null) {
            log.warn("Unable to locate AttributeContext");
            return null;
        }
        
        final Map<String, IdPAttribute> attributes = attributeCtx.getIdPAttributes();
        for (final String sourceId : attributeSourceIds) {
            
            final IdPAttribute attribute = attributes.get(sourceId);
            if (attribute == null) {
                continue;
            }
            
            final Set<IdPAttributeValue<?>> values = attribute.getValues();
            for (final IdPAttributeValue value : values) {
                if (value instanceof XMLObjectAttributeValue && value.getValue() instanceof NameIdentifier) {
                    if (SAML1ObjectSupport.areNameIdentifierFormatsEquivalent(getFormat(),
                            ((NameIdentifier) value.getValue()).getFormat())) {
                        log.info("Returning NameIdentifier from XMLObject-valued attribute {}", sourceId);
                        return (NameIdentifier) value.getValue();
                    } else {
                        log.debug("Attribute {} value was NameIdentifier, but Format did not match", sourceId);
                    }
                }
            }
        }
        
        // Fall into base class version which will ask us for an identifier.
        
        return super.doGenerate(profileRequestContext);
    }
    
    /** {@inheritDoc} */
    @Override
    @Nullable protected String getIdentifier(@Nonnull final ProfileRequestContext profileRequestContext)
            throws ProfileException {
        
        final AttributeContext attributeCtx = attributeContextLookupStrategy.apply(profileRequestContext);
        
        final Map<String, IdPAttribute> attributes = attributeCtx.getIdPAttributes();
        for (final String sourceId : attributeSourceIds) {
            log.debug("Checking for source attribute {}", sourceId);
            
            final IdPAttribute attribute = attributes.get(sourceId);
            if (attribute == null) {
                continue;
            }
            
            final Set<IdPAttributeValue<?>> values = attribute.getValues();
            for (final IdPAttributeValue value : values) {
                if (value instanceof ScopedStringAttributeValue) {
                    log.info("Generating NameIdentifier from Scoped String-valued attribute {}", sourceId);
                    return ((ScopedStringAttributeValue) value).getValue()
                            + '@' + ((ScopedStringAttributeValue) value).getScope(); 
                } else if (value instanceof StringAttributeValue) {
                    log.info("Generating NameIdentifier from String-valued attribute {}", sourceId);
                    return ((StringAttributeValue) value).getValue();
                } else {
                    log.info("Unrecognized attribute value type: {}", value.getClass().getName());
                }
            }
        }
        
        log.info("Attribute sources {} did not produce a usable identifier", attributeSourceIds);
        return null;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected String getDefaultIdPNameQualifier(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        final RelyingPartyContext rpCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpCtx != null) {
            final RelyingPartyConfiguration rpConfig = rpCtx.getConfiguration();
            if (rpConfig != null) {
                return rpConfig.getResponderEntityId();
            }
        }
        
        return null;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected String getDefaultSPNameQualifier(@Nonnull final ProfileRequestContext profileRequestContext) {

        final RelyingPartyContext rpCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpCtx != null) {
            return rpCtx.getRelyingPartyId();
        }
        
        return null;
    }

}