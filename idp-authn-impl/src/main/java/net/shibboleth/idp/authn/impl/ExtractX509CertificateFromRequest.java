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

package net.shibboleth.idp.authn.impl;


import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.authn.AbstractExtractionAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.CertificateContext;
import net.shibboleth.shared.primitive.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;


/**
 * An action that extracts an X.509 certificate from the standard servlet request attribute,
 * creates a {@link CertificateContext}, and attaches it to the {@link AuthenticationContext}.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link AuthnEventIds#NO_CREDENTIALS}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class) != null</pre>
 * @post If getHttpServletRequest() != null, the content of the "jakarta.servlet.request.X509Certificate"
 * request attribute is attached to a {@link CertificateContext}.
 */
public class ExtractX509CertificateFromRequest extends AbstractExtractionAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ExtractX509CertificateFromRequest.class);
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        final CertificateContext certCtx = new CertificateContext();
        authenticationContext.addSubcontext(certCtx, true);
        
        final HttpServletRequest httpRequest = getHttpServletRequest();
        if (httpRequest == null) {
            log.warn("{} Profile action does not contain an HttpServletRequest", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return;
        }
        
        X509Certificate[] certs =
                (X509Certificate[]) httpRequest.getAttribute("jakarta.servlet.request.X509Certificate");
        if (certs == null || certs.length == 0) {
            // Check for older Java variant (probably moot at this point).
            certs = (X509Certificate[]) httpRequest.getAttribute("javax.servlet.request.X509Certificate");
        }

        if (certs == null || certs.length == 0) {
            log.info("{} No X.509 certificate found in request", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return;
        }

        log.debug("{} {} X.509 Certificate(s) found in request", getLogPrefix(), certs.length);
        
        final X509Certificate cert = certs[0];
        log.debug("{} End-entity X.509 certificate found with subject '{}', issued by '{}'", getLogPrefix(),
                cert.getSubjectX500Principal().getName(), cert.getIssuerX500Principal().getName());

        certCtx.setCertificate(cert);

        // Add the rest starting at index 1.
        for (int i = 1; i < certs.length; i++) {
            certCtx.getIntermediates().add(certs[i]);
        }
    }

}