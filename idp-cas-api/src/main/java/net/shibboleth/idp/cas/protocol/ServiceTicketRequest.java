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

package net.shibboleth.idp.cas.protocol;

import javax.annotation.Nonnull;

import net.shibboleth.shared.logic.Constraint;

/**
 * Describes a request for a ticket to access a service.
 *
 * @author Marvin S. Addison
 */
public class ServiceTicketRequest {

    /** HTTP GET method. */
    public static final String METHOD_GET = "GET";

    /** HTTP POST method. */
    public static final String METHOD_POST = "POST";

    /** Service URL. */
    @Nonnull private final String serviceURL;

    /** CAS protocol renew flag. */
    private boolean renew;

    /** CAS protocol gateway flag. */
    private boolean gateway;

    /** Flag indicating whether ticket request is via SAML 1.1 protocol. */
    private boolean saml;

    /** CAS protocol 3.0 "method" parameter. */
    private String method = METHOD_GET;

    /**
     * Constructor.
     *
     * @param service URL of service requesting the ticket
     */
    public ServiceTicketRequest(@Nonnull final String service) {
        serviceURL = Constraint.isNotNull(service, "Service cannot be null");
    }

    /**
     * Get the service requesting the ticket.
     * 
     * @return service requesting the ticket
     */
    @Nonnull public String getService() {
        return serviceURL;
    }

    /**
     * Get whether to require fresh subject interaction to succeed.
     * 
     * @return whether subject interaction must occur
     */
    public boolean isRenew() {
        return renew;
    }

    /**
     * Set whether to require fresh subject interaction to succeed.
     * 
     * @param force whether subject interaction must occur
     */
    public void setRenew(final boolean force) {
        renew = force;
    }

    /**
     * Whether to not require fresh subject interaction to succeed.
     * 
     * @return whether subject interaction should not occur
     */
    public boolean isGateway() {
        return gateway;
    }

    /**
     * Set whether to not require fresh subject interaction to succeed.
     * 
     * @param doNotForce whether subject interaction should not occur
     */
    public void setGateway(final boolean doNotForce) {
        gateway = doNotForce;
    }

    /**
     * Get whether ticket request is via SAML 1.1 protocol.
     * 
     * @return whether ticket request is via SAML 1.1 protocol
     */
    public boolean isSAML() {
        return saml;
    }

    /**
     * Set whether ticket request is via SAML 1.1 protocol.
     * 
     * @param flag flag to set
     */
    public void setSAML(final boolean flag) {
        saml = flag;
    }

    /**
     * Gets the value of the <code>method</code> parameter. Default is {@value #METHOD_GET}.
     *
     * @return {@value #METHOD_GET} or {@value #METHOD_POST}.
     */
    @Nonnull public String getMethod() {
        return method;
    }

    /**
     * Sets the value of the <code>method</code> parameter.
     * See <a href="http://jasig.github.io/cas/development/protocol/CAS-Protocol-Specification.html#head2.1.1">
     *     http://jasig.github.io/cas/development/protocol/CAS-Protocol-Specification.html#head2.1.1</a> for more
     * information.
     *
     * @param m {@value #METHOD_GET} or {@value #METHOD_POST}.
     */
    public void setMethod(@Nonnull final String m) {
        if (METHOD_GET.equalsIgnoreCase(m)) {
            method = METHOD_GET;
        } else if (METHOD_POST.equalsIgnoreCase(m)) {
            method = METHOD_POST;
        } else {
            throw new IllegalArgumentException("Unsupported method " + m);
        }
    }
}
