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

package net.shibboleth.idp.relyingparty.impl;

import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialResolver;
import org.slf4j.Logger;

import net.shibboleth.idp.relyingparty.RelyingPartyConfigurationResolver;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.IdentifiableComponent;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;
import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.ResolverException;
import net.shibboleth.shared.service.ServiceException;
import net.shibboleth.shared.service.ServiceableComponent;
import net.shibboleth.shared.spring.service.ReloadableSpringService;

/**
 * Credential resolver whose purpose is to resolve configured IdP encryption credentials.
 */
public class EncryptionCredentialsResolver implements CredentialResolver, IdentifiableComponent {
    
    /** Logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(EncryptionCredentialsResolver.class);
    
    /** The reloading resolver which is the source of the credentials. */
    @Nonnull private ReloadableSpringService<RelyingPartyConfigurationResolver> service;
    
    /** Component ID. */
    @Nullable private String id;
    
    /**
     * Constructor.
     * 
     * @param resolverService the Spring service exposing the relying party configuration service
     */
    public EncryptionCredentialsResolver(
            @Nonnull final ReloadableSpringService<RelyingPartyConfigurationResolver> resolverService) {
        service = Constraint.isNotNull(resolverService, 
                "ReloadableSpringService for RelyingPartyConfigurationResolver cannot be null");
    }
    
    /** {@inheritDoc} */
    @Nullable public String getId() {
        return id;
    }

    /** {@inheritDoc} */
    public void setId(@Nonnull @NotEmpty final String componentId) {
        id = Constraint.isNotNull(StringSupport.trimOrNull(componentId), "Component ID can not be null or empty");
    }
    
    /** {@inheritDoc} */
    @Nullable public Credential resolveSingle(@Nullable final CriteriaSet criteriaSet) throws ResolverException {
        final Iterable<Credential> creds = resolve(criteriaSet);
        if (creds.iterator().hasNext()) {
            return creds.iterator().next();
        }
        return null;
    }
    
    /** {@inheritDoc} */
    @Nonnull public Iterable<Credential> resolve(@Nullable final CriteriaSet criteria) 
            throws ResolverException {
        try(final ServiceableComponent<RelyingPartyConfigurationResolver> component = service.getServiceableComponent()) {
            final RelyingPartyConfigurationResolver resolver = component.getComponent();
            if (resolver instanceof DefaultRelyingPartyConfigurationResolver) {
                log.trace("Saw expected instance of DefaultRelyingPartyConfigurationResolver");
                return ((DefaultRelyingPartyConfigurationResolver)resolver).getEncryptionCredentials();
            }
            log.trace("Did NOT see expected instance of DefaultRelyingPartyConfigurationResolver");
            return Collections.emptyList();
        } catch (final ServiceException e) {
            log.error("EncryptionCredentialsResolver '{}': Invalid RelyingPartyResolver configuration", getId(), e);
        }
        
        return Collections.emptyList();
    }

}