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

package edu.internet2.middleware.shibboleth.common.relyingparty.provider;

import java.util.Collection;
import java.util.HashSet;

import org.opensaml.ws.security.SecurityPolicy;
import org.opensaml.xml.security.credential.Credential;

import edu.internet2.middleware.shibboleth.common.relyingparty.ProfileConfiguration;

/**
 * Profile settings common to SAML 1 and SAML 2.
 */
public abstract class AbstractSAMLProfileConfiguration implements ProfileConfiguration {

    /** Audiences for the assertion. */
    private Collection<String> assertionAudiences;

    /** Life of the assertion in milliseconds. */
    private long assertionLifetime;

    /** 2-byte artifact type used on outbound messages. */
    private byte[] outboundArtifactType;

    /** Security policy for this profile. */
    private SecurityPolicy profileSecurityPolicy;

    /** Whether to sign assertions. */
    private boolean signAssertions;

    /** Assertion signing credential. */
    private Credential signingCredential;

    /** Whether to sign protocol requests. */
    private boolean signRequests;

    /** Whether to sign protocol responses. */
    private boolean signResponses;

    /** Constructor. */
    protected AbstractSAMLProfileConfiguration() {
        assertionAudiences = new HashSet<String>();
    }

    /**
     * Gets the list of audiences an assertion is intended for.
     * 
     * @return list of audiences an assertion is intended for
     */
    public Collection<String> getAssertionAudiences() {
        return assertionAudiences;
    }

    /**
     * Gets the lifetime, in millisecond, for an issued assertion.
     * 
     * This value should be used to compute the NotOnOrAfter condition.
     * 
     * @return lifetime, in millisecond, for an issued assertion
     */
    public long getAssertionLifetime() {
        return assertionLifetime;
    }

    /**
     * Gets the 2-byte artifact type used on outbound messages.
     * 
     * @return 2-byte artifact type used on outbound messages
     */
    public byte[] getOutboundArtifactType() {
        return outboundArtifactType;
    }

    /** {@inheritDoc} */
    public SecurityPolicy getSecurityPolicy() {
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
     * Gets the credential that should be used to sign a message.
     * 
     * @return credential that should be used to sign a message
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
    public boolean getSignResponses() {
        return signResponses;
    }

    /**
     * Sets the list of audiences an assertion is intended for.
     * 
     * @param audiences list of audiences an assertion is intended for
     */
    public void setAssertionAudiences(Collection<String> audiences) {
        assertionAudiences = audiences;
    }

    /**
     * Sets the lifetime, in millisecond, for an issued assertion.
     * 
     * @param lifetime lifetime, in millisecond, for an issued assertion
     */
    public void setAssertionLifetime(long lifetime) {
        assertionLifetime = lifetime;
    }

    /**
     * Sets the 2-byte artifact type used on outbound messages.
     * 
     * @param type 2-byte artifact type used on outbound messages.
     */
    public void setOutboundArtifactType(byte[] type) {
        outboundArtifactType = type;
    }

    /**
     * Sets the security policy for this profile.
     * 
     * @param policy security policy for this profile
     */
    public void setSecurityPolicy(SecurityPolicy policy) {
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
     * Gets the credential that should be used for sign a message. Credential <strong>MUST</strong> include a private
     * key.
     * 
     * @param credential credential that should be used for sign a message
     */
    public void setSigningCredential(Credential credential) {
        if (credential != null && credential.getPrivateKey() == null) {
            throw new IllegalArgumentException("Credential does not contain a private key");
        }
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
}