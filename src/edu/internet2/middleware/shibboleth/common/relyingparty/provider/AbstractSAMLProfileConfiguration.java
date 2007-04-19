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

import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.util.DatatypeHelper;

import edu.internet2.middleware.shibboleth.common.relyingparty.ProfileConfiguration;

/**
 * Profile settings common to SAML 1 and SAML 2.
 */
public abstract class AbstractSAMLProfileConfiguration implements ProfileConfiguration {

    /** Life of the assertion in milliseconds. */
    private long assertionLifetime;

    /** Audiences for the assertion. */
    private Collection<String> assertionAudiences;

    /** Default NameID format. */
    private String defaultNameIDFormat;

    /** Default artifact type. */
    private int defaultArtifactType;

    /** Assertion signing credential. */
    private Credential signingCredential;

    /** Whether to sign assertions. */
    private boolean signAssertions;
    
    /** Whether to sign protocol requests. */
    private boolean signRequests;
    
    /** Whether to sign protocol responses. */
    private boolean signResponses;

    /** Constructor. */
    protected AbstractSAMLProfileConfiguration() {
        assertionAudiences = new HashSet<String>();
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
     * Sets the lifetime, in millisecond, for an issued assertion.
     * 
     * @param lifetime lifetime, in millisecond, for an issued assertion
     */
    public void setAssertionLifetime(long lifetime) {
        assertionLifetime = lifetime;
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
     * Sets the list of audiences an assertion is intended for.
     * 
     * @param audiences list of audiences an assertion is intended for
     */
    public void setAssertionAudiences(Collection<String> audiences) {
        assertionAudiences = audiences;
    }

    /**
     * Gets the URI for the default NameID format.
     * 
     * @return URI for the default NameID format
     */
    public String getDefaultNameIDFormat() {
        return defaultNameIDFormat;
    }

    /**
     * Sets the URI for the default NameID format.
     * 
     * @param format URI for the default NameID format
     */
    public void setDefaultNameIDFormat(String format) {
        defaultNameIDFormat = DatatypeHelper.safeTrimOrNullString(format);
    }

    /**
     * Gets the default artifact type.
     * 
     * @return default artifact type
     */
    public int getDefaultArtifactType() {
        return defaultArtifactType;
    }

    /**
     * Sets the default artifact type.
     * 
     * @param type default artifact type
     */
    public void setDefaultArtifactType(int type) {
        defaultArtifactType = type;
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
     * Gets the credential that should be used for sign a message. Credential <strong>MUST</strong> include a private
     * key.
     * 
     * @param credential credential that should be used for sign a message
     */
    public void setSigningCredential(Credential credential) {
        if (credential.getPrivateKey() == null) {
            throw new IllegalArgumentException("Credential does not contain a private key");
        }
        signingCredential = credential;
    }
    
    /**
     * Gets whether to sign protocol requests.
     * 
     * @return whether to sign protocol requests
     */
    public boolean getSignRequests(){
        return signRequests;
    }
    
    /**
     * Sets whether to sign protocol requests.
     * 
     * @param sign whether to sign protocol requests
     */
    public void setSignRequests(boolean sign){
        signRequests = sign;
    }
    
    /**
     * Gets whether to sign protocol responses.
     * 
     * @return whether to sign protocol responses
     */
    public boolean getSignResponses(){
        return signResponses;
    }
    
    /**
     * Sets whether to sign protocol responses.
     * 
     * @param sign whether to sign protocol responses
     */
    public void setSignResponses(boolean sign){
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
}