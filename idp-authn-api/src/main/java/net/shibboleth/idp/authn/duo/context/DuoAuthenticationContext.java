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

package net.shibboleth.idp.authn.duo.context;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.BaseContext;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;

/**
 * Context that carries Duo factor and device or passcode to be used in validation.
 * 
 * <p>This is used for AuthAPI-based use of Duo rather than the usual delegation
 * of the process to their Web SDK.</p>
 * 
 * @parent {@link AuthenticationContext}
 * @added After extracting the Duo factor and device or passcode during authentication
 */
public class DuoAuthenticationContext extends BaseContext {

    /** Username. */
    @Nullable private String username;
    
    /** Client address. */
    @Nullable private String clientAddress;
    
    /** Factor. */
    @Nullable private String duoFactor;

    /** Device ID. */
    @Nullable private String duoDevice;

    /** Passcode. */
    @Nullable private String duoPasscode;
    
    /** PushInfo data. */
    @Nullable private Map<String,String> pushInfo;

    /** Constructor. */
    public DuoAuthenticationContext() {
        pushInfo = new HashMap<>();
    }
    
    /**
     * Get the username.
     * 
     * @return username
     */
    @Nullable public String getUsername() {
        return username;
    }
    
    /**
     * Set the username.
     * 
     * @param name username
     * 
     * @return this context
     */
    @Nonnull public DuoAuthenticationContext setUsername(@Nullable final String name) {
        username = name;
        return this;
    }

    /**
     * Get the client address.
     * 
     * @return address
     */
    @Nullable public String getClientAddress() {
        return clientAddress;
    }
    
    /**
     * Set the client address.
     * 
     * @param address client address
     * 
     * @return this context
     */
    @Nonnull public DuoAuthenticationContext setClientAddress(@Nullable final String address) {
        clientAddress = address;
        return this;
    }

    /**
     * Get the device ID.
     * 
     * @return the Duo device identifier
     */
    @Nullable public String getDeviceID() {
        return duoDevice;
    }

    /**
     * Set the device ID.
     * 
     * @param deviceId the Duo device identifier
     * 
     * @return this context
     */
    @Nonnull public DuoAuthenticationContext setDeviceID(@Nullable final String deviceId) {
        duoDevice = deviceId;
        return this;
    }

    /**
     * Get the factor to use.
     * 
     * @return the factor to use
     */
    @Nullable public String getFactor() {
        return duoFactor;
    }

    /**
     * Set the factor to use.
     * 
     * @param factor the Duo factor
     * 
     * @return this context
     */
    @Nonnull public DuoAuthenticationContext setFactor(@Nullable final String factor) {
        duoFactor = factor;
        return this;
    }

    /**
     * Get the passcode.
     * 
     * @return the passcode
     */
    @Nullable public String getPasscode() {
        return duoPasscode;
    }

    /**
     * Set the passcode.
     * 
     * @param passcode the passcode
     * 
     * @return this context
     */
    @Nonnull public DuoAuthenticationContext setPasscode(@Nullable final String passcode) {
        duoPasscode = passcode;
        return this;
    }

    /**
     * Get the pushinfo.
     * 
     * @return the pushinfo
     */
    @Nonnull @NonnullElements @Live public Map<String,String> getPushInfo() {
        return pushInfo;
    }
    
}