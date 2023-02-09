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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Service registry that evaluates a candidate service URL against one or more defined services, where each
 * definition contains a service URL regular expression pattern.
 *
 * <p>NOTE: This class will become an implementation component in the next major software version.</p>
 *
 * @author Marvin S. Addison
 */
public class PatternServiceRegistry extends AbstractIdentifiableInitializableComponent
        implements ServiceRegistry {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(PatternServiceRegistry.class);

    /** Map of service definitions to compiled patterns. */
    @Nonnull
    @NonnullElements
    private Map<ServiceDefinition, Pattern> definitions = CollectionSupport.emptyMap();

    /**
     * Sets the list of service definitions that back the registry.
     * 
     * @param serviceDefinitions List of service definitions, each of which defines a match pattern to evaluate a
     *            candidate service URL.
     */
    public void setDefinitions(@Nonnull @NonnullElements final List<ServiceDefinition> serviceDefinitions) {
        Constraint.noNullItems(serviceDefinitions, "Definitions cannot be null or contain null items");
        // Preserve order of services in map
        definitions = new LinkedHashMap<>(serviceDefinitions.size());
        for (final ServiceDefinition definition : serviceDefinitions) {
            definitions.put(definition, Pattern.compile(definition.getId()));
        }
    }

    @Override
    @Nullable
    public Service lookup(@Nonnull final String serviceURL) {
        Constraint.isNotNull(serviceURL, "Service URL cannot be null");
        for (final ServiceDefinition def : definitions.keySet()) {
            log.debug("Evaluating whether {} matches {}", serviceURL, def);
            if (definitions.get(def).matcher(serviceURL).matches()) {
                log.debug("Found match");
                return new Service(serviceURL, def.getGroup(), def.isAuthorizedToProxy(),
                        def.isSingleLogoutParticipant());
            }
        }
        return null;
    }
}
