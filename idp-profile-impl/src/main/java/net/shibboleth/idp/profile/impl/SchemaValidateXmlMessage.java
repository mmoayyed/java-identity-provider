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

package net.shibboleth.idp.profile.impl;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.InvalidInboundMessageException;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.logic.Assert;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectProviderRegistrySupport;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** An action that schema validates inbound XML messages. */
public class SchemaValidateXmlMessage extends AbstractIdentityProviderAction<XMLObject, Object> {

    /** Class logger. */
    private Logger log = LoggerFactory.getLogger(SchemaValidateXmlMessage.class);

    /** Schema used to validate incoming messages. */
    private Schema validationSchema;

    /**
     * Gets the schema used to validate incoming messages.
     * 
     * @return schema used to validate incoming messages, not null after action is initialized
     */
    public Schema getValidationSchema() {
        return validationSchema;
    }

    /**
     * Sets the schema used to validate incoming messages.
     * 
     * @param schema schema used to validate incoming messages
     */
    public synchronized void setValidationSchema(@Nonnull final Schema schema) {
        if (isInitialized()) {
            return;
        }

        validationSchema = Assert.isNotNull(schema);
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        super.validate();

        if (validationSchema == null) {
            throw new ComponentValidationException("No validation schema specified");
        }
    }

    /** {@inheritDoc} */
    protected Event doExecute(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            RequestContext springRequestContext, ProfileRequestContext<XMLObject, Object> profileRequestContext)
            throws ProfileException {

        log.debug("Action {}: Attempting to schema validate incoming message", getId());

        final XMLObject request = ActionSupport.getRequiredInboundMessage(this, profileRequestContext);

        final Document requestDoc = getRequestDom(request);

        final Validator schemaValidator = validationSchema.newValidator();

        try {
            schemaValidator.validate(new DOMSource(requestDoc));
        } catch (Exception e) {
            log.debug("Action {}: Incoming request {} is not schema valid",
                    new Object[] {getId(), request.getElementQName(), e});
            throw new SchemaInvalidMessageException(e.getMessage());
        }

        log.debug("Action {}: Incoming message is valid", getId());
        return ActionSupport.buildProceedEvent(this);
    }

    /**
     * Gets the {@link Document} of the incoming XML request.
     * 
     * @param request the incoming request
     * 
     * @return the {@link Document} of the incoming XML request
     * 
     * @throws InvalidInboundMessageException thrown if the given XMLObject does not have an associated DOM and can not
     *             be marshalled
     */
    protected Document getRequestDom(final XMLObject request) throws InvalidInboundMessageException {
        Element requestDom = request.getDOM();
        if (requestDom != null) {
            return requestDom.getOwnerDocument();
        }

        final Marshaller marshaller = XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(request);
        if (marshaller == null) {
            log.debug("Action {}: No marshaller available for incoming request {}", getId(), request.getElementQName());
            throw new InvalidInboundMessageException("No marshaller available for incoming request");
        }

        try {
            requestDom = marshaller.marshall(request);
        } catch (MarshallingException e) {
            log.debug("Action {}: Unable to marshall incoming request {}",
                    new Object[] {getId(), request.getElementQName(), e});
            throw new InvalidInboundMessageException("Unable to marshall inbound request", e);
        }

        return requestDom.getOwnerDocument();
    }

    /** Exception thrown if an incoming message is not schema valid. */
    public static class SchemaInvalidMessageException extends ProfileException {

        /** Serial version UID. */
        private static final long serialVersionUID = 7892341018763645081L;

        /**
         * Constructor.
         * 
         * @param message exception message
         */
        public SchemaInvalidMessageException(String message) {
            super(message);
        }

    }
}