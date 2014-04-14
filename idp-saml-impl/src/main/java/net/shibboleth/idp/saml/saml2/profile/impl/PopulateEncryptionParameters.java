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

package net.shibboleth.idp.saml.saml2.profile.impl;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.OutboundMessageContextLookup;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.criterion.RoleDescriptorCriterion;
import org.opensaml.saml.saml2.profile.context.EncryptionContext;
import org.opensaml.xmlsec.SecurityConfigurationSupport;
import org.opensaml.xmlsec.EncryptionConfiguration;
import org.opensaml.xmlsec.EncryptionParameters;
import org.opensaml.xmlsec.EncryptionParametersResolver;
import org.opensaml.xmlsec.criterion.EncryptionConfigurationCriterion;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.saml.saml2.profile.config.SAML2ProfileConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;

/**
 * Action that resolves and populates {@link EncryptionParameters} on a {@link SecurityParametersContext}
 * created/accessed via a lookup function, by default on a {@link RelyingPartyContext} child of the
 * profile request context.
 * 
 * <p>The resolution process is contingent on the active profile configuration requesting encryption
 * of some kind, and an {@link EncryptionContext} is also created to capture these requirements.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#INVALID_SEC_CFG}
 * @event {@link IdPEventIds#INVALID_RELYING_PARTY_CTX}
 * @event {@link IdPEventIds#INVALID_PROFILE_CONFIG}
 */
