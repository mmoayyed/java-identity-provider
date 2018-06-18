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
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.security.Principal;

/**
 * Container for metadata about a CAS service (i.e. relying party).
 *
 * @author Marvin S. Addison
 */
public class Service implements Principal {

    /** Service URL. */
    @Nonnull
    @NotEmpty
    private final String serviceURL;

    /** Group to which service belongs. */
    @Nullable
    private final String serviceGroup;

    /** Proxy authorization flag. */
    private final boolean authorizedToProxy;

    /** Indicates whether a service wants to receive SLO messages. */
    private final boolean singleLogoutParticipant;

    /** Source of service metadata derived from a SAML entity. */
    @Nullable
    private transient EntityDescriptor entityDescriptor;


    /**
     * Creates a new service that does not participate in SLO.
     *
     * @param url CAS service URL.
     * @param group Group to which service belongs.
     * @param proxy True to authorize proxying, false otherwise.
     */
    public Service(
            @Nonnull @NotEmpty final String url,
            @Nullable @NotEmpty final String group,
            final boolean proxy) {
        this(url, group, proxy, false);
    }

    /**
     * Creates a new service that MAY participate in SLO.
     *
     * @param url CAS service URL.
     * @param group Group to which service belongs.
     * @param proxy True to authorize proxying, false otherwise.
     * @param wantsSLO True to indicate the service wants to receive SLO messages, false otherwise.
     */
    public Service(
            @Nonnull @NotEmpty final String url,
            @Nullable @NotEmpty final String group,
            final boolean proxy,
            final boolean wantsSLO) {
        serviceURL = Constraint.isNotNull(StringSupport.trimOrNull(url), "Service URL cannot be null or empty");
        serviceGroup = StringSupport.trimOrNull(group);
        authorizedToProxy = proxy;
        singleLogoutParticipant = wantsSLO;
    }

    /**
     * Get the service URL.
     * 
     * {@inheritDoc}
     */
    @Override public String getName() {
        return serviceURL;
    }

    /**
     * Get the group to which the service belongs.
     * 
     * @return service group name
     */
    @Nullable public String getGroup() {
        return serviceGroup;
    }

    /**
     * Get whether proxying is authorized.
     * 
     * @return true if proxying is authorized, false otherwise
     */
    public boolean isAuthorizedToProxy() {
        return authorizedToProxy;
    }

    /**
     * Get whether the service wants to receive SLO message.
     * 
     * @return true to indicate the service wants to receive SLO messages, false otherwise
     */
    public boolean isSingleLogoutParticipant() {
        return singleLogoutParticipant;
    }

    /**
     * Gets the SAML entity that is the source of service metadata.
     *
     * @return Entity descriptor for service defined in SAML metadata, otherwise null.
     */
    @Nullable
    public EntityDescriptor getEntityDescriptor() {
        return entityDescriptor;
    }

    /**
     * Sets the SAML entity that is the source of service metadata.
     *
     * @param ed SAML entity descriptor.
     */
    public void setEntityDescriptor(@Nullable final EntityDescriptor ed) {
        this.entityDescriptor = ed;
    }

    @Override
    public String toString() {
        return serviceURL;
    }
}
