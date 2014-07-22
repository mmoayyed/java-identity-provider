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

package net.shibboleth.idp.profile.impl;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.metadata.resolver.RefreshableMetadataResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Action that refreshes the metadata associated with the supplied service. */
public class ReloadMetadata extends AbstractProfileAction {

    /** Log. */
    private final Logger log = LoggerFactory.getLogger(ReloadMetadata.class);

    /** The service that describes the metadata. */
    private ReloadableService<RefreshableMetadataResolver> metadataResolverService;

    /**
     * Get the service that describes the metadata.
     * 
     * @return service.
     */
    public ReloadableService<RefreshableMetadataResolver> getMetadataResolverService() {
        return metadataResolverService;
    }

    /**
     * Set the service that describes the metadata.
     * 
     * @param service what to set.
     */
    public void setMetadataResolver(ReloadableService<RefreshableMetadataResolver> service) {
        metadataResolverService = service;
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(ProfileRequestContext profileRequestContext) {
        ServiceableComponent<RefreshableMetadataResolver> component = metadataResolverService.getServiceableComponent();
        String id = "unpsecified";
        try {
            RefreshableMetadataResolver resolver = component.getComponent();
            if (resolver instanceof RefreshableMetadataResolver) {
                // Check the class - there is no certainty that we were given the correct type
                id = resolver.getId();
                resolver.refresh();
            } else {
                log.error("Injected Service resolved to class {} which does not support refresh", resolver.getClass()
                        .getName());
            }
        } catch (final ResolverException e) {
            log.error("RelyingPartyMetadataProvider '{}': Error during refresh", id, e);
        } finally {
            if (null != component) {
                component.unpinComponent();
            }
        }
        super.doExecute(profileRequestContext);
    }

}
