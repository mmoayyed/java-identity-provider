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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.idp.profile.spring.relyingparty.security.SecurityNamespaceHandler;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

/**
 * Specific parser for all BasicCredentials.<br/>
 * This does the work of putting the element values into strings. The bean factory then does the correct thing - with
 * some help from Spring doing auto-conversion.
 */
public abstract class AbstractBasicCredentialParser extends AbstractCredentialParser {

    /** &lt;PrivateKey&gt;. */
    public static final QName PRIVATE_KEY_ELEMENT_NAME = new QName(SecurityNamespaceHandler.NAMESPACE, "PrivateKey");

    /** &lt;PublicKey&gt;. */
    public static final QName PUBLIC_KEY_ELEMENT_NAME = new QName(SecurityNamespaceHandler.NAMESPACE, "PublicKey");

    /** &lt;SecretKey&gt;. */
    public static final QName SECRET_KEY_ELEMENT_NAME = new QName(SecurityNamespaceHandler.NAMESPACE, "SecretKey");

    /** log. */
    private Logger log = LoggerFactory.getLogger(AbstractBasicCredentialParser.class);

    /** {@inheritDoc} */
    @Override protected void doParse(Element element, BeanDefinitionBuilder builder) {
        super.doParse(element, builder);
        parsePrivateKey(ElementSupport.getChildElements(element, PRIVATE_KEY_ELEMENT_NAME), builder);
        parsePublicKey(ElementSupport.getChildElements(element, PUBLIC_KEY_ELEMENT_NAME), builder);
        parseSecretKey(ElementSupport.getChildElements(element, SECRET_KEY_ELEMENT_NAME), builder);
    }

    /**
     * Parse the &lt;PrivateKey&gt; element.
     * 
     * @param childElements the elements containing the private key, may be null or empty.
     * @param builder the builder
     */
    private void parsePrivateKey(@Nullable final List<Element> childElements,
            @Nonnull final BeanDefinitionBuilder builder) {
        if (null == childElements || childElements.isEmpty()) {
            return;
        }
        if (childElements.size() > 1) {
            throw new BeanCreationException("More than one <PrivateKey> Elements present.");
        }
        final Element key = childElements.get(0);
        final String value = StringSupport.trimOrNull(key.getTextContent());
        if (null == value) {
            throw new BeanCreationException("<PrivateKey> Must contain text.");
        }
        log.debug("Found a private key <Supressed>");
        builder.addPropertyValue("privateKeyInfo", value);
        builder.addPropertyValue("privateKeyPassword", key.getAttributeNS(null, "password"));
    }

    /**
     * Parse the &lt;PublicKey&gt; elements.
     * 
     * @param childElements the elements containing the public key, must have exactly one element
     * @param builder the builder
     */
    private void parsePublicKey(@Nullable final List<Element> childElements,
            @Nonnull final BeanDefinitionBuilder builder) {
        if (null == childElements || childElements.isEmpty()) {
            throw new BeanCreationException("A <PublicKey> Element should be present.");
        }
        if (childElements.size() > 1) {
            throw new BeanCreationException("More than one <PublicKey> Elements present.");
        }
        final Element key = childElements.get(0);
        final String value = StringSupport.trimOrNull(key.getTextContent());
        if (null == value) {
            throw new BeanCreationException("<PublicKey> Must contain text.");
        }
        log.debug("Found a public key {}", value);
        builder.addPropertyValue("publicKeyInfo", value);

        if (key.hasAttributeNS(null, "password")) {
            log.warn("password on public key is ignored");
        }
    }

    /**
     * Parse the &lt;SecretKey&gt; element.
     * 
     * @param childElements the elements containing the private key, may be null or empty.
     * @param builder the builder
     */
    private void parseSecretKey(@Nullable final List<Element> childElements,
            @Nonnull final BeanDefinitionBuilder builder) {
        if (null == childElements || childElements.isEmpty()) {
            return;
        }
        if (childElements.size() > 1) {
            throw new BeanCreationException("More than one <SecretKey> Elements present.");
        }
        log.warn("<SecretKey> is not supported");
        final Element key = childElements.get(0);
        final String value = StringSupport.trimOrNull(key.getTextContent());
        if (null == value) {
            throw new BeanCreationException("<SecretKey> Must contain text.");
        }
        log.debug("Found a secret key <Supressed>");
        builder.addPropertyValue("secretKeyInfo", value);
        builder.addPropertyValue("secretKeyPassword", key.getAttributeNS(null, "password"));
    }
}
