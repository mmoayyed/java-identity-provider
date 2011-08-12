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

package net.shibboleth.idp.session;

import net.shibboleth.idp.persistence.PersistenceManager;

import org.opensaml.messaging.context.Subcontext;

/** A manager of identity provider sessions. */
public interface SessionManager extends PersistenceManager<IdPSession> {

    /**
     * Creates a new session.
     * 
     * @return the new session, never null
     */
    public IdPSession createSession();

    /**
     * Adds an authentication event to an existing session.
     * 
     * @param idpSession existing session to which the event will be added, may not be null
     * @param serviceId ID of the service to which the principal authenticated, may not be null or empty
     * @param event the authentication event to add to the session, may not be null
     */
    public void addAuthenticationEvent(IdPSession idpSession, String serviceId, AuthenticationEvent event);

    /**
     * Add non-null additional contextual information to the IdP session. If the contextual information is null this
     * method is a no-op.
     * 
     * @param idpSession the existing IdP session, may not be null
     * @param subcontext context to add, may be null
     */
    public void addIdPSessionContext(IdPSession idpSession, Subcontext subcontext);

    /**
     * Adds non-null additional contextual information to a given authentication event. If the contextual information is
     * null this method is a no-op.
     * 
     * @param idpSession session containing the authentication event, may not be null
     * @param event event to which the contextual information will be added, may not be null
     * @param subcontext contextual information to be added, may be null
     */
    public void addAuthenticationEventContext(IdPSession idpSession, AuthenticationEvent event, Subcontext subcontext);

    /**
     * Adds contextual information to an existing service session. If the contextual information is null this method is
     * a no-op.
     * 
     * @param idpSession session containing the service session, may not be null
     * @param serviceSession service session to which the contextual information may be added
     * @param subcontext contextual information to be added, may be null
     */
    public void addServiceSessonContext(IdPSession idpSession, ServiceSession serviceSession, Subcontext subcontext);
}