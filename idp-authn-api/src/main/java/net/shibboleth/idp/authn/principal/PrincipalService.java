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

package net.shibboleth.idp.authn.principal;

import java.security.Principal;

import javax.annotation.Nonnull;

import net.shibboleth.shared.component.IdentifiedComponent;

/**
 * Interface that provides services for a {@link Principal} of a given type.
 * 
 * @param <T> principal type
 * 
 * @since 4.1.0
 */
public interface PrincipalService<T extends Principal> extends IdentifiedComponent {

    /**
     * Get the type of object supported.
     * 
     * @return supported type
     */
    @Nonnull Class<T> getType();
    
    /**
     * Get a serializer instance for this type of {@link Principal}.
     * 
     * @return the serializer
     */
    @Nonnull PrincipalSerializer<String> getSerializer();
}