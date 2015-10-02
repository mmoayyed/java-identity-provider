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

package net.shibboleth.idp.session.context;

import net.shibboleth.idp.session.SPSession;
import net.shibboleth.utilities.java.support.logic.Constraint;
import org.opensaml.messaging.context.BaseContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Context holding information needed to perform logout for a single SP session.
 *
 * @author Marvin S. Addison
 */
public class LogoutPropagationContext extends BaseContext {
    /** SP session to be destroyed. */
    @Nonnull private SPSession session;


    /** @return The SP session to be destroyed. */
    @Nullable public SPSession getSession() {
        return session;
    }

    /**
     * Sets the SP session to be destroyed.
     *
     * @param session Non-null SP session.
     */
    public void setSession(@Nonnull final SPSession session) {
        this.session = Constraint.isNotNull(session, "SPSession cannnot be null");
    }
}
