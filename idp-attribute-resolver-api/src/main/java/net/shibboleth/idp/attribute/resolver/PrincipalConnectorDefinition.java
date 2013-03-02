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

package net.shibboleth.idp.attribute.resolver;

import javax.annotation.Nonnull;

import org.opensaml.messaging.context.BaseContext;

import com.google.common.base.Optional;

/**
 * Definition of a Principal Connector. <b/> This is the component which takes a context and produces the unique
 * principal. Examples are
 * <ul>
 * <li><em>BaseSubjectNamePrincipalConnectors</em> which sit at the beginning of (for instance) the AA flow and collect
 * the principal from the incoming SAML message</li>
 * <li><em>Canonicalising</em>Connectors which sit immediately after authentication.</li>
 * </ul>
 * 
 * @param <ConsumedContext> The type of context which is expected.
 */
public interface PrincipalConnectorDefinition<ConsumedContext extends BaseContext> {

    /**
     * Resolve the principal with respect to the provided context.
     * 
     * @param context what to look at.
     * @return the IdP principal, or {@link Optional#absent()} if this definition wasn't applicable
     * @throws ResolutionException if we encountered a fatal processing error.
     */
    public Optional<String> resolve(@Nonnull final ConsumedContext context) throws ResolutionException;

}
