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

package net.shibboleth.idp.authn.context.navigate;

import java.security.Principal;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;

import org.opensaml.messaging.context.navigate.ContextDataLookupFunction;

/**
 * A function that returns {@link RequestedPrincipalContext#getRequestedPrincipals()} but
 * transforms the values into strings.
 */
public class RequestedPrincipalContextPrincipalLookupFunction
        implements ContextDataLookupFunction<RequestedPrincipalContext,Collection<String>> {

    /** {@inheritDoc} */
    @Nullable @NotLive @Unmodifiable public Collection<String> apply(@Nullable final RequestedPrincipalContext input) {
        
        if (input != null) {
            return input.getRequestedPrincipals()
                    .stream()
                    .map(Principal::getName)
                    .collect(Collectors.toUnmodifiableList());
        }
        return null;
    }

}