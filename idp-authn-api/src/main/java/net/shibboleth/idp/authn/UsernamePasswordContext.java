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

package net.shibboleth.idp.authn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.BaseContext;

/**
 * Context, usually attached to {@link AuthenticationRequestContext}, that carries a username/password pair to be
 * validated.
 */
public class UsernamePasswordContext extends BaseContext {

    /** The username. */
    private String username;

    /** The password associated with the username. */
    private String password;

    /**
     * Gets the username.
     * 
     * @return the username
     */
    @Nullable public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     * 
     * @param name the username
     * 
     * @return this context
     */
    public UsernamePasswordContext setUsername(@Nonnull final String name) {
        username = Constraint.isNotNull(name, "Username can not be null");
        return this;
    }

    /**
     * Gets the password associated with the username.
     * 
     * @return password associated with the username
     */
    @Nullable public String getPassword() {
        return password;
    }

    /**
     * Sets the password associated with the username.
     * 
     * @param pass password associated with the username
     * 
     * @return this context
     */
    public UsernamePasswordContext setPassword(@Nonnull final String pass) {
        password = Constraint.isNotNull(pass, "Password can not be null");
        return this;
    }
}