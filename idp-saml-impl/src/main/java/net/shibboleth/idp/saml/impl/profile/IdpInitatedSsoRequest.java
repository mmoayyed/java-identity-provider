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

package net.shibboleth.idp.saml.impl.profile;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.utilities.java.support.logic.Assert;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import com.google.common.base.Objects;

/**
 * Object representing a Shibboleth Authentication Request message.
 * 
 * This message is used for IdP-initiated authentication requests and is defined by the <a
 * href="http://shibboleth.internet2.edu/docs/internet2-mace-shibboleth-arch-protocols-200509.pdf">Shibboleth
 * Architecture Protocol and Profiles</a> specification. Note, this document was written prior to the creation of SAML 2
 * and so only mentioned version 1 but this message may be used with either version. The SAML 2 authentication request
 * should be used by SAML 2 service providers wishing to initiate authentication.
 */
@ThreadSafe
public class IdpInitatedSsoRequest {

    /** The entity ID of the requesting service provider. */
    private final String entityId;

    /**
     * The assertion consumer service endpoint, at the service provider, to which to deliver the authentication
     * response.
     */
    private final String acsUrl;

    /** An opaque value to be returned to the service provider with the authentication response. */
    private final String relayState;

    /** The current time, at the service provider, in milliseconds since the epoch. */
    private final long time;

    /**
     * Constructor.
     * 
     * @param newEntityId entity ID of the requesting service provider, never null or empty
     * @param newAcsUrl assertion consumer service endpoint, at the service provider, to which to deliver the
     *            authentication response, may be null or empty
     * @param newTarget opaque value to be returned to the service provider with the authentication response, maybe null
     *            or empty
     * @param newTime Current time, at the service provider, in milliseconds since the epoch. Must be 0 or greater. 0
     *            indicates not time was given by the service provider.
     */
    public IdpInitatedSsoRequest(String newEntityId, String newAcsUrl, String newTarget, long newTime) {
        entityId =
                Assert.isNotNull(StringSupport.trimOrNull(newEntityId), "Service provider ID can not be null or empty");

        acsUrl = StringSupport.trimOrNull(newAcsUrl);

        relayState = StringSupport.trimOrNull(newTarget);

        time = Assert.isGreaterThanOrEqual(0, newTime, "Time must be greater than or equal to 0");
    }

    /**
     * Gets the entity ID of the requesting relying party.
     * 
     * @return entity ID of the requesting relying party, never null or empty
     */
    public String getEntityId() {
        return entityId;
    }

    /**
     * Gets the assertion consumer service endpoint, at the service provider, to which to deliver the authentication
     * response.
     * 
     * @return assertion consumer service endpoint, at the service provider, to which to deliver the authentication
     *         response, may be null, never empty
     */
    public String getAcsUrl() {
        return acsUrl;
    }

    /**
     * Gets the opaque value to be returned to the service provider with the authentication response.
     * 
     * @return opaque value to be returned to the service provider with the authentication response, may be null, never
     *         empty
     */
    public String getRelayState() {
        return relayState;
    }

    /**
     * Gets the current time, at the service provider, in milliseconds since the epoch.
     * 
     * @return current time in milliseconds since the epoch or 0 if no time was given by the service provider
     */
    public long getTime() {
        return time;
    }

    /** {@inheritDoc} */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(IdpInitatedSsoRequest.class.getName());
        builder.append(" [entityId=");
        builder.append(entityId);
        builder.append(", acsUrl=");
        builder.append(acsUrl);
        builder.append(", relayState=");
        builder.append(relayState);
        builder.append(", time=");
        builder.append(time);
        builder.append("]");
        return builder.toString();
    }

    /** {@inheritDoc} */
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        result = prime * result + entityId.hashCode();

        if (acsUrl != null) {
            result = prime * result + acsUrl.hashCode();
        } else {
            result = prime * result + 0;
        }

        if (relayState != null) {
            result = prime * result + relayState.hashCode();
        } else {
            result = prime * result + 0;
        }

        result = prime * result + (int) (time ^ (time >>> 32));

        return result;
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof IdpInitatedSsoRequest)) {
            return false;
        }

        IdpInitatedSsoRequest other = (IdpInitatedSsoRequest) obj;
        return Objects.equal(entityId, other.entityId) && Objects.equal(acsUrl, other.acsUrl)
                && Objects.equal(relayState, other.relayState) && time == other.time;
    }
}