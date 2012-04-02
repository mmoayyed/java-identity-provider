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

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.BaseContext;

/** A {@link org.opensaml.messaging.context.BaseContext} that holds an {@link IdPSession}. */
public class IdPSessionContext extends BaseContext {

    /** IdP session wrapped by this adapter. */
    private final IdPSession session;

    /**
     * Constructor.
     * 
     * @param idpSession IdP session wrapped by this adapter
     */
    public IdPSessionContext(@Nonnull final IdPSession idpSession) {
        super();
        session = Constraint.isNotNull(idpSession, "IdP session can not be null");
    }

    /**
     * Gets the IdP session.
     * 
     * @return the IdP session, never null
     */
    @Nonnull public IdPSession getIdPSession() {
        return session;
    }
}