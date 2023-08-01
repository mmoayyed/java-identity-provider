/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.util.Collection;
import java.util.LinkedHashSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.BaseContext;

import net.shibboleth.shared.annotation.constraint.Live;
import net.shibboleth.shared.annotation.constraint.NotEmpty;

/**
 * A context that holds information about authentication warnings.
 *
 * <p>The login process is particularly prone to requiring detailed warning
 * information to provide appropriate user feedback and auditing, and this
 * context tracks warnings that occur and preserves detailed information about
 * the kind of warnings encountered in multi-part authentication flows.</p>
 * 
 * @parent {@link AuthenticationContext}
 * @added After a warning is generated during authentication
 */
public final class AuthenticationWarningContext extends BaseContext {

    /** Warning conditions detected through classified warning messages. */
    @Nonnull private Collection<String> classifiedWarnings;
    
    /** Constructor. */
    public AuthenticationWarningContext() {
        classifiedWarnings = new LinkedHashSet<>();
    }

    /**
     * Get a mutable collection of warning "tokens" associated with the context.
     * 
     * @return mutable collection of warning strings
     */
    @Nonnull @Live public Collection<String> getClassifiedWarnings() {
        return classifiedWarnings;
    }
    
    /**
     * Check for the presence of a particular warning condition in the context.
     * 
     * @param warning the condition to check for
     * @return  true if the context contains the warning condition specified
     */
    public boolean isClassifiedWarning(@Nonnull @NotEmpty final String warning) {
        return classifiedWarnings.contains(warning);
    }

    /**
     * Adds a classified warning to the context, ensuring that it will be returned
     * from {@link #getLastClassifiedWarning()} until another is added.
     * 
     * @param warning warning to add
     * 
     * @return this context
     * 
     * @since 5.0.0
     */
    @Nonnull public AuthenticationWarningContext addClassifiedWarning(@Nonnull @NotEmpty final String warning) {
        // This is done to preserve ordering so that the error is the "last one added".
        classifiedWarnings.remove(warning);
        classifiedWarnings.add(warning);
        return this;
    }
    
    /**
     * Gets the last classified warning added, or null if none.
     * 
     * @return last warning added or null
     * 
     * @since 5.0.0
     */
    @Nullable public String getLastClassifiedWarning() {
        return classifiedWarnings.stream().reduce((first, second) -> second).orElse(null);
    }

}