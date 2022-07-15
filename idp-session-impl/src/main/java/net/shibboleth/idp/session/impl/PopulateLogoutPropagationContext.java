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

import java.io.IOException;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.MessageException;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.idp.session.SPSessionSerializerRegistry;
import net.shibboleth.idp.session.context.LogoutContext;
import net.shibboleth.idp.session.context.LogoutPropagationContext;
import net.shibboleth.idp.session.context.LogoutPropagationContext.Result;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.security.DataSealer;
import net.shibboleth.utilities.java.support.security.DataSealerException;

/**
 * Profile action that creates a {@link LogoutPropagationContext} containing {@link SPSession} to be destroyed. The
 * SP sessions may be populated:
 * <ul>
 *     <li>By reference - via <em>SessionKey</em> request parameter that looks up one {@link SPSession} from a
 *     {@link LogoutContext} stored in the HTTP session.</li>
 *     <li>By value - reconstitutes an encrypted {@link SPSession} object in <em>SPSession</em> request parameter.</li>
 *     <li>By lookup strategy.</li>
 * </ul>
 *
 * @event SessionNotFound
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link org.opensaml.profile.action.EventIds#INVALID_PROFILE_CTX}
 * @event {@link org.opensaml.profile.action.EventIds#UNABLE_TO_DECODE}
 * @post If an {@link SPSession} is found, then a {@link LogoutPropagationContext} will be populated.
 */
public class PopulateLogoutPropagationContext extends AbstractProfileAction {

    /** Name of parameter containing session by reference. */
    @Nonnull @NotEmpty private static final String SESSION_NOT_FOUND = "SessionNotFound";

    /** Name of parameter containing session by reference. */
    @Nonnull @NotEmpty private static final String SESSION_PARAM_BYREF = "SessionKey";

    /** Name of parameter containing session by value. */
    @Nonnull @NotEmpty private static final String SESSION_PARAM_BYVAL = "SPSession";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateLogoutPropagationContext.class);
    
    /** {@link DataSealer} to use. */
    @Nullable private DataSealer dataSealer;
    
    /** Mappings between a SPSession type and a serializer implementation. */
    @Nullable private SPSessionSerializerRegistry spSessionSerializerRegistry;
    
    /** Lookup/creation function for LogoutPropagationContext. */
    @Nonnull private Function<ProfileRequestContext,LogoutPropagationContext> contextCreationStrategy;
    
    /** Lookup strategy for session. */
    @Nullable private Function<ProfileRequestContext,SPSession> sessionLookupStrategy;
    
    /** {@link SPSession} to operate on. */
    @Nullable private SPSession session;

    /** The value used to look up a session by reference. */
    @Nullable private String sessionKey;


    /** Constructor. */
    public PopulateLogoutPropagationContext() {
        contextCreationStrategy = new ChildContextLookup<>(LogoutPropagationContext.class, true);
    }
    
    /**
     * Set the {@link DataSealer} to use.
     * 
     * @param sealer the {@link DataSealer} to use
     */
    public void setDataSealer(@Nullable final DataSealer sealer) {
        checkSetterPreconditions();
        dataSealer = sealer;
    }
    
    /**
     * Set the {@link SPSessionSerializerRegistry} to use.
     * 
     * @param registry a registry of SPSession class to serializer mappings
     */
    public void setSPSessionSerializerRegistry(@Nullable final SPSessionSerializerRegistry registry) {
        checkSetterPreconditions();
        spSessionSerializerRegistry = registry;
    }
    
    /**
     * Set the creation/lookup strategy for the {@link LogoutPropagationContext}.
     * 
     * @param strategy  creation/lookup strategy
     */
    public void setLogoutPropagationContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext, LogoutPropagationContext> strategy) {
        checkSetterPreconditions();
        contextCreationStrategy = Constraint.isNotNull(strategy,
                "LogoutPropagationContext creation strategy cannot be null");
    }
    
    /**
     * Set a lookup strategy to use to obtain the session to populate.
     * 
     * @param strategy lookup strategy
     */
    public void setSessionLookupStrategy(@Nullable final Function<ProfileRequestContext,SPSession> strategy) {
        checkSetterPreconditions();
        sessionLookupStrategy = strategy;
    }
    
