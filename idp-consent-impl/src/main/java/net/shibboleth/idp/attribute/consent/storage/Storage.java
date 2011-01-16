/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.attribute.consent.storage;

import java.util.Collection;

import net.shibboleth.idp.attribute.consent.AttributeRelease;
import net.shibboleth.idp.attribute.consent.User;


/**
 *
 */
public interface Storage {

    /**
     * @param userId
     * @return
     */
    boolean containsUser(final String userId);

    /**
     * @param userId
     * @return
     */
    User readUser(final String userId);
    
    /**
     * @param user
     */
    void updateUser(User user);

    /**
     * @param user
     */
    void createUser(User user);

    /**
     * @param userId
     * @param relyingPartyId
     * @return
     */
    Collection<AttributeRelease> readAttributeReleases(final String userId, final String relyingPartyId);

    /**
     * @param userId
     * @param relyingPartyId
     */
    void deleteAttributeReleases(final String userId, final String relyingPartyId);

    /**
     * @param userId
     * @param relyingPartyId
     * @param attributeId
     * @return
     */
    boolean containsAttributeRelease(String userId, String relyingPartyId, String attributeId);

    /**
     * @param userId
     * @param relyingPartyId
     * @param attributeRelease
     */
    void updateAttributeRelease(String userId, String relyingPartyId, AttributeRelease attributeRelease);

    /**
     * @param userId
     * @param relyingPartyId
     * @param attributeRelease
     */
    void createAttributeRelease(String userId, String relyingPartyId, AttributeRelease attributeRelease);
    
}