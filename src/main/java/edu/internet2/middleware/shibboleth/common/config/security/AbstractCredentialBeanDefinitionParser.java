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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.opensaml.xml.security.SecurityHelper;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;


/**
 * Base class for credential beans.
 */
public abstract class AbstractCredentialBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {
    
    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractCredentialBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
        return element.getAttributeNS(null, "id");
    }
    
    /**
     * Parse the credential element attributes.
     * 
     * @param element credential element
     * @param builder bean definition builder
     */
    protected void parseAttributes(Element element, BeanDefinitionBuilder builder) {
        String usage = DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null, "usage"));
        if (usage != null) {
            builder.addPropertyValue("usageType", UsageType.valueOf(usage.toUpperCase()));
        } else {
            builder.addPropertyValue("usageType", UsageType.UNSPECIFIED);
        }
        
        String entityID = DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null, "entityID"));
        if (entityID != null) {
            builder.addPropertyValue("entityID", entityID);
        }
    }
    
    /**
     * Parses the common elements from the credential configuration.
     * 
     * @param configChildren children of the credential element
     * @param builder credential build
     */
    protected void parseCommon(Map<QName, List<Element>> configChildren, BeanDefinitionBuilder builder) {
        parseKeyNames(configChildren, builder);
    }

    /**
     * Parses the key names from the credential configuration.
     * 
     * @param configChildren children of the credential element
     * @param builder credential build
     */
    protected void parseKeyNames(Map<QName, List<Element>> configChildren, BeanDefinitionBuilder builder) {
        log.debug("Parsing credential key names");
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
     * Parses the private key from the credential configuration.
     * 
     * @param configChildren children of the credential element
     * @param builder credential build
     */
    protected void parsePrivateKey(Map<QName, List<Element>> configChildren, BeanDefinitionBuilder builder) {        
        List<Element> keyElems = configChildren.get(new QName(SecurityNamespaceHandler.NAMESPACE, "PrivateKey"));
        if (keyElems == null || keyElems.isEmpty()) {
            return;
        }
        
        log.debug("Parsing credential private key");
        Element privKeyElem = keyElems.get(0);
        byte[] encodedKey = getEncodedPrivateKey(DatatypeHelper.safeTrimOrNullString(privKeyElem.getTextContent()));
        String keyPassword = DatatypeHelper.safeTrimOrNullString(privKeyElem.getAttributeNS(null, "password"));
        char[] keyPasswordCharArray = null;
        if (keyPassword != null) {
            keyPasswordCharArray = keyPassword.toCharArray();
        }
        try {
            PrivateKey privKey = SecurityHelper.decodePrivateKey(encodedKey, keyPasswordCharArray);
            builder.addPropertyValue("privateKey", privKey);
        } catch (KeyException e) {
            throw new FatalBeanException("Unable to create credential, unable to parse private key", e);
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

}