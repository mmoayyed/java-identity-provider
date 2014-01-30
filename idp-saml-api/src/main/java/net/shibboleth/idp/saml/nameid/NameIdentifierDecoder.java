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

/**
 * Interface for converting back from value part of a {@link org.opensaml.saml.saml2.core.NameID} or a
 * {@link org.opensaml.saml.saml1.core.NameIdentifier}. Suitable for plugging into both styles of NameIdentifier
 * decoder.
 *
 */
public interface NameIdentifierDecoder {

    /**
     * Decode the provided value, testing or otherwise using either or both of the issuer and requester.
     *
     * @param value the value as extracted from the {@link org.opensaml.saml.saml2.core.NameID} or
     *            {@link org.opensaml.saml.saml1.core.NameIdentifier}
     * @param responderId the entityID of the which issued (and is looking at and responding to) the value
     * @param requesterId the entityID of the which is providing the and which asking for information based on it.
     * @return the principal decoded from the value
     * @throws SubjectCanonicalizationException if match conditions failed.
     * @throws NameDecoderException if an error occurred during translation.
     */
    @Nonnull public String decode(@Nonnull String value, @Nullable String responderId, @Nullable String requesterId)
            throws SubjectCanonicalizationException, NameDecoderException;

}