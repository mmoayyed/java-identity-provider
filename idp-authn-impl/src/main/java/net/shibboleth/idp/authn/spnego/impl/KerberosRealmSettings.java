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

package net.shibboleth.idp.authn.spnego.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.AbstractInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Kerberos realm settings for the SPNEGO authentication flow.
 */
public class KerberosRealmSettings extends AbstractInitializableComponent {

    /** The service's principal. */
    @NonnullAfterInit private String servicePrincipal;

    /** The keytab to use (keytab and password are mutually exclusive). */
    @Nullable private String keytab;

    /** The password to use (keytab and password are mutually exclusive). */
    @Nullable private String password;

    /**
     * Set the service principal name. Required.
     * 
     * @param principal service principal
     */
    public void setServicePrincipal(@Nonnull @NotEmpty final String principal) {
        checkSetterPreconditions();
        servicePrincipal = Constraint.isNotEmpty(StringSupport.trim(principal), "Principal cannot be null or empty");
    }

    /**
     * Get the service principal name.
     * 
     * @return service principal
     */
    @NonnullAfterInit @NotEmpty public String getServicePrincipal() {
        return servicePrincipal;
    }

    /**
     * Set the keytab to use. keytab and password are mutually exclusive.
     * 
     * @param newKeytab keytab to use
     */
    public void setKeytab(@Nullable final String newKeytab) {
        checkSetterPreconditions();
        keytab = StringSupport.trim(newKeytab);
    }

    /**
     * Get the keytab.
     * 
     * @return keytab
     */
    @Nullable public String getKeytab() {
        return keytab;
    }

    /**
     * Set the password to use. keytab and password are mutually exclusive.
     * 
     * @param newPassword password to use
     */
    public void setPassword(@Nullable final String newPassword) {
        checkSetterPreconditions();
        password = newPassword;
    }

    /**
     * Get the password.
     * 
     * @return password
     */
    @Nullable public String getPassword() {
        return password;
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (servicePrincipal == null) {
            throw new ComponentInitializationException("'servicePrincipal' must be set.");
        }

        if (keytab == null && password == null) {
            throw new ComponentInitializationException("One of 'keytab' or 'password' must be set.");
        }

        if (keytab != null && password != null) {
            throw new ComponentInitializationException("'keytab' and 'password' are mutually exclusive.");
        }
    }
    
}