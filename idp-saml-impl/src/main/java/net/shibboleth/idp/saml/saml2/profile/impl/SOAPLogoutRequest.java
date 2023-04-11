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

package net.shibboleth.idp.saml.saml2.profile.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.MessageException;
import org.opensaml.messaging.context.InOutOperationContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.MessageLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.OutboundMessageContextLookup;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.messaging.soap.SAMLSOAPClientContextBuilder;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.security.SecurityException;
import org.opensaml.soap.client.SOAPClient;
import org.opensaml.soap.client.http.PipelineFactoryHttpSOAPClient;
import org.opensaml.soap.common.SOAPException;
import org.slf4j.Logger;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.session.context.LogoutPropagationContext;
import net.shibboleth.idp.session.context.LogoutPropagationContext.Result;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Profile action that propagates a prepared {@link LogoutRequest} message to an SP via the SOAP
 * binding, encapsulating SOAP pipeline construction and execution.
 * 
 * <p>The outbound message is pulled from the {@link ProfileRequestContext} to allow the surrounding
 * flow to remain largely SOAP-unaware.</p>
 * 
 * <p>Success or failure is reflected in a {@link LogoutPropagationContext} accessed via a lookup
 * strategy.</p>
 * 
 * <p>The response message is also stored off in the inbound message context.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#INVALID_MSG_CTX}
 * @event {@link EventIds#INVALID_MESSAGE}
 * @event {@link EventIds#IO_ERROR}
 * 
 * @post {@link LogoutPropagationContext#getResult()} reflects the status of the logout attempt.
 * @post profileRequestContext.getInboundMessageContext().getMessage() is populated if a response is obtained.
 * 
 * @since 4.0.0
 */
public class SOAPLogoutRequest extends AbstractProfileAction {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SOAPLogoutRequest.class);
        
    /** Lookup strategy for {@link LogoutRequest} to process. */
    @Nonnull private Function<ProfileRequestContext,LogoutRequest> logoutRequestLookupStrategy;

    /** Lookup strategy for context in which to report result. */
    @Nonnull private Function<ProfileRequestContext,LogoutPropagationContext> propagationContextLookupStrategy;
    
    /** Strategy function for access to {@link SAMLMetadataContext} for input to SOAP client. */
    @Nonnull private Function<ProfileRequestContext,SAMLMetadataContext> metadataContextLookupStrategy;

    /** Strategy function for access to {@link SAMLEndpointContext} to retrieve address from. */
    @Nonnull private Function<ProfileRequestContext,SAMLEndpointContext> endpointContextLookupStrategy;
    
    /** SOAP client. */
    @NonnullAfterInit private SOAPClient soapClient;
    
    /** The SOAP client message pipeline name. */
    @Nullable @NotEmpty private String soapPipelineName;
    
    /** LogoutRequest to process. */
    @NonnullBeforeExec private LogoutRequest logoutRequest;
    
    /** LogoutPropagationContext. */
    @NonnullBeforeExec private LogoutPropagationContext propagationContext;
    
    /** Optional metadata for use in SOAP client. */
    @Nullable private SAMLMetadataContext mdContext;
    
    /** Endpoint context to determine destination address. */
    @NonnullBeforeExec private SAMLEndpointContext epContext;
    
    /** Constructor. */
    public SOAPLogoutRequest() {
        
        final Function<ProfileRequestContext,LogoutRequest> lrls = 
                new MessageLookup<>(LogoutRequest.class).compose(new OutboundMessageContextLookup());
        assert lrls != null;
        logoutRequestLookupStrategy = lrls;

        propagationContextLookupStrategy = new ChildContextLookup<>(LogoutPropagationContext.class);
        
        // Default: outbound msg context -> SAMLPeerEntityContext -> SAMLMetadataContext
        final Function<ProfileRequestContext,SAMLMetadataContext>  mcls =
                new ChildContextLookup<>(SAMLMetadataContext.class).compose(
                        new ChildContextLookup<>(SAMLPeerEntityContext.class).compose(
                                new OutboundMessageContextLookup()));
        assert mcls!= null;
        metadataContextLookupStrategy = mcls;

        // Default: outbound msg context -> SAMLPeerEntityContext -> SAMLEndpointContext
        final Function<ProfileRequestContext,SAMLEndpointContext> ecls =
                new ChildContextLookup<>(SAMLEndpointContext.class, true).compose(
                        new ChildContextLookup<>(SAMLPeerEntityContext.class, true).compose(
                                new OutboundMessageContextLookup()));
        assert ecls != null;
        endpointContextLookupStrategy = ecls;
    }
    
    /**
     * Set the lookup strategy for the {@link LogoutRequest} to send.
     * 
     * @param strategy  lookup strategy
     */
    public void setLogoutRequestLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,LogoutRequest> strategy) {
        checkSetterPreconditions();
        logoutRequestLookupStrategy = Constraint.isNotNull(strategy, "LogoutRequest lookup strategy cannot be null");
    }

    /**
     * Set the lookup strategy for the {@link LogoutPropagationContext} to update.
     * 
     * @param strategy  lookup strategy
     */
    public void setPropagationContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,LogoutPropagationContext> strategy) {
        checkSetterPreconditions();
        propagationContextLookupStrategy =
                Constraint.isNotNull(strategy, "LogoutPropagationContext lookup strategy cannot be null");
    }
    
    /**
     * Set lookup strategy for {@link SAMLMetadataContext} for input to SOAP client.
     * 
     * @param strategy  lookup strategy
     */
    public void setMetadataContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SAMLMetadataContext> strategy) {
        checkSetterPreconditions();
        metadataContextLookupStrategy = Constraint.isNotNull(strategy,
                "SAMLMetadataContext lookup strategy cannot be null");
    }
    
    /**
     * Set lookup strategy for {@link SAMLEndpointContext} to read from.
     * 
     * @param strategy  lookup strategy
     */
    public void setEndpointContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SAMLEndpointContext> strategy) {
        checkSetterPreconditions();
        endpointContextLookupStrategy = Constraint.isNotNull(strategy,
                "SAMLEndpointContext lookup strategy cannot be null");
    }
    
    /**
     * Set the SOAP client instance.
     * 
     * @param client the SOAP client
     */
    public void setSOAPClient(@Nonnull final SOAPClient client) {
        checkSetterPreconditions();
        soapClient = Constraint.isNotNull(client, "SOAPClient cannot be null");
    }
    
    /**
     * Set the name of the specific SOAP client message pipeline to use, 
     * for example with {@link PipelineFactoryHttpSOAPClient}.
     * 
     * @param name the pipeline name, or null
     */
    public void setSOAPPipelineName(@Nullable @NotEmpty final String name) {
        checkSetterPreconditions();
        soapPipelineName = StringSupport.trimOrNull(name);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (soapClient == null) {
            throw new ComponentInitializationException("SOAPClient cannot be null");
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        propagationContext = propagationContextLookupStrategy.apply(profileRequestContext);
        if (propagationContext == null) {
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        logoutRequest = logoutRequestLookupStrategy.apply(profileRequestContext);
        if (logoutRequest == null) {
            log.warn("{} No LogoutRequest found to process", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return false;
        }

        epContext = endpointContextLookupStrategy.apply(profileRequestContext);
        final Endpoint ep = epContext == null ? null : epContext.getEndpoint(); 
        if (ep == null|| ep.getLocation() == null) {
            log.warn("{} No destination endpoint found", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return false;
        }
        
        mdContext = metadataContextLookupStrategy.apply(profileRequestContext);
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        try {
            final InOutOperationContext opContext = new SAMLSOAPClientContextBuilder<>()
                    .setOutboundMessage(logoutRequest)
                    .setProtocol(SAMLConstants.SAML20P_NS)
                    .setPipelineName(soapPipelineName)
                    .setSecurityConfigurationProfileId(profileRequestContext.getProfileId())
                    .setPeerRoleDescriptor(mdContext != null ? mdContext.getRoleDescriptor() : null)
                    .build();
            
            final Endpoint ep = epContext.getEndpoint();
            assert ep != null && opContext != null;
            
            final String dest = ep.getLocation();
            assert dest != null;
            logoutRequest.setDestination(dest);
        
            log.debug("{} Executing LogoutRequest over SOAP 1.1 binding to endpoint: {}", getLogPrefix(), dest);
            
            soapClient.send(dest, opContext);
            final MessageContext opImc = opContext.getInboundMessageContext();
            assert opImc != null;
            final Object response = opImc.getMessage();
            
            if (response == null) {
                throw new MessageException("No response message received");
            } else if (!(response instanceof LogoutResponse)) {
                throw new MessageException("Message received was not of correct type");
            }
            
            // Store off message so audit extraction works.
            // Also mock/copy SAMLBindingContext for the same reason (it's SOAP in both directions).
            final MessageContext prcImc = profileRequestContext.getInboundMessageContext();
            assert prcImc != null;
            
            prcImc.setMessage(response);
            final SAMLBindingContext bctx = prcImc.ensureSubcontext(SAMLBindingContext.class);
            final MessageContext prcOmc = profileRequestContext.getOutboundMessageContext();
            assert prcOmc != null;
            final SAMLBindingContext omcBc = prcOmc.getSubcontext(SAMLBindingContext.class);
            assert omcBc != null;
            
            bctx.setBindingDescriptor(omcBc.getBindingDescriptor());
            
            log.debug("{} Processing LogoutResponse received via SOAP 1.1 binding from endpoint: {}", getLogPrefix(),
                    logoutRequest.getDestination());
            handleResponse(profileRequestContext, (LogoutResponse) response);
        } catch (final ClassCastException e) {
            log.warn("{} SOAP message payload was not an instance of LogoutResponse", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MESSAGE);
        } catch (final MessageException | SOAPException | SecurityException e) {
            log.warn("{} SOAP logout request failed", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
        }
    }

    /**
     * Turn status from response into an appropriate result.
     * 
     * @param profileRequestContext current profile request context
     * @param response message to examine
     */
    private void handleResponse(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final LogoutResponse response) {
        final Status status = response.getStatus();
        if (status != null) {
            StatusCode code = status.getStatusCode();
            if (code != null) {
                if (StatusCode.SUCCESS.equals(code.getValue())) {
                    code = code.getStatusCode();
                    if (code == null || code.getValue() == null || !StatusCode.PARTIAL_LOGOUT.equals(code.getValue())) {
                        log.debug("{} Logout successful", getLogPrefix());
                        assert propagationContext != null;
                        propagationContext.setResult(Result.Success);
                    } else {
                        log.debug("{} Logout partially successful", getLogPrefix());
                    }
                    return;
                }
                log.warn("{} LogoutResponse received with status code '{}'", getLogPrefix(), code.getValue());
            } else {
                log.warn("{} LogoutResponse received with no status code", getLogPrefix());
            }
        } else {
            log.warn("{} LogoutResponse received with no status", getLogPrefix());
        }
    }
    
}