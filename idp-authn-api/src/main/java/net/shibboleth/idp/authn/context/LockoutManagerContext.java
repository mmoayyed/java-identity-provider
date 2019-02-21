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

package net.shibboleth.idp.authn.context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.BaseContext;

/**
 * A context that holds information about a management operation on an
 * {@link net.shibboleth.idp.authn.AccountLockoutManager}.
 * 
 * @parent {@link org.opensaml.profile.context.ProfileRequestContext}
 * @added After the initiation of an administrative operation against a lockout manager.
 * 
 * @since 3.4.0
 */
public final class LockoutManagerContext extends BaseContext {

    /** Account lockout key. */
    @Nullable private String key;

    /**
     * Get the account lockout key to check or modify.
     * 
     * @return account lockout key
     */
    @Nullable public String getKey() {
        return key;
    }

    /**
     * Set the account lockout key to check or modify.
     * 
     * @param k account lockout key
     * 
     * @return this context
     */
    @Nonnull public LockoutManagerContext setKey(@Nullable final String k) {
        key = k;
        
        return this;
    }
    
}