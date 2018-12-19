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

package net.shibboleth.idp.session.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.StatusCode;

import com.google.common.base.Function;

import net.shibboleth.idp.session.context.LogoutContext;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * A strategy function for determining the status of a logout based on the content of
 * a {@link LogoutContext}.
 * 
 * <p>It signals an error if the context still contains any {@link IdPSession} objects,
 * indicating a logout was cancelled.</p>
 */
public class LogoutStatusStrategyFunction implements Function<ProfileRequestContext,List<String>> {
    
    /** Lookup strategy for context. */
    @Nonnull private Function<ProfileRequestContext,LogoutContext> logoutContextLookupStrategy;
    
    /** Constructor. */
    public LogoutStatusStrategyFunction() {
        logoutContextLookupStrategy = new ChildContextLookup<>(LogoutContext.class);
    }

    /**
     * Set the lookup strategy for the LogoutContext to access.
     * 
     * @param strategy  lookup strategy
     */
    public void setLogoutContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,LogoutContext> strategy) {
        
        logoutContextLookupStrategy = Constraint.isNotNull(strategy,
                "LogoutContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    public @Nullable List<String> apply(@Nullable final ProfileRequestContext input) {
        
        final LogoutContext logoutCtx = logoutContextLookupStrategy.apply(input);
        if (logoutCtx != null) {
            if (!logoutCtx.getIdPSessions().isEmpty()) {
                return Arrays.asList(StatusCode.RESPONDER, StatusCode.REQUEST_DENIED);
            }
        }
        
        return Collections.emptyList();
    }

}