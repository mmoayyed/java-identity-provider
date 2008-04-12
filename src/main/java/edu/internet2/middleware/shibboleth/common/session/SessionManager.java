/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
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

/**
 * Session managers are responsible for creating, managing, and destroying Shibboleth sessions.
 * 
 * Session managers produce a {@link LoginEvent} during session creation and a {@link LogoutEvent} during session
 * destruction.
 * 
 * @param <SessionType> type of session object managed
 */
public interface SessionManager<SessionType extends Session> {

    /**
     * Creates a Shibboleth session.
     * 
     * @param principal the principal name of the user
     * 
     * @return a Shibboleth session
     */
    public SessionType createSession(String principal);

    /**
     * Gets the user's session based on session's ID or the user's prinicpal name.
     * 
     * @param sessionID the ID of the session or the user's principal name
     * 
     * @return the session
     */
    public SessionType getSession(String sessionID);

    /**
     * Destroys the session.
     * 
     * @param sessionID the ID of the session.
     */
    public void destroySession(String sessionID);
}