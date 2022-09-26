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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.MessageLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.InboundMessageContextLookup;
import org.opensaml.saml.common.profile.SAMLEventIds;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.SessionIndex;
import org.opensaml.saml.saml2.profile.SAML2ObjectSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;

import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.idp.profile.context.navigate.ResponderIdLookupFunction;
import net.shibboleth.idp.saml.profile.config.navigate.QualifiedNameIDFormatsLookupFunction;
import net.shibboleth.idp.saml.session.SAML2SPSession;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.idp.session.SessionResolver;
import net.shibboleth.idp.session.context.LogoutContext;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.idp.session.criterion.SPSessionCriterion;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.ResolverException;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Profile action that processes a {@link LogoutRequest} by resolving matching sessions, and destroys them,
 * populating the associated {@link SPSession} objects (excepting the one initiating the logout) into a
 * {@link LogoutContext}.
 * 
 * <p>A {@link SubjectContext} is also populated. If and only if a single {@link IdPSession} is resolved,
 * a {@link SessionContext} is also populated.</p>
 * 
 * <p>Each {@link SPSession} is also assigned a unique number and inserted into the map
 * returned by {@link LogoutContext#getKeyedSessionMap()}.</p> 
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#INVALID_MESSAGE}
 * @event {@link EventIds#IO_ERROR}
 * @event {@link SAMLEventIds#SESSION_NOT_FOUND}
 * @post If at least one {@link IdPSession} was found, then a {@link SubjectContext} and {@link LogoutContext}
 *  will be populated.
 * @post If a single {@link IdPSession} was found, then a {@link SessionContext} will be populated.
 */
public class ProcessLogoutRequest extends AbstractProfileAction {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ProcessLogoutRequest.class);
    
    /** Session resolver. */
    @NonnullAfterInit private SessionResolver sessionResolver;

    /** Creation/lookup function for SubjectContext. */
    @Nonnull private Function<ProfileRequestContext,SubjectContext> subjectContextCreationStrategy;

    /** Creation/lookup function for SessionContext. */
    @Nonnull private Function<ProfileRequestContext,SessionContext> sessionContextCreationStrategy;

    /** Creation/lookup function for LogoutContext. */
    @Nonnull private Function<ProfileRequestContext,LogoutContext> logoutContextCreationStrategy;
    
    /** Function to return {@link CriteriaSet} to give to session resolver. */
    @Nonnull private Function<ProfileRequestContext,CriteriaSet> sessionResolverCriteriaStrategy;
    
    /** Lookup strategy for {@link LogoutRequest} to process. */
    @Nonnull private Function<ProfileRequestContext,LogoutRequest> logoutRequestLookupStrategy;

    /** Lookup strategy for obtaining qualifier-defaultable NameID Formats. */
    @Nonnull private Function<ProfileRequestContext,Collection<String>> qualifiedNameIDFormatsLookupStrategy;
    
    /** Optional lookup function for obtaining default NameQualifier. */
    @Nullable private Function<ProfileRequestContext,String> assertingPartyLookupStrategy;
    
    /** Optional lookup function for obtaining default SPNameQualifier. */
    @Nullable private Function<ProfileRequestContext,String> relyingPartyLookupStrategy;
    
    /** LogoutRequest to process. */
    @Nullable private LogoutRequest logoutRequest;
    
    /** {@link NameID} Formats allowing defaulted qualifiers. */
    @Nonnull private Set<String> qualifiedNameIDFormats;
    
    /** Cached lookup of assertingParty name. */
    @Nullable private String assertingParty;
    
    /** Cached lookup of relyingParty name. */
    @Nullable private String relyingParty;
    
    /** Constructor. */
    public ProcessLogoutRequest() {
        subjectContextCreationStrategy = new ChildContextLookup<>(SubjectContext.class, true);
        sessionContextCreationStrategy = new ChildContextLookup<>(SessionContext.class, true);
        logoutContextCreationStrategy = new ChildContextLookup<>(LogoutContext.class, true);
        
        sessionResolverCriteriaStrategy = new Function<>() {
            public CriteriaSet apply(final ProfileRequestContext input) {
                if (logoutRequest != null && logoutRequest.getIssuer() != null && logoutRequest.getNameID() != null) {
                    return new CriteriaSet(new SPSessionCriterion(logoutRequest.getIssuer().getValue(),
                            logoutRequest.getNameID().getValue()));
                }
                return new CriteriaSet();
            }
        };
    
        logoutRequestLookupStrategy = new MessageLookup<>(LogoutRequest.class).compose(
                new InboundMessageContextLookup());
        
        qualifiedNameIDFormatsLookupStrategy = new QualifiedNameIDFormatsLookupFunction();

        qualifiedNameIDFormats = Collections.emptySet();
        
        setAssertingPartyLookupStrategy(new ResponderIdLookupFunction());
        setRelyingPartyLookupStrategy(new RelyingPartyIdLookupFunction());
    }
    
    /**
     * Set the {@link SessionResolver} to use.
     * 
     * @param resolver  session resolver to use
     */
    public void setSessionResolver(@Nonnull final SessionResolver resolver) {
        checkSetterPreconditions();
        sessionResolver = Constraint.isNotNull(resolver, "SessionResolver cannot be null");
    }
    
    /**
     * Set the creation/lookup strategy for the {@link SubjectContext} to populate.
     * 
     * @param strategy  creation/lookup strategy
     */
    public void setSubjectContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext,SubjectContext> strategy) {
        checkSetterPreconditions();
        subjectContextCreationStrategy = Constraint.isNotNull(strategy,
                "SubjectContext creation strategy cannot be null");
    }

    /**
     * Set the creation/lookup strategy for the {@link SessionContext} to populate.
     * 
     * @param strategy  creation/lookup strategy
     */
    public void setSessionContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext,SessionContext> strategy) {
        checkSetterPreconditions();
        sessionContextCreationStrategy = Constraint.isNotNull(strategy,
                "SessionContext creation strategy cannot be null");
    }
    
    /**
     * Set the creation/lookup strategy for the {@link LogoutContext} to populate.
     * 
     * @param strategy  creation/lookup strategy
     */
    public void setLogoutContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext,LogoutContext> strategy) {
        checkSetterPreconditions();
        logoutContextCreationStrategy = Constraint.isNotNull(strategy,
                "LogoutContext creation strategy cannot be null");
    }
    
    /**
     * Set the strategy for building the {@link CriteriaSet} to feed into the {@link SessionResolver}.
     * 
     * @param strategy  building strategy
     */
    public void setSessionResolverCriteriaStrategy(
            @Nonnull final Function<ProfileRequestContext,CriteriaSet> strategy) {
        checkSetterPreconditions();
        sessionResolverCriteriaStrategy = Constraint.isNotNull(strategy,
                "SessionResolver CriteriaSet strategy cannot be null");
    }
    
    /**
     * Set the lookup strategy for the {@link LogoutRequest} to process.
     * 
     * @param strategy  lookup strategy
     */
    public void setLogoutRequestLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,LogoutRequest> strategy) {
        checkSetterPreconditions();
        logoutRequestLookupStrategy = Constraint.isNotNull(strategy, "LogoutRequest lookup strategy cannot be null");
    }
    
    /**
     * Set the lookup strategy for the {@link NameID} Formats to allow defaulted qualifiers.
     * 
     * @param strategy lookup strategy
     * 
     * @since 3.4.0
     */
    public void setQualifiedNameIDFormatsLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Collection<String>> strategy) {
        checkSetterPreconditions();
        qualifiedNameIDFormatsLookupStrategy = Constraint.isNotNull(strategy,
                "Qualified NameID Formats lookup strategy cannot be null");
    }
    
    /**
     * Set the lookup strategy to obtain the default IdP NameQualifier.
     * 
     * @param strategy lookup strategy
     * 
     * @since 3.4.0
     */
    public void setAssertingPartyLookupStrategy(
            @Nullable final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        assertingPartyLookupStrategy = strategy;
    }
    
    /**
     * Set the lookup strategy to obtain the default SPNameQualifier.
     * 
     * @param strategy lookup strategy
     * 
     * @since 3.4.0
     */
    public void setRelyingPartyLookupStrategy(
            @Nullable final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        relyingPartyLookupStrategy = strategy;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (!getActivationCondition().equals(Predicates.alwaysFalse())) {
            if (sessionResolver == null) {
                throw new ComponentInitializationException("SessionResolver cannot be null");
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        logoutRequest = logoutRequestLookupStrategy.apply(profileRequestContext);
        if (logoutRequest == null) {
            log.warn("{} No LogoutRequest found to process", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        } else if (logoutRequest.getNameID() == null) {
            log.warn("{} LogoutRequest did not contain NameID", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MESSAGE);
            return false;
        } else if (logoutRequest.getNameID().getValue() == null) {
            log.warn("{} LogoutRequest contained an empty (therefore invalid) NameID", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MESSAGE);
            return false;
        }
        
        qualifiedNameIDFormats = new HashSet<>(qualifiedNameIDFormatsLookupStrategy.apply(profileRequestContext));
        
        return true;
    }
    
// Checkstyle: CyclomaticComplexity|ReturnCount OFF
    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        try {
            final Iterable<IdPSession> sessions =
                    sessionResolver.resolve(sessionResolverCriteriaStrategy.apply(profileRequestContext));
            final Iterator<IdPSession> sessionIterator = sessions.iterator();

            LogoutContext logoutCtx = null;
            
            int count = 1;
            
            while (sessionIterator.hasNext()) {
                final IdPSession session = sessionIterator.next();
                
                if (!sessionMatches(profileRequestContext, session)) {
                    log.debug("{} IdP session {} does not contain a matching SP session", getLogPrefix(),
                            session.getId());
                    continue;
                }

                log.debug("{} LogoutRequest matches IdP session {}", getLogPrefix(), session.getId());
                
                if (logoutCtx == null) {
                    logoutCtx = logoutContextCreationStrategy.apply(profileRequestContext);
                    if (logoutCtx == null) {
                        log.error("{} Unable to create or locate LogoutContext", getLogPrefix());
                        ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
                        return;
                    }

                    final SubjectContext subjectCtx = subjectContextCreationStrategy.apply(profileRequestContext);
                    if (subjectCtx != null) {
                        subjectCtx.setPrincipalName(session.getPrincipalName());
                    }
                }

                logoutCtx.getIdPSessions().add(session);
                
                for (final SPSession spSession : session.getSPSessions()) {
                    if (!sessionMatches(profileRequestContext, spSession)) {
                        logoutCtx.getSessionMap().put(spSession.getId(), spSession);
                        logoutCtx.getKeyedSessionMap().put(Integer.toString(count++), spSession);
                    }
                }
            }
            
            if (logoutCtx == null) {
                log.info("{} No active session(s) found matching LogoutRequest", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, SAMLEventIds.SESSION_NOT_FOUND);
            } else if (logoutCtx.getIdPSessions().size() == 1) {
                final SessionContext sessionCtx = sessionContextCreationStrategy.apply(profileRequestContext);
                if (sessionCtx != null) {
                    sessionCtx.setIdPSession(logoutCtx.getIdPSessions().iterator().next());
                }
            }

        } catch (final ResolverException e) {
            log.error("{} Error resolving matching session(s)", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, SAMLEventIds.SESSION_NOT_FOUND);
        }
    }
// Checkstyle: CyclomaticComplexity|ReturnCount ON

    /**
     * Check if the session contains a {@link SAML2SPSession} with the appropriate service ID and SessionIndex.
     * 
     * @param profileRequestContext current profile request context
     * @param session {@link IdPSession} to check
     * 
     * @return  true iff the set of {@link SPSession}s includes one applicable to the logout request
     */
    private boolean sessionMatches(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final IdPSession session) {
        
        for (final SPSession spSession : session.getSPSessions()) {
            if (sessionMatches(profileRequestContext, spSession)) {
                return true;
            }
        }
        
        return false;
    }
    
// Checkstyle: CyclomaticComplexity OFF
    /**
     * Check if the {@link SPSession} has the appropriate service ID and SessionIndex.
     * 
     * @param profileRequestContext current profile request context
     * @param session {@link SPSession} to check
     * 
     * @return  true iff the {@link SPSession} directly matches the logout request
     */
    private boolean sessionMatches(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final SPSession session) {
        if (session instanceof SAML2SPSession) {
            final SAML2SPSession saml2Session = (SAML2SPSession) session;
            
            // Make sure the SP matches.
            if (!saml2Session.getId().equals(logoutRequest.getIssuer().getValue())) {
                return false;
            } 
            
            // Use the format of the original NameID to determine whether to
            // allow the qualifiers to be defaulted. If the formats don't match
            // the eventual check will fail anyway.
            String format = saml2Session.getNameID().getFormat();
            if (format == null) {
                format = NameID.UNSPECIFIED;
            }
            if (NameID.PERSISTENT.equals(format) || NameID.TRANSIENT.equals(format)
                    || qualifiedNameIDFormats.contains(format)) {
                
                if (assertingParty == null) {
                    assertingParty = assertingPartyLookupStrategy.apply(profileRequestContext);
                }
                if (relyingParty == null) {
                    relyingParty = relyingPartyLookupStrategy.apply(profileRequestContext);
                }
                
                if (!SAML2ObjectSupport.areNameIDsEquivalent(logoutRequest.getNameID(), saml2Session.getNameID(),
                        assertingParty, relyingParty)) {
                    return false;
                }
            } else if (!SAML2ObjectSupport.areNameIDsEquivalent(logoutRequest.getNameID(), saml2Session.getNameID())) {
                return false;
            }
            
            // Check SessionIndex match.
            
            if (logoutRequest.getSessionIndexes().isEmpty()) {
                return true;
            }
            
            for (final SessionIndex index : logoutRequest.getSessionIndexes()) {
                if (index.getValue() != null && index.getValue().equals(saml2Session.getSessionIndex())) {
                    return true;
                }
            }
        }
        
        return false;
    }
// Checkstyle: CyclomaticComplexity ON
    
}