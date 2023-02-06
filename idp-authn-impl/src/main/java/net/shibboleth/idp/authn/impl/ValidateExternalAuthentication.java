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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.slf4j.Logger;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.filter.AttributeFilter;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext.Direction;
import net.shibboleth.idp.authn.AbstractValidationAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.CertificateContext;
import net.shibboleth.idp.authn.context.ExternalAuthenticationContext;
import net.shibboleth.idp.authn.principal.IdPAttributePrincipal;
import net.shibboleth.idp.authn.principal.ProxyAuthenticationPrincipal;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.profile.IdPAuditFields;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.shared.service.ServiceException;
import net.shibboleth.shared.service.ServiceableComponent;

/**
 * An action that checks for an {@link ExternalAuthenticationContext} and directly produces an
 * {@link net.shibboleth.idp.authn.AuthenticationResult} or records error state based on the
 * contents.
 *  
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link AuthnEventIds#INVALID_AUTHN_CTX}
 * @event {@link AuthnEventIds#AUTHN_EXCEPTION}
 * @event {@link AuthnEventIds#NO_CREDENTIALS}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class).getAttemptedFlow() != null</pre>
 * @post If AuthenticationContext.getSubcontext(ExternalAuthenticationContext.class) != null, then
 * an {@link net.shibboleth.idp.authn.AuthenticationResult} is saved to the {@link AuthenticationContext} on a
 * successful login. On a failed login, the
 * {@link AbstractValidationAction#handleError(ProfileRequestContext, AuthenticationContext, Exception, String)}
 * method is called.
 */
public class ValidateExternalAuthentication extends AbstractAuditingValidationAction {

