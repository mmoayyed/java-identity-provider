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

import javax.annotation.Nullable;

import org.opensaml.security.credential.Credential;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * A factory bean to accumulate the information pertaining to an
 * {@link org.opensaml.security.credential.AbstractCredential}.
 * @param <T> the type of credential to create.
 */
public abstract class AbstractCredentialFactoryBean<T extends Credential> extends AbstractFactoryBean<T> {

    /** Usage type of the credential. */
    private String usageType;

    /** Names for the key represented by the credential. */
    private List<String> keyNames;

    /** Identifier for the owner of the credential. */
    private String entityID;
    
    /** The privateKey Password (if any). */
    @Nullable private char[] privateKeyPassword;

    /** For logging: The config file.*/
    private String configFile;
   
    /**
     * Gets the names for the key represented by the credential.
     * 
     * @return names for the key represented by the credential
     */
    @Nullable public List<String> getKeyNames() {
        return keyNames;
    }

    /**
     * Gets the usage type of the credential.
     * 
     * @return usage type of the credential
     */
    @Nullable public String getUsageType() {
        return usageType;
    }

    /**
     * Get the entity ID of the credential.
     * 
     * @return the entity ID
     */
    @Nullable public String getEntityID() {
        return entityID;
    }

    /**
     * Sets the names for the key represented by the credential.
     * 
     * @param names names for the key represented by the credential
     */
    public void setKeyNames(@Nullable final List<String> names) {
        keyNames = names;
    }

    /**
     * Sets the usage type of the credential.
     * 
     * @param type usage type of the credential
     */
    public void setUsageType(@Nullable final String type) {
        usageType = type;
    }

    /**
     * Set the entity ID of the credential.
     * 
     * @param newEntityID the entity ID
     */
    public void setEntityID(@Nullable final String newEntityID) {
        entityID = newEntityID;
    }
    
    /**
     * Get the password for the private key.
     * 
     * @return Returns the privateKeyPassword.
     */
    @Nullable public char[] getPrivateKeyPassword() {
        return privateKeyPassword;
    }

    /**
     * Set the password for the private key.
     * 
     * @param password The password to set.
     */
    public void setPrivateKeyPassword(@Nullable char[] password) {
        privateKeyPassword = password;
    }

    /** For logging, get the file that defined this bean.
     * @return Returns the configFile.
     */
    public String getConfigFile() {
        return configFile;
    }

    /** For logging, set the file that defined this bean.
     * @param file The configFile to set.
     */
    public void setConfigFile(String file) {
        configFile = file;
    }

}
