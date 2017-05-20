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

package net.shibboleth.idp.attribute.resolver.dc.http.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.script.ScriptException;

import net.shibboleth.ext.spring.resource.ResourceHelper;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.dc.impl.TestCache;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.repository.RepositorySupport;
import net.shibboleth.utilities.java.support.velocity.VelocityEngine;

import org.apache.http.HttpStatus;
import org.apache.http.impl.client.HttpClientBuilder;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.opensaml.security.httpclient.impl.SecurityEnhancedHttpClientSupport;
import org.springframework.core.io.ClassPathResource;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for {@link HTTPDataConnector}
 */
public class HTTPDataConnectorTest {

    private static final String TEST_CONNECTOR_NAME = "HTTPConnector";
    
    private static final String TEST_URL =
            RepositorySupport.buildHTTPSResourceURL("java-identity-provider",
                    "idp-attribute-resolver-impl/src/test/resources/net/shibboleth/idp/attribute/resolver/impl/dc/http/test.json");

    private static final String SCRIPT_PATH = "/net/shibboleth/idp/attribute/resolver/impl/dc/http/test.js";

    private HTTPDataConnector connector;
    
    @BeforeMethod public void setUp() throws IOException, ScriptException {
        connector = new HTTPDataConnector();
        connector.setId(TEST_CONNECTOR_NAME);
        connector.setHttpClient(
                HttpClientBuilder.create()
                    .setSSLSocketFactory(SecurityEnhancedHttpClientSupport.buildTLSSocketFactory(false, false))
                    .build());
        
        final ScriptedResponseMappingStrategy mapping =
                ScriptedResponseMappingStrategy.resourceScript(ResourceHelper.of(new ClassPathResource(SCRIPT_PATH)));
        mapping.setLogPrefix(TEST_CONNECTOR_NAME + ":");
        connector.setMappingStrategy(mapping);
    }
    
    @Test public void test() throws ComponentInitializationException, ResolutionException {
        final HttpClientSecurityParameters params = new HttpClientSecurityParameters();
        params.setTLSProtocols(Collections.singleton("TLSv1"));
        final TemplatedURLBuilder builder = new TemplatedURLBuilder();
        builder.setTemplateText(TEST_URL);
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.setHttpClientSecurityParameters(params);
        builder.initialize();
        
        connector.setExecutableSearchBuilder(builder);
        ((ScriptedResponseMappingStrategy) connector.getMappingStrategy()).setAcceptStatuses(Collections.singleton(HttpStatus.SC_OK));
        ((ScriptedResponseMappingStrategy) connector.getMappingStrategy()).setAcceptTypes(Collections.singleton("application/json"));
        connector.initialize();
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        final Map<String,IdPAttribute> attrs = connector.resolve(context);

        Assert.assertEquals(attrs.size(), 2);
        
        Assert.assertEquals(attrs.get("foo").getValues().size(), 1);
        Assert.assertEquals(attrs.get("foo").getValues().get(0).getValue(), "foo1");
        
        Assert.assertEquals(attrs.get("bar").getValues().size(), 2);
        Assert.assertEquals(attrs.get("bar").getValues().get(0).getValue(), "bar1");
        Assert.assertEquals(attrs.get("bar").getValues().get(1).getValue(), "bar2");
    }

    @Test(expectedExceptions=ResolutionException.class) public void testBadProtocol()
            throws ComponentInitializationException, ResolutionException {
        final HttpClientSecurityParameters params = new HttpClientSecurityParameters();
        params.setTLSProtocols(Collections.singleton("SSLv3"));
        final TemplatedURLBuilder builder = new TemplatedURLBuilder();
        builder.setTemplateText(TEST_URL);
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.setHttpClientSecurityParameters(params);
        builder.initialize();
        
        connector.setExecutableSearchBuilder(builder);
        connector.initialize();
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        connector.resolve(context);
    }

    @Test(expectedExceptions=ResolutionException.class) public void testMissing()
            throws ComponentInitializationException, ResolutionException {
        final TemplatedURLBuilder builder = new TemplatedURLBuilder();
        builder.setTemplateText("https://shibboleth.net/test.json");
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.initialize();
        
        connector.setExecutableSearchBuilder(builder);
        connector.initialize();
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        connector.resolve(context);
    }

    @Test public void testMissingOk() throws ComponentInitializationException, ResolutionException, ScriptException {
        final TemplatedURLBuilder builder = new TemplatedURLBuilder();
        builder.setTemplateText("https://shibboleth.net/test.json");
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.initialize();
                
        connector.setExecutableSearchBuilder(builder);
        
        final ScriptedResponseMappingStrategy mapping = ScriptedResponseMappingStrategy.inlineScript("1");
        mapping.setLogPrefix(TEST_CONNECTOR_NAME + ":");
        mapping.setAcceptStatuses(Collections.singleton(HttpStatus.SC_NOT_FOUND));
        connector.setMappingStrategy(mapping);
        
        connector.initialize();
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        final Map<String,IdPAttribute> attrs = connector.resolve(context);

        Assert.assertTrue(attrs == null || attrs.isEmpty());
    }
    
    @Test public void resolveWithCache() throws ComponentInitializationException, ResolutionException {
        final TemplatedURLBuilder builder = new TemplatedURLBuilder();
        builder.setTemplateText(TEST_URL);
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.initialize();
        
        connector.setExecutableSearchBuilder(builder);
        
        final TestCache cache = new TestCache();
        connector.setResultsCache(cache);
        connector.initialize();

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        Assert.assertTrue(cache.size() == 0);
        final Map<String,IdPAttribute> optional = connector.resolve(context);
        Assert.assertTrue(cache.size() == 1);
        Assert.assertEquals(cache.iterator().next(), optional);
    }
    
}