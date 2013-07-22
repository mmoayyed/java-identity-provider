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

package net.shibboleth.idp.saml.authn;

import java.security.Principal;

import javax.annotation.Nonnull;

import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.AuthnContextDeclRef;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import com.google.common.base.Objects;

/** Principal based on a SAML AuthnContextDeclRef. */
public final class AuthnContextDeclRefPrincipal implements Principal {

    /** The decl ref. */
    @Nonnull @NotEmpty private final String authnContextDeclRef;

    /**
     * Constructor.
     * 
     * @param declRef the decl ref URI, cannot be null or empty
     */
    public AuthnContextDeclRefPrincipal(@Nonnull @NotEmpty final String declRef) {
        authnContextDeclRef = Constraint.isNotNull(
                StringSupport.trimOrNull(declRef), "AuthnContextDeclRef cannot be null or empty");
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getName() {
        return authnContextDeclRef;
    }
    
    /**
     * Returns the value as a SAML {@link AuthnContextDeclRef}.
     * 
     * @return  the principal value in the form of an {@link AuthnContextDeclRef}
     */
    @Nonnull public AuthnContextDeclRef getAuthnContextDeclRef() {
        AuthnContextDeclRef ref = (AuthnContextDeclRef) Constraint.isNotNull(
                XMLObjectSupport.getBuilder(AuthnContextDeclRef.DEFAULT_ELEMENT_NAME),
                    "No builder for AuthnContextDeclRef").buildObject(AuthnContextDeclRef.DEFAULT_ELEMENT_NAME);
        ref.setAuthnContextDeclRef(getName());
        return ref;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return authnContextDeclRef.hashCode();
    }

    /** {@inheritDoc} */
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }

        if (this == other) {
            return true;
        }

        if (other instanceof AuthnContextDeclRefPrincipal) {
            return authnContextDeclRef.equals(((AuthnContextDeclRefPrincipal) other).getName());
        }

        return false;
    }

    /** {@inheritDoc} */
    public String toString() {
        return Objects.toStringHelper(this).add("authnContextDeclRef", authnContextDeclRef).toString();
    }
}