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

package net.shibboleth.idp.authn.principal;

import java.security.Principal;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;

import com.google.common.base.MoreObjects;

/** Principal that wraps an {@link AuthenticationResult}. */
public class AuthenticationResultPrincipal implements Principal {

    /** The authentication result. */
    @Nonnull private AuthenticationResult authnResult;

    /**
     * Constructor.
     * 
     * @param result the result to wrap
     */
    public AuthenticationResultPrincipal(@Nonnull @ParameterName(name="result") final AuthenticationResult result) {
        authnResult = Constraint.isNotNull(result, "AuthenticationResult cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty public String getName() {
        return authnResult.getAuthenticationFlowId();
    }
    
    /**
     * Get the {@link AuthenticationResult}.
     * 
     * @return the authentication result
     */
    @Nonnull public AuthenticationResult getAuthenticationResult() {
        return authnResult;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return authnResult.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        if (other == null) {
            return false;
        }

        if (this == other) {
            return true;
        }

        if (other instanceof AuthenticationResultPrincipal) {
            return authnResult.equals(((AuthenticationResultPrincipal) other).getAuthenticationResult());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("authnResult", authnResult).toString();
    }
    
}