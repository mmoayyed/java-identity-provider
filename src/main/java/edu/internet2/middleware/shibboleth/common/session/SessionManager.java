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
     * Creates an empty Shibboleth session. The created session only contains an session ID. No other properties are
     * populated.
     * 
     * @return a Shibboleth session
     * 
     * @since 1.1
     */
    public SessionType createSession();

    /**
     * Creates a Shibboleth session.
     * 
     * @param principal the principal name of the user
     * 
     * @return a Shibboleth session
     * 
     * @deprecated use {@link #createSession()}
     */
    public SessionType createSession(String principal);

    /**
     * Destroys the session.
     * 
     * @param index the index of the session.
     */
    public void destroySession(String index);

    /**
     * Gets the user's session based on a session index.
     * 
     * @param index the index of the session
     * 
     * @return the session
     */
    public SessionType getSession(String index);

    /**
     * Indexes a session by the given string. This index is in addition too the session's ID.
     * 
     * @param session session to index
     * @param index additional index
     * 
     * @return true if the given session is assigned the given index, false if not. This operation may fail if the given
     *         index is already assigned to another session or if the given session is not managed by this session
     *         manager.
     * 
     * @since 1.1
     */
    public boolean indexSession(SessionType session, String index);

    /**
     * Removes the given index from its associated session.
     * 
     * @param index index to be removed.
     * 
     * @since 1.1
     */
    public void removeSessionIndex(String index);
}