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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.idp.session.SPSessionSerializerRegistry;
import net.shibboleth.idp.session.context.LogoutContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.security.DataSealer;
import net.shibboleth.utilities.java.support.security.DataSealerException;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

/**
 * Profile action that reconstitutes an encrypted {@link SPSession} object from an HTTP request parameter, and
 * attaches it to a {@link LogoutContext} created with a strategy function.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link org.opensaml.profile.action.EventIds#INVALID_PROFILE_CTX}
 * @event {@link org.opensaml.profile.action.EventIds#UNABLE_TO_DECODE}
 * @post If an {@link SPSession} is found and successfully deserialized, then a {@link LogoutContext} will be populated.
 */
public class PopulateLogoutContextForPropagation extends AbstractProfileAction {
    
    /** Name of parameter containing session. */
    @Nonnull @NotEmpty private static final String SPSESSION_PARAM = "SPSession";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateLogoutContextForPropagation.class);
    
    /** {@link DataSealer} to use. */
    @NonnullAfterInit private DataSealer dataSealer;
    
    /** Mappings between a SPSession type and a serializer implementation. */
    @NonnullAfterInit private SPSessionSerializerRegistry spSessionSerializerRegistry;
    
    /** Creation/lookup function for LogoutContext. */
    @Nonnull private Function<ProfileRequestContext,LogoutContext> logoutContextCreationStrategy;
    
    /** {@link SPSession} to operate on. */
    @Nullable private SPSession session;
    
    /** Constructor. */
    public PopulateLogoutContextForPropagation() {
        logoutContextCreationStrategy = new ChildContextLookup<>(LogoutContext.class, true);
    }
    
    /**
     * Set the {@link DataSealer} to use.
     * 
     * @param sealer the {@link DataSealer} to use
     */
    public void setDataSealer(@Nonnull final DataSealer sealer) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        dataSealer = Constraint.isNotNull(sealer, "DataSealer cannot be null");
    }
    
    /**
     * Set the {@link SPSessionSerializerRegistry} to use.
     * 
     * @param registry a registry of SPSession class to serializer mappings
     */
    public void setSPSessionSerializerRegistry(@Nonnull final SPSessionSerializerRegistry registry) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        spSessionSerializerRegistry = Constraint.isNotNull(registry, "Registry cannot be null");
    }
    
    /**
     * Set the creation/lookup strategy for the LogoutContext to populate.
     * 
     * @param strategy  creation/lookup strategy
     */
    public void setLogoutContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext,LogoutContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        logoutContextCreationStrategy = Constraint.isNotNull(strategy,
                "LogoutContext creation strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (spSessionSerializerRegistry == null) {
            throw new ComponentInitializationException("SPSessionSerializerRegistry cannot be null");
        } else if (dataSealer == null) {
            throw new ComponentInitializationException("DataSealer cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        final HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            log.error("{} HttpServletRequest is not set", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.UNABLE_TO_DECODE);
            return false;
        }
        
        final String param = request.getParameter(SPSESSION_PARAM);
        if (param == null) {
            log.warn("{} No {} parameter provided, nothing to do", getLogPrefix(), SPSESSION_PARAM);
            ActionSupport.buildEvent(profileRequestContext, EventIds.UNABLE_TO_DECODE);
            return false;
        }
        
        try {
            final String decrypted = dataSealer.unwrap(param);
            final int pos = decrypted.indexOf(':');
            if (pos <= 0) {
                log.warn("{} No class identifier found in decrypted {} parameter", getLogPrefix(), SPSESSION_PARAM);
                ActionSupport.buildEvent(profileRequestContext, EventIds.UNABLE_TO_DECODE);
                return false;
            }
            
            final String sessionClassName = decrypted.substring(0,  pos);

            final StorageSerializer<? extends SPSession> spSessionSerializer =
                    spSessionSerializerRegistry.lookup(Class.forName(sessionClassName).asSubclass(SPSession.class));
            if (spSessionSerializer == null) {
                log.warn("{} No serializer registered for SPSession type: {}", getLogPrefix(), sessionClassName);
                ActionSupport.buildEvent(profileRequestContext, EventIds.UNABLE_TO_DECODE);
                return false;
            }
            
            // Deserializer starting past the colon delimiter. The fields are mostly irrelevant here,
            // we're just after the session data itself.
            session = spSessionSerializer.deserialize(
                    1, "session", "key", param.substring(pos + 1), System.currentTimeMillis());
        } catch (final ClassNotFoundException | IOException | DataSealerException e) {
            log.warn("{} Error deserializing encrypted SPSession", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.UNABLE_TO_DECODE);
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        final LogoutContext logoutCtx = logoutContextCreationStrategy.apply(profileRequestContext);
        if (logoutCtx == null) {
            log.error("{} Unable to create or locate LogoutContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return;
        }
        
        logoutCtx.getSessionMap().put(session.getId(), session);
    }
    
}