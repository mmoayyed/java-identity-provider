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

package net.shibboleth.idp.authn.revocation.impl;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.SpringRequestContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.RevocationCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.models.errors.Error;
import com.github.jasminb.jsonapi.models.errors.Errors;
import com.google.common.base.Strings;

/**
 * Action that implements a JSON REST API for the {@link RevocationCache} interface.
 * 
 * <p>The API supports GET, PUT/POST, and DELETE at the moment, using jsonapi.org conventions.</p>
 * 
 * <dl>
 *  <dt>GET</dt>
 *  <dd>Return a revocation record.</dd>
 *  
 *  <dt>PUT/POST</dt>
 *  <dd>Insert or update a revocation record.</dd>
 *  
 *  <dt>DELETE</dt>
 *  <dd>Delete revocation record.</dd>
 * </dl>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#IO_ERROR}
 */
public class DoRevocationCacheOperation extends AbstractProfileAction {
    
    /** Flow variable indicating ID of cache bean to access. */
    @Nonnull @NotEmpty public static final String CACHE_ID = "revocationCacheId";

    /** Flow variable indicating ID of account context. */
    @Nonnull @NotEmpty public static final String CONTEXT = "context";

    /** Flow variable indicating ID of account key. */
    @Nonnull @NotEmpty public static final String KEY = "key";

    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(DoRevocationCacheOperation.class);
    
    /** JSON object mapper. */
    @NonnullAfterInit private ObjectMapper objectMapper;

    /** Revocation context to operate on. */
    @Nullable @NotEmpty private String context;
    
    /** Revocation key to operate on. */
    @Nullable @NotEmpty private String key;

    /** {@link AccountLockoutManager} to operate on. */
    @Nullable private RevocationCache revocationCache;

    /**
     * Set the JSON {@link ObjectMapper} to use for serialization.
     * 
     * @param mapper object mapper
     */
    public void setObjectMapper(@Nonnull final ObjectMapper mapper) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        
        objectMapper = Constraint.isNotNull(mapper, "ObjectMapper cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (objectMapper == null) {
            throw new ComponentInitializationException("ObjectMapper cannot be null");
        }
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(final ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        } else if (getHttpServletRequest() == null || getHttpServletResponse() == null) {
            log.warn("{} No HttpServletRequest or HttpServletResponse available", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        try {
            final SpringRequestContext springRequestContext =
                    profileRequestContext.getSubcontext(SpringRequestContext.class);
            if (springRequestContext == null) {
                log.warn("{} Spring request context not found in profile request context", getLogPrefix());
                sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Internal Server Error", "System misconfiguration.");
                return false;
            }
    
            final RequestContext requestContext = springRequestContext.getRequestContext();
            if (requestContext == null) {
                log.warn("{} Web Flow request context not found in Spring request context", getLogPrefix());
                sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Internal Server Error", "System misconfiguration.");
                return false;
            }
            
            
            final String id = getParameter(requestContext, CACHE_ID);
            context = getParameter(requestContext, CONTEXT);
            key = getParameter(requestContext, KEY);
            
            if (Strings.isNullOrEmpty(id) || Strings.isNullOrEmpty(context) || Strings.isNullOrEmpty(key)) {
                sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Missing revocation cache ID, context, or key",
                        "No revocation cache ID, context, key specified.");
                return false;
            }

            revocationCache = getBean(requestContext, id, RevocationCache.class);
            if (revocationCache == null) {
                sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Invalid Revocation Cache", "Invalid revocation cache identifier in path.");
                return false;
            }

        } catch (final IOException e) {
            log.error("{} I/O error issuing API response", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
            return false;
        }

        return true;
    }
// Checkstyle: CyclomaticComplexity ON
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(final ProfileRequestContext profileRequestContext) {
        
        try {
            final String method = getHttpServletRequest().getMethod();
            final HttpServletResponse response = getHttpServletResponse();
            
            response.setContentType("application/json");
            response.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
            
            if ("GET".equals(method)) {
                doGet();
            } else if ("POST".equals(method) || "PUT".equals(method)) {
                doPost();
            } else if ("DELETE".equals(method)) {
                doDelete();
            } else {
                log.warn("{} Invalid method: {}", getLogPrefix(), method);
                sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                        "Unknown Operation", "Only GET, POST, and DELETE are supported.");
            }
            
        } catch (final IOException e) {
            log.error("{} I/O error responding to request", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
        }
    }

    /**
     * Get a revocation record.
     * 
     * @throws IOException if an I/O error occurs
     */
    private void doGet() throws IOException {
        try {
            final String revocation = revocationCache.getRevocationRecord(context, key);
            if (revocation != null) {
                getHttpServletResponse().setStatus(HttpServletResponse.SC_OK);
                final JsonFactory jsonFactory = new JsonFactory();
                try (final JsonGenerator g = jsonFactory.createGenerator(
                        getHttpServletResponse().getOutputStream()).useDefaultPrettyPrinter()) {
                    g.setCodec(objectMapper);
                    g.writeStartObject();
                    g.writeObjectFieldStart("data");
                    g.writeStringField("type", "revocation-records");
                    g.writeStringField("id", revocationCache.getId() + '/' + context + '/' + key);
                    g.writeObjectFieldStart("attributes");
                    g.writeStringField("revocation", revocation);
                }
            } else {
                getHttpServletResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (final IOException e) {
            sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error",
                    "Revocation cache error.");
        }
    }

    /**
     * Insert a revocation record.
     * 
     * @throws IOException if an I/O error occurs
     */
    private void doPost() throws IOException {
        
        final String value = getHttpServletRequest().getParameter("value");
        final String duration = getHttpServletRequest().getParameter("duration");
        
        if (value == null || duration == null) {
            sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "Request missing value/duration parameters.");
            return;
        }
        
        final Long durationSeconds;
        try {
            durationSeconds = Long.valueOf(duration);
        } catch (final NumberFormatException e) {
            sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "Duration parameter was not a long integer.");
            return;
        }
        
        if (revocationCache.revoke(context, key, value, Duration.ofSeconds(durationSeconds))) {
            getHttpServletResponse().setStatus(HttpServletResponse.SC_ACCEPTED);
        } else {
            sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error",
                    "Attempt to insert revocation record failed.");
        }
    }

    /**
     * Delete a revocation record.
     * 
     * @throws IOException if an I/O error occurs
     */
    private void doDelete() throws IOException {
        if (revocationCache.unrevoke(context, key)) {
            getHttpServletResponse().setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else {
            getHttpServletResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Output an error object.
     * 
     * @param status HTTP status
     * @param title fixed error description
     * @param detail human-readable error description
     * 
     * @throws IOException if unable to output the error
     */
    private void sendError(final int status, @Nonnull @NotEmpty final String title,
            @Nonnull @NotEmpty final String detail) throws IOException {
        
        final HttpServletResponse response = getHttpServletResponse();
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
        response.setStatus(status);
        
        final Error e = new Error();
        final Errors errors = new Errors();
        errors.setErrors(Collections.singletonList(e));
        e.setStatus(Integer.toString(status));
        e.setTitle(title);
        e.setDetail(detail);
        
        objectMapper.writer().withDefaultPrettyPrinter().writeValue(response.getOutputStream(), errors);
    }
    
}