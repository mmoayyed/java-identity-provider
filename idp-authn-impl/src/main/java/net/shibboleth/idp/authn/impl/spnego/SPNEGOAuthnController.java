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

package net.shibboleth.idp.authn.impl.spnego;

import java.io.IOException;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.authn.ExternalAuthentication;
import net.shibboleth.idp.authn.ExternalAuthenticationException;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.codec.HTMLEncoder;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.apache.commons.codec.binary.Base64;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSName;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for running SPNEGO.
 * 
 * The handler methods either return contents back to the browser by returning an appropriate ResponseEntity<String>
 * object, or they return back to the flow by calling ExternalAuthentication.finishExternalAuthentication() and
 * returning null. On unrecoverable errors, an exception is thrown.
 */
@Controller
@RequestMapping("/Authn/SPNEGO")
public class SPNEGOAuthnController {
    /**
     * Error message indicating that SPNEGO is not supported by the client or is not available for other reasons.
     */
    public static final String ERROR_SPNEGO_NOT_AVAILABLE = "SPNEGONotAvailable";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SPNEGOAuthnController.class);

    /**
     * Handle initial request that starts SPNEGO.
     * 
     * @param conversationKey the SWF conversation key
     * @param httpRequest the HTTP request
     * @param httpResponse the HTTP response
     * @return the view name
     * @throws ExternalAuthenticationException
     * @throws IOException
     */
    @RequestMapping(value = "/{conversationKey}", method = RequestMethod.GET)
    public ModelAndView startSPNEGO(@PathVariable String conversationKey, @Nonnull HttpServletRequest httpRequest,
            @Nonnull HttpServletResponse httpResponse) throws ExternalAuthenticationException, IOException {
        ProfileRequestContext prc = null;
        try {
            String key = ExternalAuthentication.startExternalAuthentication(httpRequest);
            Constraint.isTrue(key.equals(conversationKey), "Conversation key mismatch");
            prc = ExternalAuthentication.getProfileRequestContext(key, httpRequest);
        } catch (final ExternalAuthenticationException e) {
            log.error("Exception while getting ProfileRequestContext", e);
            finishWithException(conversationKey, httpRequest, httpResponse, e);
            return null;
        }
        if (getKerberosSettingsFromContext(prc) == null) {
            log.error("Kerberos settings not found in profile");
            finishWithError(conversationKey, httpRequest, httpResponse, ERROR_SPNEGO_NOT_AVAILABLE);
            return null;
        }
        // start the SPNEGO exchange
        log.trace("SPNEGO negotiation started. Answering request with 401 (WWW-Authenticate: Negotiate)");
        return replyUnauthorizedNegotiate(httpRequest, httpResponse);
    }

    @RequestMapping(value = "/{conversationKey}", method = RequestMethod.GET, headers = "Authorization")
    public ModelAndView continueSPNEGO(@PathVariable String conversationKey,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @Nonnull HttpServletRequest httpRequest, @Nonnull HttpServletResponse httpResponse)
            throws ExternalAuthenticationException, IOException {
        /**
         * Kerberos configuration: general settings (see conf/authn/spnego-authn-config.xml)
         * 
         * kerberosSettings: getKerberosCfg(): kerberos configuration file (e.g.: /etc/krb5.conf) (required)
         * getCustomUnauthorized(): filesystem path to custom unauthorized html file (optional)
         */
        /**
         * Kerberos configuration: realms (see conf/authn/spnego-authn-config.xml)
         * 
         * kerberosRealms: List of KerberosRealmSettings KerberosRealmSettings: getDomain(): kerberos domain (required)
         * getPrincipal(): kerberos service principal (required) getKeytab(): path to the keytab file containing the
         * kerberos service principal's credentials (optional; either "p:keytab" or "p:password" is required)
         * getPassword(): kerberos service principal's password (optional; either "p:keytab" or "p:password" is
         * required)
         */
        GSSContextAcceptor krbGSSContextAcceptor = null;
        try {
            // get SPNEGOContext from AuthenticationContext
            ProfileRequestContext prc = ExternalAuthentication.getProfileRequestContext(conversationKey, httpRequest);
            KerberosSettings kerberosSettings = getKerberosSettingsFromContext(prc);
            if (kerberosSettings != null) {
                krbGSSContextAcceptor = createKrbGSSContextAcceptor(kerberosSettings);
            } else {
                log.error("Kerberos settings not found in profile");
                finishWithError(conversationKey, httpRequest, httpResponse, ERROR_SPNEGO_NOT_AVAILABLE);
                return null;
            }
        } catch (final ExternalAuthenticationException e) {
            if (conversationKey == null) {
                throw e;
            } else {
                finishWithException(conversationKey, httpRequest, httpResponse, e);
                return null;
            }
        } catch (GSSException e) {
            log.error("Couldn't create KrbContextAcceptor");
            finishWithException(conversationKey, httpRequest, httpResponse, e);
            return null;
        }

        if (!authorizationHeader.startsWith("Negotiate ")) {
            return replyUnauthorizedNegotiate(httpRequest, httpResponse);
        }

        byte[] gssapiData = Base64.decodeBase64(authorizationHeader.substring(10).getBytes());
        log.trace("SPNEGO negotiation. Authorization header received. gssapi-data: {}", gssapiData);

        // NTLM Authentication is not supported
        if (isNTLMMechanism(gssapiData)) {
            log.error("Security context fail. Unsupported NTLM mechanism");
            finishWithError(conversationKey, httpRequest, httpResponse, ERROR_SPNEGO_NOT_AVAILABLE);
            return null;
        }

        byte[] tokenBytes;
        try {
            tokenBytes = krbGSSContextAcceptor.acceptSecContext(gssapiData, 0, gssapiData.length);
            log.trace("Security context accepted");
        } catch (Exception e) {
            log.error("Error validation the security context", e);
            finishWithException(conversationKey, httpRequest, httpResponse, e);
            return null;
        }

        /**
         * If the context is established, we can attempt to retrieve the name of the "context initiator." In the case of
         * the Kerberos mechanism, the context initiator is the Kerberos principal of the client. Additionally, the
         * client may be delegating credentials.
         */
        if (krbGSSContextAcceptor.getContext() != null && krbGSSContextAcceptor.getContext().isEstablished()) {
            log.debug("GSS security context is complete.");

            GSSName clientGSSName;
            try {
                clientGSSName = krbGSSContextAcceptor.getContext().getSrcName();
                final KerberosPrincipal kerberosPrincipal = new KerberosPrincipal(clientGSSName.toString());

                // TODO: Is there an use-case where the 'GSSName' is not the same as 'principal name'?
                log.info("SPNEGO/Kerberos authentication succeeded. Principal: " + clientGSSName.toString());

                // finish the external authentication task and return to the flow
                finishWithSuccess(conversationKey, httpRequest, httpResponse, kerberosPrincipal);
                return null;
            } catch (GSSException e) {
                log.error("Error getting the principal name from security context.", e);
                finishWithException(conversationKey, httpRequest, httpResponse, e);
                return null;
            }
        } else {
            // The context is not complete yet.
            // return "WWW-Authenticate: Negotiate <data>" to the browser
            log.trace("SPNEGO negotiation in process. Answering request with 401 (WWW-Authenticate: Negotiate [gssapi-data])");
            return replyUnauthorizedNegotiate(httpRequest, httpResponse, Base64.encodeBase64String(tokenBytes));
        }
    }

    // handle errors detected in the browser
    @RequestMapping(value = "/{conversationKey}/error", method = RequestMethod.GET)
    public void handleError(@PathVariable String conversationKey, @Nonnull HttpServletRequest httpRequest,
            @Nonnull HttpServletResponse httpResponse) throws ExternalAuthenticationException, IOException {

        // finished; signal error
        log.warn("SPNEGO authentication problem");

        try {
            finishWithError(conversationKey, httpRequest, httpResponse, ERROR_SPNEGO_NOT_AVAILABLE);
        } catch (final ExternalAuthenticationException e) {
            // handle ExternalAuthenticationException and other exceptions
            if (conversationKey != null) {
                finishWithException(conversationKey, httpRequest, httpResponse, e);
            }
        }
    }

    protected GSSContextAcceptor createKrbGSSContextAcceptor(KerberosSettings kerberosSettings) throws GSSException {
        return new GSSContextAcceptor(kerberosSettings);
    }

    /**
     * Finish on success. Sets the attribute ExternalAuthentication.SUBJECT_KEY.
     * 
     * @param key the conversation key
     * @param httpRequest the HTTP request
     * @param httpResponse the HTTP response
     * @param subject the Kerberos principal to return
     * @throws ExternalAuthenticationException
     * @throws IOException
     */
    private void finishWithSuccess(@Nonnull @NotEmpty String key, @Nonnull HttpServletRequest httpRequest,
            @Nonnull HttpServletResponse httpResponse, @Nonnull KerberosPrincipal kerberosPrincipal)
            throws ExternalAuthenticationException, IOException {

        Constraint.isNotNull(httpRequest, "httpRequest must not be null");
        Constraint.isNotNull(kerberosPrincipal, "kerberosPrincipal must not be null");

        // store the user as a username and as a real KerberosPrincipal object
        final Subject subject = new Subject();
        subject.getPrincipals().add(new UsernamePrincipal(kerberosPrincipal.getName()));
        subject.getPrincipals().add(kerberosPrincipal);

        // finish the external authentication task and return to the flow
        httpRequest.setAttribute(ExternalAuthentication.SUBJECT_KEY, subject);
        ExternalAuthentication.finishExternalAuthentication(key, httpRequest, httpResponse);
    }

    /**
     * Finish on error. Sets the attribute ExternalAuthentication.AUTHENTICATION_ERROR_KEY.
     * 
     * @param key the conversation key
     * @param httpRequest the HTTP request
     * @param httpResponse the HTTP response
     * @param error the error string to return (e.g. AuthnEventIds.NO_CREDENTIALS)
     * @throws ExternalAuthenticationException
     * @throws IOException
     */
    private void finishWithError(@Nonnull @NotEmpty String key, @Nonnull HttpServletRequest httpRequest,
            @Nonnull HttpServletResponse httpResponse, @Nonnull @NotEmpty String error)
            throws ExternalAuthenticationException, IOException {

        Constraint.isNotNull(httpRequest, "httpRequest must not be null");

        // finish the external authentication task and return to the flow
        httpRequest.setAttribute(ExternalAuthentication.AUTHENTICATION_ERROR_KEY, error);
        ExternalAuthentication.finishExternalAuthentication(key, httpRequest, httpResponse);
    }

    /**
     * Finish on error. Sets the attribute ExternalAuthentication.AUTHENTICATION_EXCEPTION_KEY.
     * 
     * @param key the conversation key
     * @param httpRequest the HTTP request
     * @param httpResponse the HTTP response
     * @param ex the exception that has been thrown
     * @throws ExternalAuthenticationException
     * @throws IOException
     */
    private void finishWithException(@Nonnull @NotEmpty String key, @Nonnull HttpServletRequest httpRequest,
            @Nonnull HttpServletResponse httpResponse, @Nonnull Exception ex) throws ExternalAuthenticationException,
            IOException {

        Constraint.isNotNull(httpRequest, "httpRequest must not be null");

        // finish the external authentication task and return to the flow
        httpRequest.setAttribute(ExternalAuthentication.AUTHENTICATION_EXCEPTION_KEY, ex);
        ExternalAuthentication.finishExternalAuthentication(key, httpRequest, httpResponse);
    }

    @Nullable
    private KerberosSettings getKerberosSettingsFromContext(@Nonnull ProfileRequestContext prc) {
        AuthenticationContext authnContext = prc.getSubcontext(AuthenticationContext.class);
        if (authnContext != null) {
            SPNEGOContext spnegoContext = authnContext.getSubcontext(SPNEGOContext.class);
            if (spnegoContext != null) {
                return spnegoContext.getKerberosSettings();
            }
        }
        return null;
    }

    @Nonnull
    private ModelAndView replyUnauthorizedNegotiate(@Nonnull HttpServletRequest httpRequest,
            @Nonnull HttpServletResponse httpResponse) {
        return replyUnauthorizedNegotiate(httpRequest, httpResponse, "");
    }

    @Nonnull
    private ModelAndView replyUnauthorizedNegotiate(@Nonnull HttpServletRequest httpRequest,
            @Nonnull HttpServletResponse httpResponse, @Nonnull String base64Token) {
        StringBuilder authenticateHeader = new StringBuilder("Negotiate");
        if (!base64Token.isEmpty()) {
            authenticateHeader.append(" " + base64Token);
        }
        httpResponse.addHeader(HttpHeaders.WWW_AUTHENTICATE, authenticateHeader.toString());
        httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
        return createModelAndView(httpRequest);
    }

    /**
     * Create a ModelAndView object to return.
     * 
     * @param httpRequest the HTTP request
     * @return the ModelAndView object to return
     */
    @Nonnull
    private ModelAndView createModelAndView(@Nonnull HttpServletRequest httpRequest) {
        ModelAndView modelAndView = new ModelAndView("spnego-unauthorized");
        modelAndView.addObject("request", httpRequest);
        modelAndView.addObject("encoder", HTMLEncoder.class);
        StringBuffer errorUrl = httpRequest.getRequestURL();
        errorUrl.append("/error");
        String queryString = httpRequest.getQueryString();
        if (queryString != null) {
            errorUrl.append("?").append(queryString);
        }
        modelAndView.addObject("errorUrl", errorUrl.toString());
        return modelAndView;
    }

    /**
     * Check if the GSSApi data represents a NTLM mechanism request.
     * 
     * @param gssapiData Byte array retrieved from the Authorization header.
     * @return true if it represents a NTLM mechanism.
     */
    private boolean isNTLMMechanism(byte[] gssapiData) {
        byte NTLMSSP[] = {(byte) 0x4E, (byte) 0x54, (byte) 0x4C, (byte) 0x4D, (byte) 0x53, (byte) 0x53, (byte) 0x50};
        return Arrays.equals(NTLMSSP, Arrays.copyOfRange(gssapiData, 0, 7));
    }
}
