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

import javax.annotation.Nonnull;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.RevocationCache;
import org.slf4j.Logger;
import org.springframework.webflow.execution.RequestContext;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.models.errors.Error;
import com.github.jasminb.jsonapi.models.errors.Errors;
import com.google.common.base.Strings;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.SpringRequestContext;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.xml.DOMTypeSupport;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

    /** Revocation Cache ID. */
    @NonnullBeforeExec @NotEmpty private String cacheId;

    /** Revocation context to operate on. */
    @NonnullBeforeExec @NotEmpty private String context;
    /** Revocation key to operate on. */
    @NonnullBeforeExec @NotEmpty private String key;

    /** {@link RevocationCache} to operate on. */
    @NonnullBeforeExec private RevocationCache revocationCache;

    /**
     * Set the JSON {@link ObjectMapper} to use for serialization.
     * 
     * @param mapper object mapper
     */
    public void setObjectMapper(@Nonnull final ObjectMapper mapper) {
        checkSetterPreconditions();
        
        objectMapper = Constraint.isNotNull(mapper, "ObjectMapper cannot be null");
    }

    /** Null safe getter.
     * @return Returns the revocationCache.
     */
    @Nonnull private RevocationCache getRevocationCache() {
        assert isPreExecuteCalled();
        return revocationCache;
    }

    /** Null safe getter.
     * @return Returns the cacheId.
     */
    @Nonnull private String getCacheId() {
        assert isPreExecuteCalled();
        return cacheId;
    }

    /** Null safe getter.
     * @return Returns the key.
     */
    @Nonnull private String getKey() {
        assert isPreExecuteCalled();
        return key;
    }

    /** Null safe getter.
     * @return Returns the context.
     */
    @Nonnull private String getContext() {
        assert isPreExecuteCalled();
        return context;
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
    protected boolean doPreExecute(final @Nonnull ProfileRequestContext profileRequestContext) {
        
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
            
            
            final String cId = cacheId = getParameter(requestContext, CACHE_ID);
            context = getParameter(requestContext, CONTEXT);
            key = getParameter(requestContext, KEY);
            
            if (Strings.isNullOrEmpty(cId) || Strings.isNullOrEmpty(context) || Strings.isNullOrEmpty(key)) {
                sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Missing revocation cache ID, context, or key",
                        "No revocation cache ID, context, key specified.");
                return false;
            }

            assert cId != null;
            revocationCache = getBean(requestContext, cId, RevocationCache.class);
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
    protected void doExecute(final @Nonnull ProfileRequestContext profileRequestContext) {

        try {
            final HttpServletRequest request = getHttpServletRequest();
            final HttpServletResponse response = getHttpServletResponse();
            assert request != null && response != null;
            final String method = request.getMethod();
            
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
            assert revocationCache!=null && context!=null && key!=null;
            final String revocation = revocationCache.getRevocationRecord(context, key);
            final HttpServletRequest request = getHttpServletRequest();
            final HttpServletResponse response = getHttpServletResponse();
            assert request != null && response != null;
            if (revocation != null) {
                response.setStatus(HttpServletResponse.SC_OK);
                final JsonFactory jsonFactory = new JsonFactory();
                try (final JsonGenerator g = jsonFactory.createGenerator(
                        response.getOutputStream()).useDefaultPrettyPrinter()) {
                    g.setCodec(objectMapper);
                    g.writeStartObject();
                    g.writeObjectFieldStart("data");
                    g.writeStringField("type", "revocation-records");
                    g.writeStringField("id", getCacheId()  + '/' + context + '/' + key);
                    g.writeObjectFieldStart("attributes");
                    g.writeStringField("revocation", revocation);
                }
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
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
        
        final HttpServletRequest request = getHttpServletRequest();
        final HttpServletResponse response = getHttpServletResponse();
        assert request != null && response != null;
        final String value = request.getParameter("value");
        final String duration = request.getParameter("duration");
        
        if (value == null) {
            sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "Request missing value parameter.");
            return;
        }
        
        Duration durationSeconds = null;
        
        if (duration != null) {
            if (duration.startsWith("P")) {
                durationSeconds = DOMTypeSupport.stringToDuration(duration);
            } else {
                try {
                    durationSeconds = Duration.ofSeconds(Long.valueOf(duration));
                } catch (final NumberFormatException e) {
                    sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request",
                            "Duration parameter was not a long integer.");
                    return;
                }
            }
        }
        
        final boolean result;
        if (durationSeconds != null) {
            result = getRevocationCache().revoke(getContext(), getKey(), value, durationSeconds);
        } else {
            result = getRevocationCache().revoke(getContext(), getKey(), value);
        }
        
        if (result) {
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
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
        final HttpServletResponse response = getHttpServletResponse();
        assert response != null;
        if (getRevocationCache().unrevoke(getContext(), getKey())) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
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
        assert response != null;
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
        response.setStatus(status);
        
        final Error e = new Error();
        final Errors errors = new Errors();
        errors.setErrors(CollectionSupport.singletonList(e));
        e.setStatus(Integer.toString(status));
        e.setTitle(title);
        e.setDetail(detail);
        
        objectMapper.writer().withDefaultPrettyPrinter().writeValue(response.getOutputStream(), errors);
    }
    
}
