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

import java.util.List;

import org.opensaml.xml.security.credential.UsageType;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Abstract factory bean for building {@link org.opensaml.xml.security.credential.Credential}s.
 */
public abstract class AbstractCredentialFactoryBean extends AbstractFactoryBean {
    
    /** Usage type of the credential. */
    private UsageType usageType;

    /** Names for the key represented by the credential. */
    private List<String> keyNames;
    
    /** Identifier for the owner of the credential. */
    private String entityID;
    
    /**
     * Gets the names for the key represented by the credential.
     * 
     * @return names for the key represented by the credential
     */
    public List<String> getKeyNames() {
        return keyNames;
    }

    /**
     * Gets the usage type of the credential.
     * 
     * @return usage type of the credential
     */
    public UsageType getUsageType(){
        return usageType;
    }
    
    /**
     * Get the entity ID of the credential.
     * 
     * @return the entity ID
     */
    public String getEntityID() {
        return entityID;
    }

    /**
     * Sets the names for the key represented by the credential.
     * 
     * @param names names for the key represented by the credential
     */
    public void setKeyNames(List<String> names) {
        keyNames = names;
    }

    /**
     * Sets the usage type of the credential.
     * 
     * @param type usage type of the credential
     */
    public void setUsageType(UsageType type){
        usageType = type;
    }
    
    /**
     * Set the entity ID of the credential.
     * 
     * @param newEntityID the entity ID
     */
    public void setEntityID(String newEntityID) {
        entityID = newEntityID;
    }
}