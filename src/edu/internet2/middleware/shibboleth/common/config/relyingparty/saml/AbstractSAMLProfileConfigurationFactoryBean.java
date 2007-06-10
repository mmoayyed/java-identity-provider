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

package edu.internet2.middleware.shibboleth.common.config.relyingparty.saml;

import java.util.List;

import org.opensaml.xml.security.credential.Credential;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import edu.internet2.middleware.shibboleth.common.relyingparty.provider.AbstractSAMLProfileConfiguration;

/**
 * Base Spring factory bean for creating SAML profile configurations.
 */
public abstract class AbstractSAMLProfileConfigurationFactoryBean extends AbstractFactoryBean {

    /** Audiences of issued assertions. */
    private List<String> audiences;

    /** Amount of time before an issued assertion expires. */
    private long assertionLifetime;

    /** Default artifact type for the relying party. */
    private int defaultArtifactType;

    /** Whether assertions should be signed. */
    private boolean signAssertions;

    /** Whether to sign protocol requests. */
    private boolean signRequests;

    /** Whether to sign protocol responses. */
    private boolean signResponses;

    /** Credential used to sign assertions. */
    private Credential signingCredential;

    /**
     * Gets the amount of time, in milliseconds, before an issued assertion expires. A negative value indicates the
     * assertion never expires.
     * 
     * @return amount of time before an issued assertion expires
     */
    public long getAssertionLifetime() {
        return assertionLifetime;
    }

    /**
     * Sets the amount of time before an issued assertion expires.
     * 
     * @param lifetime amount of time before an issued assertion expires
     */
    public void setAssertionLifetime(long lifetime) {
        assertionLifetime = lifetime;
    }

    /**
     * Gets the audiences of issued assertions.
     * 
     * @return audiences of issued assertions
     */
    public List<String> getAudiences() {
        return audiences;
    }

    /**
     * Sets the audiences of issued assertions.
     * 
     * @param newAudiences audiences of issued assertions
     */
    public void setAudiences(List<String> newAudiences) {
        audiences = newAudiences;
    }

    /**
     * Gets the default artifact type for the relying party.
     * 
     * @return default artifact type for the relying party
     */
    public int getDefaultArtifactType() {
        return defaultArtifactType;
    }

    /**
     * Sets the default artifact type for the relying party.
     * 
     * @param type default artifact type for the relying party
     */
    public void setDefaultArtifactType(int type) {
        defaultArtifactType = type;
    }

    /**
     * Gets whether to sign protocol requests.
     * 
     * @return whether to sign protocol requests
     */
    public boolean getSignRequests() {
        return signRequests;
    }

    /**
     * Sets whether to sign protocol requests.
     * 
     * @param sign whether to sign protocol requests
     */
    public void setSignRequests(boolean sign) {
        signRequests = sign;
    }

    /**
     * Gets whether to sign protocol responses.
     * 
     * @return whether to sign protocol responses
     */
    public boolean getSignResposnes() {
        return signResponses;
    }

    /**
     * Sets whether to sign protocol responses.
     * 
     * @param sign whether to sign protocol responses
     */
    public void setSignResponses(boolean sign) {
        signResponses = sign;
    }

    /**
     * Gets whether assertions should be signed.
     * 
     * @return whether assertions should be signed
     */
    public boolean getSignAssertions() {
        return signAssertions;
    }

    /**
     * Sets whether assertions should be signed.
     * 
     * @param sign whether assertions should be signed
     */
    public void setSignAssertions(boolean sign) {
        signAssertions = sign;
    }

    /**
     * Gets the credential used to sign assertions.
     * 
     * @return credential used to sign assertions
     */
    public Credential getSigningCredential() {
        return signingCredential;
    }

    /**
     * Sets the credential used to sign assertions.
     * 
     * @param credential credential used to sign assertions
     */
    public void setSigningCredential(Credential credential) {
        signingCredential = credential;
    }

    /**
     * Populates the given profile configuration with standard information.
     * 
     * @param configuration configuration to populate
     */
    protected void populateBean(AbstractSAMLProfileConfiguration configuration) {
        configuration.setAssertionAudiences(getAudiences());
        configuration.setAssertionLifetime(getAssertionLifetime());
        configuration.setDefaultArtifactType(getDefaultArtifactType());
        configuration.setSignRequests(getSignRequests());
        configuration.setSignResponses(getSignResposnes());
        configuration.setSignAssertions(getSignAssertions());
        configuration.setSigningCredential(getSigningCredential());
    }
}