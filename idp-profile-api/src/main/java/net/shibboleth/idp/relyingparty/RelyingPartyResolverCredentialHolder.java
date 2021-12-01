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

package net.shibboleth.idp.relyingparty;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.security.credential.Credential;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;

/**
 * This is a utility class used as an auto-wiring source for collections of
 * signing and encryption {@link Credential} objects so that other layers of the
 * system can gain access to the complete set of them.
 * 
 * @since 4.2.0
 */
public class RelyingPartyResolverCredentialHolder {

    /** Credentials to expose. */
    @Nonnull @NonnullElements private final List<Credential> credentials;
    
    /**
     * Constructor.
     *
     * @param creds credentials to expose to other components
     */
    public RelyingPartyResolverCredentialHolder(@Nullable @NonnullElements final Collection<Credential> creds) {
        if (creds != null) {
            credentials = List.copyOf(creds);
        } else {
            credentials = Collections.emptyList();
        }
    }
    
    /**
     * Get the credentials to expose to other components.
     * 
     * @return credentials to expose
     */
    @Nonnull @NonnullElements public Collection<Credential> getCredentials() {
        return credentials;
    }
    
}