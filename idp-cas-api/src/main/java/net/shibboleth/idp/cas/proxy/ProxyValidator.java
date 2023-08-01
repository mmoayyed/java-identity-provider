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

package net.shibboleth.idp.cas.proxy;

import java.net.URI;
import java.security.GeneralSecurityException;
import javax.annotation.Nonnull;

import org.opensaml.profile.context.ProfileRequestContext;

/**
 * Strategy pattern component for proxy callback endpoint validation.
 *
 * @author Marvin S. Addison
 */
public interface ProxyValidator {
    
    /**
     * Validates the proxy callback endpoint.
     *
     * @param profileRequestContext Profile request context.
     * @param proxyCallbackUri Proxy callback URI to validate.
     *
     * @throws GeneralSecurityException On validation failure.
     */
    void validate(@Nonnull ProfileRequestContext profileRequestContext, @Nonnull URI proxyCallbackUri)
            throws GeneralSecurityException;

}