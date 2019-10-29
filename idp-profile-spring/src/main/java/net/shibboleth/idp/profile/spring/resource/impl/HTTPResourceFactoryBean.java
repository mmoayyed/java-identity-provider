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

package net.shibboleth.idp.profile.spring.resource.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.cryptacular.EncodingException;
import org.cryptacular.StreamException;
import org.cryptacular.util.KeyPairUtil;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.impl.StaticCredentialResolver;
import org.opensaml.security.httpclient.HttpClientSecurityContextHandler;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.opensaml.security.trust.impl.ExplicitKeyTrustEngine;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.security.x509.PKIXValidationInformation;
import org.opensaml.security.x509.X509Support;
import org.opensaml.security.x509.impl.BasicPKIXValidationInformation;
import org.opensaml.security.x509.impl.PKIXX509CredentialTrustEngine;
import org.opensaml.security.x509.impl.StaticPKIXValidationInformationResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.io.Resource;

import net.shibboleth.ext.spring.resource.FileBackedHTTPResource;
import net.shibboleth.ext.spring.resource.HTTPResource;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.httpclient.HttpClientContextHandler;

/**
 * Factory bean for simple use cases that auto-configure PKIX or key pinning for
 * an {@link HTTPResource}.
 * 
 * @since 3.4.0
 */
public class HTTPResourceFactoryBean extends AbstractFactoryBean<HTTPResource> {

    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(FileBackedHTTPResource.class);

    /** Backing file path. */
    @Nullable private String backingResource;

    /** HTTP Client used to pull the resource. */
    @Nullable private HttpClient httpClient;
    
    /** URL to the Resource. */
    @Nullable private URL resourceURL;
    
    /** Optional handler to pre- and post-process context. */
    @Nullable private HttpClientContextHandler httpClientContextHandler;
    
    /** The resources to be turned into keys. */
    @Nonnull private List<Resource> keyResources;

    /** The resources to be turned into certificates. */
    @Nonnull private List<Resource> certificateResources;
    
    /** Use a PKIX trust engine. */
    private boolean usePKIX;
    
    /** PKIX verify depth. */
    @Nullable private Integer verifyDepth;

    /** Constructor. */
    public HTTPResourceFactoryBean() {
        keyResources = Collections.emptyList();
        certificateResources = Collections.emptyList();
    }

    /**
     * Set the URL.
     * 
     * @param url the URL
     */
    public void setURL(@Nullable final URL url) {
        resourceURL = url;
    }

    /**
     * Set the backing resource.
     * 
     * @param resource the backing resource
     */
    public void setBackingResource(@Nullable @NotEmpty final String resource) {
        backingResource = resource;
    }
    
    /**
     * Set the {@link HttpClient} instance.
     * 
     * @param client client instance
     */
    public void setHttpClient(@Nullable final HttpClient client) {
        httpClient = client;
    }
        
    /**
     * Set a handler to manipulate the {@link HttpClientContext}.
     * 
     * @param handler the handler to install
     */
    public void setHttpClientContextHandler(@Nullable final HttpClientContextHandler handler) {
        httpClientContextHandler = handler;
    }
    
    /**
     * Set the resources which we will convert into certificates.
     * 
     * @param keys the resources
     */
    public void setPublicKeys(@Nullable final List<Resource> keys) {
        keyResources = keys != null ? keys : Collections.emptyList();
    }
    
    /**
     * Set the resources which we will convert into certificates.
     * 
     * @param certs the resources
     */
    public void setCertificates(@Nullable final List<Resource> certs) {
        certificateResources = certs != null ? certs : Collections.emptyList();
    }
    
    /**
     * 
     * Set whether to use a PKIX trust engine.
     * 
     * @param flag flag to set
     */
    public void setUsePKIX(final boolean flag) {
        usePKIX = flag;
    }
    
    /**
     * Set the verify depth.
     * 
     * @param depth value to set
     */
    public void setVerifyDepth(@Nullable final Integer depth) {
        verifyDepth = depth;
    }

