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

import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.namespace.QName;

import org.springframework.beans.FatalBeanException;

/**
 * Spring bean definition parser for filesytem-based credential configuration elements.
 */
public class FilesystemBasicCredentialBeanDefinitionParser extends AbstractBasicCredentialBeanDefinitionParser {

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(SecurityNamespaceHandler.NAMESPACE, "BasicFilesystem");

    /** {@inheritDoc} */
    protected byte[] getEncodedPrivateKey(String keyConfigContent) {
        try {
            FileInputStream ins = new FileInputStream(keyConfigContent);
            byte[] encoded = new byte[ins.available()];
            ins.read(encoded);
            return encoded;
        } catch (IOException e) {
            throw new FatalBeanException("Unable to read private key from file " + keyConfigContent, e);
        }
    }

    /** {@inheritDoc} */
    protected byte[] getEncodedSecretKey(String keyConfigContent) {
        try {
            FileInputStream ins = new FileInputStream(keyConfigContent);
            byte[] encoded = new byte[ins.available()];
            ins.read(encoded);
            return encoded;
        } catch (IOException e) {
            throw new FatalBeanException("Unable to read secret key from file " + keyConfigContent, e);
        }
    }

    /** {@inheritDoc} */
    protected byte[] getEncodedPublicKey(String keyConfigContent) {
        try {
            FileInputStream ins = new FileInputStream(keyConfigContent);
            byte[] encoded = new byte[ins.available()];
            ins.read(encoded);
            return encoded;
        } catch (IOException e) {
            throw new FatalBeanException("Unable to read public key from file " + keyConfigContent, e);
        }
    }
    
}