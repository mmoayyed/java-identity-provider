/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.config.security;

import java.security.KeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.xml.namespace.QName;

import org.opensaml.xml.security.SecurityHelper;
import org.opensaml.xml.security.x509.X509Util;
import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;


/**
 * Base class for X509 credential beans.
 */
public abstract class AbstractX509CredentialBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return X509CredentialFactoryBean.class;
    }

    /** {@inheritDoc} */
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
        return element.getAttributeNS(null, "id");
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
        Map<QName, List<Element>> configChildren = XMLHelper.getChildElements(element);

        parseKeyNames(configChildren, builder);
        parseSecretKey(configChildren, builder);
        parsePrivateKey(configChildren, builder);
        parseCertificates(configChildren, builder);
        parseCRLs(configChildren, builder);
    }

    /**
     * Parses the key names from the credential configuration.
     * 
     * @param configChildren children of the credential element
     * @param builder credential build
     */
    protected void parseKeyNames(Map<QName, List<Element>> configChildren, BeanDefinitionBuilder builder) {
        List<Element> keyNameElems = configChildren.get(new QName(SecurityNamespaceHandler.NAMESPACE, "KeyName"));
        if (keyNameElems == null || keyNameElems.isEmpty()) {
            return;
        }

        String keyName;
        ArrayList<String> keyNames = new ArrayList<String>();
        for (Element keyNameElem : keyNameElems) {
            keyName = DatatypeHelper.safeTrimOrNullString(keyNameElem.getTextContent());
            if (keyName != null) {
                keyNames.add(keyName);
            }
        }

        builder.addPropertyValue("keyNames", keyNames);
    }

    /**
     * Parses the secret key from the credential configuration.
     * 
     * @param configChildren children of the credential element
     * @param builder credential build
     */
    protected void parseSecretKey(Map<QName, List<Element>> configChildren, BeanDefinitionBuilder builder) {
        List<Element> keyElems = configChildren.get(new QName(SecurityNamespaceHandler.NAMESPACE, "SecretKey"));
        if (keyElems == null || keyElems.isEmpty()) {
            return;
        }

        Element secretKeyElem = keyElems.get(0);
        byte[] encodedKey = getEncodedSecretKey(DatatypeHelper.safeTrimOrNullString(secretKeyElem.getTextContent()));
        String keyPassword = DatatypeHelper.safeTrimOrNullString(secretKeyElem.getAttributeNS(null, "password"));
        try {
            SecretKey key = SecurityHelper.decodeSecretKey(encodedKey, keyPassword.toCharArray());
            builder.addPropertyValue("secretKey", key);
        } catch (KeyException e) {
            throw new FatalBeanException("Unable to create X509 credential, unable to parse secret key", e);
        }
    }

    /**
     * Extracts the secret key bytes from the content of the SecretKey configuration element.
     * 
     * @param keyConfigContent content of the SecretKey configuration element
     * 
     * @return secret key bytes
     */
    protected abstract byte[] getEncodedSecretKey(String keyConfigContent);

    /**
     * Parses the private from the credential configuration.
     * 
     * @param configChildren children of the credential element
     * @param builder credential build
     */
    protected void parsePrivateKey(Map<QName, List<Element>> configChildren, BeanDefinitionBuilder builder) {
        List<Element> keyElems = configChildren.get(new QName(SecurityNamespaceHandler.NAMESPACE, "PrivateKey"));
        if (keyElems == null || keyElems.isEmpty()) {
            return;
        }

        Element privKeyElem = keyElems.get(0);
        byte[] encodedKey = getEncodedPrivateKey(DatatypeHelper.safeTrimOrNullString(privKeyElem.getTextContent()));
        String keyPassword = DatatypeHelper.safeTrimOrNullString(privKeyElem.getAttributeNS(null, "password"));
        try {
            PrivateKey privKey = SecurityHelper.decodePrivateKey(encodedKey, keyPassword.toCharArray());
            PublicKey pubKey = SecurityHelper.derivePublicKey(privKey);
            builder.addPropertyValue("privateKey", privKey);
            builder.addPropertyValue("publicKey", pubKey);
        } catch (KeyException e) {
            throw new FatalBeanException("Unable to create X509 credential, unable to parse private key", e);
        }
    }

    /**
     * Extracts the private key bytes from the content of the PrivateKey configuration element.
     * 
     * @param keyConfigContent content of the Private configuration element
     * 
     * @return private key bytes
     */
    protected abstract byte[] getEncodedPrivateKey(String keyConfigContent);

    /**
     * Parses the certificates from the credential configuration.
     * 
     * @param configChildren children of the credential element
     * @param builder credential build
     */
    protected void parseCertificates(Map<QName, List<Element>> configChildren, BeanDefinitionBuilder builder) {
        List<Element> certElems = configChildren.get(new QName(SecurityNamespaceHandler.NAMESPACE, "Certificate"));
        if (certElems == null || certElems.isEmpty()) {
            return;
        }

        ArrayList<X509Certificate> certs = new ArrayList<X509Certificate>();
        byte[] encodedCert;
        Collection<X509Certificate> decodedCerts;
        for (Element certElem : certElems) {
            encodedCert = getEncodedCertificate(DatatypeHelper.safeTrimOrNullString(certElem.getTextContent()));
            if (encodedCert == null) {
                continue;
            }

            try {
                decodedCerts = X509Util.decodeCertificate(encodedCert);
                certs.addAll(decodedCerts);
            } catch (CertificateException e) {
                throw new FatalBeanException("Unable to create X509 credential, unable to parse certificates", e);
            }
        }

        builder.addPropertyValue("certificates", certs);
    }

    /**
     * Extracts the certificate bytes from the content of a Certificate configuration element.
     * 
     * @param certConfigContent content of a Certificate configuration element
     * 
     * @return certificate bytes
     */
    protected abstract byte[] getEncodedCertificate(String certConfigContent);

    /**
     * Parses the CRLs from the credential configuration.
     * 
     * @param configChildren children of the credential element
     * @param builder credential build
     */
    protected void parseCRLs(Map<QName, List<Element>> configChildren, BeanDefinitionBuilder builder) {
        List<Element> crlElems = configChildren.get(new QName(SecurityNamespaceHandler.NAMESPACE, "CRL"));
        if (crlElems == null || crlElems.isEmpty()) {
            return;
        }

        ArrayList<X509CRL> crls = new ArrayList<X509CRL>();
        byte[] encodedCRL;
        Collection<X509CRL> decodedCRLs;
        for (Element crlElem : crlElems) {
            encodedCRL = getEncodedCRL(DatatypeHelper.safeTrimOrNullString(crlElem.getTextContent()));
            if (encodedCRL == null) {
                continue;
            }

            try {
                decodedCRLs = X509Util.decodeCRLs(encodedCRL);
                crls.addAll(decodedCRLs);
            } catch (CRLException e) {
                throw new FatalBeanException("Unable to create X509 credential, unable to parse CRLs", e);
            }
        }

        builder.addPropertyValue("crls", crls);
    }

    /**
     * Extracts the CRL(s) bytes from the content of a CRL configuration element.
     * 
     * @param certCRLContent content of a CRL configuration element
     * 
     * @return CRL bytes
     */
    protected abstract byte[] getEncodedCRL(String certCRLContent);
}