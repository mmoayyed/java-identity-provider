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

package net.shibboleth.idp.authn.duo.impl;

import com.duosecurity.duoweb.DuoWeb;
import com.duosecurity.duoweb.DuoWebException;

import net.shibboleth.idp.authn.duo.DuoIntegration;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Nonnull;

/**
 * Simple wrapper for DuoWeb operations.
 * 
 * @since 3.3.0
 */
public final class DuoSupport {
    
    /** Constructor. */
    private DuoSupport() {
    }

    /**
     * Created a signed request to Duo for a user.
     * 
     * @param duo integration parameters to use
     * @param username user to authenticate
     * @return the signed request string
     * 
     * @throws DuoWebException if an error occurs
     */
    @Nonnull @NotEmpty public static String generateSignedRequestToken(@Nonnull final DuoIntegration duo,
            @Nonnull @NotEmpty final String username)
            throws DuoWebException {
        final String signedRequestToken = DuoWeb.signRequest(duo.getIntegrationKey(), duo.getSecretKey(),
                duo.getApplicationKey(), username);
        if (signedRequestToken.startsWith("ERR|")) {
            throw new DuoWebException(signedRequestToken);
        }
        return signedRequestToken;
    }

// Checkstyle: ThrowsCount OFF
    /**
     * Verify a signed response from Duo and extract the username.
     * 
     * @param duo integration parameters to use
     * @param signedResponseToken    response to validate
     * 
     * @return the username from the response
     * @throws DuoWebException if a Duo failure occurs
     * @throws InvalidKeyException if a key is invalid
     * @throws IOException if an I/O error occurs
     * @throws NoSuchAlgorithmException if the hashing algorithm is unavailable
     */
    @Nonnull @NotEmpty public static String validateSignedResponseToken(@Nonnull final DuoIntegration duo,
            @Nonnull @NotEmpty final String signedResponseToken)
        throws DuoWebException, InvalidKeyException, IOException, NoSuchAlgorithmException {
        
        final String username = DuoWeb.verifyResponse(duo.getIntegrationKey(), duo.getSecretKey(),
                duo.getApplicationKey(), signedResponseToken);
        return username;
    }
// Checkstyle: ThrowsCount ON
    
}