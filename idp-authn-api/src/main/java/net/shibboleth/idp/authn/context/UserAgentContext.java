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

package net.shibboleth.idp.authn.context;

import java.net.InetAddress;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import org.opensaml.messaging.context.BaseContext;

/**
 * A context containing data about the user agent.
 * 
 * @parent {@link org.opensaml.profile.context.ProfileRequestContext}, {@link AuthenticationContext}
 */
public final class UserAgentContext extends BaseContext {

    /** Address of the user-agent host. */
    @Nullable private InetAddress address;
    
    /** An identification string (such as a User-Agent header). */
    @Nullable private String identifier;

    /** Parsed User-Agent. */
    @Nullable private UserAgent userAgent;


    /**
     * Get the address of the user-agent host.
     * 
     * @return address of the user-agent host
     */
    @Nullable public InetAddress getAddress() {
        return address;
    }

    /**
     * Set the address of the user-agent host.
     * 
     * @param userAgentAddress address of the user-agent host
     * 
     * @return this context
     */
    @Nonnull public UserAgentContext setAddress(@Nullable final InetAddress userAgentAddress) {
        address = userAgentAddress;
        return this;
    }

    /**
     * Get the user agent identifier.
     * 
     * @return identifier for the user agent
     */
    @Nullable public String getIdentifier() {
        return identifier;
    }
    
    /**
     * Set the user agent identifier. The parsed user agent is available via {@link #getUserAgent()} upon calling
     * this method.
     * 
     * @param id identifier for the user agent
     * 
     * @return this context
     */
    @Nonnull public UserAgentContext setIdentifier(@Nullable final String id) {
        identifier = id;
        userAgent = new UserAgent(id);
        return this;
    }

    /**
     * Gets the parsed user agent.
     *
     * @return Parsed user agent or null if {@link #setIdentifier(String)} has not been called
     * 
     * @deprecated
     */
    @Deprecated(since="4.3.0", forRemoval=true)
    @Nullable public UserAgent getUserAgent() {
        return userAgent;
    }

    /**
     * Determines whether this user agent is an instance of the given browser.
     *
     * @param browser browser to check
     *
     * @return True if this user agent is an instance of the given browser, false otherwise
     * 
     * @deprecated
     */
    @Deprecated(since="4.3.0", forRemoval=true)
    public boolean isInstance(@Nonnull final Browser browser) {
        Constraint.isNotNull(browser, "Browser cannot be null");
        if (userAgent == null) {
            return false;
        }
        return userAgent.getBrowser().getGroup().equals(browser) || userAgent.getBrowser().equals(browser);
    }

    /**
     * Determines whether this user agent is an instance of the given operating system.
     *
     * @param os operating system to check
     *
     * @return True if this user agent is an instance of the given operating system, false otherwise
     * 
     * @deprecated
     */
    @Deprecated(since="4.3.0", forRemoval=true)
    public boolean isInstance(@Nonnull final OperatingSystem os) {
        Constraint.isNotNull(os, "OperatingSystem cannot be null");
        if (userAgent == null) {
            return false;
        }
        return userAgent.getOperatingSystem().getGroup().equals(os) || userAgent.getOperatingSystem().equals(os);
    }
}