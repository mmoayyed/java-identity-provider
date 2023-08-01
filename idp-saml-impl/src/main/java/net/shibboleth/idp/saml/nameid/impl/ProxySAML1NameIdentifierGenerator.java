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
import javax.annotation.Nullable;

import net.shibboleth.idp.saml.nameid.NameIdentifierGenerationService;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.shared.service.ServiceException;
import net.shibboleth.shared.service.ServiceableComponent;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLException;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.opensaml.saml.saml1.profile.SAML1NameIdentifierGenerator;

/**
 * A compound implementation of the {@link SAML1NameIdentifierGenerator} interface that wraps a sequence of
 * candidate generators along with a default to try if no format-specific options are available.
 */
public class ProxySAML1NameIdentifierGenerator implements SAML1NameIdentifierGenerator {

    /** Service used to get the generator to proxy. */
    @Nonnull private final ReloadableService<NameIdentifierGenerationService> generatorService;
    
    /**
     * Constructor.
     *
     * @param service the service providing the generators to proxy
     */
    public ProxySAML1NameIdentifierGenerator(
            @Nonnull final ReloadableService<NameIdentifierGenerationService> service) {
        generatorService = Constraint.isNotNull(service, "NameIdentifierGenerationService cannot be null");
    }

    /** {@inheritDoc} */
    @Nullable public NameIdentifier generate(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull @NotEmpty final String format) throws SAMLException {
        
        try (final ServiceableComponent<NameIdentifierGenerationService> component
                = generatorService.getServiceableComponent()) {
            return component.getComponent().getSAML1NameIdentifierGenerator().generate(profileRequestContext, format);
        } catch (final ServiceException e) {
            throw new SAMLException("Invalid NameIdentifierGenerationService configuration", e);
        }
    }

}