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

package net.shibboleth.idp.profile.spring.relyingparty.security.credential;

import java.security.PrivateKey;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.collection.LazyList;

import org.cryptacular.util.KeyPairUtil;
import org.opensaml.security.x509.X509Support;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.FatalBeanException;

/**
 * A factory bean to understand X509Inline credentials.
 */
public class X509InlineCredentialFactoryBean extends AbstractX509CredentialFactoryBean {

    /** log. */
    private final Logger log = LoggerFactory.getLogger(X509InlineCredentialFactoryBean.class);

    /** The entity certificate. */
    private byte[] entityCertificate;

    /** The certificates. */
    private List<byte[]> certificates;

    /** The private key. */
    private byte[] privateKey;

    /** The crls. */
    private List<byte[]> crls;
    
    /**
     * Set the file with the entity certificate.
     * 
     * @param file The file to set.
     */
    public void setEntity(@Nonnull final byte[] file) {
        entityCertificate = file;
    }

    /**
     * Sets the files which contain the certificates.
     * 
     * @param certs The value to set.
     */
    public void setCertificates(@Nullable @NotEmpty final List<byte[]> certs) {
        certificates = certs;
    }

    /**
     * Set the file with the entity certificate.
     * 
     * @param key The file to set.
     */
    public void setPrivateKey(@Nullable final byte[] key) {
        privateKey = key;
    }

    /**
     * Sets the files which contain the crls.
     * 
     * @param list The value to set.
     */
    public void setCrls(@Nullable @NotEmpty final List<byte[]> list) {
        crls = list;
    }

    /** {@inheritDoc}. */
    @Override @Nullable protected X509Certificate getEntityCertificate() {

        if (null == entityCertificate) {
            return null;
        }
        try {
            final Collection<X509Certificate> certs = X509Support.decodeCertificates(entityCertificate);
            if (certs.size() > 1) {
                log.error("{}: Configuration element indicated an entityCertificate,"
                        + " but multiple certificates were decoded");
                throw new FatalBeanException("Configuration element indicated an entityCertificate,"
                        + " but multiple certificates were decoded");
            }
            return certs.iterator().next();
        } catch (CertificateException e) {
            log.error("{}: Could not decode provided Entity Certificate:{}", getConfigFile(), e);
            throw new FatalBeanException("Could not decode provided Entity Certificate", e);
        }
    }

    /** {@inheritDoc} */
    @Override @Nonnull protected List<X509Certificate> getCertificates() {
        List<X509Certificate> certs = new LazyList<>();
        for (byte[] cert : certificates) {
            try {
                certs.addAll(X509Support.decodeCertificates(cert));
            } catch (CertificateException e) {
                log.error("{}: Could not decode provided Certificate:{}", getConfigFile(), e);
                throw new FatalBeanException("Could not decode provided Certificate", e);
            }
        }
        return certs;
    }

    /** {@inheritDoc} */
    @Override @Nullable protected PrivateKey getPrivateKey() {
        if (null == privateKey) {
            return null;
        }
        return KeyPairUtil.decodePrivateKey(privateKey, getPrivateKeyPassword());
    }

    /** {@inheritDoc} */
    @Override @Nullable protected List<X509CRL> getCrls() {
        if (null == crls) {
            return null;
        }
        List<X509CRL> result = new LazyList<>();
        for (byte[] crl : crls) {
            try {
                result.addAll(X509Support.decodeCRLs(crl));
            } catch (CRLException e) {
                log.error("{}: Could not decode provided CRL:{}", getConfigFile(), e);
                throw new FatalBeanException("Could not decode provided CRL", e);
            }
        }
        return result;
    }
}
