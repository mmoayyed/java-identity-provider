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

package net.shibboleth.idp.saml.nameid;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.SubjectCanonicalizationException;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

import org.opensaml.saml.saml2.core.NameID;

/** Interface for converting a {@link NameID} back into a principal name. */
public interface NameIDDecoder {

    /**
     * Decode the provided {@link NameID}, testing or otherwise using either or both of the issuer and requester.
     * 
     * <p>If relevant, the implementation is expected to test the {@link NameID#getNameQualifier()} and
     * {@link NameID#getSPNameQualifier()} against the responder and requester, as well as any stored or
     * decrypted values.</p>
     * 
     * @param nameID the object to decode
     * @param responderId the entityID that issued (and is looking at and responding to) the value
     * @param requesterId the entityID that provided the name and is asking for information based on it
     * 
     * @return the principal decoded from the value
     * @throws SubjectCanonicalizationException if match conditions failed
     * @throws NameDecoderException if an error occurred during translation
     */
    @Nonnull @NotEmpty String decode(@Nonnull final NameID nameID, @Nullable final String responderId,
            @Nullable final String requesterId) throws SubjectCanonicalizationException, NameDecoderException;

}