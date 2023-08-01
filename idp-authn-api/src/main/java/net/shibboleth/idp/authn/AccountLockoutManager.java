/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.opensaml.profile.context.ProfileRequestContext;

/**
 * A component that manages lockout state for accounts.
 */
public interface AccountLockoutManager {
    
    /**
     * Check if the authentication credentials associated with the request are subject to lockout.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return true iff the credentials correspond to a locked account
     */
    boolean check(@Nonnull final ProfileRequestContext profileRequestContext);

    /**
     * Increment the lockout counter for the authentication credentials associated with the request.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return true iff the counter was incremented
     */
    boolean increment(@Nonnull final ProfileRequestContext profileRequestContext);
    
    /**
     * Clear the lockout state for the authentication credentials associated with the request.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return true iff the state was successfully cleared
     */
    boolean clear(@Nonnull final ProfileRequestContext profileRequestContext);

}