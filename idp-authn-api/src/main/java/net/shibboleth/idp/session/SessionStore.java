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

/**
 * A manager of identity provider sessions.
 * 
 * Implementations of this manager should implement some method to expire/eject entries from the persistent store. It is
 * recommended that this strategy implement a hard lifetime on the session as well as in activity timeout.
 */
public interface SessionStore extends PersistenceManager<IdPSession> {

}