// Checkstyle: ReturnCount|CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        if (sessionLookupStrategy != null) {
            session = sessionLookupStrategy.apply(profileRequestContext);
            if (session != null) {
                log.debug("{} Got session to propagate logout: {}", getLogPrefix(), session);
                return true;
            }
            log.debug("{} No sessions remaining for logout propagation", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, SESSION_NOT_FOUND);
            return false;
        }

        final RequestContext requestContext = getRequestContext(profileRequestContext);
        if (requestContext == null) {
            log.error("{} Spring RequestContext is not set", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.UNABLE_TO_DECODE);
            return false;
        }
        
        final String sessionRef = requestContext.getRequestParameters().get(SESSION_PARAM_BYREF);
        final String sessionVal = requestContext.getRequestParameters().get(SESSION_PARAM_BYVAL);
        try {
            if (sessionRef != null) {
                sessionKey = sessionRef;
                session = getSessionByReference(requestContext, sessionKey);
            } else if (sessionVal != null) {
                if (dataSealer != null && spSessionSerializerRegistry != null) {
                    session = getSessionByValue(sessionVal);
                } else {
                    log.error("{} No DataSealer/SerializerRegistry provided, unable to process session passed by value",
                            getLogPrefix());
                    ActionSupport.buildEvent(profileRequestContext, EventIds.UNABLE_TO_DECODE);
                    return false;
                }
            } else {
                log.warn("{} No session parameter provided, nothing to do", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, EventIds.UNABLE_TO_DECODE);
                return false;
            }
            log.debug("{} Got session to propagate logout: {}", getLogPrefix(), session);
        } catch (final MessageDecodingException e) {
            log.warn("{} Message decoding exception", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.UNABLE_TO_DECODE);
            return false;
        } catch (final MessageException e) {
            log.warn("{} Required state not found", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }

        return true;
    }
// Checkstyle: ReturnCount|CyclomaticComplexity ON

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        final LogoutPropagationContext logoutPropCtx = contextCreationStrategy.apply(profileRequestContext);
        if (logoutPropCtx == null) {
            log.error("{} Unable to create or locate LogoutPropagationContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return;
        }
        logoutPropCtx.setSession(session);
        logoutPropCtx.setSessionKey(sessionKey);
        logoutPropCtx.setResult(Result.Failure);
        logoutPropCtx.setDetail(null);
    }

    /**
     * Get an {@link SPSession} by reference.
     * 
     * @param requestContext Spring request context
     * @param key session key identifying the SP session
     * 
     * @return the SP session
     * @throws MessageException if an error occurs
     */
    @Nonnull private SPSession getSessionByReference(
            @Nonnull final RequestContext requestContext, @Nonnull final String key) throws MessageException {
        final LogoutContext logoutCtx = requestContext.getExternalContext().getSessionMap().get(
                SaveLogoutContext.LOGOUT_CONTEXT_KEY, LogoutContext.class);
        if (logoutCtx == null) {
            throw new MessageException("LogoutContext not found in HTTP session.");
        }
        
        final SPSession s = logoutCtx.getKeyedSessionMap().get(key);
        if (s == null) {
            throw new MessageException("Session not found for key: " + key);
        }
        
        return s;
    }

    /**
     * Get an {@link SPSession} by value.
     * 
     * @param sessionVal serialized SP session
     * 
     * @return the SP session
     * @throws MessageDecodingException if an error occurs
     */
    @Nonnull private SPSession getSessionByValue(@Nonnull final String sessionVal) throws MessageDecodingException {
        try {
            final String decrypted = dataSealer.unwrap(sessionVal);
            final int pos = decrypted.indexOf(':');
            if (pos <= 0) {
                throw new MessageDecodingException("No class identifier found in decrypted message");
            }

            final String sessionClassName = decrypted.substring(0,  pos);

            final StorageSerializer<? extends SPSession> spSessionSerializer =
                    spSessionSerializerRegistry.lookup(Class.forName(sessionClassName).asSubclass(SPSession.class));
            if (spSessionSerializer == null) {
                throw new MessageDecodingException("No serializer registered for session type: " + sessionClassName);
            }

            // Deserialize starting past the colon delimiter. The fields are mostly irrelevant here,
            // we're just after the session data itself.
            return spSessionSerializer.deserialize(
                    1, "session", "key", decrypted.substring(pos + 1), System.currentTimeMillis());
        } catch (final ClassNotFoundException | IOException | DataSealerException e) {
            throw new MessageDecodingException("Error deserializing encrypted SPSession", e);
        }
    }
}