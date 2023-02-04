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

package net.shibboleth.idp.admin.impl;

import java.io.IOException;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageService;
import org.opensaml.storage.VersionMismatchException;
import org.slf4j.Logger;
import org.springframework.webflow.execution.RequestContext;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.models.errors.Error;
import com.github.jasminb.jsonapi.models.errors.Errors;
import com.google.common.base.Strings;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.SpringRequestContext;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Action that implements a JSON REST API for accessing {@link StorageService} records.
 * 
 * <p>The API supports GET and DELETE at the moment, using jsonapi.org conventions.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#IO_ERROR}
 */
public class DoStorageOperation extends AbstractProfileAction {
    
    /** Flow variable indicating ID of storage service bean to reload. */
    @Nonnull @NotEmpty public static final String SERVICE_ID = "storageServiceId";

    /** Flow variable indicating ID of storage context. */
    @Nonnull @NotEmpty public static final String CONTEXT = "context";

    /** Flow variable indicating ID of storage key. */
    @Nonnull @NotEmpty public static final String KEY = "key";

    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(DoStorageOperation.class);
    
    /** JSON object mapper. */
    @NonnullAfterInit private ObjectMapper objectMapper;
    
    /** {@link StorageService} to operate on. */
    @Nullable private StorageService storageService;
    
    /** Storage context to operate on. */
    @Nullable @NotEmpty private String context;

    /** Storage key to operate on. */
    @Nullable @NotEmpty private String key;

    /**
     * Set the JSON {@link ObjectMapper} to use for serialization.
     * 
     * @param mapper object mapper
     */
    public void setObjectMapper(@Nonnull final ObjectMapper mapper) {
        checkSetterPreconditions();
        objectMapper = Constraint.isNotNull(mapper, "ObjectMapper cannot be null");
    }
    
