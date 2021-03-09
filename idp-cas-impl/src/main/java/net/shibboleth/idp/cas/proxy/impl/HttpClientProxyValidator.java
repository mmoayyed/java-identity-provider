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

package net.shibboleth.idp.cas.proxy.impl;

import static org.opensaml.security.httpclient.HttpClientSecurityConstants.CONTEXT_KEY_CRITERIA_SET;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.auth.login.CredentialException;
import javax.security.auth.login.FailedLoginException;

import net.shibboleth.idp.cas.config.AbstractProtocolConfiguration;
import net.shibboleth.idp.cas.protocol.ProtocolContext;
import net.shibboleth.idp.cas.proxy.ProxyValidator;
import net.shibboleth.idp.cas.service.Service;
import net.shibboleth.idp.cas.service.ServiceContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.criterion.ProtocolCriterion;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.criteria.UsageCriterion;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.opensaml.security.httpclient.HttpClientSecuritySupport;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.x509.TrustedNamesCriterion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authenticates a CAS proxy callback endpoint using an {@link org.apache.http.client.HttpClient} instance to establish
 * the connection and a {@link TrustEngine} to verify the TLS certificate presented by the remote peer. The endpoint
 * is validated if and only if the following requirements are met:
 *
 * <ol>
 *     <li>Proxy callback URI specifies the <code>https</code> scheme.</li>
 *     <li>The TLS certificate presented by the remote peer is trusted.</li>
 *     <li>The HTTP response status code is approved via {@link #setAllowedResponseCodes(Set)}
 *     (only 200 by default).</li>
 * </ol>
 *
 * @author Marvin S. Addison
 */
public class HttpClientProxyValidator implements ProxyValidator {

    /** Required https scheme for proxy callbacks. */
    @Nonnull @NotEmpty protected static final String HTTPS_SCHEME = "https";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(HttpClientProxyValidator.class);

    /** Looks up a ServiceContext from the profile request context. */
    @Nonnull private final Function<ProfileRequestContext,ServiceContext> serviceCtxLookupFunction =
            new ChildContextLookup<>(ServiceContext.class).compose(
                    new ChildContextLookup<>(ProtocolContext.class));

    /** HTTP client that connects to proxy callback endpoint. */
    @Nonnull private final HttpClient httpClient;

    /** HTTP client security parameters. */
    @Nonnull private final HttpClientSecurityParameters securityParameters;

    /** List of HTTP response codes permitted for successful proxy callback. */
    @NotEmpty @NonnullElements private Set<Integer> allowedResponseCodes;

    /**
     * 
     * Constructor.
     *
     * @param client HttpClient to use
     * @param parameters security parameters for client
     */
    public HttpClientProxyValidator(
            @Nonnull final HttpClient client, @Nonnull final HttpClientSecurityParameters parameters) {
        httpClient = Constraint.isNotNull(client, "HTTP client cannot be null");
        securityParameters = Constraint.isNotNull(parameters, "HTTP client security parameters cannot be null");
        allowedResponseCodes = Collections.singleton(200);
    }

    /**
     * Sets the HTTP response codes permitted for successful authentication of the proxy callback URL.
     *
     * @param responseCodes One or more HTTP response codes.
     */
    public void setAllowedResponseCodes(@NotEmpty @NonnullElements final Set<Integer> responseCodes) {
        Constraint.isNotEmpty(responseCodes, "Response codes cannot be null or empty.");
        Constraint.noNullItems(responseCodes.toArray(), "Response codes cannot contain null elements.");
        allowedResponseCodes = Set.copyOf(responseCodes);
    }

    /** {@inheritDoc} */
    @Override
    public void validate(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final URI proxyCallbackUri) throws GeneralSecurityException {

        Constraint.isNotNull(proxyCallbackUri, "Proxy callback URI cannot be null");
        if (!HTTPS_SCHEME.equalsIgnoreCase(proxyCallbackUri.getScheme())) {
            throw new GeneralSecurityException(proxyCallbackUri + " is not an https URI as required.");
        }
        final ServiceContext serviceContext = serviceCtxLookupFunction.apply(profileRequestContext);
        if (serviceContext == null) {
            throw new IllegalStateException("Service context not found in profile request context as required");
        }
        final int status = connect(proxyCallbackUri, serviceContext.getService());
        if (!allowedResponseCodes.contains(status)) {
            throw new FailedLoginException(proxyCallbackUri + " returned unacceptable HTTP status code: " + status);
        }
    }

    /**
     * Connect to the given CAS proxy callback endpoint and return the HTTP response code. TLS peer certificate
     * validation is an essential security aspect of establishing the connection.
     *
     * @param uri CAS proxy callback URI to connect to.
     * @param service CAS service requesting the connection.
     * @return HTTP response code.
     * @throws GeneralSecurityException On connection errors, e.g. invalid/untrusted cert.
     */
    protected int connect(@Nonnull final URI uri, @Nonnull final Service service) throws GeneralSecurityException {
        final HttpClientContext clientContext = HttpClientContext.create();
        HttpClientSecuritySupport.marshalSecurityParameters(clientContext, securityParameters, true);
        setCASTLSTrustEngineCriteria(clientContext, uri, service);
        HttpResponse response = null;
        try {
            log.debug("Attempting to validate CAS proxy callback URI {}", uri);
            final HttpGet request = new HttpGet(uri);
            response = httpClient.execute(request, clientContext);
            HttpClientSecuritySupport.checkTLSCredentialEvaluated(clientContext, request.getURI().getScheme());
            return response.getStatusLine().getStatusCode();
        } catch (final ClientProtocolException e) {
            throw new GeneralSecurityException("HTTP protocol error", e);
        } catch (final SSLPeerUnverifiedException e) {
            throw new CredentialException("Untrusted certificate presented by CAS proxy callback endpoint");
        } catch (final SSLException e) {
            if (e.getCause() instanceof CertificateException) {
                throw (CertificateException) e.getCause();
            }
            throw new GeneralSecurityException("SSL connection error", e);
        } catch (final IOException e) {
            throw new GeneralSecurityException("IO error", e);
        } finally {
            if (response != null && CloseableHttpResponse.class.isInstance(response)) {
                try {
                    CloseableHttpResponse.class.cast(response).close();
                } catch (final IOException e) {
                    log.debug("Error closing HttpResponse", e);
                }
            }
        }
    }

    /**
     * Install TLS trust criteria.
     * 
     * @param context client context
     * @param requestUri URI to obtain hostname
     * @param service CAS service
     */
    private static void setCASTLSTrustEngineCriteria(
            final HttpClientContext context, final URI requestUri, final Service service) {
        final String entityID;
        if (service.getEntityDescriptor() != null) {
            entityID = service.getEntityDescriptor().getEntityID();
        } else {
            entityID = service.getName();
        }
        final CriteriaSet criteria = new CriteriaSet(
                new EntityIdCriterion(entityID),
                new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME),
                new ProtocolCriterion(AbstractProtocolConfiguration.PROTOCOL_URI),
                new UsageCriterion(UsageType.SIGNING),
                new TrustedNamesCriterion(Collections.singleton(requestUri.getHost())));
        context.setAttribute(CONTEXT_KEY_CRITERIA_SET, criteria);
    }

}