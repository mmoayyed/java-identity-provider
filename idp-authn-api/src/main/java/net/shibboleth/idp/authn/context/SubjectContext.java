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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import net.shibboleth.idp.authn.AuthenticationEvent;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;

import org.opensaml.messaging.context.BaseContext;

import com.google.common.collect.HashMultimap;

/**
 * A {@link BaseContext} that holds information about the subject of a transaction.
 * 
 * <p>The subject may or may not be authenticated, such as in a back-channel profile, but
 * profiles that operate on subjects can treat the information as "trusted" for their purposes.
 * This context must not be used to carry speculative or unverified subject information.</p>
 * 
 * <p>The use of a multimap enables a subject to be associated with more than one instance of
 * authentication by a particular workflow.</p>
 */
public class SubjectContext extends BaseContext {

    /** Canonical principal name of subject. */
    @Nullable private String principalName;

    /** The active authentication events for the subject. */
    @Nonnull private final HashMultimap<String, AuthenticationEvent> authenticationEvents;
    
    /** Constructor. */
    public SubjectContext() {
        super();
        
        authenticationEvents = HashMultimap.create(5, 1);
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
     */
    public void setPrincipalName(@Nullable String name) {
        principalName = name;
    }

    /**
     * Get a mutable multimap of workflow IDs to authentication events.
     * 
     * @return  mutable multimap of workflow IDs to authentication events
     */
    @Nonnull @NonnullElements public HashMultimap<String, AuthenticationEvent> getAuthenticationEvents() {
        return authenticationEvents;
    }
    
    /**
     * Get an immutable list of Subjects extracted from every AuthenticationEvent
     * associated with the context.
     * 
     * @return immutable list of Subjects 
     */
    @Nonnull @Unmodifiable @NonnullElements public List<Subject> getSubjects() {
        List<Subject> composite = new ArrayList<>();
        for (AuthenticationEvent e : getAuthenticationEvents().values()) {
            composite.addAll(e.getSubjects());
        }
        return Collections.unmodifiableList(composite);
    }
    
}