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

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.security.auth.login.FailedLoginException;

import com.google.common.base.Function;
import net.shibboleth.idp.cas.config.impl.AbstractProtocolConfiguration;
import net.shibboleth.idp.cas.proxy.ProxyValidator;
import net.shibboleth.idp.cas.service.Service;
import net.shibboleth.idp.cas.service.ServiceContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.criterion.ProtocolCriterion;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.criteria.UsageCriterion;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.security.x509.X509Credential;
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
 *     <li>The HTTP response status code is in the set of {@link #allowedResponseCodes} (only 200 by default).</li>
 * </ol>
 *
 * @author Marvin S. Addison
 */
public class HttpClientProxyValidator implements ProxyValidator {

    /** Required https scheme for proxy callbacks. */
    protected static final String HTTPS_SCHEME = "https";

    /** Default connection and socket timeout in ms. */
    private static final int DEFAULT_TIMEOUT = 800;

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(HttpClientProxyValidator.class);

    /** Trust engine that validates proxy endpoint TLS certificates. */
    @Nonnull
    private final TrustEngine<? super X509Credential> trustEngine;

    /** Looks up a ServiceContext from the profile request context. */
    private final Function<ProfileRequestContext, ServiceContext> serviceCtxLookupFunction =
            new ChildContextLookup<>(ServiceContext.class);

    /** List of HTTP response codes permitted for successful proxy callback. */
    @NotEmpty
    @NonnullElements
    private Set<Integer> allowedResponseCodes = Collections.singleton(200);

    /** Connection and socket timeout. */
    @Positive
    private int timeout = DEFAULT_TIMEOUT;


    /**
     * Creates a new instance.
     *
     * @param engine Trust engine to use for validating proxy X.509 certificate credentials.
     */
    public HttpClientProxyValidator(@Nonnull final TrustEngine<? super X509Credential> engine) {
        trustEngine = Constraint.isNotNull(engine, "Trust engine cannot be null");
    }

    /**
     * Sets connect and socket timeouts for HTTP connection to proxy callback endpoint.
     *
     * @param timeoutMillis Non-zero timeout in milliseconds for both connection and socket timeouts.
     */
    public void setTimeout(@Positive final int timeoutMillis) {
        timeout = (int) Constraint.isGreaterThan(0, timeoutMillis, "Timeout must be positive");
    }

    /**
     * Sets the HTTP response codes permitted for successful authentication of the proxy callback URL.
     *
     * @param responseCodes One or more HTTP response codes.
     */
    public void setAllowedResponseCodes(@NotEmpty @NonnullElements final Set<Integer> responseCodes) {
        Constraint.isNotEmpty(responseCodes, "Response codes cannot be null or empty.");
        Constraint.noNullItems(responseCodes.toArray(), "Response codes cannot contain null elements.");
        allowedResponseCodes = responseCodes;
    }

    @Override
    public void validate (
            @Nonnull final ProfileRequestContext profileRequestContext, @Nonnull final URI proxyCallbackUri)
            throws GeneralSecurityException {

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
    protected int connect(@Nonnull final URI uri, @Nonnull Service service) throws GeneralSecurityException {

        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = createHttpClient(service);
            log.debug("Attempting to connect to {}", uri);
            final HttpGet request = new HttpGet(uri);
            request.setConfig(
                    RequestConfig.custom()
                            .setConnectTimeout(timeout)
                            .setSocketTimeout(timeout)
                            .build());
            response = httpClient.execute(request);
            return response.getStatusLine().getStatusCode();
        } catch (final ClientProtocolException e) {
            throw new GeneralSecurityException("HTTP protocol error", e);
        } catch (final SSLException e) {
            if (e.getCause() instanceof CertificateException) {
                throw (CertificateException) e.getCause();
            }
            throw new GeneralSecurityException("SSL connection error", e);
        } catch (final IOException e) {
            throw new GeneralSecurityException("IO error", e);
        } finally {
            close(response);
            close(httpClient);
        }
    }

    /**
     * Build HTTP client.
     * 
     * @param service CAS service.
     * @return HTTP client
     */
    protected CloseableHttpClient createHttpClient(final Service service) {
        final SSLConnectionSocketFactory socketFactory;
        try {
            final SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(null, new TrustEngineTrustStrategy(service))
                    .build();
            socketFactory = new SSLConnectionSocketFactory(sslContext);
        } catch (final Exception e) {
            throw new RuntimeException("SSL initialization error", e);
        }
        final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register(HTTPS_SCHEME, socketFactory).build();
        final BasicHttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager(registry);
        return HttpClients.custom().setConnectionManager(connectionManager).build();
    }

    /**
     * Close the resource.
     * 
     * @param resource the resource to close
     */
    private void close(final Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (final IOException e) {
                log.warn("Error closing " + resource, e);
            }
        }
    }

    /**
     * Delegates X.509 certificate trust to an underlying OpenSAML <code>TrustEngine</code>.
     */
    private class TrustEngineTrustStrategy implements TrustStrategy {

        /** Class logger. */
        private final Logger log = LoggerFactory.getLogger(TrustEngineTrustStrategy.class);

        /** CAS protocol service. */
        private final Service service;


        public TrustEngineTrustStrategy(final Service s) {
            service = s;
        }

        @Override
        public boolean isTrusted(final X509Certificate[] certificates, final String authType)
                throws CertificateException {
            if (certificates == null || certificates.length < 1) {
                return false;
            }
            // Assume the first certificate is the end-entity cert
            try {
                log.debug("Validating cert {} issued by {}",
                        certificates[0].getSubjectDN().getName(),
                        certificates[0].getIssuerDN().getName());
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
                        new UsageCriterion(UsageType.SIGNING));
                return trustEngine.validate(new BasicX509Credential(certificates[0]), criteria);
            } catch (final SecurityException e) {
                throw new CertificateException("X509 validation error", e);
            }
        }
    }
}
