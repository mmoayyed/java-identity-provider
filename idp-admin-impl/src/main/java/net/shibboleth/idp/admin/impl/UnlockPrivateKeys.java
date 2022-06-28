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

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.cryptacular.EncodingException;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.security.credential.MutableCredential;
import org.opensaml.security.crypto.KeySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * Action that creates private key objects and injects them into existing
 * {@link MutableCredential} objects.
 * 
 * <p>An error event will be signaled after the first unsuccessful unlock operation.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#INVALID_MESSAGE}
 * 
 * @since 3.4.0
 */
public class UnlockPrivateKeys extends AbstractProfileAction {
    
    /** Name of private key password parameter. */
    @Nonnull @NotEmpty public static final String KEY_PASSWORD_PARAM_NAME = "privateKeyPassword";

    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(UnlockPrivateKeys.class);
    
    /** Credentials to operate on. */
    @Nonnull @NonnullElements private Collection<MutableCredential> credentials;

    /** Keys to load. */
    @Nonnull @NonnullElements private Collection<Resource> keyResources;

    /** Constructor. */
    public UnlockPrivateKeys() {
        credentials = Collections.emptyList();
        keyResources = Collections.emptyList();
    }
    
    /**
     * Set the credentials to load keys into.
     * 
     * @param creds credentials to load keys into
     */
    public void setCredentials(@Nullable @NonnullElements final Collection<MutableCredential> creds) {
        throwSetterPreconditionExceptions();
        
        if (creds != null) {
            credentials = List.copyOf(creds);
        } else {
            credentials = Collections.emptyList();
        }
    }

    /**
     * Set the key resources to load.
     * 
     * @param keys key resources to load
     */
    public void setKeyResources(@Nullable @NonnullElements final Collection<Resource> keys) {
        throwSetterPreconditionExceptions();
        
        if (keys != null) {
            keyResources = List.copyOf(keys);
        } else {
            keyResources = Collections.emptyList();
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (keyResources.size() != credentials.size()) {
            throw new ComponentInitializationException("Size of credential and key resource collections don't match.");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(final ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext) || credentials.isEmpty() || keyResources.isEmpty()) {
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

        final String[] keyPasswords = getHttpServletRequest().getParameterValues(KEY_PASSWORD_PARAM_NAME);
        
        if (keyPasswords == null || keyPasswords.length != credentials.size()) {
            log.warn("{} Password parameter count does not match number of configured credentials", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MESSAGE);
            return;
        }
        
        int i = 0;
        final Iterator<MutableCredential> cIter = credentials.iterator();
        final Iterator<Resource> kIter = keyResources.iterator();
        while (cIter.hasNext() && kIter.hasNext()) {
            final MutableCredential cred = cIter.next();
            final Resource resource = kIter.next();
            
            if (keyPasswords[i] == null) {
                log.warn("{} Empty password supplied at index {}", getLogPrefix(), i);
                ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MESSAGE);
                return;
            }

            log.info("{} Attempting unlock of private key in {}", getLogPrefix(), resource.getDescription());
            
            try (final InputStream is = resource.getInputStream()) {
                cred.setPrivateKey(KeySupport.decodePrivateKey(is, keyPasswords[i].toCharArray()));
                log.info("{} Unlocked and injected private key in {}", getLogPrefix(), resource.getDescription());
            } catch (final KeyException | IOException | EncodingException e) {
                log.warn("{} Failed to unlock private key with index {}", getLogPrefix(), i, e);
                ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MESSAGE);
                return;
            }
            
            i++;
        }
    }
    
}