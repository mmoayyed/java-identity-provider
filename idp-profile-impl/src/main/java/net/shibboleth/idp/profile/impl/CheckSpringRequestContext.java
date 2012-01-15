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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.context.ExternalContext;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.execution.Action;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * A class which check to make sure a {@link RequestContext} is setup as expected. This class is mostly used for
 * debugging purposes.
 */
@ThreadSafe
public class CheckSpringRequestContext extends AbstractIdentifiableInitializableComponent implements
        ValidatableComponent, Action {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(CheckSpringRequestContext.class);

    /** {@inheritDoc} */
    public synchronized void setId(String componentId) {
        super.setId(componentId);
    }

    /** {@inheritDoc} */
    public Event execute(final RequestContext springRequestContext) throws ProfileException {
        final ExternalContext externalContext = springRequestContext.getExternalContext();
        if (externalContext == null || !(externalContext instanceof ServletExternalContext)) {
            log.error("Action {}: Spring RequestContext did not contain a ServletExternalContext", getId());
            throw new InvalidSpringRequestContextException("Invalid Spring ExternalContext");
        }

        final HttpServletRequest httpRequest = (HttpServletRequest) externalContext.getNativeRequest();
        if (httpRequest == null) {
            log.error("Action {}: HTTP Servlet request is not available", getId());
            throw new InvalidSpringRequestContextException("HTTP Servlet request is not available");
        }

        final HttpServletResponse httpResponse = (HttpServletResponse) externalContext.getNativeResponse();
        if (httpResponse == null) {
            log.error("Action {}: HTTP Servlet response is not available", getId());
            throw new InvalidSpringRequestContextException("HTTP Servlet response is not available");
        }

        return ActionSupport.buildProceedEvent(this);
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        // nothing to do here
    }

    /** Exception thrown if there is a problem with the Spring Webflow {@link RequestContext}. */
    public static class InvalidSpringRequestContextException extends ProfileException {

        /** Serial version UID. */
        private static final long serialVersionUID = 6755119755655758329L;

        /**
         * Constructor.
         * 
         * @param message exception message
         */
        public InvalidSpringRequestContextException(String message) {
            super(message);
        }
    }
}