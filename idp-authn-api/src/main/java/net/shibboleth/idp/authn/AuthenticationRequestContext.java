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

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import net.jcip.annotations.ThreadSafe;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.messaging.context.AbstractSubcontext;
import org.opensaml.messaging.context.SubcontextContainer;
import org.opensaml.util.collections.CollectionSupport;

/** A context representing the state of an authentication attempt. */
@ThreadSafe
public final class AuthenticationRequestContext extends AbstractSubcontext {

    /** Time when the authentication process started. */
    private final DateTime initiationInstant;

    /** Whether authentication must occur even if an existing authentication method exists and is still valid. */
    private final boolean forcingAuthentication;

    /** List of requested authentication methods. */
    private SortedSet<AuthenticationMethod> authenticationMethods;

    /** Time when authentication process completed. */
    private DateTime completionInstant;

    /**
     * Constructor.
     * 
     * @param parent context that owns this context
     * @param isForcedAuthentication whether authentication is to be forced
     */
    public AuthenticationRequestContext(final SubcontextContainer parent, final boolean isForcedAuthentication) {
        super(parent);
        initiationInstant = new DateTime(ISOChronology.getInstanceUTC());
        forcingAuthentication = isForcedAuthentication;
        authenticationMethods = new TreeSet<AuthenticationMethod>();
    }

    /**
     * Gets the time when the authentication event started.
     * 
     * @return time when the authentication event started, never null
     */
    public DateTime getInitiationInstant() {
        return initiationInstant;
    }

    /**
     * Gets the time when the authentication event ended.
     * 
     * @return time when the authentication event ended, may be null
     */
    public DateTime getCompletionInstant() {
        return completionInstant;
    }

    /** Sets the completion time of the authentication attempt to the current time. */
    public void setCompletionInstant() {
        completionInstant = new DateTime(ISOChronology.getInstanceUTC());
    }

    /**
     * Gets whether authentication must occur even if an existing authentication method exists and is still valid.
     * 
     * @return Returns the forcingAuthentication.
     */
    public boolean isForcingAuthentication() {
        return forcingAuthentication;
    }

    /**
     * Gets the unmodifiable set of authentication methods that may be used to authenticate the user. This returned
     * collection is never null nor contains any null entries.
     * 
     * @return list of authentication methods that may be used to authenticate the user
     */
    public SortedSet<AuthenticationMethod> getAuthenticationMethods() {
        return Collections.unmodifiableSortedSet(authenticationMethods);
    }

    /**
     * Sets the authentication methods that may be used to authenticate the user. This replaces all existing methods.
     * 
     * @param methods methods to be used, may be null or contain null entries
     */
    public void setAuthenticationMethods(final Collection<AuthenticationMethod> methods) {
        CollectionSupport.nonNullReplace(methods, authenticationMethods);
    }

    /**
     * Adds an authentication method that may be used to authenticate the user.
     * 
     * @param method method to be added, may be null
     * 
     * @return true of the collection of authentication methods has changed due to this addition, false otherwise
     */
    public boolean addAuthenticationMethod(final AuthenticationMethod method) {
        return CollectionSupport.nonNullAdd(authenticationMethods, method);
    }

    /**
     * Removes an authentication method that may be used to authenticate the user.
     * 
     * @param method method to be removed, may be null
     * 
     * @return true of the collection of authentication methods has changed due to this removal, false otherwise
     */
    public boolean removeAuthenticationMethod(final AuthenticationMethod method) {
        return CollectionSupport.nonNullRemove(authenticationMethods, method);
    }
}