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

package net.shibboleth.idp.attribute.resolver.spring.dc.ldap;

import java.io.File;
import java.security.KeyException;
import java.security.PrivateKey;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.opensaml.security.crypto.KeySupport;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.security.x509.X509Support;
import org.w3c.dom.Element;

import net.shibboleth.utilities.java.support.xml.DomTypeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

/** Support for parsing X509 credentials in the security schema. */
final class X509CredentialSupport {

    /** Security namespace. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:security";

    /** Type of X509 credentials. */
    public enum Type {

        /** Inline data. */
        X509Inline,

        /** Filesystem data. */
        X509Filesystem
    };

    /** Default constructor. */
    private X509CredentialSupport() {
    }

    /**
     * Parses the supplied security element and configures a {@link BasicX509Credential} with the data.
     * 
     * @param securityElement to parse
     * 
     * @return credential
     */
    @Nullable public static X509Credential parseX509Credential(@Nullable final Element securityElement) {
        if (securityElement == null) {
            return null;
        }
        final Type type = Type.valueOf(DomTypeSupport.getXSIType(securityElement).getLocalPart());

        final List<X509Certificate> certs =
                parseX509Certificates(
                        ElementSupport.getChildElements(securityElement, new QName(NAMESPACE, "Certificate")), type);
        if (certs.isEmpty()) {
            return null;
        }

        final PrivateKey key =
                parsePrivateKey(
                        ElementSupport.getFirstChildElement(securityElement, new QName(NAMESPACE, "PrivateKey")), type);

        final List<X509CRL> crls =
                parseX509CRLs(ElementSupport.getChildElements(securityElement, new QName(NAMESPACE, "CRL")), type);

        final BasicX509Credential credential = new BasicX509Credential(certs.get(0));
        if (certs.size() > 1) {
            credential.setEntityCertificateChain(certs);
        }
        if (key != null) {
            credential.setPrivateKey(key);
        }
        if (crls.size() > 0) {
            credential.setCRLs(crls);
        }
        return credential;
    }

    /**
     * Parses the supplied certificate elements.
     * 
     * @param certElements to parse
     * @param type of X509 credential
     * 
     * @return certificates
     */
    @Nonnull protected static List<X509Certificate> parseX509Certificates(@Nonnull final List<Element> certElements,
            @Nonnull final Type type) {

        final List<X509Certificate> certs = new ArrayList<X509Certificate>();
        if (certElements != null) {
            for (Element certElement : certElements) {
                try {
                    switch (type) {
                        case X509Inline:
                            certs.add(X509Support.decodeCertificate(certElement.getTextContent().trim()));
                            break;
                        case X509Filesystem:
                            certs.add(X509Support.decodeCertificate(new File(certElement.getTextContent().trim())));
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown xsi type: " + type);
                    }
                } catch (CertificateException e) {
                    throw new IllegalArgumentException("Could not parse certificate", e);
                }
            }
        }
        return certs;
    }

    /**
     * Parses the supplied private key element.
     * 
     * @param keyElement to parse
     * @param type of X509 credential
     * 
     * @return private key
     */
    @Nullable protected static PrivateKey parsePrivateKey(@Nullable final Element keyElement, @Nonnull final Type type) {

        PrivateKey key = null;
        if (keyElement != null) {
            try {
                switch (type) {
                    case X509Inline:
                        key = KeySupport.buildJavaPrivateKey(keyElement.getTextContent().trim());
                        break;
                    case X509Filesystem:
                        key = KeySupport.decodePrivateKey(new File(keyElement.getTextContent().trim()), null);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown xsi type: " + type);
                }
            } catch (KeyException e) {
                throw new IllegalArgumentException("Could not parse key", e);
            }
        }
        return key;
    }

    /**
     * Parses the supplied CRL elements.
     * 
     * @param crlElements to parse
     * @param type of X509 credential
     * 
     * @return CRLs
     */
    @Nonnull protected static List<X509CRL> parseX509CRLs(@Nonnull final List<Element> crlElements,
            @Nonnull final Type type) {
        final List<X509CRL> crls = new ArrayList<X509CRL>();
        if (crlElements != null) {
            for (Element crlElement : crlElements) {
                try {
                    switch (type) {
                        case X509Inline:
                            crls.add(X509Support.decodeCRL(crlElement.getTextContent().trim()));
                            break;
                        case X509Filesystem:
                            crls.addAll(X509Support.decodeCRLs(new File(crlElement.getTextContent().trim())));
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown xsi type: " + type);
                    }
                } catch (CRLException e) {
                    throw new IllegalArgumentException("Could not parse CRL", e);
                } catch (CertificateException e) {
                    throw new IllegalArgumentException("Could not parse CRL", e);
                }
            }
        }
        return crls;
    }
}
