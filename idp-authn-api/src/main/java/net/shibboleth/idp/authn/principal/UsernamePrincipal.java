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
import net.shibboleth.shared.primitive.StringSupport;

import com.google.common.base.MoreObjects;

/** Principal based on a username. */
public class UsernamePrincipal implements CloneablePrincipal {

    /** The username. */
    @Nonnull @NotEmpty private String username;

    /**
     * Constructor.
     * 
     * @param user the username, can not be null or empty
     */
    public UsernamePrincipal(@Nonnull @NotEmpty @ParameterName(name="user") final String user) {
        username = Constraint.isNotNull(StringSupport.trimOrNull(user), "Username cannot be null or empty");
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getName() {
        return username;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return username.hashCode();
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

        if (other instanceof UsernamePrincipal) {
            return username.equals(((UsernamePrincipal) other).getName());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("username", username).toString();
    }
    
    /** {@inheritDoc} */
    @Nonnull public UsernamePrincipal clone() throws CloneNotSupportedException {
        final UsernamePrincipal copy = (UsernamePrincipal) super.clone();
        copy.username = username;
        return copy;
    }
}