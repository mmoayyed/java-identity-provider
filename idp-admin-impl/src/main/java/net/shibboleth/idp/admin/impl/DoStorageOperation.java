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
import java.time.Instant;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
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
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.webflow.execution.RequestContext;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.models.errors.Error;
import com.github.jasminb.jsonapi.models.errors.Errors;
import com.google.common.base.Strings;

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
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
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
            
            storageService = getStorageService(requestContext);
            if (storageService == null) {
                sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Invalid Storage Service", "Invalid storage service identifier in path.");
                return false;
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

    /** {@inheritDoc} */
    @Override protected void doExecute(final ProfileRequestContext profileRequestContext) {
        
        try {
            final HttpServletRequest request = getHttpServletRequest();
            final HttpServletResponse response = getHttpServletResponse();
            
            response.setContentType("application/json");
            response.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
            
            if ("GET".equals(request.getMethod())) {
                final StorageRecord record;
                try {
                    record = storageService.read(context, key);
                    if (record != null) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        final JsonFactory jsonFactory = new JsonFactory();
                        final JsonGenerator g = jsonFactory.createGenerator(
                                response.getOutputStream()).useDefaultPrettyPrinter();
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
                            g.writeObject(Instant.ofEpochMilli(record.getExpiration()));
                        }
                        g.close();
                    } else {
                        sendError(HttpServletResponse.SC_NOT_FOUND,
                                "Record Not Found", "The specified record was not present or has expired.");
                    }
                } catch (final IOException e) {
                    sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "Storage error.");
                }
            } else if ("DELETE".equals(request.getMethod())) {
                try {
                    if (storageService.delete(context, key)) {
                        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    } else {
                        sendError(HttpServletResponse.SC_NOT_FOUND,
                                "Record Not Found", "The specified record was not present or has expired.");
                    }
                } catch (final IOException e) {
                    sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "Storage error.");
                }
                
            } else {
                log.warn("{} Invalid method: {}", getLogPrefix(), request.getMethod());
                sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                        "Unknown Operation", "Only GET and DELETE are supported.");
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
        
        try {
            final Object bean = requestContext.getActiveFlow().getApplicationContext().getBean(id);
            if (bean != null && bean instanceof StorageService) {
                return (StorageService) bean;
            }
        } catch (final BeansException e) {
            
        }
        
        log.warn("{} No bean of the correct type found named {}", getLogPrefix(), id);
        return null;
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