    /** Default prefix for metrics. */
    @Nonnull @NotEmpty private static final String DEFAULT_METRIC_NAME = "net.shibboleth.idp.authn.external"; 

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ValidateExternalAuthentication.class);

    /** Service used to get the engine used to filter attributes. */
    @Nullable private ReloadableService<AttributeFilter> attributeFilterService;

    /** Optional supplemental metadata source for filtering. */
    @Nullable private MetadataResolver metadataResolver;
    
    /** A regular expression to apply for acceptance testing. */
    @Nullable private Pattern matchExpression;
    
    /** Context containing the result to validate. */
    @Nullable private ExternalAuthenticationContext extContext;
    
    /** Context for externally supplied inbound attributes. */
    @Nullable private AttributeContext attributeContext;
    
    /** Constructor. */
    public ValidateExternalAuthentication() {
        this(null);
    }

    /**
     * Constructor.
     *
     * @param filterService optional filter service for inbound attributes
     * 
     * @since 4.0.0
     */
    public ValidateExternalAuthentication(@Nullable final ReloadableService<AttributeFilter> filterService) {
        setMetricName(DEFAULT_METRIC_NAME);
        attributeFilterService = filterService;
    }
    
    /**
     * Set a matching expression to apply for username acceptance. 
     * 
     * @param expression a matching expression
     */
    public void setMatchExpression(@Nullable final Pattern expression) {
        checkSetterPreconditions();
        if (expression != null && !expression.pattern().isEmpty()) {
            matchExpression = expression;
        } else {
            matchExpression = null;
        }
    }

    /**
     * Set a metadata source to use during filtering.
     * 
     * @param resolver metadata resolver
     * 
     * @since 4.0.0
     */
    public void setMetadataResolver(@Nullable final MetadataResolver resolver) {
        checkSetterPreconditions();
        metadataResolver = resolver;
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
            return false;
        }
        
        extContext = authenticationContext.getSubcontext(ExternalAuthenticationContext.class);
        if (extContext == null) {
            log.debug("{} No ExternalAuthenticationContext available within authentication context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_AUTHN_CTX);
            recordFailure(profileRequestContext);
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
 // Checkstyle: ReturnCount|CyclomaticComplexity|MethodLength OFF
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        @Nonnull ExternalAuthenticationContext localExtContext = Constraint.isNotNull(extContext, "external Authn Context cannot be null");
        if (localExtContext.getAuthnException() != null) {
            log.info("{} External authentication produced exception", getLogPrefix(), localExtContext.getAuthnException());
            handleError(profileRequestContext, authenticationContext, localExtContext.getAuthnException(),
                    AuthnEventIds.AUTHN_EXCEPTION);
            recordFailure(profileRequestContext);
            return;
        } else if (localExtContext.getAuthnError() != null) {
            log.info("{} External authentication produced error message: {}", getLogPrefix(),
                    localExtContext.getAuthnError());
            handleError(profileRequestContext, authenticationContext, localExtContext.getAuthnError(),
                    AuthnEventIds.AUTHN_EXCEPTION);
            recordFailure(profileRequestContext);
            return;
        }
        if (localExtContext.getSubject() != null) {
            log.info("{} External authentication succeeded for Subject", getLogPrefix());
        } else if (localExtContext.getPrincipal() != null) {
            log.info("{} External authentication succeeded for Principal: {}", getLogPrefix(),
                    localExtContext.getPrincipal());
            localExtContext.setSubject(new Subject(false, Collections.singleton(localExtContext.getPrincipal()),
                    Collections.emptySet(), Collections.emptySet()));
        } else if (localExtContext.getPrincipalName() != null) {
            log.info("{} External authentication succeeded for user: {}", getLogPrefix(),
                    localExtContext.getPrincipalName());
            localExtContext.setSubject(new Subject(false,
                    Collections.singleton(new UsernamePrincipal(localExtContext.getPrincipalName())),
                    Collections.emptySet(), Collections.emptySet()));
        } else {
            log.info("{} External authentication failed, no user identity or error information returned",
                    getLogPrefix());
            handleError(profileRequestContext, authenticationContext, AuthnEventIds.NO_CREDENTIALS,
                    AuthnEventIds.NO_CREDENTIALS);
            return;
        }
        
        if (!checkUsername(localExtContext.getSubject())) {
            handleError(profileRequestContext, authenticationContext, AuthnEventIds.INVALID_CREDENTIALS,
                    AuthnEventIds.INVALID_CREDENTIALS);
            recordFailure(profileRequestContext);
            return;
        }
        
        recordSuccess(profileRequestContext);
        
        if (!localExtContext.getAuthenticatingAuthorities().isEmpty()) {
            final ProxyAuthenticationPrincipal proxied =
                    new ProxyAuthenticationPrincipal(localExtContext.getAuthenticatingAuthorities());
            localExtContext.getSubject().getPrincipals().add(proxied);
        }
        
        if (localExtContext.doNotCache()) {
            log.debug("{} Disabling caching of authentication result", getLogPrefix());
            authenticationContext.setResultCacheable(false);
        }
        
        filterAttributes();
        
        buildAuthenticationResult(profileRequestContext, authenticationContext);
        
        if (authenticationContext.getAuthenticationResult() != null) {
            if (localExtContext.getAuthnInstant() != null) {
                authenticationContext.getAuthenticationResult().setAuthenticationInstant(localExtContext.getAuthnInstant());
            }
            if (localExtContext.isPreviousResult()) {
                authenticationContext.getAuthenticationResult().setPreviousResult(true);
            }
        }
    }
 // Checkstyle: ReturnCount|CyclomaticComplexity|MethodLength ON
    
    /** {@inheritDoc} */
    @Override
    @Nonnull protected Subject populateSubject(@Nonnull final Subject subject) {
        // Override supplied Subject with our own, after transferring over any custom Principals
        // and adding any filtered inbound attributes.
        @Nonnull ExternalAuthenticationContext localExtContext = Constraint.isNotNull(extContext, "external Authn Context cannot be null");
        localExtContext.getSubject().getPrincipals().addAll(subject.getPrincipals());
        
        if (attributeContext != null && !attributeContext.getIdPAttributes().isEmpty()) {
            log.debug("{} Adding filtered inbound attributes to Subject", getLogPrefix());
            localExtContext.getSubject().getPrincipals().addAll(
                attributeContext.getIdPAttributes().values().stream().map(
                        (IdPAttribute a) -> new IdPAttributePrincipal(a)).collect(Collectors.toList()));
        }
        
        return localExtContext.getSubject();
    }
    
    /**
     * Validate the username if necessary.
     * 
     * @param subject   subject containing a {@link UsernamePrincipal} to check
     * 
     * @return true iff the username is acceptable
     */
    private boolean checkUsername(@Nonnull final Subject subject) {
        
        if (matchExpression != null) {
            final String name = getUsername(subject);
            if (name != null) {
                if (matchExpression.matcher(name).matches()) {
                    return true;
                }
                
                log.info("{} Username {} did not match expression", getLogPrefix(), name);
                return false;
            }
            
            log.info("{} Match expression set, but no UsernamePrincipal found");
            return false;
        }
        
        return true;
    }
    
    /**
     * Get the username from a {@link UsernamePrincipal} inside the subject.
     * 
     * @param subject input subject
     * 
     * @return username, or null
     */
    @Nullable private String getUsername(@Nonnull final Subject subject) {
        
        final Set<UsernamePrincipal> princs = subject.getPrincipals(UsernamePrincipal.class);
        if (princs != null && !princs.isEmpty()) {
            return princs.iterator().next().getName();
        }
        
        return null;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected Map<String,String> getAuditFields(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (extContext != null && extContext.getSubject() != null) {
            final String name = getUsername(extContext.getSubject());
            if (name != null) {
                return Collections.singletonMap(IdPAuditFields.USERNAME, name);
            }
        }
        
        return super.getAuditFields(profileRequestContext);
    }
    
    /**
     * Check for inbound attributes and apply filtering.
     */
    private void filterAttributes() {
        
        attributeContext = extContext.getSubcontext(AttributeContext.class);
        if (attributeContext == null) {
            log.debug("{} No attribute context, no attributes to filter", getLogPrefix());
            return;
        }

        if (attributeContext.getIdPAttributes().isEmpty()) {
            log.debug("{} No attributes to filter", getLogPrefix());
            return;
        }

        if (attributeFilterService == null) {
            log.warn("{} No AttributeFilter service provided, clearing inbound attributes", getLogPrefix());
            attributeContext.setIdPAttributes(null);
            return;
        }
        final AttributeFilterContext filterContext = extContext.getSubcontext(AttributeFilterContext.class, true);
        
        populateFilterContext(filterContext);
        
        try (final ServiceableComponent<AttributeFilter> component = attributeFilterService.getServiceableComponent()) {
            final AttributeFilter filter = component.getComponent();
            filter.filterAttributes(filterContext);
            filterContext.getParent().removeSubcontext(filterContext);
            attributeContext.setIdPAttributes(filterContext.getFilteredIdPAttributes().values());
        } catch (final AttributeFilterException e) {
            log.error("{} Error while filtering inbound attributes", getLogPrefix(), e);
            attributeContext.setIdPAttributes(null);
        } catch (final ServiceException e) {
            log.error("{} Invalid AttributeFilter configuration", getLogPrefix(), e);
            attributeContext.setIdPAttributes(null);
        }
    }
    
    /**
     * Fill in the filter context data.
     * 
     * <p>This is a very minimally populated context with nothing much set except possibly issuer,
     * based on the AuthenticationAuthorities data.</p>
     * 
     * @param filterContext context to populate
     */
    private void populateFilterContext(@Nonnull final AttributeFilterContext filterContext) {
        
        filterContext.setDirection(Direction.INBOUND)
            .setPrefilteredIdPAttributes(attributeContext.getIdPAttributes().values())
            .setMetadataResolver(metadataResolver)
            .setRequesterMetadataContextLookupStrategy(null)
            .setProxiedRequesterContextLookupStrategy(null);
        
        if (!extContext.getAuthenticatingAuthorities().isEmpty()) {
            filterContext.setAttributeIssuerID(extContext.getAuthenticatingAuthorities().iterator().next());
        }
    }

    /**
     * A default cleanup hook that removes a {@link CertificateContext} from the tree.
     * 
     * @since 4.3.0
     */
    public static class CertificateCleanupHook implements Consumer<ProfileRequestContext> {

        /** {@inheritDoc} */
        public void accept(@Nullable final ProfileRequestContext input) {
            if (input != null) {
                final AuthenticationContext authnCtx = input.getSubcontext(AuthenticationContext.class);
                if (authnCtx != null) {
                    final CertificateContext cc = authnCtx.getSubcontext(CertificateContext.class);
                    if (cc != null) {
                        authnCtx.removeSubcontext(cc);
                    }
                }
            }
        }
    }

}
