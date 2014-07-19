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

package net.shibboleth.idp.security;

import javax.annotation.Nonnull;

import net.shibboleth.idp.service.ReloadableService;
import net.shibboleth.idp.service.ServiceableComponent;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.security.AccessControl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class uses the service interface to implement {@link AccessControlService}.
 */
public class AccessControlService extends AbstractIdentifiableInitializableComponent
    implements net.shibboleth.utilities.java.support.security.AccessControlService {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AccessControlService.class);

    /** The service which manages the reloading. */
    private final ReloadableService<net.shibboleth.utilities.java.support.security.AccessControlService> service;

    /**
     * Constructor.
     * 
     * @param acService the service which will manage the loading.
     */
    public AccessControlService(@Nonnull
            final ReloadableService<net.shibboleth.utilities.java.support.security.AccessControlService> acService) {
        service = Constraint.isNotNull(acService, "AccessControlService cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    public AccessControl getInstance(@Nonnull final String name) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ServiceableComponent<net.shibboleth.utilities.java.support.security.AccessControlService> component = null;
        try {
            component = service.getServiceableComponent();
            if (null == component) {
                log.error("AccessControlService '{}': Error accessing underlying component: Invalid configuration.",
                        getId());
            } else {
                final net.shibboleth.utilities.java.support.security.AccessControlService svc =
                        component.getComponent();
                return svc.getInstance(name);
            }
        } finally {
            if (null != component) {
                component.unpinComponent();
            }
        }
        return null;
    }
    
}