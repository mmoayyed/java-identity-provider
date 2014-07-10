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

package net.shibboleth.idp.service;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.logic.Constraint;

import org.springframework.context.ApplicationContext;

import com.google.common.base.Function;

/**
 * Strategy to create {@link ServiceableComponent}s from the {@link ApplicationContext}.
 * 
 * @param <T> the service type to look for; defaults to {@link ServiceableComponent}
 */
public class ClassBasedServiceStrategy<T> implements Function<ApplicationContext, ServiceableComponent<T>> {

    /** The class we are looking for. */
    @Nonnull private final Class<? extends ServiceableComponent> serviceClaz;

    /**
     * Constructor.
     */
    public ClassBasedServiceStrategy() {
        this(ServiceableComponent.class);
    }

    /**
     * Constructor.
     * 
     * @param serviceableClaz what to look for.
     */
    public ClassBasedServiceStrategy(final Class<? extends ServiceableComponent> serviceableClaz) {
        serviceClaz = Constraint.isNotNull(serviceableClaz, "Serviceable Class cannot be null");
    }

    /** {@inheritDoc} */
    @Override @Nullable public ServiceableComponent<T> apply(@Nullable ApplicationContext appContext) {
        final Collection<? extends ServiceableComponent> components = appContext.getBeansOfType(serviceClaz).values();

        if (components.size() == 0) {
            throw new ServiceException("Reload did not produce any bean of type" + serviceClaz.getName());
        }
        if (components.size() > 1) {
            throw new ServiceException("Reload produced " + components.size() + " ServiceableComponents");
        }

        return components.iterator().next();
    }
}