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

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import javax.annotation.Nonnull;

/**
 * Wrapper for use of Duo.
 * 
 * @since 3.3.0
 */
public class BasicDuoIntegration implements DuoIntegration {

    /** API host. */
    @Nonnull @NotEmpty private String apiHost;
    
    /** Application key. */
    @Nonnull @NotEmpty private String applicationKey;
    
    /** Integration key. */
    @Nonnull @NotEmpty private String integrationKey;
    
    /** Secret key. */
    @Nonnull @NotEmpty private String secretKey;
    
    /**
     * Constructor.
     * 
     * @param host API host
     * @param akey application key
     * @param ikey integration key
     * @param skey secret key
     */
    public BasicDuoIntegration(@Nonnull @NotEmpty final String host, @Nonnull @NotEmpty final String akey,
            @Nonnull @NotEmpty final String ikey, @Nonnull @NotEmpty final String skey) {
        setAPIHost(host);
        setApplicationKey(akey);
        setIntegrationKey(ikey);
        setSecretKey(skey);
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getAPIHost() {
        return apiHost;
    }
    
    /**
     * Set the API host to use.
     * 
     * @param host API host
     */
    public void setAPIHost(@Nonnull @NotEmpty final String host) {
        apiHost = Constraint.isNotNull(StringSupport.trimOrNull(host), "API host cannot be null or empty");
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getApplicationKey() {
        return applicationKey;
    }
    
    /**
     * Set the application key to use.
     * 
     * @param key application key
     */
    public void setApplicationKey(@Nonnull @NotEmpty final String key) {
        applicationKey = Constraint.isNotNull(StringSupport.trimOrNull(key), "Application key cannot be null or empty");
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getIntegrationKey() {
        return integrationKey;
    }
    
    /**
     * Set the integration key to use.
     * 
     * @param key integration key
     */
    public void setIntegrationKey(@Nonnull @NotEmpty final String key) {
        integrationKey = Constraint.isNotNull(StringSupport.trimOrNull(key), "Integration key cannot be null or empty");
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getSecretKey() {
        return secretKey;
    }
    
    /**
     * Set the secret key to use.
     * 
     * @param key secret key
     */
    public void setSecretKey(@Nonnull @NotEmpty final String key) {
        secretKey = Constraint.isNotNull(StringSupport.trimOrNull(key), "Secret key cannot be null or empty");
    }
    
}