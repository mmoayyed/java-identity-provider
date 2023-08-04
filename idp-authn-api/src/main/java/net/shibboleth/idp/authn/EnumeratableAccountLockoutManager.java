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
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.authn.context.LockoutManagerContext;

/**
 * An extension to {@link AccountLockoutManager} that allows for enumeration over
 * partial matches of a key.
 * 
 * @since 5.0.0
 */
public interface EnumeratableAccountLockoutManager extends AccountLockoutManager {
    
    /**
     * Return iterable collection of locked out keys that match a supplied partial key (i.e., are prefixed by it).
     * 
     * <p>The key MUST be supplied via a {@link LockoutManagerContext} subcontext of the input context.</p>
     * 
     * @param profileRequestContext current profile request context 
     * 
     * @return the locked out keys, or a null if an error occurs
     */
    @Nullable Iterable<String> enumerate(@Nonnull final ProfileRequestContext profileRequestContext);

}