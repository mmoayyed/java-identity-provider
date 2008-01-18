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
import java.security.PublicKey;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.xml.namespace.QName;

import org.opensaml.xml.security.SecurityHelper;
import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;


/**
 * Base class for X509 credential beans.
 */
public abstract class AbstractBasicCredentialBeanDefinitionParser extends AbstractCredentialBeanDefinitionParser {
    
    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractBasicCredentialBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return BasicCredentialFactoryBean.class;
    }

    /** {@inheritDoc} */
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
        return element.getAttributeNS(null, "id");
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
        log.debug("Parsing basic credential: {}", element.getAttributeNS(null, "id"));
        
        parseAttributes(element, builder);
        
        Map<QName, List<Element>> configChildren = XMLHelper.getChildElements(element);
        
        parseCommon(configChildren, builder);

        parseSecretKey(configChildren, builder);
        parsePrivateKey(configChildren, builder);
        parsePublicKey(configChildren, builder);
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

        log.debug("Parsing credential secret key");
        Element secretKeyElem = keyElems.get(0);
        byte[] encodedKey = getEncodedSecretKey(DatatypeHelper.safeTrimOrNullString(secretKeyElem.getTextContent()));
        String keyPassword = DatatypeHelper.safeTrimOrNullString(secretKeyElem.getAttributeNS(null, "password"));
        try {
            SecretKey key = SecurityHelper.decodeSecretKey(encodedKey, keyPassword.toCharArray());
            builder.addPropertyValue("secretKey", key);
        } catch (KeyException e) {
            throw new FatalBeanException("Unable to create credential, unable to parse secret key", e);
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
     * Parses the public key from the credential configuration.
     * 
     * @param configChildren children of the credential element
     * @param builder credential build
     */
    protected void parsePublicKey(Map<QName, List<Element>> configChildren, BeanDefinitionBuilder builder) {
        List<Element> keyElems = configChildren.get(new QName(SecurityNamespaceHandler.NAMESPACE, "PublicKey"));
        if (keyElems == null || keyElems.isEmpty()) {
            return;
        }
        
        log.debug("Parsing credential public key");
        Element pubKeyElem = keyElems.get(0);
        byte[] encodedKey = getEncodedPublicKey(DatatypeHelper.safeTrimOrNullString(pubKeyElem.getTextContent()));
        String keyPassword = DatatypeHelper.safeTrimOrNullString(pubKeyElem.getAttributeNS(null, "password"));
        char[] keyPasswordCharArray = null;
        if (keyPassword != null) {
            keyPasswordCharArray = keyPassword.toCharArray();
        }
        try {
            PublicKey pubKey = SecurityHelper.decodePublicKey(encodedKey, keyPasswordCharArray);
            builder.addPropertyValue("publicKey", pubKey);
        } catch (KeyException e) {
            throw new FatalBeanException("Unable to create credential, unable to parse public key", e);
        }
    }
    
    /**
     * Extracts the public key bytes from the content of the PublicKey configuration element.
     * 
     * @param keyConfigContent content of the PublicKey configuration element
     * 
     * @return private key bytes
     */
    protected abstract byte[] getEncodedPublicKey(String keyConfigContent);
 
}