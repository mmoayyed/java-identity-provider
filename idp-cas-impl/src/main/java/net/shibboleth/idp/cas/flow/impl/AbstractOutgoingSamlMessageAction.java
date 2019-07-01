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
import javax.xml.namespace.QName;

import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.idp.cas.protocol.TicketValidationResponse;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.binding.BindingDescriptor;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.opensaml.saml.saml1.core.Response;
import org.opensaml.soap.messaging.context.SOAP11Context;
import org.opensaml.soap.soap11.Body;
import org.opensaml.soap.soap11.Envelope;
import org.opensaml.soap.util.SOAPConstants;

/**
 * Base class for all actions that build SAML {@link Response} messages for output.
 *
 * @author Marvin S. Addison
 */
public abstract class AbstractOutgoingSamlMessageAction extends
        AbstractCASProtocolAction<TicketValidationRequest, TicketValidationResponse> {

    /** CAS namespace. */
    @Nonnull @NotEmpty protected static final String NAMESPACE = "http://www.ja-sig.org/products/cas/";
    
    /** SOAP envelope needed for old/broken CAS clients. */
    @Nonnull
    private QName envelopeName = new QName(SOAPConstants.SOAP11_NS, Envelope.DEFAULT_ELEMENT_LOCAL_NAME, "SOAP-ENV");

    /** SOAP body needed for old/broken CAS clients. */
    @Nonnull private QName bodyName = new QName(SOAPConstants.SOAP11_NS, Body.DEFAULT_ELEMENT_LOCAL_NAME, "SOAP-ENV");
    
    /** Descriptor for outgoing SOAP binding. */
    @NonnullAfterInit private BindingDescriptor outgoingBinding;
    
    /**
     * Set the {@link BindingDescriptor} describing the outbound binding to use.
     * 
     * @param descriptor the descriptor
     * 
     * @since 4.0.0
     */
    public void setOutgoingBinding(@Nonnull final BindingDescriptor descriptor) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        outgoingBinding = Constraint.isNotNull(descriptor, "Outgoing BindingDescriptor cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (outgoingBinding == null) {
            throw new ComponentInitializationException("Outgoing BindingDescriptor cannot be null");
        }
    }

    /**
     * Build the SAML object.
     * 
     * @param type the type of SAML object being built
     * @param elementName name of the 
     * @param <T> type of SAML object
     * 
     * @return SAML object
     */
    @Nonnull protected static <T extends SAMLObject> T newSAMLObject(final Class<T> type, final QName elementName) {
        final SAMLObjectBuilder<T> builder = (SAMLObjectBuilder<T>) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .<T> getBuilderOrThrow(elementName);
        return builder.buildObject();
    }

    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final MessageContext<SAMLObject> msgContext = new MessageContext<>();
        try {
            msgContext.setMessage(buildSamlResponse(profileRequestContext));
        } catch (final EventException e) {
            ActionSupport.buildEvent(profileRequestContext, ProtocolError.IllegalState.event(this));
            return;
        }
        final SAMLBindingContext bindingContext = new SAMLBindingContext();
        bindingContext.setBindingUri(outgoingBinding.getId());
        bindingContext.setBindingDescriptor(outgoingBinding);
        msgContext.addSubcontext(bindingContext);

        // Ensure message uses SOAP-ENV ns prefix required by old/broken CAS clients
        final Envelope envelope = (Envelope) XMLObjectSupport.buildXMLObject(envelopeName);
        envelope.setBody((Body) XMLObjectSupport.buildXMLObject(bodyName));
        final SOAP11Context soapCtx = new SOAP11Context();
        soapCtx.setEnvelope(envelope);
        msgContext.addSubcontext(soapCtx);

        profileRequestContext.setOutboundMessageContext(msgContext);
    }

    /**
     * Build the SAML response.
     * 
     * @param profileRequestContext profile request context
     * 
     * @return SAML response
     * 
     * @throws EventException to signal an event
     */
    @Nonnull protected abstract Response buildSamlResponse(
            @Nonnull final ProfileRequestContext<SAMLObject,SAMLObject> profileRequestContext) throws EventException;

}