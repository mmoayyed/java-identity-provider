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

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.idp.authn.AbstractExtractionAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UsernameContext;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

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
    @Nonnull private final Logger log = LoggerFactory.getLogger(ExtractRemoteUser.class);
    
    /** Whether to check REMOTE_USER for an identity. Defaults to true. */
    private boolean checkRemoteUser;
    
    /** List of request attributes to check for an identity. */
    @Nonnull private Collection<String> checkAttributes;

    /** List of request headers to check for an identity. */
    @Nonnull private Collection<String> checkHeaders;
    
    /** Constructor. */
    public ExtractRemoteUser() {
        checkRemoteUser = true;
        checkAttributes = CollectionSupport.emptyList();
        checkHeaders = CollectionSupport.emptyList();
    }
    
    /**
     * Set whether to check REMOTE_USER for an identity.
     * 
     * @param flag value to set  
     */
    public void setCheckRemoteUser(final boolean flag) {
        checkSetterPreconditions();
        checkRemoteUser = flag;
    }

    /**
     * Set the list of request attributes to check for an identity.
     * 
     * @param attributes    list of request attributes to check
     */
    public void setCheckAttributes(@Nullable final Collection<String> attributes) {
        checkSetterPreconditions();
        checkAttributes = StringSupport.normalizeStringCollection(attributes);
    }

    /**
     * Set the list of request headers to check for an identity.
     * 
     * @param headers list of request headers to check
     */
    public void setCheckHeaders(@Nullable final Collection<String> headers) {
        checkSetterPreconditions();
        checkHeaders = StringSupport.normalizeStringCollection(headers);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (!checkRemoteUser && checkAttributes.isEmpty() && checkHeaders.isEmpty()) {
            log.debug("{} Configuration contains no headers or attributes to check", getLogPrefix());
            throw new ComponentInitializationException("ExtractRemoteUser action configuration is invalid");
        }
    }
    
// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    // CheckStyle: ReturnCount OFF
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        final HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            log.debug("{} Profile action does not contain an HttpServletRequest", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return;
        }
        
        String username;
        if (checkRemoteUser) {
            username = request.getRemoteUser();
            if (username != null && !username.isEmpty()) {
                log.debug("{} User identity extracted from REMOTE_USER: {}", getLogPrefix(), username);
                authenticationContext.ensureSubcontext(UsernameContext.class).setUsername(
                        applyTransforms(username));
                return;
            }
        }
        
        for (final String s : checkAttributes) {
            final Object attr = request.getAttribute(s);
            if (attr != null && !attr.toString().isEmpty()) {
                log.debug("{} User identity extracted from attribute {}: {}", getLogPrefix(), s, attr);
                authenticationContext.ensureSubcontext(UsernameContext.class).setUsername(
                        applyTransforms(attr.toString()));
                return;
            }
        }

        for (final String s : checkHeaders) {
            username = request.getHeader(s);
            if (username != null && !username.isEmpty()) {
                log.debug("{} User identity extracted from header {}: {}", getLogPrefix(), s, username);
                authenticationContext.ensureSubcontext(UsernameContext.class).setUsername(
                        applyTransforms(username));
                return;
            }
        }
        
        log.debug("{} No user identity found in request", getLogPrefix());
        ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
    }
    // CheckStyle: ReturnCount ON
// Checkstyle: CyclomaticComplexity ON
    
}
