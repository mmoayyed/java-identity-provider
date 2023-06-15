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

package net.shibboleth.idp.authn.duo;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.principal.CloneablePrincipal;
import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;

import com.google.common.base.MoreObjects;

/** Principal based on a Duo authentication. */
public class DuoPrincipal implements CloneablePrincipal {

    /** The username. */
    @Nonnull @NotEmpty private String username;

    /**
     * Constructor.
     * 
     * @param name the username
     */
    public DuoPrincipal(@Nonnull @NotEmpty @ParameterName(name="name") final String name) {
        username = Constraint.isNotNull(StringSupport.trimOrNull(name), "Username cannot be null or empty");
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

        if (other instanceof DuoPrincipal) {
            return username.equals(((DuoPrincipal) other).getName());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("username", username).toString();
    }
    
    /** {@inheritDoc} */
    @Nonnull public DuoPrincipal clone() throws CloneNotSupportedException {
        final DuoPrincipal copy = (DuoPrincipal) super.clone();
        copy.username = username;
        return copy;
    }
}