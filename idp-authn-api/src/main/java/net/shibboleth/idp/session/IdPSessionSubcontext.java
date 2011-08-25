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

import org.opensaml.messaging.context.AbstractSubcontext;
import org.opensaml.messaging.context.SubcontextContainer;

/** A {@link org.opensaml.messaging.context.Subcontext} that holds an {@link IdPSession}. */
public class IdPSessionSubcontext extends AbstractSubcontext {

    /** IdP session wrapped by this adapter. */
    private IdPSession session;

    /**
     * Constructor.
     * 
     * @param owningContext context that will own this subcontext
     * @param idpSession IdP session wrapped by this adapter
     */
    public IdPSessionSubcontext(final SubcontextContainer owningContext, final IdPSession idpSession) {
        super(owningContext);

        if (idpSession == null) {
            if (owningContext != null) {
                owningContext.removeSubcontext(this);
            }
            throw new IllegalArgumentException("IdP session can not be null");
        }

        session = idpSession;
    }

    /**
     * Gets the IdP session.
     * 
     * @return the IdP session, never null
     */
    public IdPSession getIdPSession() {
        return session;
    }
}