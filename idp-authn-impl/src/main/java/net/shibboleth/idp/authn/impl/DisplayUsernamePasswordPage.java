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
import javax.annotation.concurrent.ThreadSafe;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthenticationRequestContext;
import net.shibboleth.idp.profile.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.net.HttpServletSupport;
import net.shibboleth.utilities.java.support.velocity.Template;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.VelocityException;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

//TODO(lajoie) internationalized pages based on Accept-Language

/** A stage which renders a username/password collection page. */
@ThreadSafe
public class DisplayUsernamePasswordPage extends AbstractAuthenticationAction {

    /** Name of the Velocity {@link Context} attribute to which the current {@link ProfileRequestContext} is bound. */
    public static final String REQUEST_CTX_VCTX_ATTRIB = "context";

    /**
     * Name of the Velocity {@link Context} attribute to which the URL to which the username/password form should submit
     * is bound. Note, this URL has already been properly encoded for inclusion in the form's action attribute.
     */
    public static final String ACTION_URL_VCTCX_ATTRIB = "usernameFieldName";

    /** Name of the Velocity {@link Context} attribute to which the name of the username form field is bound. */
    public static final String USERNAME_FIELD_VCTCX_ATTRIB = "usernameFieldName";

    /** Name of the Velocity {@link Context} attribute to which the name of the password form field is bound. */
    public static final String PASSWORD_FIELD_VCTCX_ATTRIB = "passwordFieldName";

    /** Name of the HTML form field that will carry the username. */
    public static final String USERNAME_FIELD_NAME = "username";

    /** Name of the HTML form field that will carry the password. */
    public static final String PASSWORD_FIELD_NAME = "password";

    /** Template used to render the username/password collection page. */
    private final Template pageTemplate;

    /**
     * Constructor.
     * 
     * @param template template used to render the collection page
     */
    public DisplayUsernamePasswordPage(@Nonnull final Template template) {
        setId(DisplayUsernamePasswordPage.class.getName());

        pageTemplate = Constraint.isNotNull(template, "Page template cannot be null");
    }

    /** {@inheritDoc} */
    protected Event doExecute(@Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationRequestContext authenticationContext) throws AuthenticationException {

        final HttpServletResponse httpResponse =
                Constraint.isNotNull(profileRequestContext.getHttpResponse(), "HttpServletResponse cannot be null");
        
        final Context templateContext = buildTemplateContext(profileRequestContext);

        HttpServletSupport.setContentType(httpResponse, "text/html");
        HttpServletSupport.setUTF8Encoding(httpResponse);
        HttpServletSupport.addNoCacheHeaders(httpResponse);

        try {
            pageTemplate.merge(templateContext, httpResponse.getWriter());
        } catch (IOException e) {
            throw new InvalidTemplateException("Unable to write page to HttpServletResponse writer", e);
        } catch (VelocityException e) {
            throw new InvalidTemplateException("Unable to render page", e);
        }

        return ActionSupport.buildProceedEvent(this);
    }

    /**
     * Builds a Velocity {@link Context} which contains the current {@link ProfileRequestContext}.
     * 
     * @param profileRequestContext the current profile request context
     * 
     * @return the constructed Velocity context
     */
    @Nonnull protected Context buildTemplateContext(@Nonnull final ProfileRequestContext profileRequestContext) {

        VelocityContext templateContext = new VelocityContext();

        templateContext.put(REQUEST_CTX_VCTX_ATTRIB, templateContext);

        // TODO(lajoie) need to generate, encode and add submission form action URL to velocity context
        templateContext.put(ACTION_URL_VCTCX_ATTRIB, null);

        templateContext.put(USERNAME_FIELD_VCTCX_ATTRIB, USERNAME_FIELD_NAME);
        templateContext.put(PASSWORD_FIELD_VCTCX_ATTRIB, PASSWORD_FIELD_NAME);

        return templateContext;
    }

    /** Exception thrown when there is a problem rendering the username/password collection page. */
    public static class InvalidTemplateException extends AuthenticationException {

        /** Serial version UID. */
        private static final long serialVersionUID = -4206933885462310219L;

        /** Constructor. */
        public InvalidTemplateException() {
            super();
        }

        /**
         * Constructor.
         * 
         * @param message exception message
         */
        public InvalidTemplateException(String message) {
            super(message);
        }

        /**
         * Constructor.
         * 
         * @param wrappedException exception to be wrapped by this one
         */
        public InvalidTemplateException(Exception wrappedException) {
            super(wrappedException);
        }

        /**
         * Constructor.
         * 
         * @param message exception message
         * @param wrappedException exception to be wrapped by this one
         */
        public InvalidTemplateException(String message, Exception wrappedException) {
            super(message, wrappedException);
        }
    }
}