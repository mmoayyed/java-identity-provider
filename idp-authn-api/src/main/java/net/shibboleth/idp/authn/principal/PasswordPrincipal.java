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

package net.shibboleth.idp.authn.principal;

import javax.annotation.Nonnull;

import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;

import com.google.common.base.MoreObjects;

/** Principal that wraps a password. */
public class PasswordPrincipal implements CloneablePrincipal {

    /** The password. */
    @Nonnull @NotEmpty private String password;

    /**
     * Constructor.
     * 
     * @param pw the password
     */
    public PasswordPrincipal(@Nonnull @NotEmpty @ParameterName(name="pw") final String pw) {
        password = Constraint.isNotEmpty(pw, "Password cannot be null or empty");
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getName() {
        return password;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return password.hashCode();
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

        if (other instanceof PasswordPrincipal) {
            return password.equals(((PasswordPrincipal) other).getName());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("password", "<elided>").toString();
    }
    
    /** {@inheritDoc} */
    @Nonnull public PasswordPrincipal clone() throws CloneNotSupportedException {
        final PasswordPrincipal copy = (PasswordPrincipal) super.clone();
        copy.password = password;
        return copy;
    }
    
}