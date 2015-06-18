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

package net.shibboleth.idp.cas.service;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.regex.Pattern;

/**
 * Defines a registered CAS service (i.e. relying party).
 *
 * @author Marvin S. Addison
 */
public class ServiceDefinition {
    /** Pattern used to match candidate services against. */
    @Nonnull
    private final Pattern matchPattern;

    /** Logical group to which matching servcies belong. */
    @Nullable
    private String group;

    /** Proxy authorization flag. */
    private boolean authorizedToProxy;

    /** Indicates whether a service wants to receive SLO messages. */
    private boolean singleLogoutParticipant;


    /**
     * Creates a new instance with the given regular expression match pattern.
     *
     * @param regex CAS service URL match pattern as Java regular expression string.
     */
    public ServiceDefinition(@Nonnull @NotEmpty final String regex) {
        matchPattern = Pattern.compile(
                Constraint.isNotNull(StringSupport.trimOrNull(regex), "Regular expression cannot be null or empty"));
    }

    /**
     * Creates a new instance with the given regular expression match pattern.
     *
     * @param pattern CAS service URL match pattern.
     */
    public ServiceDefinition(@Nonnull final Pattern pattern) {
        matchPattern = Constraint.isNotNull(pattern, "Pattern cannot be null or empty");
    }

    /**
     * @return Group name to which services matching this definition belong.
     */
    @Nullable public String getGroup() {
        return group;
    }

    /**
     * Sets the group name.
     *
     * @param group Group name.
     */
    public void setGroup(@NotEmpty final String group) {
        this.group = StringSupport.trimOrNull(group);
    }

    /** @return True if proxy is authorized, false otherwise. */
    public boolean isAuthorizedToProxy() {
        return authorizedToProxy;
    }

    /**
     * Sets the proxy authorization flag.
     *
     * @param proxy True to allow the service to request proxy-granting tickets, false otherwise.
     */
    public void setAuthorizedToProxy(final boolean proxy) {
        this.authorizedToProxy = proxy;
    }

    /** @return True to indicate the service wants to receive SLO messages, false otherwise. */
    public boolean isSingleLogoutParticipant() {
        return singleLogoutParticipant;
    }

    /**
     * Determines whether the service participates in SLO.
     *
     * @param wantsSLO True to indicate the service wants to receive SLO messages, false otherwise.
     */
    public void setSingleLogoutParticipant(final boolean wantsSLO) {
        this.singleLogoutParticipant = wantsSLO;
    }

    /**
     * Determines whether the given CAS service URL matches the pattern.
     *
     * @param serviceURL CAS service URL to test; MUST NOT be URL encoded.
     *
     * @return True if given service URL matches the pattern, false otherwise.
     */
    public boolean matches(final String serviceURL) {
        return matchPattern.matcher(serviceURL).matches();
    }

    @Override
    public String toString() {
        return matchPattern.pattern();
    }
}
