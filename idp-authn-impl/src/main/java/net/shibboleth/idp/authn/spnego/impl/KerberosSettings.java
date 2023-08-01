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

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Kerberos settings for the SPNEGO authentication flow.
 */
public class KerberosSettings {

    /** Class name of JAAS LoginModule to acquire Kerberos credentials. */
    @Nonnull @NotEmpty private String loginModuleClassName;

    /** Refresh the Kerberos config before running? */
    private boolean refreshKrb5Config;

    /** List of realms (KerberosRealmSettings objects). */
    @Nonnull private Collection<KerberosRealmSettings> realmSettings;

    /** Constructor. */
    public KerberosSettings() {
        loginModuleClassName = "com.sun.security.auth.module.Krb5LoginModule";
        realmSettings = CollectionSupport.emptyList();
    }

    /**
     * Set the name of the JAAS LoginModule to use to acquire Kerberos credentials.
     * 
     * @param name name of login module class
     */
    public void setLoginModuleClassName(@Nonnull @NotEmpty final String name) {
        loginModuleClassName =
                Constraint.isNotNull(StringSupport.trimOrNull(name), "Class name cannot be null or empty");
    }

    /**
     * Return name of the JAAS LoginModule to use to acquire Kerberos credentials.
     * 
     * @return name of login module class
     */
    @Nonnull @NotEmpty public String getLoginModuleClassName() {
        return loginModuleClassName;
    }

    /**
     * Set whether to refresh the Kerberos configuration before running.
     * 
     * @param flag flag to set
     */
    public void setRefreshKrb5Config(final boolean flag) {
        refreshKrb5Config = flag;
    }

    /**
     * Return whether to refresh the Kerberos configuration before running.
     * 
     * @return true if Kerberos configuration is to be refreshed
     */
    public boolean getRefreshKrb5Config() {
        return refreshKrb5Config;
    }

    /**
     * Collection of realms (KerberosRealmSettings objects).
     * 
     * @param realms realms to set.
     */
    public void setRealms(@Nullable final Collection<KerberosRealmSettings> realms) {
        if (realms != null) {
            realmSettings = CollectionSupport.copyToList(realms);
        } else {
            realmSettings = CollectionSupport.emptyList();
        }
    }

    /**
     * Get list of realms.
     * 
     * @return list of realms
     */
    @Nonnull @NotLive @Unmodifiable public Collection<KerberosRealmSettings> getRealms() {
        return realmSettings;
    }
    
}