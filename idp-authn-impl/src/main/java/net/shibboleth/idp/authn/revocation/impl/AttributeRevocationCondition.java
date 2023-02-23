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

package net.shibboleth.idp.authn.revocation.impl;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.messaging.context.ScratchContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.attribute.DateTimeAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.profile.context.navigate.ResponderIdLookupFunction;
import net.shibboleth.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.AbstractInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;
import net.shibboleth.shared.service.ReloadableService;

/**
 * A condition for login flows that checks for revocation against a resolved
 * {@link IdPAttribute}.
 * 
 * @since 4.3.0
 */
public class AttributeRevocationCondition extends AbstractInitializableComponent
        implements BiPredicate<ProfileRequestContext,AuthenticationResult> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AttributeRevocationCondition.class);
    
    /** Lookup strategy for principal name. */
    @NonnullAfterInit private Function<ProfileRequestContext,String> principalNameLookupStrategy;

    /** Strategy used to locate the identity of the issuer associated with the attribute resolution. */
    @Nullable private Function<ProfileRequestContext,String> issuerLookupStrategy;

    /** Strategy used to locate the identity of the recipient associated with the attribute resolution. */
    @Nullable private Function<ProfileRequestContext,String> recipientLookupStrategy;
    
    /** Attribute Resolver service. */
    @NonnullAfterInit private ReloadableService<AttributeResolver> attributeResolver;
    
    /** Attribute ID to resolve. */
    @NonnullAfterInit @NotEmpty private String attributeId;
    
    /** Constructor. */
    public AttributeRevocationCondition() {
        issuerLookupStrategy = new ResponderIdLookupFunction();
        recipientLookupStrategy = new RelyingPartyIdLookupFunction();
    }
        
    /**
     * Set lookup strategy for principal name.
     * 
     * @param strategy lookup strategy
     */
    public void setPrincipalNameLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        
        principalNameLookupStrategy = Constraint.isNotNull(strategy, "Principal name lookup strategy cannot be null");
    }
    
    /**
     * Set the strategy used to lookup the issuer for this attribute resolution.
     * 
     * @param strategy  lookup strategy
     */
    public void setIssuerLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        
        issuerLookupStrategy = strategy;
    }

    /**
     * Set the strategy used to lookup the recipient for this attribute resolution.
     * 
     * @param strategy  lookup strategy
     */
    public void setRecipientLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();

        recipientLookupStrategy = strategy;
    }
    
    /**
     * Set {@link AttributeResolver} to use.
     * 
     * @param service attribute resolver service
     */
    public void setAttributeResolver(@Nonnull final ReloadableService<AttributeResolver> service) {
        checkSetterPreconditions();
        
        attributeResolver = Constraint.isNotNull(service, "ReloadableService<AttributeResolver> cannot be null");
    }
    
    /**
     * Set the ID of an {@link IdPAttribute} to resolve to obtain revocation records for the principal.
     * 
     * @param id attribute ID to resolve
     */
    public void setAttributeId(@Nonnull @NotEmpty final String id) {
        checkSetterPreconditions();
        
        attributeId = Constraint.isNotNull(StringSupport.trimOrNull(id), "Attribute ID cannot be null or empty");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (attributeResolver == null) {
            throw new ComponentInitializationException("ReloadableService<AttributeResolver> cannot be null");
        } else if (principalNameLookupStrategy == null) {
            throw new ComponentInitializationException("Principal name lookup strategy cannot be null");
        } else if (attributeId == null) {
            throw new ComponentInitializationException("Attribute ID to resolve cannot be null or empty");
        }
    }

    /** {@inheritDoc} */
    public boolean test(@Nullable final ProfileRequestContext input,  @Nullable final AuthenticationResult input2) {
        checkComponentActive();
        
        if (input == null || input2 == null) {
            log.error("Called with null inputs");
            return true;
        }
        
        final String principal = principalNameLookupStrategy.apply(input);
        if (principal == null) {
            log.error("Principal lookup strategy returned null value");
            return true;
        }
        
        log.debug("Checking revocation for principal name {} for {} result via attribute resolver", principal,
                input2.getAuthenticationFlowId());
        
        final ScratchContext context = input.getOrCreateSubcontext(ScratchContext.class);
        
        if (!context.getMap().containsKey(getClass())) {
            final AttributeResolutionContext resolutionContext = buildResolutionContext(input, principal);
            assert attributeResolver != null;
            resolutionContext.resolveAttributes(attributeResolver);
            
            final Collection<Instant> records = new ArrayList<>();
            if (resolutionContext.getResolvedIdPAttributes().containsKey(attributeId)) {
                for (final IdPAttributeValue value :
                        resolutionContext.getResolvedIdPAttributes().get(attributeId).getValues()) {
                    if (value instanceof DateTimeAttributeValue) {
                        records.add(((DateTimeAttributeValue) value).getValue());
                    } else if (value instanceof StringAttributeValue) {
                        try {
                            records.add(Instant.ofEpochSecond(Long.valueOf(((StringAttributeValue) value).getValue())));
                            
                        } catch (final NumberFormatException|DateTimeException e) {
                            log.error("Error parsing timestamp '{}' into epoch",
                                    ((StringAttributeValue) value).getValue(), e);
                        }
                        
                    } else {
                        log.warn("Ignoring non-string attribute value type: {}", value.getClass().getName());
                    }
                }
            } else {
                log.debug("Resolver did not return an IdPAttribute named {} for principal {}", attributeId, principal);
            }
            
            context.getMap().put(getClass(), records);
            final BaseContext parent = resolutionContext.getParent();
            assert parent != null;
            parent.removeSubcontext(resolutionContext);
        }
        
        return isRevoked(principal, input2, (Collection<Instant>) context.getMap().get(getClass()));
    }
    
    /**
     * Build an {@link AttributeResolutionContext} to use.
     * 
     * @param profileRequestContext profile request context
     * @param principal name of principal
     * 
     * @return the attached context
     */
    @Nonnull private AttributeResolutionContext buildResolutionContext(
            @Nonnull final ProfileRequestContext profileRequestContext, @Nonnull @NotEmpty final String principal) {
        
        final AttributeResolutionContext resolutionContext = new AttributeResolutionContext();
        
        resolutionContext
            .setPrincipal(principal)
            .setResolutionLabel("authn/revocation");
        assert attributeId != null;
        resolutionContext.setRequestedIdPAttributeNames(CollectionSupport.singletonList(attributeId));
        
        if (recipientLookupStrategy != null) {
            resolutionContext.setAttributeRecipientID(recipientLookupStrategy.apply(profileRequestContext));
        }

        if (issuerLookupStrategy != null) {
            resolutionContext.setAttributeIssuerID(issuerLookupStrategy.apply(profileRequestContext));
        }
        
        profileRequestContext.addSubcontext(resolutionContext, true);
        return resolutionContext;
    }

    /**
     * Check the revocation records' timestamps for applicability.
     * 
     * @param principal name of principal
     * @param result active result being checked 
     * @param revocationRecords the records from the cache
     * 
     * @return true iff the revocation applies to this result
     */
    protected boolean isRevoked(@Nonnull @NotEmpty final String principal, @Nonnull final AuthenticationResult result,
            @Nonnull @NonnullElements final Collection<Instant> revocationRecords) {
        
        for (final Instant i : revocationRecords) {
            if (result.getAuthenticationInstant().isBefore(i)) {
                log.info("Authentication result {} for principal {} has been revoked", result.getAuthenticationFlowId(),
                        principal);
                return true;
            }
        }
        
        return false;
    }
    
}