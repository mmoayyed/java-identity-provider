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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.session.context.LogoutPropagationContext;
import net.shibboleth.idp.session.context.LogoutPropagationContext.Result;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.messaging.MessageException;
import org.opensaml.messaging.context.InOutOperationContext;
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
import org.opensaml.security.SecurityException;
import org.opensaml.soap.client.SOAPClient;
import org.opensaml.soap.client.http.PipelineFactoryHttpSOAPClient;
import org.opensaml.soap.common.SOAPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;

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

    /** Strategy function for access to {@link SAMLEndpointContext} to populate. */
    @Nonnull private Function<ProfileRequestContext,SAMLEndpointContext> endpointContextLookupStrategy;
    
    /** SOAP client. */
    @NonnullAfterInit private SOAPClient soapClient;
    
    /** The SOAP client message pipeline name. */
    @Nullable @NotEmpty private String soapPipelineName;
    
    /** LogoutRequest to process. */
    @Nullable private LogoutRequest logoutRequest;
    
    /** LogoutPropagationContext. */
    @Nullable private LogoutPropagationContext propagationContext;
    
    /** Optional metadata for use in SOAP client. */
    @Nullable private SAMLMetadataContext mdContext;
    
    /** Endpoint context to determine destination address. */
    @Nullable private SAMLEndpointContext epContext;
    
    /** Constructor. */
    public SOAPLogoutRequest() {
        
        logoutRequestLookupStrategy = Functions.compose(new MessageLookup<>(LogoutRequest.class),
                new OutboundMessageContextLookup());

        propagationContextLookupStrategy = new ChildContextLookup<>(LogoutPropagationContext.class);
        
        // Default: outbound msg context -> SAMLPeerEntityContext -> SAMLMetadataContext
        metadataContextLookupStrategy = Functions.compose(
                new ChildContextLookup<>(SAMLMetadataContext.class),
                Functions.compose(new ChildContextLookup<>(SAMLPeerEntityContext.class),
                        new OutboundMessageContextLookup()));

        // Default: outbound msg context -> SAMLPeerEntityContext -> SAMLEndpointContext
        endpointContextLookupStrategy = Functions.compose(
                new ChildContextLookup<>(SAMLEndpointContext.class, true),
                Functions.compose(new ChildContextLookup<>(SAMLPeerEntityContext.class, true),
                        new OutboundMessageContextLookup()));
    }
    
    /**
     * Set the lookup strategy for the {@link LogoutRequest} to send.
     * 
     * @param strategy  lookup strategy
     */
    public void setLogoutRequestLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,LogoutRequest> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        logoutRequestLookupStrategy = Constraint.isNotNull(strategy, "LogoutRequest lookup strategy cannot be null");
    }

    /**
     * Set the lookup strategy for the {@link LogoutPropagationContext} to update.
     * 
     * @param strategy  lookup strategy
     */
    public void setPropagationContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,LogoutPropagationContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
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
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
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
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        endpointContextLookupStrategy = Constraint.isNotNull(strategy,
                "SAMLEndpointContext lookup strategy cannot be null");
    }
    
    /**
     * Set the SOAP client instance.
     * 
     * @param client the SOAP client
     */
    public void setSOAPClient(@Nonnull final SOAPClient client) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        soapClient = Constraint.isNotNull(client, "SOAPClient cannot be null");
    }
    
    /**
     * Set the name of the specific SOAP client message pipeline to use, 
     * for example with {@link PipelineFactoryHttpSOAPClient}.
     * 
     * @param name the pipeline name, or null
     */
    public void setSOAPPipelineName(@Nullable @NotEmpty final String name) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
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
        if (epContext == null || epContext.getEndpoint() == null || epContext.getEndpoint().getLocation() == null) {
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
            final InOutOperationContext<LogoutResponse,LogoutRequest> opContext = new SAMLSOAPClientContextBuilder()
                    .setOutboundMessage(logoutRequest)
                    .setProtocol(SAMLConstants.SAML20P_NS)
                    .setPipelineName(soapPipelineName)
                    .setSecurityConfigurationProfileId(profileRequestContext.getProfileId())
                    .setPeerRoleDescriptor(mdContext != null ? mdContext.getRoleDescriptor() : null)
                    .build();
            
            logoutRequest.setDestination(epContext.getEndpoint().getLocation());
        
            log.debug("{} Executing LogoutRequest over SOAP 1.1 binding to endpoint: {}", getLogPrefix(),
                    logoutRequest.getDestination());
            
            soapClient.send(logoutRequest.getDestination(), opContext);
            final LogoutResponse response = opContext.getInboundMessageContext().getMessage();
            
            if (response == null) {
                throw new MessageException("No response message received");
            }
            
            // Store off message so audit extraction works.
            // Also mock/copy SAMLBindingContext for the same reason (it's SOAP in both directions).
            profileRequestContext.getInboundMessageContext().setMessage(response);
            final SAMLBindingContext bctx =
                    profileRequestContext.getInboundMessageContext().getSubcontext(SAMLBindingContext.class, true);
            bctx.setBindingDescriptor(
                    profileRequestContext.getOutboundMessageContext().getSubcontext(
                            SAMLBindingContext.class).getBindingDescriptor());
            
            log.debug("{} Processing LogoutResponse received via SOAP 1.1 binding from endpoint: {}", getLogPrefix(),
                    logoutRequest.getDestination());
            handleResponse(profileRequestContext, response);
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
            final StatusCode code = status.getStatusCode();
            if (code != null) {
                if (StatusCode.SUCCESS.equals(code.getValue())) {
                    log.debug("{} LogoutResponse was successful", getLogPrefix());
                    propagationContext.setResult(Result.Success);
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