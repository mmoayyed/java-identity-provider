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

import java.net.InetAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import com.google.common.base.Strings;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UserAgentContext;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.profile.IdPAuditFields;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.net.IPRange;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * An action that ensures that a user-agent address found within a {@link UserAgentContext}
 * is within a given range and generates an {@link net.shibboleth.idp.authn.AuthenticationResult}.
 *  
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link AuthnEventIds#NO_CREDENTIALS}
 * @event {@link AuthnEventIds#INVALID_CREDENTIALS}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class, false).getAttemptedFlow() != null</pre>
 * @post If AuthenticationContext.getSubcontext(UserAgentContext.class, false) != null, and the content of getAddress()
 * satisfies a configured address range, an {@link net.shibboleth.idp.authn.AuthenticationResult} is saved to the
 * {@link AuthenticationContext}.
 */
public class ValidateUserAgentAddress extends AbstractAuditingValidationAction {

    /** Default prefix for metrics. */
    @Nonnull @NotEmpty private static final String DEFAULT_METRIC_NAME = "net.shibboleth.idp.authn.address";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ValidateUserAgentAddress.class);

    /** Map of IP ranges to principal names. */
    @Nonnull private Map<String,Collection<IPRange>> mappings;

    /** User Agent context containing address to evaluate. */
    @NonnullBeforeExec private UserAgentContext uaContext;
    
    /** The principal name established by the action, if any. */
    @Nullable private String principalName;
    
    /** Constructor. */
    public ValidateUserAgentAddress() {
        setMetricName(DEFAULT_METRIC_NAME);
        mappings = CollectionSupport.emptyMap();
    }
    
    /**
     * Set the IP range(s) to authenticate as particular principals.
     * 
     * @param newMappings the IP range(s) to authenticate as particular principals
     */
    public void setMappings(@Nullable final Map<String,Collection<IPRange>> newMappings) {
        checkSetterPreconditions();
        if (newMappings != null) {
            mappings = new HashMap<>(newMappings.size());
            for (final Map.Entry<String,Collection<IPRange>> e : newMappings.entrySet()) {
                if (!Strings.isNullOrEmpty(e.getKey())) {
                    mappings.put(e.getKey(), List.copyOf(e.getValue()));
                }
            }
        } else {
            mappings = CollectionSupport.emptyMap();
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
            return false;
        }

        uaContext = authenticationContext.getSubcontext(UserAgentContext.class);
        if (uaContext == null) {
            log.debug("{} No UserAgentContext available within authentication context", getLogPrefix());
            handleError(profileRequestContext, authenticationContext, AuthnEventIds.NO_CREDENTIALS,
                    AuthnEventIds.NO_CREDENTIALS);
            return false;
        }

        if (uaContext.getAddress() == null) {
            log.debug("{} No address available within UserAgentContext", getLogPrefix());
            handleError(profileRequestContext, authenticationContext, AuthnEventIds.NO_CREDENTIALS,
                    AuthnEventIds.NO_CREDENTIALS);
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        final InetAddress addr = uaContext.getAddress();
        assert addr != null;
        for (final Map.Entry<String,Collection<IPRange>> e : mappings.entrySet()) {
            final Collection<IPRange> ranges = e.getValue();
            assert ranges != null;
            if (isAuthenticated(addr, ranges)) {
                principalName = e.getKey();
                log.info("{} Authenticated user agent with address {} as {}",
                        getLogPrefix(), addr.getHostAddress(), principalName);
                recordSuccess(profileRequestContext);
                buildAuthenticationResult(profileRequestContext, authenticationContext);
                return;
            }
        }

        log.debug("{} User agent with address {} was not authenticated", getLogPrefix(),
                addr.getHostAddress());
        handleError(profileRequestContext, authenticationContext, AuthnEventIds.INVALID_CREDENTIALS,
                AuthnEventIds.INVALID_CREDENTIALS);
        recordFailure(profileRequestContext);
    }

    /**
     * Checks whether the given IP address meets a set of IP range requirements.
     * 
     * @param address the IP address to check
     * @param ranges the ranges to check
     * 
     * @return true if the given IP address meets this stage's IP range requirements, false otherwise
     */
    private boolean isAuthenticated(@Nonnull final InetAddress address, @Nonnull final Collection<IPRange> ranges) {
        final byte[] resolvedAddress = address.getAddress();
        assert resolvedAddress != null;
        for (final IPRange range : ranges) {
            if (range.contains(resolvedAddress)) {
                return true;
            }
        }
        
        return false;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected Subject populateSubject(@Nonnull final Subject subject) {
        assert principalName != null;
        final UsernamePrincipal principal = new UsernamePrincipal(principalName);
        subject.getPrincipals().add(principal);
        return subject;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable @Unmodifiable @NotLive protected Map<String,String> getAuditFields(
            @Nonnull final ProfileRequestContext profileRequestContext) {
        if (principalName != null) {
            return CollectionSupport.singletonMap(IdPAuditFields.USERNAME, principalName);
        }
        
        return super.getAuditFields(profileRequestContext);
    }

}