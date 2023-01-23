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

import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.AuthenticationContext;

import org.opensaml.messaging.context.navigate.ContextDataLookupFunction;

/**
 * A function that returns the value of {@link net.shibboleth.idp.authn.AuthenticationResult#isPreviousResult()}
 * or null if the input context is null or {@link AuthenticationContext#getAuthenticationResult()} is null.
 */
public class PreviousResultLookupFunction implements ContextDataLookupFunction<AuthenticationContext,Boolean> {

    /** {@inheritDoc} */
    @Nullable public Boolean apply(@Nullable final AuthenticationContext input) {
        if (input != null) {
            final AuthenticationResult result = input.getAuthenticationResult();
            if (result != null) {
                return result.isPreviousResult();
            }
        }
        return null;
    }

}