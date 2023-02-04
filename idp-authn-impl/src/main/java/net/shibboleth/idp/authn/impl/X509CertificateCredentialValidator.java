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

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.security.SecurityException;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.security.x509.X509Credential;
import org.slf4j.Logger;

import net.shibboleth.idp.authn.AbstractCredentialValidator;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.CertificateContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.shared.annotation.constraint.ThreadSafeAfterInit;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.resolver.CriteriaSet;

/**
 * A credential validator that validates an X.509 certificate.
 * 
 * @since 4.2.0
 */
@ThreadSafeAfterInit
public class X509CertificateCredentialValidator extends AbstractCredentialValidator {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(X509CertificateCredentialValidator.class);

    /** Lookup strategy for cert context. */
    @Nonnull private Function<AuthenticationContext,CertificateContext> certContextLookupStrategy;

    /** Optional trust engine to validate certificates against. */
    @Nullable private TrustEngine<? super X509Credential> trustEngine;
    
    /** Whether to save the certificate in the Java Subject's public credentials. */
    private boolean saveCertificateToCredentialSet;
    
    /** Constructor. */
    public X509CertificateCredentialValidator() {
        certContextLookupStrategy = new ChildContextLookup<>(CertificateContext.class);
    }
    
    /**
     * Set the lookup strategy to locate the {@link UsernamePasswordContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setCertificateContextLookupStrategy(
            @Nonnull final Function<AuthenticationContext,CertificateContext> strategy) {
        checkSetterPreconditions();
        certContextLookupStrategy = Constraint.isNotNull(strategy,
                "CertificateContextLookupStrategy cannot be null");
    }
    
    /**
     * Set a {@link TrustEngine} to use.
     * 
     * @param tm trust engine to use  
     */
    public void setTrustEngine(@Nullable final TrustEngine<? super X509Credential> tm) {
        checkSetterPreconditions();
        trustEngine = tm;
    }
    
    /**
     * Set whether to save the certificate in the Java Subject's public credentials.
     * 
     * <p>Defaults to true</p>
     * 
     * @param flag flag to set
     */
    public void setSaveCertificateToCredentialSet(final boolean flag) {
        checkSetterPreconditions();
        saveCertificateToCredentialSet = flag;
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    @Nullable protected Subject doValidate(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext,
            @Nullable final WarningHandler warningHandler,
            @Nullable final ErrorHandler errorHandler) throws Exception {

        final CertificateContext certContext = certContextLookupStrategy.apply(authenticationContext);
        if (certContext == null) {
            log.debug("{} No CertificateContext available within authentication context", getLogPrefix());
            return null;
        } else if (certContext.getCertificate() == null || !(certContext.getCertificate() instanceof X509Certificate)) {
            log.debug("{} No X.509 certificate available within CertificateContext", getLogPrefix());
            return null;
        }

        if (trustEngine != null) {
            log.debug("{} Attempting to validate certificate using trust engine", getLogPrefix());
            try {
                final BasicX509Credential cred =
                        new BasicX509Credential((X509Certificate) certContext.getCertificate());
                if (!certContext.getIntermediates().isEmpty()) {
                    cred.getEntityCertificateChain().add((X509Certificate) certContext.getCertificate());
                    for (final Certificate extra : certContext.getIntermediates()) {
                        if (extra instanceof X509Certificate) {
                            cred.getEntityCertificateChain().add((X509Certificate) extra);
                        }
                    }
                }
                if (trustEngine.validate(cred, new CriteriaSet())) {
                    log.debug("{} Trust engine validated X.509 certificate", getLogPrefix());
                } else {
                    log.warn("{} Trust engine failed to validate X.509 certificate", getLogPrefix());
                    final LoginException e = new LoginException(AuthnEventIds.INVALID_CREDENTIALS);
                    if (errorHandler != null) {
                        errorHandler.handleError(profileRequestContext, authenticationContext, e,
                                AuthnEventIds.INVALID_CREDENTIALS);
                    }
                    throw e;
                }
            } catch (final SecurityException e) {
                log.error("{} Exception raised by trust engine", getLogPrefix(), e);
                if (errorHandler != null) {
                    errorHandler.handleError(profileRequestContext, authenticationContext, e,
                            AuthnEventIds.INVALID_CREDENTIALS);
                }
                throw e;
            }
        } else {
            log.debug("{} No trust engine configured, certificate will be trusted", getLogPrefix());
        }

        log.info("{} Login by '{}' succeeded", getLogPrefix(),
                ((X509Certificate) certContext.getCertificate()).getSubjectX500Principal().getName());
        
        return populateSubject((X509Certificate) certContext.getCertificate());
    }
// Checkstyle: CyclomaticComplexity ON
    
    /**
     * Builds a subject with "standard" content from the validation.
     *
     * @param certificate the certificate validated
     * 
     * @return the decorated subject
     */
    @Nonnull protected Subject populateSubject(@Nonnull final X509Certificate certificate) {
       
        final Subject subject = new Subject();
       
        subject.getPrincipals().add(certificate.getSubjectX500Principal());
        if (saveCertificateToCredentialSet) {
            subject.getPublicCredentials().add(certificate);
        }
       
        return super.populateSubject(subject);
    }

}