    /**
     * Sets the {@link StorageService} to use.
     * 
     * <p>Primarily for testing, to bypass use of Spring to obtain the service to use.</p>
     * 
     * @param storage storage service
     */
    public void setStorageService(@Nullable final StorageService storage) {
        checkSetterPreconditions();
        
        storageService = storage;
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
            
            if (storageService == null) {
                storageService = getStorageService(requestContext);
                if (storageService == null) {
                    sendError(HttpServletResponse.SC_NOT_FOUND,
                            "Invalid Storage Service", "Invalid storage service identifier in path.");
                    return false;
                }
            }
            
            context = (String) requestContext.getFlowScope().get(CONTEXT);
            key = (String) requestContext.getFlowScope().get(KEY);
            if (Strings.isNullOrEmpty(context) || Strings.isNullOrEmpty(key)) {
                sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Missing Context or Key", "No context or key specified.");
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
    @Override protected void doExecute(final ProfileRequestContext profileRequestContext) {
        
        try {
            final HttpServletRequest request = getHttpServletRequest();
            final HttpServletResponse response = getHttpServletResponse();
            
            response.setContentType("application/json");
            response.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
            
            if ("GET".equals(request.getMethod())) {
                doRead();
            } else if ("PUT".equals(request.getMethod())) {
                doCreate();
            } else if ("POST".equals(request.getMethod())) {
                doUpdate();
            } else if ("DELETE".equals(request.getMethod())) {
                doDelete();
            } else {
                log.warn("{} Invalid method: {}", getLogPrefix(), request.getMethod());
                sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                        "Unknown Operation", "GET, PUT, POST, DELETE are supported.");
            }
        } catch (final IOException e) {
            log.error("{} I/O error responding to request", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
        }
    }

    /**
     * Helper method to get the storage service bean to operate on.
     * 
     * @param requestContext current SWF request context
     * 
     * @return storage service or null
     */
    @Nullable private StorageService getStorageService(@Nonnull final RequestContext requestContext) {
        
        final String id = (String) requestContext.getFlowScope().get(SERVICE_ID);
        if (id == null) {
            log.warn("{} No {} flow variable found in request", getLogPrefix(), SERVICE_ID);
            return null;
        }
        
        return getBean(requestContext, id, StorageService.class);
    }
    
    /**
     * Perform read operation.
     * 
     * @throws IOException if an error is raised
     */
    private void doRead() throws IOException {
        final StorageRecord<?> record;
        try {
            record = storageService.read(context, key);
            if (record != null) {
                getHttpServletResponse().setStatus(HttpServletResponse.SC_OK);
                final JsonFactory jsonFactory = new JsonFactory();
                try (final JsonGenerator g = jsonFactory.createGenerator(
                        getHttpServletResponse().getOutputStream()).useDefaultPrettyPrinter()) {
                    g.setCodec(objectMapper);
                    g.writeStartObject();
                    g.writeObjectFieldStart("data");
                    g.writeStringField("type", "records");
                    g.writeStringField("id", storageService.getId() + '/' + context +'/' + key);
                    g.writeObjectFieldStart("attributes");
                    g.writeStringField("value", record.getValue());
                    g.writeNumberField("version", record.getVersion());
                    if (record.getExpiration() != null) {
                        g.writeFieldName("expiration");
                        g.writeObject(record.getExpiration());
                    }
                }
            } else {
                sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Record Not Found", "The specified record was not present or has expired.");
            }
        } catch (final IOException e) {
            sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "Storage error.");
        }
    }
    
    
    /**
     * Perform create operation.
     * 
     * @throws IOException if an error is raised
     */
    private void doCreate() throws IOException {
        final JsonFactory jsonFactory = new JsonFactory();
        final JsonParser parser = jsonFactory.createParser(getHttpServletRequest().getInputStream());
        
        if (parser.nextToken() != JsonToken.START_OBJECT) {
            throw new IOException("Expected data to start with an Object");
        }
        
        String value = null;
        Long exp = null;
        
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            final String fieldName = parser.getCurrentName();
            parser.nextToken();
            if ("value".equals(fieldName)) {
                value = parser.getText();
            } else if ("expiration".equals(fieldName)) {
                exp = parser.getLongValue();
            }
        }
        
        if (value == null) {
            throw new IOException("Input missing 'val' field");
        }
        
        if (storageService.create(context, key, value, exp)) {
            getHttpServletResponse().setStatus(HttpServletResponse.SC_CREATED);
        } else {
            sendError(HttpServletResponse.SC_CONFLICT, "Duplicate Record",
                    "Context and key matched an existing record.");
        }
    }

// Checkstyle: CyclomaticComplexity OFF
    /**
     * Perform update operation.
     * 
     * @throws IOException if an error is raised
     */
    private void doUpdate() throws IOException {
        final JsonFactory jsonFactory = new JsonFactory();
        final JsonParser parser = jsonFactory.createParser(getHttpServletRequest().getInputStream());
        
        if (parser.nextToken() != JsonToken.START_OBJECT) {
            throw new IOException("Expected data to start with an Object");
        }
        
        String value = null;
        Long version = null;
        Long exp = null;
        
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            final String fieldName = parser.getCurrentName();
            parser.nextToken();
            if ("value".equals(fieldName)) {
                value = parser.getText();
            } else if ("expiration".equals(fieldName)) {
                exp = parser.getLongValue();
            } else if ("version".equals(fieldName)) {
                version = parser.getLongValue();
            }
        }
        
        if (value == null) {
            throw new IOException("Input missing 'value' field");
        }
        
        if (version != null) {
            try {
                version = storageService.updateWithVersion(version, context, key, value, exp);
                if (version != null) {
                    getHttpServletResponse().setStatus(HttpServletResponse.SC_OK);
                } else {
                    sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found", "Record to update was absent.");
                }
            } catch (final VersionMismatchException e) {
                sendError(HttpServletResponse.SC_CONFLICT, "Version Mismatch", "Record version did not match.");
            }
        } else {
            if (storageService.update(context, key, value, exp)) {
                getHttpServletResponse().setStatus(HttpServletResponse.SC_OK);
            } else if (storageService.create(context, key, value, exp)) {
                getHttpServletResponse().setStatus(HttpServletResponse.SC_CREATED);
            } else {
                sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error",
                        "Record to update was absent and create attempt failed.");
            }
        }
    }
// Checkstyle: CyclomaticComplexity ON
    
    /**
     * Perform delete operation.
     * 
     * @throws IOException if an error is raised
     */
    private void doDelete() throws IOException {
        try {
            if (storageService.delete(context, key)) {
                getHttpServletResponse().setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Record Not Found", "The specified record was not present or has expired.");
            }
        } catch (final IOException e) {
            sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "Storage error.");
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
