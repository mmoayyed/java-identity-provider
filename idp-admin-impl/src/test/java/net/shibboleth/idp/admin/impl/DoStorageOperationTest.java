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
import java.text.ParseException;
import java.time.Duration;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.Instant;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.impl.MemoryStorageService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * Unit test for {@link DoStorageOperation} action.
 */
public class DoStorageOperationTest {

    /** Test context. */
    @Nonnull @NotEmpty private final static String CONTEXT = "testContext";

    /** Test key. */
    @Nonnull @NotEmpty private final static String KEY = "testKey";

    /** Test value. */
    @Nonnull @NotEmpty private final static String VALUE = "testValue";

    private MemoryStorageService storageService;
    private ObjectMapper mapper;
    private DoStorageOperation action;

    private RequestContext rc;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    
    /**
     * Set up test.
     * 
     * @throws ComponentInitializationException
     */
    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        
        rc = new RequestContextBuilder().buildRequestContext();
        
        request = (MockHttpServletRequest) rc.getExternalContext().getNativeRequest();
        response = (MockHttpServletResponse) rc.getExternalContext().getNativeResponse();

        storageService = new MemoryStorageService();
        storageService.setId("test");
        storageService.setCleanupInterval(Duration.ZERO);
        storageService.initialize();
        
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
        
