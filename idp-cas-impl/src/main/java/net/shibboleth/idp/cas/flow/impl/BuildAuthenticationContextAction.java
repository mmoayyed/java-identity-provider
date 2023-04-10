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

package net.shibboleth.idp.cas.flow.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.cas.config.ConfigLookupFunction;
import net.shibboleth.idp.cas.config.LoginConfiguration;
import net.shibboleth.idp.cas.protocol.ServiceTicketRequest;
import net.shibboleth.idp.cas.protocol.ServiceTicketResponse;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventException;
import org.opensaml.profile.context.ProfileRequestContext;

/**
 * Builds an authentication context from an incoming {@link ServiceTicketRequest} message.
 *
 * @author Marvin S. Addison
 */
public class BuildAuthenticationContextAction
        extends AbstractCASProtocolAction<ServiceTicketRequest,ServiceTicketResponse> {

    /** Profile configuration lookup function. */
    @Nonnull private final ConfigLookupFunction<LoginConfiguration> configLookupFunction;

    /** Stores off CAS request. */
    @NonnullBeforeExec private ServiceTicketRequest request;
    
    /** Constructor. */
    public BuildAuthenticationContextAction() {
        configLookupFunction = new ConfigLookupFunction<>(LoginConfiguration.class);
    }
    
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        try {
            request = getCASRequest(profileRequestContext);
        } catch (final EventException e) {
            ActionSupport.buildEvent(profileRequestContext, e.getEventID());
            return false;
        }
        
        return true;
    }
    
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final AuthenticationContext ac = new AuthenticationContext();
        
        ac.setForceAuthn(request.isRenew());

        final LoginConfiguration config = configLookupFunction.apply(profileRequestContext);

        if (!ac.isForceAuthn()) {
            if (config != null) {
                ac.setForceAuthn(config.isForceAuthn(profileRequestContext));
            }
        }
        
        if (!ac.isForceAuthn()) {
            ac.setIsPassive(request.isGateway());
        }
        
        if (config != null) {
            ac.setProxyCount(config.getProxyCount(profileRequestContext));
        }
        
        profileRequestContext.addSubcontext(ac, true);
        profileRequestContext.setBrowserProfile(true);
    }
    
}