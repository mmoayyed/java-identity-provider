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

package edu.internet2.middleware.shibboleth.common.relyingparty;

import org.opensaml.xml.security.credential.Credential;

/**
 * SAML specific relying party configurations.
 */
public interface SAMLRelyingPartyConfiguration extends RelyingPartyConfiguration {

    /**
     * Gets the entity ID to use as the issuer in SAML elements.
     * 
     * @return entity ID to use as the issuer in SAML elements
     */
    public String getIssuerID();

    /**
     * Gets whether providing attributes in a response an authentication request is preferred.
     * 
     * @return whether providing attributes in a response an authentication request is preferred
     */
    public boolean isAttributePushPreferred();

    /**
     * Gets the credential that should be used to sign a message. Credential <strong>MUST</strong> include a private
     * key.
     * 
     * @return credential that should be used to sign a message
     */
    public Credential getSigningCredential();

    /**
     * Gets the credential that should be used to decrypt a message. Credential <strong>MUST</strong> include a private
     * key.
     * 
     * @return credential that should be used to decrypt a message
     */
    public Credential getDecryptionCredential();

    /**
     * Gets the URI for the default NameID format.
     * 
     * @return URI for the default NameID format
     */
    public String getDefaultNameIDFormat();

    /**
     * Gets the URI for the default authentication method.
     * 
     * @return URI for the default authentication method
     */
    public String getDefaultAuthenticationMethod();

    /**
     * Gets the default artifact type.
     * 
     * @return default artifact type
     */
    public int getDefaultArtifactType();

    /**
     * Gets whether assertions should be signed.
     * 
     * @return whether assertions should be signed
     */
    public boolean signAssertions();

    /**
     * Gets whether NameIDs should be encrypted.
     * 
     * @return whether NameIDs should be encrypted
     */
    public boolean encryptNameID();

    /**
     * Gets whether assertions should be encrypted.
     * 
     * @return whether assertions should be encrypted
     */
    public boolean encryptAssertion();
}