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

import org.opensaml.saml.common.SAMLObject;

/**
 * Interface for all canonicalizers that go from a NameID/NameIdentifier to a String.
 * 
 * @param <NameIdType> What this deals with. Usually {@link SAMLObject}, {@link org.opensaml.saml.saml2.core.NameID} or
 *            {@link org.opensaml.saml.saml1.core.NameIdentifier}
 */
public interface NameCanonicalizer<NameIdType extends SAMLObject> {

    /**
     * Is this canonicalizer appropriate?
     * 
     * @param identifier the NameID or NameIdentifier
     * @param requesterId The SP ID
     * @return whether this cna be applied
     * @throws NameCanonicalizationException if the parameters are invalid
     */
    boolean isApplicable(SAMLObject identifier, String requesterId) throws NameCanonicalizationException;

    /**
     * Canonicalization action.
     * 
     * @param identifier the NameID or NameIdentifier
     * @param requesterId the SP entity ID.
     * @param responderId the IdP entity ID
     * @return the canonicalized Name
     * @throws NameCanonicalizationException if canonicalization could not proceed.
     */
    String canonicalize(NameIdType identifier, String requesterId, String responderId) 
            throws NameCanonicalizationException;

}
