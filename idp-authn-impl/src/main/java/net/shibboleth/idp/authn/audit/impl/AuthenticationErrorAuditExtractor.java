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

package net.shibboleth.idp.authn.audit.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.AuthenticationErrorContext;

/**
 * {@link Function} that returns any classified errors found in a subordinate {@link AuthenticationErrorContext},
 * or "Success" if none.
 * 
 * @since 4.3.0
 */
public class AuthenticationErrorAuditExtractor implements Function<ProfileRequestContext,Collection<String>> {

    /** {@inheritDoc} */
    @Nullable public Collection<String> apply(@Nullable final ProfileRequestContext input) {

        final AuthenticationContext authnCtx = input.getSubcontext(AuthenticationContext.class);
        if (authnCtx != null) {
            final AuthenticationErrorContext errorCtx = authnCtx.getSubcontext(AuthenticationErrorContext.class);
            if (errorCtx != null) {
                return errorCtx.getClassifiedErrors();
            }
            
            return Collections.singletonList("Success");
        }
        
        return null;
    }
    
}