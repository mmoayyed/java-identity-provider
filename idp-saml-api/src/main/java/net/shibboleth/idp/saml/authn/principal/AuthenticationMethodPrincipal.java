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

package net.shibboleth.idp.saml.authn.principal;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.principal.CloneablePrincipal;
import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;

import com.google.common.base.MoreObjects;

/** Principal based on a SAML 1.x AuthenticationMethod. */
public final class AuthenticationMethodPrincipal implements CloneablePrincipal {

    /** The method. */
    @Nonnull @NotEmpty private String authnMethod;

    /**
     * Constructor.
     * 
     * @param method the method URI
     */
    public AuthenticationMethodPrincipal(@Nonnull @NotEmpty @ParameterName(name="method") final String method) {
        authnMethod = Constraint.isNotNull(
                StringSupport.trimOrNull(method), "AuthenticationMethod cannot be null or empty");
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getName() {
        return authnMethod;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return authnMethod.hashCode();
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

        if (other instanceof AuthenticationMethodPrincipal) {
            return authnMethod.equals(((AuthenticationMethodPrincipal) other).getName());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("authnMethod", authnMethod).toString();
    }

    /** {@inheritDoc} */
    @Nonnull public AuthenticationMethodPrincipal clone() throws CloneNotSupportedException {
        final AuthenticationMethodPrincipal copy = (AuthenticationMethodPrincipal) super.clone();
        copy.authnMethod = authnMethod;
        return copy;
    }
}