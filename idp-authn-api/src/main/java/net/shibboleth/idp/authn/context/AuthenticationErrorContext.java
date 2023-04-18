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
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.shared.annotation.constraint.Live;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;

import org.opensaml.messaging.context.BaseContext;

/**
 * A context that holds information about authentication failures.
 *
 * <p>The login process is particularly prone to requiring detailed error
 * information to provide appropriate user feedback and auditing, and this
 * context tracks errors that occur and preserves detailed information about
 * the kind of errors encountered in multi-part authentication flows.</p>
 * 
 * @parent {@link AuthenticationContext}
 * @added After authentication fails
 */
public final class AuthenticationErrorContext extends BaseContext {

    /** Ordered list of exceptions encountered. */
    @Nonnull @NonnullElements private List<Exception> exceptions;
    
    /** Error conditions detected through classified error messages. */
    @Nonnull @NonnullElements private Collection<String> classifiedErrors;
    
    /** Constructor. */
    public AuthenticationErrorContext() {
        exceptions = new ArrayList<>();
        classifiedErrors = new LinkedHashSet<>();
    }

    /**
     * Get an immutable list of the exceptions encountered.
     * 
     * @return  immutable list of exceptions
     */
    @Nonnull @NonnullElements @Live public List<Exception> getExceptions() {
        return exceptions;
    }
    
    /**
     * Get a mutable collection of error "tokens" associated with the context.
     * 
     * @return mutable collection of error strings
     */
    @Nonnull @NonnullElements @Live public Collection<String> getClassifiedErrors() {
        return classifiedErrors;
    }
    
    /**
     * Check for the presence of a particular error condition in the context.
     * 
     * @param error the condition to check for
     * @return  true iff the context contains the error condition specified
     */
    public boolean isClassifiedError(@Nonnull @NotEmpty final String error) {
        return classifiedErrors.contains(error);
    }
    
    /**
     * Adds a classified error to the context, ensuring that it will be returned
     * from {@link #getLastClassifiedError()} until another is added.
     * 
     * @param error error to add
     * 
     * @return this context
     * 
     * @since 5.0.0
     */
    @Nonnull public AuthenticationErrorContext addClassifiedError(@Nonnull @NotEmpty final String error) {
        // This is done to preserve ordering so that the error is the "last one added".
        classifiedErrors.remove(error);
        classifiedErrors.add(error);
        return this;
    }
    
    /**
     * Gets the last classified error added, or null if none.
     * 
     * @return last error added or null
     * 
     * @since 5.0.0
     */
    @Nullable public String getLastClassifiedError() {
        return classifiedErrors.stream().reduce((first, second) -> second).orElse(null);
    }
    
}