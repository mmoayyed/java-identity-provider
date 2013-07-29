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

import java.util.List;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UsernameContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

/**
 * An action that extracts an asserted user identity from the incoming request, creates a
 * {@link UsernameContext}, and attaches it to the {@link AuthenticationContext}.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link AuthnEventIds#NO_CREDENTIALS}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class, false) != null</pre>
 * @post If ProfileRequestContext.getHttpRequest() != null, the content of either the getRemoteUser()
 * method or a designated header or attribute will be attached via a {@link UsernameContext}.
 */
public class ExtractRemoteUser extends AbstractAuthenticationAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ExtractRemoteUser.class);
    
    /** Whether to check REMOTE_USER for an identity. Defaults to true. */
    private boolean checkRemoteUser;
    
    /** List of request attributes to check for an identity. */
    @Nonnull @NonnullElements private ImmutableList<String> checkAttributes;

    /** List of request headers to check for an identity. */
    @Nonnull @NonnullElements private ImmutableList<String> checkHeaders;
    
    /** Constructor. */
    ExtractRemoteUser() {
        checkRemoteUser = true;
        checkAttributes = ImmutableList.of();
        checkHeaders = ImmutableList.of();
    }

    /**
     * Get whether to check REMOTE_USER for an identity.
     * 
     * @return  whether to check REMOTE_USER for an identity
     */
    boolean getCheckRemoteUser() {
        return checkRemoteUser;
    }
    
    /**
     * Set whether to check REMOTE_USER for an identity.
     * 
     * @param flag value to set  
     */
    void setCheckRemoteUser(boolean flag) {
        checkRemoteUser = flag;
    }
    
    /**
     * Get an immutable list of request attributes to check for an identity.
     * 
     * @return list of request attributes to check for an identity
     */
    @Nonnull @NonnullElements @Unmodifiable List<String> getCheckAttributes() {
        return checkAttributes;
    }

    /**
     * Set the list of request attributes to check for an identity.
     * 
     * @param attributes    list of request attributes to check
     */
    void setCheckAttributes(List<String> attributes) {
        checkAttributes = ImmutableList.copyOf(Collections2.filter(attributes, Predicates.notNull()));
    }
    
    /**
     * Get an immutable list of request headers to check for an identity.
     * 
     * @return list of request headers to check for an identity
     */
    @Nonnull @NonnullElements @Unmodifiable List<String> getCheckHeaders() {
        return checkHeaders;
    }

    /**
     * Set the list of request headers to check for an identity.
     * 
     * @param headers list of request headers to check
     */
    void setCheckHeaders(List<String> headers) {
        checkHeaders = ImmutableList.copyOf(Collections2.filter(headers, Predicates.notNull()));
    }
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        
        if (!getCheckRemoteUser() && getCheckAttributes().isEmpty() && getCheckHeaders().isEmpty()) {
            log.debug("{} configuration contains no headers or attributes to check", getLogPrefix());
            throw new ComponentInitializationException("ExtractRemoteUser action configuration is invalid");
        }
    }
    
    /** {@inheritDoc} */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {

        final HttpServletRequest request = profileRequestContext.getHttpRequest();
        if (request == null) {
            log.debug("{} profile request context does not contain an HttpServletRequest", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return;
        }
        
        String username;
        if (getCheckRemoteUser()) {
            username = request.getRemoteUser();
            if (username != null && !username.isEmpty()) {
                log.debug("{} user identity extracted from REMOTE_USER: {}", getLogPrefix(), username);
                authenticationContext.getSubcontext(UsernameContext.class, true).setUsername(username);
                return;
            }
        }
        
        for (String s : getCheckAttributes()) {
            Object attr = request.getAttribute(s);
            if (attr != null && !attr.toString().isEmpty()) {
                log.debug("{} user identity extracted from attribute {}: {}", getLogPrefix(), s, attr);
                authenticationContext.getSubcontext(UsernameContext.class, true).setUsername(attr.toString());
                return;
            }
        }

        for (String s : getCheckHeaders()) {
            username = request.getHeader(s);
            if (username != null && !username.isEmpty()) {
                log.debug("{} user identity extracted from header {}: {}", getLogPrefix(), s, username);
                authenticationContext.getSubcontext(UsernameContext.class, true).setUsername(username);
                return;
            }
        }
        
        log.debug("{} no user identity found in request", getLogPrefix());
        ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
    }
}