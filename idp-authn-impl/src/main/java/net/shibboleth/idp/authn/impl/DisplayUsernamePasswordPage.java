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

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.AbstractProfileAction;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Encoder;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.net.HttpServletSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.velocity.Template;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.VelocityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: internationalized pages based on Accept-Language

/**
 * An action that renders a web page for collection of a username and password.
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#IO_ERROR}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class, false) != null</pre>
 * @pre <pre>ProfileRequestContext.getHttpResponse() != null</pre>
 * @post A Velocity template is rendered to the client.
 */
public class DisplayUsernamePasswordPage extends AbstractProfileAction {
    
    /** Name of the Velocity {@link Context} attribute to which the current {@link ProfileRequestContext} is bound. */
    public static final String REQUEST_CTX_VCTX_ATTRIB = "context";

    /** Name of the Velocity {@link Context} attribute to which the ESAPI {@link Encoder} is bound. */
    public static final String ESAPI_VCTCX_ATTRIB = "encoder";

    /** Name of the Velocity {@link Context} attribute to which the URL to which the form should submit is bound. */
    public static final String ACTION_URL_VCTCX_ATTRIB = "action";

    /** Name of the Velocity {@link Context} attribute to which the name of the username form field is bound. */
    public static final String USERNAME_FIELD_VCTCX_ATTRIB = "usernameFieldName";

    /** Name of the Velocity {@link Context} attribute to which the name of the password form field is bound. */
    public static final String PASSWORD_FIELD_VCTCX_ATTRIB = "passwordFieldName";

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(DisplayUsernamePasswordPage.class);

    /** Parameter name for username. */
    @Nonnull @NotEmpty  private String usernameFieldName;

    /** Parameter name for password. */
    @Nonnull @NotEmpty private String passwordFieldName;

    /** Template used to render the username/password collection page. */
    @NonnullAfterInit private Template pageTemplate;
    
    /** Constructor. */
    DisplayUsernamePasswordPage() {
        usernameFieldName = "username";
        passwordFieldName = "password";
    }
    
    /**
     * Get the username parameter name, defaults to "username".
     * 
     * @return the username parameter name
     */
    @Nonnull @NotEmpty public String getUsernameFieldName() {
        return usernameFieldName;
    }

    /**
     * Set the username parameter name.
     * 
     * @param fieldName the username parameter name
     */
    public void setUsernameFieldName(@Nonnull @NotEmpty final String fieldName) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        usernameFieldName = Constraint.isNotNull(
                StringSupport.trimOrNull(fieldName), "Username field name cannot be null or empty");
    }

    /**
     * Get the password parameter name, defaults to "password".
     * 
     * @return the password parameter name
     */
    @Nonnull @NotEmpty public String getPasswordFieldName() {
        return passwordFieldName;
    }

    /**
     * Set the password parameter name.
     * 
     * @param fieldName the password parameter name
     */
    public void setPasswordFieldName(@Nonnull @NotEmpty final String fieldName) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        passwordFieldName = Constraint.isNotNull(
                StringSupport.trimOrNull(fieldName), "Password field name cannot be null or empty");
    }
    
    /**
     * Get the Velocity template to render.
     * 
     * @return Velocity template
     */
    @NonnullAfterInit public Template getTemplate() {
        return pageTemplate;
    }   
    
    /**
     * Set the Velocity template to render.
     * 
     * @param template Velocity template
     */
    public void setTemplate(@Nonnull final Template template) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        pageTemplate = Constraint.isNotNull(template, "Template cannot be null");
    }
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        if (pageTemplate == null) {
            throw new ComponentInitializationException("Template cannot be null");
        }
    }
    
    /** {@inheritDoc} */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {

        final HttpServletResponse httpResponse = profileRequestContext.getHttpResponse();
        if (httpResponse == null || profileRequestContext.getHttpRequest() == null) {
            log.debug("{} profile request context did not contain HttpServletRequest and HttpServletResponse",
                    getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return;
        }
        
        final Context templateContext = buildTemplateContext(profileRequestContext);

        HttpServletSupport.setContentType(httpResponse, "text/html");
        HttpServletSupport.setUTF8Encoding(httpResponse);
        HttpServletSupport.addNoCacheHeaders(httpResponse);

        try {
            pageTemplate.merge(templateContext, httpResponse.getWriter());
        } catch (IOException e) {
            log.error(getLogPrefix() + " unable to write page to HttpServletResponse writer", e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
            return;
        } catch (VelocityException e) {
            log.error(getLogPrefix() + " unable to render page", e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
            return;
        }
    }

    /**
     * Builds a Velocity {@link Context} which contains the current {@link ProfileRequestContext}.
     * 
     * @param profileRequestContext the current profile request context
     * 
     * @return the constructed Velocity context
     */
    @Nonnull protected Context buildTemplateContext(@Nonnull final ProfileRequestContext profileRequestContext) {

        final Encoder esapiEncoder = ESAPI.encoder();
        final HttpServletRequest request = profileRequestContext.getHttpRequest();

        final VelocityContext templateContext = new VelocityContext();

        templateContext.put(REQUEST_CTX_VCTX_ATTRIB, profileRequestContext);
        templateContext.put(ESAPI_VCTCX_ATTRIB, esapiEncoder);

        templateContext.put(ACTION_URL_VCTCX_ATTRIB,
                esapiEncoder.encodeForHTMLAttribute(request.getContextPath() + request.getServletPath()));

        templateContext.put(USERNAME_FIELD_VCTCX_ATTRIB, getUsernameFieldName());
        templateContext.put(PASSWORD_FIELD_VCTCX_ATTRIB, getPasswordFieldName());

        return templateContext;
    }
    
}