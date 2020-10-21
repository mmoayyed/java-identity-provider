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

package net.shibboleth.idp.attribute.resolver.dc.storage.impl;

import static org.testng.Assert.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import javax.script.ScriptException;

import org.opensaml.storage.impl.MemoryStorageService;
import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.reporters.Files;

import net.shibboleth.ext.spring.resource.ResourceHelper;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.NoResultAnErrorResolutionException;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.dc.impl.TestCache;
import net.shibboleth.idp.saml.impl.testing.TestSources;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.velocity.VelocityEngine;

/**
 * Tests for {@link HTTPDataConnector}
 */
@SuppressWarnings("javadoc")
public class StorageServiceDataConnectorTest {

    private static final String TEST_CONNECTOR_NAME = "StorageServiceConnector";
    
    private static final String SCRIPT_PATH = "/net/shibboleth/idp/attribute/resolver/impl/dc/storage/";
    
    private MemoryStorageService storage;
    private StorageServiceDataConnector connector;

    @BeforeMethod public void setUp() throws Exception {
        
        storage = new MemoryStorageService();
        storage.setId("ss");
        storage.setCleanupInterval(Duration.ZERO);
        storage.initialize();
        
        connector = new StorageServiceDataConnector();
        connector.setId(TEST_CONNECTOR_NAME);
        connector.setStorageService(storage);
    }
    
    @AfterMethod public void tearDown() {
        connector.destroy();
        storage.destroy();
    }
    
    @Test public void testSimpleMissing() throws ComponentInitializationException, ResolutionException, ScriptException, IOException {
        
        final TemplatedSearchBuilder builder = new TemplatedSearchBuilder();
        builder.setContextTemplateText("foo");
        builder.setKeyTemplateText("bar");
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.initialize();
        
        connector.setExecutableSearchBuilder(builder);
        connector.setGeneratedAttributeID("foobar");
        connector.initialize();
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);

        final Map<String,IdPAttribute> attrs = connector.resolve(context);
        
        assertTrue(attrs.isEmpty());
    }

    @Test(expectedExceptions=NoResultAnErrorResolutionException.class)
    public void testSimpleMissingError() throws ComponentInitializationException, ResolutionException, ScriptException, IOException {
        
        final TemplatedSearchBuilder builder = new TemplatedSearchBuilder();
        builder.setContextTemplateText("foo");
        builder.setKeyTemplateText("bar");
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.initialize();
        
        connector.setExecutableSearchBuilder(builder);
        connector.setGeneratedAttributeID("foobar");
        connector.setNoResultAnError(true);
        connector.initialize();
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        connector.resolve(context);
    }

    @Test public void testSimple() throws ComponentInitializationException, ResolutionException, ScriptException, IOException {
        
        final TemplatedSearchBuilder builder = new TemplatedSearchBuilder();
        builder.setContextTemplateText("foo");
        builder.setKeyTemplateText("bar");
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.initialize();
        
        connector.setExecutableSearchBuilder(builder);
        connector.setGeneratedAttributeID("foobar");
        connector.initialize();
        
        storage.create("foo", "bar", "test", null);

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String,IdPAttribute> attrs = connector.resolve(context);

        assertEquals(attrs.size(), 1);
        
        assertEquals(attrs.get("foobar").getValues().size(), 1);
        assertEquals(((StringAttributeValue) attrs.get("foobar").getValues().get(0)).getValue(), "test");
    }
    
    @Test public void resolveWithCache() throws ComponentInitializationException, ResolutionException, ScriptException, IOException {
        
        final TemplatedSearchBuilder builder = new TemplatedSearchBuilder();
        builder.setContextTemplateText("foo");
        builder.setKeyTemplateText("bar");
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.initialize();
        
        connector.setExecutableSearchBuilder(builder);
        connector.setGeneratedAttributeID("foobar");
        
        final TestCache cache = new TestCache();
        connector.setResultsCache(cache);

        connector.initialize();
        
        storage.create("foo", "bar", "test", null);

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        assertTrue(cache.size() == 0);
        final Map<String,IdPAttribute> optional = connector.resolve(context);
        assertTrue(cache.size() == 1);
        assertEquals(cache.iterator().next(), optional);
    }

    @Test public void testScripted() throws ComponentInitializationException, ResolutionException, ScriptException, IOException {
        
        final TemplatedSearchBuilder builder = new TemplatedSearchBuilder();
        builder.setContextTemplateText("foo");
        builder.setKeyTemplateText("bar");
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.initialize();
        
        connector.setExecutableSearchBuilder(builder);
        
        final ScriptedStorageMappingStrategy mapper = ScriptedStorageMappingStrategy.resourceScript(
                ResourceHelper.of(new ClassPathResource((SCRIPT_PATH) + "test.js")));
        
        connector.setMappingStrategy(mapper);
        
        connector.initialize();
        
        storage.create("foo", "bar", Files.streamToString(getClass().getResourceAsStream(SCRIPT_PATH + "test.json")), null);

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String,IdPAttribute> attrs = connector.resolve(context);

        assertEquals(attrs.size(), 2);
        
        assertEquals(attrs.get("foo").getValues().size(), 1);
        assertEquals(((StringAttributeValue) attrs.get("foo").getValues().get(0)).getValue(), "foo1");
        
        assertEquals(attrs.get("bar").getValues().size(), 2);
        assertEquals(((StringAttributeValue)attrs.get("bar").getValues().get(0)).getValue(), "bar1");
        assertEquals(((StringAttributeValue)attrs.get("bar").getValues().get(1)).getValue(), "bar2");
    }
}