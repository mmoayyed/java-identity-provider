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

package net.shibboleth.idp.cas.service.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import net.shibboleth.idp.cas.service.Service;
import net.shibboleth.idp.cas.service.ServiceRegistry;
import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.shared.service.ServiceException;
import net.shibboleth.shared.service.ServiceableComponent;

/**
 * Service registry wrapper around a {@link net.shibboleth.shared.service.ReloadableService}.
 *
 * @author Marvin S. Addison
 */
public class ReloadingServiceRegistry extends AbstractIdentifiableInitializableComponent implements ServiceRegistry {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ReloadingServiceRegistry.class);

    /** The service that manages the reloading. */
    private final ReloadableService<ServiceRegistry> service;

    /**
     * Creates a new instance.
     *
     * @param delegate The service to which operations are delegated.
     */
    public ReloadingServiceRegistry(
            @Nonnull @ParameterName(name="delegate") final ReloadableService<ServiceRegistry> delegate) {
        service = Constraint.isNotNull(delegate, "ReloadableService cannot be null");
    }

    @Override
    @Nullable public Service lookup(@Nonnull final String serviceURL) {
        try (final ServiceableComponent<ServiceRegistry> component = service.getServiceableComponent()) {
            return component.getComponent().lookup(serviceURL);
        } catch (final ServiceException e) {
            log.error("ServiceRegistry '{}': Invalid CAS service registry configuration.", getId(), e);
            return null;
        }
    }

}