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

import org.opensaml.ws.security.SecurityPolicy;
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

    /** 2-byte artifact type used for outbound messages. */
    private byte[] outboundArtifactType;

    /** Whether assertions should be signed. */
    private boolean signAssertions;

    /** Whether to sign protocol requests. */
    private boolean signRequests;

    /** Whether to sign protocol responses. */
    private boolean signResponses;

    /** Credential used to sign assertions. */
    private Credential signingCredential;
    
    /** Security policy for this profile. */
    private SecurityPolicy profileSecurityPolicy;
    
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
     * Gets the audiences of issued assertions.
     * 
     * @return audiences of issued assertions
     */
    public List<String> getAudiences() {
        return audiences;
    }

    /**
     * Gets the 2-byte artifact type used for outbound messages.
     * 
     * @return 2-byte artifact type used for outbound messages
     */
    public  byte[] getOutboundArtifactType() {
        return outboundArtifactType;
    }

    /**
     * Gets the security policy for this profile.
     * 
     * @return security policy for this profile
     */
    public SecurityPolicy getProfileSecurityPolicy() {
        return profileSecurityPolicy;
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
     * Gets the credential used to sign assertions.
     * 
     * @return credential used to sign assertions
     */
    public Credential getSigningCredential() {
        return signingCredential;
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
     * Gets whether to sign protocol responses.
     * 
     * @return whether to sign protocol responses
     */
    public boolean getSignResposnes() {
        return signResponses;
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
     * Sets the audiences of issued assertions.
     * 
     * @param newAudiences audiences of issued assertions
     */
    public void setAudiences(List<String> newAudiences) {
        audiences = newAudiences;
    }

    /**
     * Sets the 2-byte artifact type used for outbound messages.
     * 
     * @param type 2-byte artifact type used for outbound messages
     */
    public void setOutboundArtifactType(byte[] type) {
        outboundArtifactType = type;
    }

    /**
     * Sets the security policy for this profile.
     * 
     * @param policy security policy for this profile
     */
    public void setProfileSecurityPolicy(SecurityPolicy policy) {
        profileSecurityPolicy = policy;
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
     * Sets the credential used to sign assertions.
     * 
     * @param credential credential used to sign assertions
     */
    public void setSigningCredential(Credential credential) {
        signingCredential = credential;
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
     * Sets whether to sign protocol responses.
     * 
     * @param sign whether to sign protocol responses
     */
    public void setSignResponses(boolean sign) {
        signResponses = sign;
    }
    
    /**
     * Populates the given profile configuration with standard information.
     * 
     * @param configuration configuration to populate
     */
    protected void populateBean(AbstractSAMLProfileConfiguration configuration) {
        configuration.setAssertionAudiences(getAudiences());
        configuration.setAssertionLifetime(getAssertionLifetime());
        configuration.setSecurityPolicy(getProfileSecurityPolicy());
        configuration.setOutboundArtifactType(getOutboundArtifactType());
        configuration.setSignRequests(getSignRequests());
        configuration.setSignResponses(getSignResposnes());
        configuration.setSignAssertions(getSignAssertions());
        configuration.setSigningCredential(getSigningCredential());
    }
}