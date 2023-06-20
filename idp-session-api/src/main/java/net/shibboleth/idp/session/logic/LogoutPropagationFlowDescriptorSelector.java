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

package net.shibboleth.idp.session.logic;

import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.session.LogoutPropagationFlowDescriptor;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;

/**
 * Selection function to retrieve the logout propagation flow descriptor that is suitable for a given {@link SPSession}.
 */
public class LogoutPropagationFlowDescriptorSelector implements Function<SPSession,LogoutPropagationFlowDescriptor> {

    /** List of available flows. */
    @Nonnull private final List<LogoutPropagationFlowDescriptor> availableFlows;

    /**
     * Constructor.
     *
     * @param flows the logout propagation flows to select from
     */
    public LogoutPropagationFlowDescriptorSelector(
            @Nonnull @ParameterName(name="flows") final List<LogoutPropagationFlowDescriptor> flows) {
        availableFlows = CollectionSupport.copyToList(Constraint.isNotNull(flows, "Flows cannot be null"));
    }

    /** {@inheritDoc} */
    @Nullable public LogoutPropagationFlowDescriptor apply(@Nullable final SPSession input) {

        if (input == null) {
            return null;
        }
        
        for (final LogoutPropagationFlowDescriptor flowDescriptor : availableFlows) {
            if (flowDescriptor.isSupported(input)) {
                return flowDescriptor;
            }
        }
        return null;
    }
    
}