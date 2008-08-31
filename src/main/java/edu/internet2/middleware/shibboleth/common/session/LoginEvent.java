/*
 * Copyright 2006 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.common.session;

import org.springframework.context.ApplicationEvent;

/** An event representing the creation of a Shibboleth session that occurred because a user logged into the system. */
public class LoginEvent extends ApplicationEvent {

    /** Serial version UID. */
    private static final long serialVersionUID = -898463237588351558L;

    /**
     * Constructor.
     * 
     * @param session session for the user logging in
     */
    public LoginEvent(Session session) {
        super(session);
    }

    /**
     * Gets the session for the user logging in.
     * 
     * @return session for the user logging in
     */
    public Session getUserSession() {
        return (Session) getSource();
    }
}