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

package net.shibboleth.idp.profile.spring.relyingparty.security.trustengine.impl;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.cryptacular.EncodingException;
import org.cryptacular.StreamException;
import org.cryptacular.util.KeyPairUtil;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.impl.StaticCredentialResolver;
import org.opensaml.security.trust.impl.ExplicitKeyTrustEngine;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.security.x509.X509Support;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.io.Resource;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;

/**
 * Factory bean for simple use cases involving the {@link ExplicitKeyTrustEngine} and static credentials.
 */
public class StaticExplicitKeyFactoryBean extends AbstractFactoryBean<ExplicitKeyTrustEngine> {

    /** log. */
    private Logger log = LoggerFactory.getLogger(StaticExplicitKeyFactoryBean.class);

    /** The resources to be turned into keys. */
    private List<Resource> keyResources;

    /** The resources to be turned into certificates. */
    private List<Resource> certificateResources;

    /** Constructor. */
    public StaticExplicitKeyFactoryBean() {
        keyResources = Collections.<Resource>emptyList();
        certificateResources = Collections.<Resource>emptyList();
    }
    
    /**
     * Set the resources which we will convert into certificates.
     * 
     * @param keys the resources
     */
    public void setPublicKeys(@Nullable final List<Resource> keys) {
        keyResources = keys != null ? keys : Collections.<Resource>emptyList();
    }
    
    /**
     * Set the resources which we will convert into certificates.
     * 
     * @param certs the resources
     */
    public void setCertificates(@Nullable final List<Resource> certs) {
        certificateResources = certs != null ? certs : Collections.<Resource>emptyList();
    }

    /**
     * Get the configured certificates.
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
                for (final X509Certificate x : Collections2.filter(raw, Predicates.notNull())) {
                    credentials.add(new BasicX509Credential(x));
                }
            } catch (final CertificateException | IOException e) {
                log.error("Could not decode certificate from {}", f.getDescription(), e);
                throw new FatalBeanException("Could not decode certificate from: " + f.getDescription(), e);
            }
        }

        return credentials;
    }

    /** {@inheritDoc} */
    @Override
    public Class<?> getObjectType() {
        return ExplicitKeyTrustEngine.class;
    }

    /** {@inheritDoc} */
    @Override
    protected ExplicitKeyTrustEngine createInstance() throws Exception {
        return new ExplicitKeyTrustEngine(new StaticCredentialResolver(getCredentials()));
    }
    
}