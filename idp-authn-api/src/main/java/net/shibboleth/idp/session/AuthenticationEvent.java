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

package net.shibboleth.idp.session;

import java.security.Principal;

import org.opensaml.messaging.context.AbstractSubcontextContainer;
import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;

//TODO implement hashCode/equals - need to implement this for AbstractSubcontextContainer as well

/**
 * Describes an authentication event that took place within the scope of an {@link IdPSession}.
 * 
 * Properties of this object <strong>must not</strong> be modifiable directly. Instead, use the modification methods
 * available via the {@link SessionStore} that created the associate {@link IdPSession}.
 */
public class AuthenticationEvent extends AbstractSubcontextContainer {

    /** Service for which the principal was authenticated. */
    private String serviceId;
    
    /** The principal established by the authentication event. */
    private Principal principal;

    /** The identifier of the method used to authenticate the principal. */
    private String authnMethod;

    /** The time, in milliseconds since the epoch, that the authentication completed. */
    private long autnInstant;

    /**
     * Time, in milliseconds since the epoch, when this authentication method expires. A value of 0 or less indicates
     * the authentication method does not have an absolute expiration instant.
     */
    private long expirationInstant;

    /**
     * Gets the identifier of the service for which the principal was authenticated.
     * 
     * @return identifier of the service for which the principal was authenticated
     */
    public String getServiceId() {
        return serviceId;
    }
    
    /**
     * Sets the identifier of the service for which the principal was authenticated.
     * 
     * @param id identifier of the service for which the principal was authenticated, may not be null or empty
     */
    protected void setServiceId(String id) {
        serviceId = StringSupport.trimOrNull(id);
        Assert.isNotNull(serviceId, "Service ID can not be null or empty");
    }
    
    /**
     * Gets the principal established by the authentication event.
     * 
     * @return principal established by the authentication event, never null
     */
    public Principal getPrincipal() {
        return principal;
    }

    /**
     * Sets the principal established by the authentication event.
     * 
     * @param authenticationPrincipal principal established by the authentication event, never null
     */
    protected void setPrincipal(Principal authenticationPrincipal) {
        Assert.isNotNull(authenticationPrincipal, "Authentication principal can not be null");
        principal = authenticationPrincipal;
    }

    /**
     * Gets the method used to authenticate the principal.
     * 
     * @return method used to authenticate the principal, never null
     */
    public String getAuthenticationMethod() {
        return authnMethod;
    }

    /**
     * Sets the method used to authenticate the principal.
     * 
     * @param method method used to authenticate the principal, never null nor empty
     */
    protected void setAuthenticationMethod(String method) {
        String trimmedMethod = StringSupport.trimOrNull(method);
        Assert.isNotNull(trimmedMethod, "Authentication method can not be null nor empty");
        authnMethod = trimmedMethod;
    }

    /**
     * Gets the time, in milliseconds since the epoch, that the authentication completed.
     * 
     * @return time, in milliseconds since the epoch, that the authentication completed, never less than 0
     */
    public long getAuthenticationInstant() {
        return autnInstant;
    }

    /**
     * Sets the time, in milliseconds since the epoch, that the authentication completed.
     * 
     * @param instant time, in milliseconds since the epoch, that the authentication completed, must be greater than 0
     */
    protected void setAuthenticationInstant(long instant) {
        Assert.isGreaterThan(0, instant, "Authentication instant must be greater than 0");
        autnInstant = instant;
    }

    /**
     * Gets the time, in milliseconds since the epoch, when this authentication method expires. A value of 0 or less
     * indicates the authentication method does not have an absolute expiration instant.
     * 
     * @return time, in milliseconds since the epoch, when this authentication method expires
     */
    public long getExpirationInstant() {
        return expirationInstant;
    }

    /**
     * Sets the time, in milliseconds since the epoch, when this authentication method expires.
     * 
     * @param instant time, in milliseconds since the epoch, when this authentication method expires
     */
    protected void setExpirationInstant(long instant) {
        expirationInstant = instant;
    }
}