public class PopulateEncryptionParameters extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateEncryptionParameters.class);
    
    /** Strategy used to look up a {@link RelyingPartyContext} for configuration options. */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;
    
    /** Strategy used to look up the {@link EncryptionContext} to store parameters in. */
    @Nonnull private Function<ProfileRequestContext,EncryptionContext> encryptionContextLookupStrategy;
    
    /** Strategy used to look up a per-request {@link EncryptionConfiguration}. */
    @Nullable private Function<ProfileRequestContext,EncryptionConfiguration> configurationLookupStrategy;

    /** Strategy used to look up a SAML metadata context. */
    @Nullable private Function<ProfileRequestContext,SAMLMetadataContext> metadataContextLookupStrategy;
    
    /** Resolver for parameters to store into context. */
    @NonnullAfterInit private EncryptionParametersResolver resolver;
    
    /** Is encryption optional in the case no parameters can be resolved? */
    private boolean encryptionOptional;
    
    /** Flag tracking whether assertion encryption is required. */
    private boolean encryptAssertions;

    /** Flag tracking whether assertion encryption is required. */
    private boolean encryptIdentifiers;

    /** Flag tracking whether assertion encryption is required. */
    private boolean encryptAttributes;

    /**
     * Constructor.
     * 
     * Initializes {@link #messageMetadataContextLookupStrategy} to {@link ChildContextLookup}.
     */
    public PopulateEncryptionParameters() {
        
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
        
        // Create context by default.
        encryptionContextLookupStrategy = Functions.compose(
                new ChildContextLookup<>(EncryptionContext.class, true),
                new ChildContextLookup<ProfileRequestContext,RelyingPartyContext>(RelyingPartyContext.class));

        // Default: outbound msg context -> SAMLPeerEntityContext -> SAMLMetadataContext
        metadataContextLookupStrategy = Functions.compose(
                new ChildContextLookup<>(SAMLMetadataContext.class),
                Functions.compose(new ChildContextLookup<>(SAMLPeerEntityContext.class),
                        new OutboundMessageContextLookup()));
    }
    
    /**
     * Set the strategy used to return the {@link RelyingPartyContext} for configuration options.
     * 
     * @param strategy lookup strategy
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy cannot be null");
    }
    

    /**
     * Set the strategy used to look up the {@link EncryptionContext} to set the flags for.
     * 
     * @param strategy lookup strategy
     */
    public void setEncryptionContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,EncryptionContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        encryptionContextLookupStrategy = Constraint.isNotNull(strategy,
                "EncryptionContext lookup strategy cannot be null");
    }
    
    
    /**
     * Set the strategy used to look up a per-request {@link EncryptionConfiguration}.
     * 
     * @param strategy lookup strategy
     */
    public void setConfigurationLookupStrategy(
            @Nullable final Function<ProfileRequestContext,EncryptionConfiguration> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        configurationLookupStrategy = strategy;
    }
    
    /**
     * Set lookup strategy for {@link SAMLMetadataContext} for input to resolution.
     * 
     * @param strategy  lookup strategy
     */
    public void setMetadataContextLookupStrategy(
            @Nullable final Function<ProfileRequestContext,SAMLMetadataContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        metadataContextLookupStrategy = strategy;
    }
    
    /**
     * Set the resolver to use for the parameters to store into the context.
     * 
     * @param newResolver   resolver to use
     */
    public void setEncryptionParametersResolver(
            @Nonnull final EncryptionParametersResolver newResolver) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        resolver = Constraint.isNotNull(newResolver, "EncryptionParametersResolver cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (resolver == null) {
            throw new ComponentInitializationException("EncryptionParametersResolver cannot be null");
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {
        
        final RelyingPartyContext rpContext = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpContext == null) {
            log.debug("{} Unable to locate RelyingPartyContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return false;
        } else if (rpContext.getProfileConfig() == null) {
            log.debug("{} Unable to locate RelyingPartyContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_PROFILE_CONFIG);
            return false;
        } else if (!(rpContext.getProfileConfig() instanceof SAML2ProfileConfiguration)) {
            log.debug("{} Not a SAML 2 profile configuration, nothing to do", getLogPrefix());
            return false;
        }
        
        SAML2ProfileConfiguration profileConfiguration = (SAML2ProfileConfiguration) rpContext.getProfileConfig();
        
        encryptAssertions = profileConfiguration.getEncryptAssertionsPredicate().apply(profileRequestContext);
        encryptIdentifiers = profileConfiguration.getEncryptNameIDsPredicate().apply(profileRequestContext);
        encryptAttributes = profileConfiguration.getEncryptAttributesPredicate().apply(profileRequestContext);
        
        if (!encryptAssertions && !encryptIdentifiers && !encryptAttributes) {
            log.debug("{} No encryption requested, nothing to do", getLogPrefix());
            return false;
        }
        
        encryptionOptional = profileConfiguration.isEncryptionOptional();
        
        log.debug("{} Encryption for assertions ({}), identifiers ({}), attributes({})", getLogPrefix(),
                encryptAssertions, encryptIdentifiers, encryptAttributes);
        
        return super.doPreExecute(profileRequestContext);
    }
    
// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {

        log.debug("{} Resolving EncryptionParameters for request", getLogPrefix());
        
        final EncryptionContext encryptCtx = encryptionContextLookupStrategy.apply(profileRequestContext);
        if (encryptCtx == null) {
            log.debug("{} No EncryptionContext returned by lookup strategy", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return;
        }
        
        // TODO: do we include anything but the global default and the per-profile configs?
        // Maybe a global IdP config in addition or instead of the OpenSAML one?
        
        final List<EncryptionConfiguration> configs = Lists.newArrayList();
        configs.add(SecurityConfigurationSupport.getGlobalEncryptionConfiguration());
        
        if (configurationLookupStrategy != null) {
            log.debug("{} Looking up per-request EncryptionConfiguration", getLogPrefix());
            final EncryptionConfiguration perRequestConfig = configurationLookupStrategy.apply(profileRequestContext);
            if (perRequestConfig != null) {
                configs.add(perRequestConfig);
            }
        }
        
        final CriteriaSet criteria = new CriteriaSet(new EncryptionConfigurationCriterion(configs));
        
        if (metadataContextLookupStrategy != null) {
            final SAMLMetadataContext metadataCtx = metadataContextLookupStrategy.apply(profileRequestContext);
            if (metadataCtx != null && metadataCtx.getRoleDescriptor() != null) {
                log.debug("{} Adding metadata to resolution criteria for signing/digest algorithms", getLogPrefix());
                criteria.add(new RoleDescriptorCriterion(metadataCtx.getRoleDescriptor()));
            }
        }
        
        try {
            final EncryptionParameters params = resolver.resolveSingle(criteria);
            log.debug("{} {} EncryptionParameters", getLogPrefix(),
                    params != null ? "Resolved" : "Failed to resolve");
            if (params != null) {
                if (encryptAssertions) {
                    encryptCtx.setAssertionEncryptionParameters(params);
                }
                if (encryptIdentifiers) {
                    encryptCtx.setIdentifierEncryptionParameters(params);
                }
                if (encryptAttributes) {
                    encryptCtx.setAttributeEncryptionParameters(params);
                }
            } else {
                log.warn("{} Resolver returned no EncryptionParameters", getLogPrefix());
                if (encryptionOptional) {
                    log.info("{} Encryption is optional, ignoring inability to encrypt", getLogPrefix());
                } else {
                    ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_SEC_CFG);
                }
            }
        } catch (final ResolverException e) {
            log.error(getLogPrefix() + " Error resolving EncryptionParameters", e);
            if (encryptionOptional) {
                log.info("{} Encryption is optional, ignoring inability to encrypt", getLogPrefix());
            } else {
                ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_SEC_CFG);
            }
        }
    }
// Checkstyle: CyclomaticComplexity ON
    
}