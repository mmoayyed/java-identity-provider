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

import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.shared.logic.Constraint;

/**
 * {@link PrincipalService} for most principal types that just exposes the proper {@link PrincipalSerializer}.
 * 
 * <p>Mainly provided in the event that the service API gets more complex.</p>
 * 
 * @param <T> type of principal
 * 
 * @since 4.1.0
 */
public class GenericPrincipalService<T extends Principal> extends AbstractIdentifiableInitializableComponent
        implements PrincipalService<T> {
    
    /** Type of principal. */
    @Nonnull private final Class<T> principalType;
    
    /** Generic principal serializer. */
    @Nonnull private final PrincipalSerializer<String> principalSerializer;

    /** 
     * Constructor.
     *  
     * @param claz the principal type
     * @param serializer the principal serializer to use
     */
    public GenericPrincipalService(@Nonnull @ParameterName(name="claz") final Class<T> claz,
            @Nonnull @ParameterName(name="serializer") final PrincipalSerializer<String> serializer) {
        principalType = Constraint.isNotNull(claz, "Type cannot be null");
        principalSerializer = Constraint.isNotNull(serializer, "PrincipalSerializer cannot be null");
    }
    
    /** {@inheritDoc} */
    @Nonnull public Class<T> getType() {
        return principalType;
    }

    /** {@inheritDoc} */
    @Nonnull public PrincipalSerializer<String> getSerializer() {
        return principalSerializer;
    }

}