        action = new DoStorageOperation();
        action.setHttpServletRequest(request);
        action.setHttpServletResponse(response);
        action.setStorageService(storageService);
        action.setObjectMapper(mapper);
        action.initialize();
    }
    
    /**
     * Tear down test.
     */
    @AfterMethod
    public void tearDown() {
        action.destroy();
        storageService.destroy();
    }

    /**
     * Test with no inputs.
     */
    @Test
    public void noParams() {
        final Event event = action.execute(rc);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(response.getStatus(), HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Test invalidMethod
     */
    @Test
    public void invalidMethod() {
        
        request.setMethod("FOO");
        rc.getFlowScope().put(DoStorageOperation.CONTEXT, CONTEXT);
        rc.getFlowScope().put(DoStorageOperation.KEY, KEY);
        
        final Event event = action.execute(rc);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(response.getStatus(), HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Test with no inputs.
     */
    @Test
    public void missingGet() {
        
        request.setMethod("GET");
        rc.getFlowScope().put(DoStorageOperation.CONTEXT, CONTEXT);
        rc.getFlowScope().put(DoStorageOperation.KEY, KEY);
        
        final Event event = action.execute(rc);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(response.getStatus(), HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Test successful get.
     * 
     * @throws IOException 
     * @throws ParseException 
     */
    @SuppressWarnings("unchecked")
    @Test
    public void successGet() throws IOException, ParseException {
        
        final long exp = Instant.now().getMillis() + Duration.ofMinutes(15).toMillis();
        
        storageService.create(CONTEXT, KEY, VALUE, exp);
        
        request.setMethod("GET");
        rc.getFlowScope().put(DoStorageOperation.CONTEXT, CONTEXT);
        rc.getFlowScope().put(DoStorageOperation.KEY, KEY);
        
        final Event event = action.execute(rc);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(response.getStatus(), HttpServletResponse.SC_OK);
        
        final Map<String,Object> record = mapper.readerFor(Map.class).readValue(response.getContentAsByteArray());
        final Map<String,Object> data = (Map<String, Object>) record.get("data");
        Assert.assertEquals(data.get("type"), "records");
        Assert.assertEquals(data.get("id"), "test/" + CONTEXT + "/" + KEY);
        final Map<String,Object> attributes = (Map<String, Object>) data.get("attributes");
        Assert.assertEquals(attributes.get("value"), VALUE);
        Assert.assertEquals(attributes.get("version"), 1);
        Assert.assertEquals(attributes.get("expiration"), exp);
    }
    
    /**
     * Test missing delete.
     * 
     * @throws IOException 
     * @throws ParseException 
     */
    @Test
    public void missingDelete() throws IOException, ParseException {
        
        storageService.create(CONTEXT, KEY, VALUE, null);
        
        request.setMethod("DELETE");
        rc.getFlowScope().put(DoStorageOperation.CONTEXT, CONTEXT);
        rc.getFlowScope().put(DoStorageOperation.KEY, KEY + "2");
        
        final Event event = action.execute(rc);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(response.getStatus(), HttpServletResponse.SC_NOT_FOUND);
        Assert.assertNotNull(storageService.read(CONTEXT, KEY));
    }
    
    /**
     * Test successful delete.
     * 
     * @throws IOException 
     * @throws ParseException 
     */
    @Test
    public void successDelete() throws IOException, ParseException {
        
        storageService.create(CONTEXT, KEY, VALUE, null);
        
        request.setMethod("DELETE");
        rc.getFlowScope().put(DoStorageOperation.CONTEXT, CONTEXT);
        rc.getFlowScope().put(DoStorageOperation.KEY, KEY);
        
        final Event event = action.execute(rc);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(response.getStatus(), HttpServletResponse.SC_NO_CONTENT);
        
        Assert.assertNull(storageService.read(CONTEXT, KEY));
    }

    /**
     * Test successful create.
     * 
     * @throws IOException 
     * @throws ParseException 
     */
    @Test
    public void successCreate() throws IOException, ParseException {
        
        request.setMethod("PUT");
        rc.getFlowScope().put(DoStorageOperation.CONTEXT, CONTEXT);
        rc.getFlowScope().put(DoStorageOperation.KEY, KEY);
        request.setContent("{ \"value\": \"testValue\" }".getBytes());
        
        final Event event = action.execute(rc);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(response.getStatus(), HttpServletResponse.SC_CREATED);
        
        final StorageRecord<?> record = storageService.read(CONTEXT, KEY);
        Assert.assertEquals(record.getVersion(), 1);
        Assert.assertEquals(record.getValue(), VALUE);
    }

    /**
     * Test duplicate create.
     * 
     * @throws IOException 
     * @throws ParseException 
     */
    @Test
    public void duplicateCreate() throws IOException, ParseException {
        
        storageService.create(CONTEXT, KEY, VALUE, null);
        
        request.setMethod("PUT");
        rc.getFlowScope().put(DoStorageOperation.CONTEXT, CONTEXT);
        rc.getFlowScope().put(DoStorageOperation.KEY, KEY);
        request.setContent("{ \"value\": \"testValue\" }".getBytes());
        
        final Event event = action.execute(rc);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(response.getStatus(), HttpServletResponse.SC_CONFLICT);
    }

    /**
     * Test successful update.
     * 
     * @throws IOException 
     * @throws ParseException 
     */
    @Test
    public void successUpdate() throws IOException, ParseException {
        
        storageService.create(CONTEXT, KEY, VALUE, null);
        
        request.setMethod("POST");
        rc.getFlowScope().put(DoStorageOperation.CONTEXT, CONTEXT);
        rc.getFlowScope().put(DoStorageOperation.KEY, KEY);
        request.setContent("{ \"value\": \"changed\" }".getBytes());
        
        final Event event = action.execute(rc);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(response.getStatus(), HttpServletResponse.SC_OK);
        
        final StorageRecord<?> record = storageService.read(CONTEXT, KEY);
        Assert.assertEquals(record.getVersion(), 2);
        Assert.assertEquals(record.getValue(), "changed");
    }

    /**
     * Test successful update as a create.
     * 
     * @throws IOException 
     * @throws ParseException 
     */
    @Test
    public void successUpdateAsCreate() throws IOException, ParseException {
        
        request.setMethod("POST");
        rc.getFlowScope().put(DoStorageOperation.CONTEXT, CONTEXT);
        rc.getFlowScope().put(DoStorageOperation.KEY, KEY);
        request.setContent("{ \"value\": \"testValue\" }".getBytes());
        
        final Event event = action.execute(rc);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(response.getStatus(), HttpServletResponse.SC_CREATED);
        
        final StorageRecord<?> record = storageService.read(CONTEXT, KEY);
        Assert.assertEquals(record.getVersion(), 1);
        Assert.assertEquals(record.getValue(), VALUE);
    }

    /**
     * Test successful update with a version.
     * 
     * @throws IOException 
     * @throws ParseException 
     */
    @Test
    public void successUpdateWithVersion() throws IOException, ParseException {
        
        storageService.create(CONTEXT, KEY, VALUE, null);
        
        request.setMethod("POST");
        rc.getFlowScope().put(DoStorageOperation.CONTEXT, CONTEXT);
        rc.getFlowScope().put(DoStorageOperation.KEY, KEY);
        request.setContent("{ \"value\": \"changed\", \"version\": 1 }".getBytes());
        
        final Event event = action.execute(rc);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(response.getStatus(), HttpServletResponse.SC_OK);
        
        final StorageRecord<?> record = storageService.read(CONTEXT, KEY);
        Assert.assertEquals(record.getVersion(), 2);
        Assert.assertEquals(record.getValue(), "changed");
    }

    /**
     * Test failed update with a version.
     * 
     * @throws IOException 
     * @throws ParseException 
     */
    @Test
    public void failedUpdateWithVersion() throws IOException, ParseException {
        
        storageService.create(CONTEXT, KEY, VALUE, null);
        
        request.setMethod("POST");
        rc.getFlowScope().put(DoStorageOperation.CONTEXT, CONTEXT);
        rc.getFlowScope().put(DoStorageOperation.KEY, KEY);
        request.setContent("{ \"value\": \"changed\", \"version\": 2 }".getBytes());
        
        final Event event = action.execute(rc);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(response.getStatus(), HttpServletResponse.SC_CONFLICT);
        
        final StorageRecord<?> record = storageService.read(CONTEXT, KEY);
        Assert.assertEquals(record.getVersion(), 1);
        Assert.assertEquals(record.getValue(), VALUE);
    }

    /**
     * Test failed update with a version when record missing.
     * 
     * @throws IOException 
     * @throws ParseException 
     */
    @Test
    public void missingUpdateWithVersion() throws IOException, ParseException {
        
        request.setMethod("POST");
        rc.getFlowScope().put(DoStorageOperation.CONTEXT, CONTEXT);
        rc.getFlowScope().put(DoStorageOperation.KEY, KEY);
        request.setContent("{ \"value\": \"changed\", \"version\": 2 }".getBytes());
        
        final Event event = action.execute(rc);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(response.getStatus(), HttpServletResponse.SC_NOT_FOUND);
        Assert.assertNull(storageService.read(CONTEXT, KEY));
    }

}