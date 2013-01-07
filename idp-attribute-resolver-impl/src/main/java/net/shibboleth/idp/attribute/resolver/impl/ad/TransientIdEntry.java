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

package net.shibboleth.idp.attribute.resolver.impl.ad;

import org.joda.time.DateTime;
import org.opensaml.util.storage.AbstractExpiringObject;

/** Storage service entry used to store information associated with a transient ID. */
//
// TODO - this class needs to be changed in response to the storage service work.
// It appears that 

public class TransientIdEntry extends AbstractExpiringObject {
    /** Serial version UID. */
    private static final long serialVersionUID = 3594553144206354129L;

    /** Relying party the token was issued to. */
    private String relyingPartyId;

    /** Principal for which the token was issued. */
    private String principalName;

    /** Transient id. */
    private String id;

    /**
     * Constructor.
     * 
     * @param lifetime lifetime of the token in milliseconds
     * @param relyingParty relying party the token was issued to
     * @param principal principal the token was issued for
     * @param randomId the random ID token
     */
    public TransientIdEntry(long lifetime, String relyingParty, String principal, String randomId) {
        super(new DateTime().plus(lifetime));
        relyingPartyId = relyingParty;
        principalName = principal;
        id = randomId;
    }

    /**
     * Gets the principal the token was issued for.
     * 
     * @return principal the token was issued for
     */
    public String getPrincipalName() {
        return principalName;
    }

    /**
     * Gets the relying party the token was issued to.
     * 
     * @return relying party the token was issued to
     */
    public String getRelyingPartyId() {
        return relyingPartyId;
    }

    /**
     * Gets the ID.
     * 
     * @return ID
     */
    public String getId() {
        return id;
    }
}
