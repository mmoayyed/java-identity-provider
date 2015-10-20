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

package net.shibboleth.idp.authn.impl.spnego;

import java.util.List;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Kerberos settings for the SPNEGO authentication flow.
 */
public class KerberosSettings extends AbstractInitializableComponent {

    /** Class name of JAAS LoginModule to acquire Kerberos credentials. */
    @NonnullAfterInit @NotEmpty private String loginModuleClassName;

    /** Refresh the Kerberos config before running? */
    private boolean refreshKrb5Config;

    /** List of realms (KerberosRealmSettings objects). */
    @NonnullAfterInit private List<KerberosRealmSettings> realms;

    /** Constructor. */
    public KerberosSettings() {
        loginModuleClassName = "com.sun.security.auth.module.Krb5LoginModule";
    }

    /**
     * Set the name of the JAAS LoginModule to use to acquire Kerberos credentials.
     * 
     * @param name name of login module class
     */
    public void setLoginModuleClassName(@Nonnull final String name) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        loginModuleClassName =
                Constraint.isNotNull(StringSupport.trimOrNull(name), "Class name cannot be null or empty");
    }

    /**
     * Return name of the JAAS LoginModule to use to acquire Kerberos credentials.
     * 
     * @return name of login module class
     */
    @Nonnull
    @NotEmpty
    public String getLoginModuleClassName() {
        return loginModuleClassName;
    }

    /**
     * Set whether to refresh the Kerberos configuration before running.
     * 
     * @param flag flag to set
     */
    public void setRefreshKrb5Config(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        refreshKrb5Config = flag;
    }

    /**
     * Return whether to refresh the Kerberos configuration before running.
     * 
     * @return true if Kerberos configuration is to be refreshed
     */
    public boolean getReRefreshKrb5Config() {
        return refreshKrb5Config;
    }

    /**
     * List of realms (KerberosRealmSettings objects).
     * 
     * @param list list of realms to set.
     */
    public void setRealms(@Nonnull @NotEmpty final List<KerberosRealmSettings> list) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        realms = Constraint.isNotNull(list, "The list of realms cannot be null");
        Constraint.isFalse(realms.isEmpty(), "The list of realms cannot be empty");
    }

    /**
     * Get list of realms.
     * 
     * @return list of realms
     */
    @Nonnull
    @NotEmpty
    public List<KerberosRealmSettings> getRealms() {
        return realms;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (realms == null) {
            throw new ComponentInitializationException("realms must be set");
        }

        if (realms.isEmpty()) {
            throw new ComponentInitializationException("At least one realm must be configured");
        }
    }
}
