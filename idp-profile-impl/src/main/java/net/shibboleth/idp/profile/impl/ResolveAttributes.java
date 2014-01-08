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

import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.idp.service.ReloadableService;
import net.shibboleth.idp.service.ServiceableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.AbstractProfileAction;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

/**
 * Action that invokes the {@link AttributeResolver} for the current request.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link IdPEventIds#INVALID_RELYING_PARTY_CTX}
 * @event {@link IdPEventIds#INVALID_SUBJECT_CTX}
 * @event {@link IdPEventIds#UNABLE_RESOLVE_ATTRIBS}
 * 
 * @post If resolution is successful, the relevant RelyingPartyContext.getSubcontext(AttributeContext.class, false) !=
 *       null
 */
public final class ResolveAttributes extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ResolveAttributes.class);

    /** Service used to get the resolver used to fetch attributes. */
    @Nonnull private final ReloadableService<AttributeResolver> attributeResolverService;

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    @Nonnull private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextLookupStrategy;

    /**
     * Strategy used to locate the {@link SubjectContext} associated with a given {@link ProfileRequestContext}.
     */
    @Nonnull private Function<ProfileRequestContext, SubjectContext> subjectContextLookupStrategy;

    /**
     * Strategy used to locate the {@link AuthenticationContext} associated with a given {@link ProfileRequestContext}.
     */
    @Nonnull private Function<ProfileRequestContext, AuthenticationContext> authnContextLookupStrategy;

    /** RelyingPartyContext to operate on. */
    @Nullable private RelyingPartyContext rpContext;

    /** SubjectContext to work from. */
    @Nullable private SubjectContext subjectContext;

    /** AuthenticationContext to work from (if any). */
    @Nullable private AuthenticationContext authenticationContext;

    /**
     * Constructor. Initializes {@link #relyingPartyContextLookupStrategy}, {@link #authnContextLookupStrategy} and
     * {@link #subjectContextLookupStrategy} to {@link ChildContextLookup}.
     * 
     * @param resolverService resolver used to fetch attributes
     */
    public ResolveAttributes(@Nonnull final ReloadableService<AttributeResolver> resolverService) {
        attributeResolverService = Constraint.isNotNull(resolverService, "AttributeResolver cannot be null");
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class, false);
        subjectContextLookupStrategy = new ChildContextLookup<>(SubjectContext.class, false);
        authnContextLookupStrategy = new ChildContextLookup<>(AuthenticationContext.class, false);
    }

    /**
     * Set the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link RelyingPartyContext} associated with a given
     *            {@link ProfileRequestContext}
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the {@link SubjectContext} associated with a given {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link SubjectContext} associated with a given
     *            {@link ProfileRequestContext}
     */
    public void setSubjectContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, SubjectContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        subjectContextLookupStrategy = Constraint.isNotNull(strategy, "SubjectContext lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the {@link AuthenticationContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link AuthenticationContext} associated with a given
     *            {@link ProfileRequestContext}
     */
    public void setAuthenticationContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, AuthenticationContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        authnContextLookupStrategy =
                Constraint.isNotNull(strategy, "AuthenticationContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {

        rpContext = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpContext == null) {
            log.debug("{} No relying party context available.", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return false;
        }

        subjectContext = subjectContextLookupStrategy.apply(profileRequestContext);
        if (subjectContext == null) {
            log.debug("{} No subject context available.", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_SUBJECT_CTX);
            return false;
        }

        authenticationContext = authnContextLookupStrategy.apply(profileRequestContext);
        if (authenticationContext == null) {
            log.debug("{} No authentication context available.", getLogPrefix());
        }

        return super.doPreExecute(profileRequestContext);
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {

        // Get the resolution context from the profile request
        // this may already exist but if not, auto-create it
        final AttributeResolutionContext resolutionContext =
                profileRequestContext.getSubcontext(AttributeResolutionContext.class, true);
        
        resolutionContext.setPrincipal(subjectContext.getPrincipalName());
        
        // TODO(rdw) This navigation is subject to change

        resolutionContext.setPrincipalAuthenticationMethod(null);
        if (null != authenticationContext) {
            final AuthenticationResult result = authenticationContext.getAuthenticationResult();
            if (null != result) {
                resolutionContext.setPrincipalAuthenticationMethod(result.getAuthenticationFlowId());
            }
        }

        resolutionContext.setAttributeRecipientID(rpContext.getRelyingPartyId());

        // TODO(rdw) This navigation is subject to change
        final RelyingPartyConfiguration config = rpContext.getConfiguration();
        if (null != config) {
            resolutionContext.setAttributeIssuerID(config.getResponderEntityId());
        } else {
            resolutionContext.setAttributeIssuerID(null);
        }

        ServiceableComponent<AttributeResolver> component = null;
        try {
            component = attributeResolverService.getServiceableComponent();
            if (null == component) {
                log.error("{} Error resolving attributes: Invalid Attribute resolver configuration.", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, IdPEventIds.UNABLE_RESOLVE_ATTRIBS);
            } else {
                final AttributeResolver attributeResolver = component.getComponent();
                attributeResolver.resolveAttributes(resolutionContext);
                profileRequestContext.removeSubcontext(resolutionContext);

                final AttributeContext attributeCtx = rpContext.getSubcontext(AttributeContext.class, true);
                attributeCtx.setIdPAttributes(resolutionContext.getResolvedIdPAttributes().values());

            }
        } catch (ResolutionException e) {
            log.error("{} Error resolving attributes", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.UNABLE_RESOLVE_ATTRIBS);
        } finally {
            if (null != component) {
                component.unpinComponent();
            }
        }
    }

}