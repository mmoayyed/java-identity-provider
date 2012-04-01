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

package net.shibboleth.idp.authn;

import java.net.InetAddress;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.logic.Assert;

import org.opensaml.messaging.context.BaseContext;

/** A context containing the IP address of the user agent. */
public class UserAgentAddressContext extends BaseContext {

    /** Address of the user-agent host. */
    private InetAddress address;

    /**
     * Gets the address of the user-agent host.
     * 
     * @return address of the user-agent host
     */
    public InetAddress getUserAgentAddress() {
        return address;
    }

    /**
     * Sets the address of the user-agent host.
     * 
     * @param userAgentAddress address of the user-agent host
     * 
     * @return this context
     */
    public UserAgentAddressContext setUserAgentAddress(@Nonnull final InetAddress userAgentAddress) {
        address = Assert.isNotNull(userAgentAddress, "User-agent address can not be null");
        return this;
    }
}