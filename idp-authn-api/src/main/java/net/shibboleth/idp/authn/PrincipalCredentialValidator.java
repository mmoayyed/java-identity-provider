/*
 * Licensed to the University Corporation for Advanced Internet Development, Inc.
 * under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache 
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

package net.shibboleth.idp.authn;

/**
 * A credential validator, as its name implies, is meant to validate a {@link PrincipalCredential}. The validator may
 * also prompt the user for credential information. For example by display a username/password form.
 * 
 * The general flow for a UI based validator is that
 * {@link #validateCredential(AuthenticationMethodContext, PrincipalCredential)} will first be invoked with a null
 * credential, the validator will send a response page that prompts the user for a credential and the validator will be
 * invoked a second time, this time with the user supplied credential.
 */
public interface PrincipalCredentialValidator {

    /**
     * Determines if the credential type is supported by this validator.
     * 
     * @param credentialType type of the {@link PrincipalCredential}
     * 
     * @return true if this validator supports the given credential type, otherwise false
     */
    public boolean supportCredentialType(final Class<? extends PrincipalCredential> credentialType);

    /**
     * Gets whether this validator supports forcefully re-authenticating a user.
     * 
     * @return whether this validator supports forcefully re-authenticating a user
     */
    public boolean supportsForcedAuthentication();

    /**
     * Gets whether this validator supports passive authentication. Passive authentication is act of validating the
     * user, given their credential, without displaying a UI or otherwise prompting for additional information.
     * 
     * @return whether this validator supports passive authentication
     */
    public boolean supportsPassiveAuthentication();

    /**
     * Validates a given credentials, possibly prompting for more information if necessary.
     * 
     * <strong>NOTE</strong> this method must not flush the HTTP response. This will be done at a later processing
     * stage.
     * 
     * @param requestContext current authentication request context
     * @param credential current principal credential, may be null if none are yet available
     * 
     * @return TODO what should this return? Needs to produce state (completed, failed, request for info),
     *         principal/subject
     * 
     * @throws Exception thrown if there is an error validating the credential or requesting more information
     */
    public Object validateCredential(final AuthenticationRequestContext requestContext,
            final PrincipalCredential credential) throws Exception;
}
