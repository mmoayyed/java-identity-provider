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

import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.saml.nameid.NameDecoderException;
import net.shibboleth.idp.saml.nameid.NameIdentifierDecoder;
import net.shibboleth.shared.annotation.constraint.NotEmpty;

import org.opensaml.saml.saml1.core.NameIdentifier;

/**
 * Processes a transient {@link NameIdentifier}, checks that its {@link NameIdentifier#getNameQualifier()} is
 * correct, and decodes {@link NameIdentifier#getValue()} via the base class (reversing the work done by
 * {@link net.shibboleth.idp.saml.nameid.impl.CryptoTransientIdGenerationStrategy}).
 */
public class CryptoTransientNameIdentifierDecoder extends BaseCryptoTransientDecoder implements NameIdentifierDecoder {

    /** {@inheritDoc} */
    @Override
    @Nullable @NotEmpty public String decode(@Nonnull final SubjectCanonicalizationContext c14nContext,
            @Nonnull final NameIdentifier nameIdentifier) throws NameDecoderException {

        final String val = nameIdentifier.getValue();
        final String id = c14nContext.getRequesterId();
        assert val != null &&id != null;
        
        return super.decode(val, id);
    }

}