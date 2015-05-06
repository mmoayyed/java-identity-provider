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

package net.shibboleth.idp.cas.service;

import net.shibboleth.ext.spring.service.AbstractServiceableComponent;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.IdentifiableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Service registry that evaluates a candidate service URL against one or more defined services, where each
 * definition contains a service URL regular expression pattern.
 *
 * @author Marvin S. Addison
 */
public class PatternServiceRegistry extends AbstractServiceableComponent<ServiceRegistry>
        implements IdentifiableComponent, ServiceRegistry {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(PatternServiceRegistry.class);

    /** List of service definitions that back registry. */
    @Nonnull
    @NonnullElements
    private List<ServiceDefinition> definitions = Collections.emptyList();

    @Override
    public void setId(@Nonnull final String componentId) {
        super.setId(componentId);
    }

    /**
     * Sets the list of service definitions that back the registry.
     * @param definitions List of service definitions, each of which defines a match pattern to evaluate a candidate
     *                    service URL.
     */
    public void setDefinitions(@Nonnull @NonnullElements List<ServiceDefinition> definitions) {
        this.definitions = Constraint.isNotNull(definitions, "Service definition list cannot be null");
    }

    @Nonnull
    @Override
    public ServiceRegistry getComponent() {
        return this;
    }

    @Override
    @Nullable
    public Service lookup(@Nonnull String serviceURL) {
        Constraint.isNotNull(serviceURL, "Service URL cannot be null");
        for (ServiceDefinition def : definitions) {
            log.debug("Evaluating whether {} matches {}", serviceURL, def);
            if (def.matches(serviceURL)) {
                log.debug("Found match");
                return new Service(serviceURL, def.getGroup(), def.isAuthorizedToProxy());
            }
        }
        return null;
    }
}
