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

package net.shibboleth.idp.session.context.navigate;

import java.util.Collection;

import javax.annotation.Nullable;

import net.shibboleth.idp.session.SPSession;
import net.shibboleth.idp.session.context.LogoutContext;

import org.opensaml.messaging.context.navigate.ContextDataLookupFunction;

/**
 * A function that returns a session from a {@link LogoutContext}
 * and removes it from that context at the same time.
 * 
 * <p>The exact session returned is unspecified.</p>
 */
public class LogoutContextSessionLookupFunction implements ContextDataLookupFunction<LogoutContext,SPSession> {
    
    /** {@inheritDoc} */
    @Nullable public SPSession apply(@Nullable final LogoutContext input) {
        
        if (input != null) {
            final Collection<SPSession> sessions = input.getSessionMap().values();
            if (sessions != null && !sessions.isEmpty()) {
                final SPSession session = sessions.iterator().next();
                if (session != null) {
                    sessions.remove(session);
                    return session;
                }
            }
        }
        return null;
    }

}