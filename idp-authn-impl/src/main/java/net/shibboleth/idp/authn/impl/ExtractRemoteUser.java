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

package net.shibboleth.idp.authn.impl;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import net.shibboleth.idp.authn.AbstractExtractionAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UsernameContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * An action that extracts an asserted user identity from the incoming request, creates a
 * {@link UsernameContext}, and attaches it to the {@link AuthenticationContext}.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link AuthnEventIds#NO_CREDENTIALS}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class, false) != null</pre>
 * @post If getHttpServletRequest() != null, the content of either the getRemoteUser()
 * method or a designated header or attribute will be attached via a {@link UsernameContext}.
 */
public class ExtractRemoteUser extends AbstractExtractionAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ExtractRemoteUser.class);
    
    /** Whether to check REMOTE_USER for an identity. Defaults to true. */
    private boolean checkRemoteUser;
    
    /** List of request attributes to check for an identity. */
    @Nonnull @NonnullElements private List<String> checkAttributes;

    /** List of request headers to check for an identity. */
    @Nonnull @NonnullElements private List<String> checkHeaders;
    
    /** Constructor. */
    ExtractRemoteUser() {
        checkRemoteUser = true;
        checkAttributes = Collections.emptyList();
        checkHeaders = Collections.emptyList();
    }
    
    /**
     * Set whether to check REMOTE_USER for an identity.
     * 
     * @param flag value to set  
     */
    void setCheckRemoteUser(boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        checkRemoteUser = flag;
    }

    /**
     * Set the list of request attributes to check for an identity.
     * 
     * @param attributes    list of request attributes to check
     */
    void setCheckAttributes(@Nonnull @NonnullElements final List<String> attributes) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        checkAttributes = Lists.newArrayList(Collections2.filter(attributes, Predicates.notNull()));
    }

    /**
     * Set the list of request headers to check for an identity.
     * 
     * @param headers list of request headers to check
     */
    void setCheckHeaders(@Nonnull @NonnullElements final List<String> headers) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        checkHeaders = Lists.newArrayList(Collections2.filter(headers, Predicates.notNull()));
    }
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        
        if (!checkRemoteUser && checkAttributes.isEmpty() && checkHeaders.isEmpty()) {
            log.debug("{} configuration contains no headers or attributes to check", getLogPrefix());
            throw new ComponentInitializationException("ExtractRemoteUser action configuration is invalid");
        }
    }
    
    // Checkstyle: CyclomaticComplexity OFF
    
    /** {@inheritDoc} */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {

        final HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            log.debug("{} profile action does not contain an HttpServletRequest", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return;
        }
        
        String username;
        if (checkRemoteUser) {
            username = request.getRemoteUser();
            if (username != null && !username.isEmpty()) {
                log.debug("{} user identity extracted from REMOTE_USER: {}", getLogPrefix(), username);
                authenticationContext.getSubcontext(UsernameContext.class, true).setUsername(
                        applyTransforms(username));
                return;
            }
        }
        
        for (String s : checkAttributes) {
            Object attr = request.getAttribute(s);
            if (attr != null && !attr.toString().isEmpty()) {
                log.debug("{} user identity extracted from attribute {}: {}", getLogPrefix(), s, attr);
                authenticationContext.getSubcontext(UsernameContext.class, true).setUsername(
                        applyTransforms(attr.toString()));
                return;
            }
        }

        for (String s : checkHeaders) {
            username = request.getHeader(s);
            if (username != null && !username.isEmpty()) {
                log.debug("{} user identity extracted from header {}: {}", getLogPrefix(), s, username);
                authenticationContext.getSubcontext(UsernameContext.class, true).setUsername(
                        applyTransforms(username));
                return;
            }
        }
        
        log.debug("{} no user identity found in request", getLogPrefix());
        ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
    }

    // Checkstyle: CyclomaticComplexity ON
    
}