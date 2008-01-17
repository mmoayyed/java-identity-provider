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

import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.opensaml.xml.security.x509.X509Util;
import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;


/**
 * Base class for PKIXValidationInformation beans.
 */
public abstract class AbstractPKIXValidationInformationBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {
    
    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractX509CredentialBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return PKIXValidationInformationFactoryBean.class;
    }

    /** {@inheritDoc} */
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
        return element.getAttributeNS(null, "id");
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
        log.debug("Parsing PKIX ValidationInfo: {}", element.getAttributeNS(null, "id"));
        
        String depth = DatatypeHelper.safeTrim(element.getAttributeNS(null, "VerifyDepth"));
        builder.addPropertyValue("verifyDepth", new Integer(depth));
        
        Map<QName, List<Element>> configChildren = XMLHelper.getChildElements(element);

        parseCertificates(configChildren, builder);
        parseCRLs(configChildren, builder);
    }

    /**
     * Parses the certificates from the validation info configuration.
     * 
     * @param configChildren children of the validation set element
     * @param builder validation set build
     */
    protected void parseCertificates(Map<QName, List<Element>> configChildren, BeanDefinitionBuilder builder) {
        List<Element> certElems = configChildren.get(new QName(SecurityNamespaceHandler.NAMESPACE, "Certificate"));
        if (certElems == null || certElems.isEmpty()) {
            return;
        }

        log.debug("Parsing PKIX validation info certificates");
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
                throw new FatalBeanException("Unable to create PKIX validation info, unable to parse certificates", e);
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
     * Parses the CRLs from the validation info configuration.
     * 
     * @param configChildren children of the validation info element
     * @param builder validation info build
     */
    protected void parseCRLs(Map<QName, List<Element>> configChildren, BeanDefinitionBuilder builder) {
        List<Element> crlElems = configChildren.get(new QName(SecurityNamespaceHandler.NAMESPACE, "CRL"));
        if (crlElems == null || crlElems.isEmpty()) {
            return;
        }

        log.debug("Parsing PKIX validation info CRLs");
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
                throw new FatalBeanException("Unable to create PKIX validation info, unable to parse CRLs", e);
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