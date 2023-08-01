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

package net.shibboleth.idp.ui.csrf.impl;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import net.shibboleth.idp.ui.csrf.CSRFToken;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;


/**
 * A default, immutable, implementation of a {@link CSRFToken}. 
 */
@Immutable
public class SimpleCSRFToken implements CSRFToken{

    /** Serial UID. */
    private static final long serialVersionUID = 3742188179558262003L;

    /** The anti-csrf token value.*/
    @Nonnull @NotEmpty private String token;
    
    /** The HTTP parameter that holds the token value. */
    @Nonnull @NotEmpty private String parameterName;
    
    /**
     * 
     * Constructor.
     *
     * @param csrfToken the anti-csrf token value
     * @param paramName the HTTP parameter name that holds the anti-csrf token.
     */
    public SimpleCSRFToken(@Nonnull @NotEmpty final String csrfToken, @Nonnull @NotEmpty final String paramName) {
        token = Constraint.isNotEmpty(csrfToken, "CSRF Tokens can not be null or empty");
        parameterName = Constraint.isNotEmpty(paramName, "CSRF parameter name can not be null or empty");
    }

    /** {@inheritDoc} */
    @Nonnull public String getParameterName() {
        return parameterName;
    }

    /** {@inheritDoc} */
    public @Nonnull String getToken() {
       return token;
    }

}