    /**
     * Get the configured keys and certificates lumped together as credentials.
     * 
     * @return the certificates null
     */
    @Nullable @NonnullElements protected List<Credential> getCredentials() {
        
        final List<Credential> credentials = new ArrayList<>(keyResources.size() + certificateResources.size());

        for (final Resource f : keyResources) {
            try(final InputStream is = f.getInputStream()) {
                credentials.add(new BasicCredential(KeyPairUtil.readPublicKey(is)));
            } catch (final EncodingException|StreamException|IOException e) {
                log.error("Could not decode public key from {}", f.getDescription(), e);
                throw new FatalBeanException("Could not decode public key from: " + f.getDescription(), e);
            }
        }
                
        for (final Resource f : certificateResources) {
            try(final InputStream is = f.getInputStream()) {
                final Collection<X509Certificate> raw = X509Support.decodeCertificates(is);
                if (raw != null) {
                    raw.forEach(x -> {
                        if (x != null) {
                            credentials.add(new BasicX509Credential(x));
                        }
                    });
                }
            } catch (final CertificateException | IOException e) {
                log.error("Could not decode certificate from {}", f.getDescription(), e);
                throw new FatalBeanException("Could not decode certificate from: " + f.getDescription(), e);
            }
        }

        return credentials;
    }
    
    /**
     * Get the configured certificates in their native form.
     * 
     * @return the certificates
     */
    @Nullable @NonnullElements protected List<X509Certificate> getCertificates() {
        if (certificateResources == null) {
            return null;
        }
        
        final List<X509Certificate> certificates = new ArrayList<>(certificateResources.size());
        for (final Resource f : certificateResources) {
            try(final InputStream is = f.getInputStream()) {
                certificates.addAll(X509Support.decodeCertificates(is));
            } catch (final CertificateException | IOException e) {
                log.error("Could not decode Certificate at {}", f.getDescription(), e);
                throw new FatalBeanException("Could not decode provided CertificateFile: " + f.getDescription(), e);
            }
        }
        return certificates;
    }

    /** {@inheritDoc} */
    @Override
    public Class<?> getObjectType() {
        return HTTPResource.class;
    }

    /** {@inheritDoc} */
    @Override
    protected HTTPResource createInstance() throws Exception {
        
        final HTTPResource theResource;
        if (backingResource != null) {
            theResource = new FileBackedHTTPResource(backingResource, httpClient, resourceURL);
        } else {
            theResource = new HTTPResource(httpClient, resourceURL);
        }
        
        if (httpClientContextHandler != null) {
            if (!keyResources.isEmpty() || !certificateResources.isEmpty()) {
                log.warn("httpClientContextHandler set, ignoring supplied keys/certificates");
            }
            theResource.setHttpClientContextHandler(httpClientContextHandler);
        } else if (usePKIX) {
            if (!keyResources.isEmpty()) {
                log.warn("usePKIX set, ignoring supplied keys");
            }
            log.debug("Auto-wiring PKIXX509CredentialTrustEngine into HTTPResource");
            final BasicPKIXValidationInformation info =
                    new BasicPKIXValidationInformation(getCertificates(), null, verifyDepth);
            final StaticPKIXValidationInformationResolver resolver =
                    new StaticPKIXValidationInformationResolver(
                            Collections.<PKIXValidationInformation>singletonList(info), null, false);
            // Second parameter of null disables name checking, since this is already handled for HTTPS.
            final PKIXX509CredentialTrustEngine trustEngine = new PKIXX509CredentialTrustEngine(resolver, null);
            final HttpClientSecurityParameters params = new HttpClientSecurityParameters();
            params.setTLSTrustEngine(trustEngine);
            final HttpClientSecurityContextHandler handler = new HttpClientSecurityContextHandler();
            handler.setHttpClientSecurityParameters(params);
            handler.initialize();
            theResource.setHttpClientContextHandler(handler);
        } else {
            log.debug("Auto-wiring ExplicitKeyTrustEngine into HTTPResource");
            final ExplicitKeyTrustEngine trustEngine =
                    new ExplicitKeyTrustEngine(new StaticCredentialResolver(getCredentials()));
            final HttpClientSecurityParameters params = new HttpClientSecurityParameters();
            params.setTLSTrustEngine(trustEngine);
            final HttpClientSecurityContextHandler handler = new HttpClientSecurityContextHandler();
            handler.setHttpClientSecurityParameters(params);
            handler.initialize();
            theResource.setHttpClientContextHandler(handler);
        }
        
        return theResource;
    }
    
}