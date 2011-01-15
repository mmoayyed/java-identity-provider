/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.idp.authn;

import java.util.List;

import javax.security.auth.Subject;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.messaging.context.SubcontextContainer;
import org.opensaml.messaging.context.impl.BaseSubcontext;

/**
 *
 */
public final class AuthenticationContext extends BaseSubcontext {

    /** Time when the authentication process started. */
    private DateTime initiationInstant;
    
    /** Should subject authentication be forced regardless of any existing state. */
    private boolean forceAuth;

    /** Default authentication method to use if no other method is requested. */
    private String defaultMethod;

    /** List of requested authentication methods. */
    private List<String> requestedMethods;

    /** Authentication methods that authenticated the subject. */
    private List<String> satisfiedMethods;
    
    /** The subject that was authenticated. */
    private Subject authenticatedSubject;
    
    /** Time when authentication process completed. */
    private DateTime completionInstant;

    /**
     * Constructor.
     *
     * @param parent context that owns this context
     */
    public AuthenticationContext(SubcontextContainer parent) {
        this(parent, null);
    }
    
    /**
     * Constructor.
     *
     * @param parent context that owns this context
     * @param subject existing subject record to which additional authentication information may be added
     */
    public AuthenticationContext(SubcontextContainer parent, Subject subject) {
        super(parent);
        initiationInstant = new DateTime(ISOChronology.getInstanceUTC());
        authenticatedSubject = subject;
    }
}