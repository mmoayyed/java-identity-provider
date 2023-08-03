/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.assertion.ValidationContext;
import org.opensaml.saml.common.assertion.ValidationProcessingData;
import org.opensaml.saml.common.assertion.ValidationResult;
import org.opensaml.saml.saml2.assertion.SAML2AssertionValidationParameters;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Response;
import org.slf4j.Logger;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Perform processing of a SAML 2 Response's Assertions that have been validated by earlier actions
 * for use in finalization of SAML-based authentication by later actions. 
 */
public class ProcessAssertionsForAuthentication extends AbstractAuthenticationAction {
    
    /** Logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ProcessAssertionsForAuthentication.class);
    
    /** The resolver for the response to be processed. */
    @Nonnull private Function<ProfileRequestContext, Response> responseResolver;
    
    /** Lookup strategy to locate the SAML context. */
    @Nonnull private Function<ProfileRequestContext,SAMLAuthnContext> samlContextLookupStrategy;
    
    /** Selection strategy for multiple valid authn Assertions. */
    @Nonnull private Function<List<Assertion>,Assertion> authnAssertionSelectionStrategy;
    
    /** Selection strategy for multiple AuthnStatements. */
    @Nonnull private Function<Assertion,AuthnStatement> authnStatementSelectionStrategy;
    
    /** The Response to process. */
    @NonnullBeforeExec private Response response;
    
    /** The SAML authentication context. */
    @NonnullBeforeExec private SAMLAuthnContext samlAuthnContext;
    
