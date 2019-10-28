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

package net.shibboleth.idp.admin.impl;

import java.security.KeyException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.security.impl.BasicKeystoreKeyStrategy;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action that sets keystore and key passwords for one or more DataSealer KeyStrategy
 * objects based on query parameters.
 * 
 * <p>The only type supported is the basic strategy type provided with the software.</p>
 * 
 * <p>An error event will be signaled after the first unsuccessful unlock operation.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#INVALID_MESSAGE}
 * 
 * @since 3.4.0
 */
public class UnlockDataSealers extends AbstractProfileAction {
    
    /** Name of keystore password parameter. */
    @Nonnull @NotEmpty public static final String KEYSTORE_PASSWORD_PARAM_NAME = "keystorePassword";

    /** Name of key password parameter. */
    @Nonnull @NotEmpty public static final String KEY_PASSWORD_PARAM_NAME = "keyPassword";

    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(UnlockDataSealers.class);
    
    /** Key source(s) to operate on. */
    @Nonnull @NonnullElements private Collection<BasicKeystoreKeyStrategy> keyStrategies;

    /** Constructor. */
    public UnlockDataSealers() {
        keyStrategies = Collections.emptyList();
    }
    
    /**
     * Set the {@link BasicKeystoreKeyStrategy} objects to inject passwords into.
     * 
     * @param strategies objects to unlock
     */
    public void setKeyStrategies(@Nullable @NonnullElements final Collection<BasicKeystoreKeyStrategy> strategies) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        if (strategies != null) {
            keyStrategies = List.copyOf(strategies);
        } else {
            keyStrategies = Collections.emptyList();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(final ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext) || keyStrategies.isEmpty()) {
            return false;
        } else if (getHttpServletRequest() == null) {
            log.warn("{} No HttpServletRequest available", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(final ProfileRequestContext profileRequestContext) {

        final HttpServletRequest request = getHttpServletRequest();
        
        final String[] keystorePasswords = request.getParameterValues(KEYSTORE_PASSWORD_PARAM_NAME);
        final String[] keyPasswords = request.getParameterValues(KEY_PASSWORD_PARAM_NAME);
        
        if (keystorePasswords == null || keyPasswords == null || keystorePasswords.length != keyStrategies.size()
                || keyPasswords.length != keyStrategies.size()) {
            log.warn("{} Password parameter count does not match size of configured KeyStrategy inputs",
                    getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MESSAGE);
            return;
        }
        
        int i = 0;
        final Iterator<BasicKeystoreKeyStrategy> iter = keyStrategies.iterator();
        while (iter.hasNext()) {
            final BasicKeystoreKeyStrategy ks = iter.next();
            
            if (keystorePasswords[i] == null || keyPasswords[i] == null) {
                log.warn("{} Empty password supplied at index {}", getLogPrefix(), i);
                ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MESSAGE);
                return;
            }
            
            ks.setKeystorePassword(keystorePasswords[i]);
            ks.setKeyPassword(keyPasswords[i]);
            
            try {
                ks.getDefaultKey();
            } catch (final KeyException e) {
                log.warn("{} Failed to unlock key strategy in collection with index {}", getLogPrefix(), i);
                ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MESSAGE);
                return;
            }
            
            i++;
        }
    }
    
}