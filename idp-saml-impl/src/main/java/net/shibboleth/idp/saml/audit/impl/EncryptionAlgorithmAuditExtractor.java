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

package net.shibboleth.idp.saml.audit.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.profile.context.EncryptionContext;

import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.shared.logic.Constraint;

/** {@link Function} that returns the data encryption algorithm used. */
public class EncryptionAlgorithmAuditExtractor implements Function<ProfileRequestContext,String> {

    /** Lookup strategy for {@link EncryptionContext}. */
    @Nonnull private Function<ProfileRequestContext,EncryptionContext> encryptionContextLookupStrategy;
    
    /** Constructor. */
    public EncryptionAlgorithmAuditExtractor() {
        encryptionContextLookupStrategy =
                new ChildContextLookup<>(EncryptionContext.class).compose(
                        new ChildContextLookup<>(RelyingPartyContext.class));
    }

    /**
     * Sets the lookup strategy for the {@link EncryptionContext}.
     *
     * @param strategy lookup strategy for context
     */
    public void setEncryptionContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,EncryptionContext> strategy) {
        encryptionContextLookupStrategy =
                Constraint.isNotNull(strategy, "EncryptionContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Nullable public String apply(@Nullable final ProfileRequestContext input) {
        
        final EncryptionContext encryptionCtx = encryptionContextLookupStrategy.apply(input);
        if (encryptionCtx != null) {
            if (encryptionCtx.getAssertionEncryptionParameters() != null) {
                return encryptionCtx.getAssertionEncryptionParameters().getDataEncryptionAlgorithm();
            } else if (encryptionCtx.getAttributeEncryptionParameters() != null) {
                return encryptionCtx.getAttributeEncryptionParameters().getDataEncryptionAlgorithm();
            } else if (encryptionCtx.getIdentifierEncryptionParameters() != null) {
                return encryptionCtx.getIdentifierEncryptionParameters().getDataEncryptionAlgorithm();
            }
        }
        return null;
    }

}