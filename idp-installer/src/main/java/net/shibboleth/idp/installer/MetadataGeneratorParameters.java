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

package net.shibboleth.idp.installer;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.shared.annotation.constraint.NotEmpty;

/**
 * Interface which describes metadata that needs to be generated.
 */
public interface MetadataGeneratorParameters {

    /**
     * Get the (mutli-line) string representations of the encryption certs.
     *
     * @return Returns the encryption cert or null if none available.
     */
    @Nullable public List<String> getEncryptionCert();

    /**
     * Get the (mutli-line) string representation of the signing cert.
     *
     * @return Returns the signing cert or null if none available.
     */
    @Nullable public List<String> getSigningCert();

    /**
     * Get the (mutli-line)string representation of the back channel cert.
     *
     * @return Returns the back channel cert or null if non available.
     */
    @Nullable public List<String> getBackchannelCert();

    /**
     * Returns the entityID.
     *
     * @return the entityID.
     */
    @Nonnull @NotEmpty public String getEntityID();

    /**
     * Returns the dnsName (for use in endpoints).
     *
     * @return the dnsname.
     */
    @Nonnull @NotEmpty public String getDnsName();

    /**
     * Returns the scope used.
     *
     * @return the scope.
     */
    public String getScope();

}
