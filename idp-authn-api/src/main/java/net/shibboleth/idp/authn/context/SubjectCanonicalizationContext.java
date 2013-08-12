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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.messaging.context.BaseContext;

/**
 * A {@link BaseContext} that holds an input {@link Subject} to canonicalize into a
 * principal name.
 */
public class SubjectCanonicalizationContext extends BaseContext {

    /** Subject to canonicalize. */
    @Nonnull private final Subject subject;
    
    /** Canonical principal name of subject. */
    @Nullable private String principalName;
    
    /** Exception raised by a failed canonicalization. */
    @Nullable private Exception canonicalizationError;
    
    /**
     * Constructor.
     * 
     * @param newSubject subject to canonicalize
     */
    public SubjectCanonicalizationContext(@Nonnull final Subject newSubject) {
        super();
        
        subject = Constraint.isNotNull(newSubject, "Subject cannot be null");
    }

    /**
     * Get the Subject to canonicalize.
     * 
     * @return Subject to canonicalize
     */
    @Nonnull public Subject getSubject() {
        return subject;
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
    public void setPrincipalName(@Nonnull @NotEmpty final String name) {
        principalName = Constraint.isNotNull(
                StringSupport.trimOrNull(name), "Principal name cannot be null or empty");
    }
    
    /**
     * Get the exception raised by a failed canonicalization.
     * 
     * @return  exception raised by a failed canonicalization
     */
    @Nullable public Exception getException() {
        return canonicalizationError;
    }
    
    /**
     * Set the exception raised by a failed canonicalization.
     * 
     * @param e  exception raised by a failed canonicalization
     */
    public void setException(@Nonnull final Exception e) {
        canonicalizationError = Constraint.isNotNull(e, "Exception cannot be null");
    }
}