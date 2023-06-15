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

package net.shibboleth.idp.authn.context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.shared.annotation.constraint.Live;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;

import org.opensaml.messaging.context.BaseContext;

/**
 * A context that holds information about the subject of a request.
 * 
 * <p>The subject may or may not be authenticated, such as in a back-channel profile, but
 * profiles that operate on subjects can treat the information as "trusted" for their purposes.
 * This context must not be used to carry speculative or unverified subject information.</p>
 * 
 * <p>This is the ultimate product of a successful authentication process.</p>
 * 
 * <p>A second field is available to store an identity that is impersonating the effective
 * subject identity. Profiles should operate on the effective subject unless they need to
 * be aware of both identities.</p>
 * 
 * @parent {@link org.opensaml.profile.context.ProfileRequestContext}
 * @added After the subject of a request is determined
 */
public final class SubjectContext extends BaseContext {

    /** Canonical principal name of an impersonating identity. */
    @Nullable private String impersonatingPrincipalName;
    
    /** Canonical principal name of subject. */
    @Nullable private String principalName;

    /** The active authentication results for the subject. */
    @Nonnull private final Map<String,AuthenticationResult> authenticationResults;
    
    /** Constructor. */
    public SubjectContext() {
        authenticationResults = new HashMap<>(2);
    }

    /**
     * Get the canonical principal name of the subject.
     * 
     * @return the canonical principal name
     */
    @Nullable public String getPrincipalName() {
        return principalName;
    }

    /**
     * Set the canonical principal name of the subject.
     * 
     * @param name the canonical principal name
     * 
     * @return this context
     */
    @Nonnull public SubjectContext setPrincipalName(@Nullable final String name) {
        principalName = name;
        
        return this;
    }

    /**
     * Get the canonical principal name of an identity that is impersonating the subject.
     * 
     * @return the canonical principal name of an impersonating identity
     * 
     * @since 3.4.0
     */
    @Nullable public String getImpersonatingPrincipalName() {
        return impersonatingPrincipalName;
    }

    /**
     * Set the canonical principal name of an identity that is impersonating the subject.
     * 
     * @param name the canonical principal name of an impersonating identity
     * 
     * @return this context
     * 
     * @since 3.4.0
     */
    @Nonnull public SubjectContext setImpersonatingPrincipalName(@Nullable final String name) {
        impersonatingPrincipalName = name;
        
        return this;
    }
    
    /**
     * Get a mutable map of authentication flow IDs to authentication results.
     * 
     * @return  mutable map of authentication flow IDs to authentication results
     */
    @Nonnull @Live public Map<String,AuthenticationResult> getAuthenticationResults() {
        return authenticationResults;
    }
    
    /**
     * Get an immutable list of Subjects extracted from every AuthenticationResult
     * associated with the context.
     * 
     * @return immutable list of Subjects 
     */
    @Nonnull @Unmodifiable @NotLive public List<Subject> getSubjects() {
        return authenticationResults.values()
                .stream()
                .map(AuthenticationResult::getSubject)
                .collect(CollectionSupport.nonnullCollector(Collectors.toUnmodifiableList())).
                get();
    }
    
}