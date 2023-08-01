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

package net.shibboleth.idp.saml.nameid.impl;

import javax.annotation.Nonnull;

import org.opensaml.saml.common.SAMLException;

import net.shibboleth.shared.annotation.constraint.NotEmpty;

/**
 * Generates and manages transient identifiers according to specific strategies.
 */
public interface TransientIdGenerationStrategy {

    /**
     * Generate a new Transient ID.
     * 
     * @param relyingPartyId    the relying party for whom we're generating
     * @param principalName     the principal to map to
     * 
     * @return  the new identifier
     * @throws SAMLException if an error occurs generating the identifier
     */
    @Nonnull @NotEmpty String generate(@Nonnull @NotEmpty final String relyingPartyId,
            @Nonnull @NotEmpty final String principalName) throws SAMLException;

}