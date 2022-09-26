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

import java.security.Principal;
import java.util.Collection;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.component.AbstractInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Wrapper for use of Duo.
 * 
 * @since 3.3.0
 */
public class BasicDuoIntegration extends AbstractInitializableComponent implements DuoIntegration {

    /** API host. */
    @NonnullAfterInit @NotEmpty private String apiHost;
    
    /** Application key. */
    @Nullable @NotEmpty private String applicationKey;
    
    /** Integration key. */
    @NonnullAfterInit @NotEmpty private String integrationKey;
    
    /** Secret key. */
    @NonnullAfterInit @NotEmpty private String secretKey;
    
    /** Container for supported principals. */
    @Nonnull private final Subject supportedPrincipals;
    
    /** Constructor. */
    public BasicDuoIntegration() {
        supportedPrincipals = new Subject();
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
        checkSetterPreconditions();
        apiHost = Constraint.isNotNull(StringSupport.trimOrNull(host), "API host cannot be null or empty");
    }

    /** {@inheritDoc} */
    @Nullable @NotEmpty public String getApplicationKey() {
        return applicationKey;
    }
    
    /**
     * Set the application key to use.
     * 
     * @param key application key
     */
    public void setApplicationKey(@Nullable @NotEmpty final String key) {
        checkSetterPreconditions();
        applicationKey = StringSupport.trimOrNull(key);
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
        checkSetterPreconditions();
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
        checkSetterPreconditions();
        secretKey = Constraint.isNotNull(StringSupport.trimOrNull(key), "Secret key cannot be null or empty");
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @Unmodifiable
    public <T extends Principal> Set<T> getSupportedPrincipals(@Nonnull final Class<T> c) {
        return supportedPrincipals.getPrincipals(c);
    }
    
    /**
     * Set supported non-user-specific principals that the action will include in the subjects
     * it generates, in place of any default principals from the flow.
     * 
     * <p>Setting to a null or empty collection will maintain the default behavior of relying on the flow.</p>
     * 
     * @param <T> a type of principal to add, if not generic
     * @param principals supported principals to include
     */
    public <T extends Principal> void setSupportedPrincipals(
            @Nullable @NonnullElements final Collection<T> principals) {
        checkSetterPreconditions();

        supportedPrincipals.getPrincipals().clear();
        
        if (principals != null && !principals.isEmpty()) {
            supportedPrincipals.getPrincipals().addAll(Set.copyOf(principals));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        if (apiHost == null || integrationKey == null || secretKey == null) {
            throw new ComponentInitializationException("API host and integration keys must be set");
        }
    }
    
}