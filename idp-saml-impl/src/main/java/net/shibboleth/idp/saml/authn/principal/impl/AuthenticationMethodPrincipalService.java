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

package net.shibboleth.idp.saml.authn.principal.impl;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.principal.PrincipalService;
import net.shibboleth.idp.saml.authn.principal.AuthenticationMethodPrincipal;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;

/**
 * {@link PrincipalService} for {@link AuthenticationMethodPrincipal}.
 * 
 * @since 4.1.0
 */
public class AuthenticationMethodPrincipalService extends AbstractIdentifiableInitializableComponent
        implements PrincipalService<AuthenticationMethodPrincipal> {

    /** {@inheritDoc} */
    @Nonnull public Class<AuthenticationMethodPrincipal> getType() {
        return AuthenticationMethodPrincipal.class;
    }
    
    /** {@inheritDoc} */
    public AuthenticationMethodPrincipal newInstance(@Nonnull @NotEmpty final String name) {
        return new AuthenticationMethodPrincipal(name);
    }

}