    /**
     * Constructor.
     */
    public ProcessAssertionsForAuthentication() {
        final Function<ProfileRequestContext, Response> rr =
                new DefaultResponseResolver().compose(
                        new ChildContextLookup<>(ProfileRequestContext.class).compose(
                                new ChildContextLookup<>(AuthenticationContext.class)));
        assert rr != null;
        responseResolver = rr;
        
        // PRC -> AC -> SAMLAuthnContext
        final Function<ProfileRequestContext,SAMLAuthnContext> scls =
                new ChildContextLookup<>(SAMLAuthnContext.class).compose(
                        new ChildContextLookup<>(AuthenticationContext.class));
        assert scls!= null;
        samlContextLookupStrategy = scls;
        
        // Get the Assertion containing the earliest child AuthnStatement#SessionNotOnOrAfter,
        // with null values converted to Instant.MAX and therefore having the lowest precedence.
        authnAssertionSelectionStrategy = assertions -> assertions.stream()
                .filter(Objects::nonNull)
                // Sort with key extractor which extracts the lowest-valued AuthnStatement#SessionNotOnOrAfter value,
                // or Instant.Max if all are null
                .sorted(Comparator.<Assertion,Instant>comparing(assertion -> assertion.getAuthnStatements().stream()
                        .filter(Objects::nonNull)
                        .map(AuthnStatement::getSessionNotOnOrAfter)
                        .filter(Objects::nonNull)
                        .sorted()
                        .findFirst().orElse(Instant.MAX)))
                .findFirst().orElse(null);
            
        // Get the AuthnStatement with the earliest SessionNotOnOrAfter, with null values having lowest precedence.
        authnStatementSelectionStrategy = assertion -> assertion.getAuthnStatements().stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(AuthnStatement::getSessionNotOnOrAfter,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .findFirst().orElse(null);
    }

    /**
     * Set the strategy function for selecting which of multiple valid Assertions to use.
     * 
     * @param strategy the new strategy function
     */
    public void setAuthnAssertionSelectionStrategy(@Nonnull final Function<List<Assertion>, Assertion> strategy) {
        checkSetterPreconditions();
        authnAssertionSelectionStrategy =
                Constraint.isNotNull(strategy, "Authentication assertion selection strategy cannot be null");
    }
    
    /**
     * Set the strategy function for selecting which of multiple AuthnStatements to use.
     * 
     * @param strategy the new strategy function
     */
    public void setAuthnStatementSelectionStrategy(@Nonnull final Function<Assertion, AuthnStatement> strategy) {
        checkSetterPreconditions();
        authnStatementSelectionStrategy =
                Constraint.isNotNull(strategy, "Authentication statement selection strategy cannot be null");
    }
    
    /**
     * Set the strategy function which resolves the response to process.
     * 
     * @param strategy the new strategy function
     */
    public void setResponseResolver(@Nonnull final Function<ProfileRequestContext, Response> strategy) {
        checkSetterPreconditions();
        responseResolver = Constraint.isNotNull(strategy, "Response resolver strategy cannot be null");
    }
    
    /**
     * Set the lookup strategy used to locate the {@link SAMLAuthnContext}.
     * 
     * @param strategy the new strategy function
     */
    public void setSAMLAuthnContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SAMLAuthnContext> strategy) {
        checkSetterPreconditions();
        samlContextLookupStrategy = Constraint.isNotNull(strategy, "SAMLAuthnContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
            return false;
        }

        response = responseResolver.apply(profileRequestContext);
        if (response == null || response.getAssertions() == null || response.getAssertions().isEmpty()) {
            log.info("{} Profile context contained no candidate Assertions to process. Skipping further processing",
                    getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_CREDENTIALS);
            return false;
        }
        
        samlAuthnContext = samlContextLookupStrategy.apply(profileRequestContext);
        if (samlAuthnContext == null) {
            log.info("{} No SAMLAuthnContext available within authentication context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_CREDENTIALS);
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        // Completely remove any non-valid Assertions from the Response
        final List<Assertion> nonValid = response.getAssertions().stream()
                .filter(new AssertionIsValid().negate())
                .collect(Collectors.toList());
        log.debug("{} Removing {} non-valid Assertions from Response", getLogPrefix(), nonValid.size());
        response.getAssertions().removeAll(nonValid);

        // For authn purposes, select only Assertions which contain at least 1 AuthnStatement and a confirmed Subject
        final Predicate<Assertion> selector = new AssertionContainsAuthenticationStatement()
                .and(new AssertionContainsConfirmedSubject());
        
        final List<Assertion> assertions = response.getAssertions().stream()
                .filter(selector)
                .collect(Collectors.toList());
        if (assertions.isEmpty()) {
            log.info("{} No valid SAML Assertions suitable for authentication were found", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_CREDENTIALS);
            return;
        }
        
        Assertion authnAssertion = null;
        if (assertions.size() == 1) {
            authnAssertion = assertions.get(0);
            log.debug("{} Saw single suitable SAML Assertion, selecting for authentication", getLogPrefix());
        } else {
            log.debug("{} Attempting to select from {} suitable SAML Assertions for authentication",
                    getLogPrefix(), assertions.size());
            authnAssertion = authnAssertionSelectionStrategy.apply(assertions);
        }
        if (authnAssertion == null) {
            log.info("{} Could not select a single valid SAML Assertion for authentication", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_CREDENTIALS);
            return;
        }
        
        log.debug("{} Selected SAML Assertion for authentication: {}", getLogPrefix(), authnAssertion.getID());
        
        AuthnStatement authnStatement = null;
        if (authnAssertion.getAuthnStatements().size() == 1) {
            authnStatement = authnAssertion.getAuthnStatements().get(0);
            log.debug("{} Saw single AuthnStatement, selecting for authentication", getLogPrefix());
        } else {
            log.debug("{} Attempting to select from multiple AuthnStatements for authentication", getLogPrefix());
            authnStatement = authnStatementSelectionStrategy.apply(authnAssertion);
            if (authnStatement == null) {
                log.info("{} Could not select a single AuthnStatement for authentication", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_CREDENTIALS);
                return;
            }
        }
        
        samlAuthnContext.setAuthnStatement(authnStatement).setSubject(authnAssertion.getSubject());
    }

    /**
     * The default response resolver function. NOTE: this is relative to the nested profile request context.
     * Need to compose with other lookup function against the main/outer profile request context.
     */
    private class DefaultResponseResolver implements Function<ProfileRequestContext, Response> {

        /** {@inheritDoc} */
        @Nullable public Response apply(@Nullable final ProfileRequestContext profileContext) {
            if (profileContext != null) {
                final MessageContext imc = profileContext.ensureInboundMessageContext();
                if (imc.getMessage() instanceof Response r) {
                    return r;
                }
            }
            
            return null;
        }
        
    }
    
    /**
     * Predicate for valid assertions.
     */
    private class AssertionIsValid implements Predicate<Assertion> {

        /** {@inheritDoc} */
        public boolean test(@Nullable final Assertion assertion) {
            if (assertion == null) {
                return false;
            }
            
            final Optional<ValidationProcessingData> validationData = assertion.getObjectMetadata()
                    .get(ValidationProcessingData.class).stream().findFirst();
            if (validationData.isEmpty()) {
                return false;
            }
            
            return validationData.get().getResult() == ValidationResult.VALID;
        }
        
    }
    
    /**
     * Predicate for assertions containing at least 1 AuthenticationStatement.
     */
    private class AssertionContainsAuthenticationStatement implements Predicate<Assertion> {

        /** {@inheritDoc} */
        public boolean test(@Nullable final Assertion assertion) {
            if (assertion == null) {
                return false;
            }
            
            return ! assertion.getAuthnStatements().isEmpty();
        }
        
    }

    /**
     * Predicate for assertions which have been validated and have a confirmed Subject.
     */
    private class AssertionContainsConfirmedSubject implements Predicate<Assertion> {

        /** {@inheritDoc} */
        @SuppressWarnings("unused")
        public boolean test(@Nullable final Assertion assertion) {
            if (assertion == null) {
                return false;
            }
            
            final Optional<ValidationProcessingData> validationData = assertion.getObjectMetadata()
                    .get(ValidationProcessingData.class).stream().findFirst();
            if (validationData.isEmpty()) {
                return false;
            }
            
            final ValidationContext validationContext = validationData.get().getContext();
            if (validationContext == null) {
                return false;
            }
            
            return validationContext.getDynamicParameters()
                    .get(SAML2AssertionValidationParameters.CONFIRMED_SUBJECT_CONFIRMATION) != null;
        }
        
    }

}
