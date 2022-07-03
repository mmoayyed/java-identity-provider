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

package net.shibboleth.idp.authn.duo.impl;

import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.duo.DuoAuthAPI;
import net.shibboleth.idp.authn.duo.context.DuoAuthenticationContext;
import net.shibboleth.idp.ui.context.RelyingPartyUIContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.net.HttpServletSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * An action that extracts the Duo factor and device or passcode from HTTP request headers into a
 * {@link DuoAuthenticationContext}, and attaches it to the {@link AuthenticationContext}.

 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link AuthnEventIds#NO_CREDENTIALS}
 * @pre
 *      <pre>
 *      ProfileRequestContext.getSubcontext(AuthenticationContext.class) != null
 *      </pre>
 * 
 * @post If getHttpServletRequest() != null, the content of the headers are checked.
 *      The information found will be attached via a {@link DuoAuthenticationContext}.
 */
public class ExtractDuoAuthenticationFromHeaders extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ExtractDuoAuthenticationFromHeaders.class);

    /** Whether "auto" should be the default for factor and device. */
    private boolean autoAuthenticationSupported;
    
    /** Whether to trust, and extract, the client address. */
    private boolean clientAddressTrusted;

    /** Header name for factor. */
    @Nonnull @NotEmpty private String factorHeaderName;

    /** Header name for device. */
    @Nonnull @NotEmpty private String deviceHeaderName;

    /** Header name for passcode. */
    @Nonnull @NotEmpty private String passcodeHeaderName;
    
    /** Strategy function for populating pushinfo AuthAPI parameter. */
    @Nullable private Function<ProfileRequestContext,Map<String,String>> pushInfoLookupStrategy;

    /** Constructor. */
    ExtractDuoAuthenticationFromHeaders() {
        autoAuthenticationSupported = true;
        clientAddressTrusted = true;

        factorHeaderName = DuoAuthAPI.DUO_FACTOR_HEADER_NAME;
        deviceHeaderName = DuoAuthAPI.DUO_DEVICE_HEADER_NAME;
        passcodeHeaderName = DuoAuthAPI.DUO_PASSCODE_HEADER_NAME;
    }

    /**
     * Set the factor header name.
     * 
     * @param headerName the factor header name
     */
    public void setFactorHeader(@Nonnull @NotEmpty final String headerName) {
        throwSetterPreconditionExceptions();
        factorHeaderName = Constraint.isNotNull(StringSupport.trimOrNull(headerName),
                "Factor header name cannot be null or empty.");
    }

    /**
     * Set the device header name.
     * 
     * @param headerName the factor header name
     */
    public void setDeviceHeader(@Nonnull @NotEmpty final String headerName) {
        throwSetterPreconditionExceptions();
        deviceHeaderName = Constraint.isNotNull(StringSupport.trimOrNull(headerName),
                "Device header name cannot be null or empty.");
    }

    /**
     * Set the passcode header name.
     * 
     * @param headerName the factor header name
     */
    public void setPasscodeHeader(@Nonnull @NotEmpty final String headerName) {
        throwSetterPreconditionExceptions();
        passcodeHeaderName = Constraint.isNotNull(StringSupport.trimOrNull(headerName),
                "Passcode header name cannot be null or empty.");
    }

    /**
     * Get whether the client address should be trusted for use in API calls.
     * 
     * @return whether client address should be trusted
     */
    public boolean isClientAddressTrusted() {
        return clientAddressTrusted;
    }
    
    /**
     * Set whether the client address should be trusted for use in API calls.
     * 
     * @param flag flag to set
     */
    public void setClientAdddressTrusted(final boolean flag) {
        throwSetterPreconditionExceptions();
        clientAddressTrusted = flag;
    }
    
    /**
     * Get whether "auto" is the default setting.
     * 
     * @return whether "auto" is the default setting
     */
    public boolean isAutoAuthenticationSupported() {
        return autoAuthenticationSupported;
    }

    /**
     * Set whether "auto" is the default setting.
     * 
     * @param flag flag to set
     */
    public void setAutoAuthenticationSupported(final boolean flag) {
        throwSetterPreconditionExceptions();
        autoAuthenticationSupported = flag;
    }
    
    /**
     * Set lookup strategy for AuthAPI pushinfo parameter.
     * 
     * @param strategy lookup strategy
     */
    public void setPushInfoLookupStrategy(
            @Nullable final Function<ProfileRequestContext,Map<String,String>> strategy) {
        throwSetterPreconditionExceptions();
        pushInfoLookupStrategy = strategy;
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        log.debug("{} Checking for Duo authentication headers", getLogPrefix());
        
        final DuoAuthenticationContext duoCtx = new DuoAuthenticationContext();
        
        extractHeaders(duoCtx);

        if (duoCtx.getFactor() == null) {
            if (autoAuthenticationSupported && !profileRequestContext.isBrowserProfile()) {
                log.debug("{} Non-browser request with no Duo factor specified, enabling auto method", getLogPrefix());
                duoCtx.setFactor(DuoAuthAPI.DUO_FACTOR_AUTO);
            } else {
                log.debug("{} No Duo factor specified, auto method will not be attempted", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
                return;
            }
        }
        
        // Check for missing passcode.
        if (DuoAuthAPI.DUO_FACTOR_PASSCODE.equals(duoCtx.getFactor())) {
            if (duoCtx.getPasscode() == null) {
                log.warn("{} Request for passcode-based Duo login with no password supplied", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
                return;
            }
        } else if (autoAuthenticationSupported && duoCtx.getDeviceID() == null) {
            // Set auto device if needed.
            duoCtx.setDeviceID(DuoAuthAPI.DUO_DEVICE_AUTO);
        }

        // Populate pushinfo either customized or just with service name.
        if (pushInfoLookupStrategy != null) {
            final Map<String,String> pushinfo = pushInfoLookupStrategy.apply(profileRequestContext);
            if (pushinfo != null) {
                duoCtx.getPushInfo().putAll(pushinfo);
            }
        } else {
            final RelyingPartyUIContext uiCtx = authenticationContext.getSubcontext(RelyingPartyUIContext.class);
            if (uiCtx != null) {
                final String name = uiCtx.getServiceName();
                if (name != null) {
                    duoCtx.getPushInfo().put("service", uiCtx.getServiceName());
                }
            }
        }
        
        authenticationContext.addSubcontext(duoCtx, true);

        log.debug("{} Duo AuthAPI parameters extracted from request (Factor: {}, Device: {}, Passcode: {})",
                getLogPrefix(), duoCtx.getFactor(), duoCtx.getDeviceID(),
                duoCtx.getPasscode() != null ? "set" : "not set");
    }
 // Checkstyle: CyclomaticComplexity ON
    
    /**
     * Extracts the Duo API arguments passed in via the request headers.
     * 
     * @param context the DuoApiAuthContext to store the parameters in
     */
    protected void extractHeaders(@Nonnull final DuoAuthenticationContext context) {

        final HttpServletRequest httpRequest = getHttpServletRequest();
        if (httpRequest == null) {
            return;
        }
        
        if (clientAddressTrusted) {
            context.setClientAddress(HttpServletSupport.getRemoteAddr(httpRequest));
        }
        
        final String factor = httpRequest.getHeader(factorHeaderName);
        if (factor != null && !factor.isEmpty()) {
            context.setFactor(factor);
        }

        final String device = httpRequest.getHeader(deviceHeaderName);
        if (device != null && !device.isEmpty()) {
            context.setDeviceID(device);
        }

        final String passcode = httpRequest.getHeader(passcodeHeaderName);
        if (passcode != null && !passcode.isEmpty()) {
            context.setPasscode(passcode);
        }
    }

}
