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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptException;

import org.apache.http.HttpStatus;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.opensaml.saml.metadata.resolver.impl.FileBackedHTTPMetadataResolver;
import org.opensaml.security.credential.impl.StaticCredentialResolver;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.opensaml.security.httpclient.impl.SecurityEnhancedHttpClientSupport;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.trust.impl.ExplicitKeyTrustEngine;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.security.x509.PKIXValidationInformation;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.security.x509.X509Support;
import org.opensaml.security.x509.impl.BasicPKIXValidationInformation;
import org.opensaml.security.x509.impl.BasicX509CredentialNameEvaluator;
import org.opensaml.security.x509.impl.CertPathPKIXTrustEvaluator;
import org.opensaml.security.x509.impl.PKIXX509CredentialTrustEngine;
import org.opensaml.security.x509.impl.StaticPKIXValidationInformationResolver;
import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.io.ByteStreams;

import net.shibboleth.ext.spring.resource.ResourceHelper;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.dc.impl.TestCache;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;
import net.shibboleth.utilities.java.support.test.repository.RepositorySupport;
import net.shibboleth.utilities.java.support.velocity.VelocityEngine;

/**
 * Tests for {@link HTTPDataConnector}
 */
@SuppressWarnings("javadoc")
public class HTTPDataConnectorTest {

    private static final String TEST_CONNECTOR_NAME = "HTTPConnector";
    
    private static final String TEST_URL =
            RepositorySupport.buildHTTPResourceURL("java-identity-provider",
                    "idp-attribute-resolver-impl/src/test/resources/net/shibboleth/idp/attribute/resolver/impl/dc/http/test.json", false);

    private static final String SCRIPT_PATH = "/net/shibboleth/idp/attribute/resolver/impl/dc/http/";
    
    private HTTPDataConnector connector;
    
    @BeforeMethod public void setUp() throws Exception {
        connector = new HTTPDataConnector();
        connector.setId(TEST_CONNECTOR_NAME);
        final HttpClientBuilder builder = new HttpClientBuilder();
        builder.setTLSSocketFactory(SecurityEnhancedHttpClientSupport.buildTLSSocketFactory(false, false));
        connector.setHttpClient(builder.buildClient());
    }
    
    @Test public void test() throws ComponentInitializationException, ResolutionException, ScriptException, IOException {
        final HttpClientSecurityParameters params = new HttpClientSecurityParameters();
        params.setTLSProtocols(Collections.singleton("TLSv1"));
        connector.setHttpClientSecurityParameters(params);
        
        final TemplatedURLBuilder builder = new TemplatedURLBuilder();
        builder.setTemplateText(TEST_URL);
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.setHttpClientSecurityParameters(params);
        builder.setHeaders(Collections.singletonMap("Accept", "test/html"));
        builder.initialize();
        
        connector.setExecutableSearchBuilder(builder);
        
        
        final ScriptedResponseMappingStrategy mapping =
                ScriptedResponseMappingStrategy.resourceScript(
                        ResourceHelper.of(new ClassPathResource((SCRIPT_PATH) + "test.js")));
        mapping.setLogPrefix(TEST_CONNECTOR_NAME + ":");
        mapping.setAcceptStatuses(Collections.singleton(HttpStatus.SC_OK));
        mapping.setAcceptTypes(Collections.singleton("application/json"));
        connector.setMappingStrategy(mapping);
        connector.initialize();
        
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

    @Test(expectedExceptions=ResolutionException.class) public void testBadProtocol()
            throws Exception {
        final HttpClientBuilder clientBuilder = new HttpClientBuilder();
        clientBuilder.setTLSSocketFactory(buildSocketFactory());
        connector.setHttpClient(clientBuilder.buildClient());

        final HttpClientSecurityParameters params = new HttpClientSecurityParameters();
        params.setTLSProtocols(Collections.singleton("SSLv3"));
        params.setTLSTrustEngine(buildExplicitKeyTrustEngine("repo-entity.crt"));
        connector.setHttpClientSecurityParameters(params);

        final TemplatedURLBuilder builder = new TemplatedURLBuilder();
        builder.setTemplateText(RepositorySupport.buildHTTPSResourceURL("java-identity-provider",
                    "idp-attribute-resolver-impl/src/test/resources/net/shibboleth/idp/attribute/resolver/impl/dc/http/test.json"));
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.setHttpClientSecurityParameters(params);
        builder.initialize();        
        connector.setExecutableSearchBuilder(builder);
        
        final ScriptedResponseMappingStrategy mapping =
                ScriptedResponseMappingStrategy.resourceScript(
                        ResourceHelper.of(new ClassPathResource((SCRIPT_PATH) + "test.js")));
        mapping.setLogPrefix(TEST_CONNECTOR_NAME + ":");
        mapping.setAcceptStatuses(Collections.singleton(HttpStatus.SC_OK));
        mapping.setAcceptTypes(Collections.singleton("application/json"));
        connector.setMappingStrategy(mapping);
        
        connector.initialize();
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        connector.resolve(context);
    }
    
    @Test(expectedExceptions=ResolutionException.class) public void testSize()
            throws ComponentInitializationException, ResolutionException, ScriptException, IOException {
        final TemplatedURLBuilder builder = new TemplatedURLBuilder();
        builder.setTemplateText(TEST_URL);
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.initialize();
        connector.setExecutableSearchBuilder(builder);
        
        final ScriptedResponseMappingStrategy mapping =
                ScriptedResponseMappingStrategy.resourceScript(
                        ResourceHelper.of(new ClassPathResource((SCRIPT_PATH) + "testsize.js")));
        mapping.setLogPrefix(TEST_CONNECTOR_NAME + ":");
        mapping.setAcceptStatuses(Collections.singleton(HttpStatus.SC_OK));
        mapping.setAcceptTypes(Collections.singleton("application/json"));
        connector.setMappingStrategy(mapping);

        connector.initialize();
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        connector.resolve(context);
    }

    @Test(expectedExceptions=ResolutionException.class) public void testMissing()
            throws ComponentInitializationException, ResolutionException, ScriptException, IOException {
        final TemplatedURLBuilder builder = new TemplatedURLBuilder();
        builder.setTemplateText("https://shibboleth.net/test.json");
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.initialize();
        connector.setExecutableSearchBuilder(builder);
        
        final ScriptedResponseMappingStrategy mapping =
                ScriptedResponseMappingStrategy.resourceScript(
                        ResourceHelper.of(new ClassPathResource((SCRIPT_PATH) + "test.js")));
        mapping.setLogPrefix(TEST_CONNECTOR_NAME + ":");
        mapping.setAcceptStatuses(Collections.singleton(HttpStatus.SC_OK));
        mapping.setAcceptTypes(Collections.singleton("application/json"));
        connector.setMappingStrategy(mapping);

        connector.initialize();
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        connector.resolve(context);
    }
    
    @Test public void testMissingOk() throws ComponentInitializationException, ResolutionException, ScriptException {
        final TemplatedURLBuilder builder = new TemplatedURLBuilder();
        builder.setTemplateText("https://build.shibboleth.net/test.json");
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

        assertTrue(attrs == null || attrs.isEmpty());
    }
    
    @Test public void resolveWithCache() throws ComponentInitializationException, ResolutionException, ScriptException, IOException {
        final TemplatedURLBuilder builder = new TemplatedURLBuilder();
        builder.setTemplateText(TEST_URL);
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.initialize();
        connector.setExecutableSearchBuilder(builder);
        
        final ScriptedResponseMappingStrategy mapping =
                ScriptedResponseMappingStrategy.resourceScript(
                        ResourceHelper.of(new ClassPathResource((SCRIPT_PATH) + "test.js")));
        mapping.setLogPrefix(TEST_CONNECTOR_NAME + ":");
        mapping.setAcceptStatuses(Collections.singleton(HttpStatus.SC_OK));
        mapping.setAcceptTypes(Collections.singleton("application/json"));
        connector.setMappingStrategy(mapping);
        
        final TestCache cache = new TestCache();
        connector.setResultsCache(cache);

        connector.initialize();

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        assertTrue(cache.size() == 0);
        final Map<String,IdPAttribute> optional = connector.resolve(context);
        assertTrue(cache.size() == 1);
        assertEquals(cache.iterator().next(), optional);
    }
    
    @Test(enabled=false) public void testPOST() throws ComponentInitializationException, ResolutionException, ScriptException, IOException {
        final TemplatedBodyBuilder builder = new TemplatedBodyBuilder();
        builder.setURLTemplateText("https://shibboleth.net/cgi-bin/_frobnitz.cgi");
        builder.setBodyTemplateText("[{\"name\" : \"foo\",\"values\" : [ \"foo1\" ]},{\"name\" : \"bar\",\"values\" : [ \"bar1\", \"bar2\" ]}]");
        builder.setMIMEType("application/json");
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.initialize();
        connector.setExecutableSearchBuilder(builder);
        
        final ScriptedResponseMappingStrategy mapping =
                ScriptedResponseMappingStrategy.resourceScript(
                        ResourceHelper.of(new ClassPathResource((SCRIPT_PATH) + "test.js")));
        mapping.setLogPrefix(TEST_CONNECTOR_NAME + ":");
        mapping.setAcceptStatuses(Collections.singleton(HttpStatus.SC_OK));
        mapping.setAcceptTypes(Collections.singleton("application/json"));
        connector.setMappingStrategy(mapping);
        connector.initialize();
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        final Map<String,IdPAttribute> attrs = connector.resolve(context);

        assertEquals(attrs.size(), 2);
        
        assertEquals(attrs.get("foo").getValues().size(), 1);
        assertEquals(((StringAttributeValue)attrs.get("foo").getValues().get(0)).getValue(), "foo1");
        
        assertEquals(attrs.get("bar").getValues().size(), 2);
        assertEquals(((StringAttributeValue)attrs.get("bar").getValues().get(0)).getValue(), "bar1");
        assertEquals(((StringAttributeValue)attrs.get("bar").getValues().get(1)).getValue(), "bar2");
    }

    @Test(enabled=false) public void testCacheable() throws ComponentInitializationException, ResolutionException, ScriptException, IOException {
        final TemplatedBodyBuilder builder = new TemplatedBodyBuilder();
        builder.setURLTemplateText("https://shibboleth.net/cgi-bin/_frobnitz.cgi");
        builder.setBodyTemplateText("[{\"name\" : \"foo\",\"values\" : [ \"foo1\" ]},{\"name\" : \"bar\",\"values\" : [ \"bar1\", \"bar2\" ]}]");
        builder.setCacheKeyTemplateText("foo");
        builder.setMIMEType("application/json");
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.initialize();
        connector.setExecutableSearchBuilder(builder);
        
        final ScriptedResponseMappingStrategy mapping =
                ScriptedResponseMappingStrategy.resourceScript(
                        ResourceHelper.of(new ClassPathResource((SCRIPT_PATH) + "test.js")));
        mapping.setLogPrefix(TEST_CONNECTOR_NAME + ":");
        mapping.setAcceptStatuses(Collections.singleton(HttpStatus.SC_OK));
        mapping.setAcceptTypes(Collections.singleton("application/json"));
        connector.setMappingStrategy(mapping);
        
        final TestCache cache = new TestCache();
        connector.setResultsCache(cache);
        
        connector.initialize();
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        assertTrue(cache.size() == 0);
        final Map<String,IdPAttribute> optional = connector.resolve(context);
        assertTrue(cache.size() == 1);
        assertEquals(cache.iterator().next(), optional);
    }
    
    @Test(enabled=false) public void testUncacheable() throws ComponentInitializationException, ResolutionException, ScriptException, IOException {
        final TemplatedBodyBuilder builder = new TemplatedBodyBuilder();
        builder.setURLTemplateText("https://shibboleth.net/cgi-bin/_frobnitz.cgi");
        builder.setBodyTemplateText("[{\"name\" : \"foo\",\"values\" : [ \"foo1\" ]},{\"name\" : \"bar\",\"values\" : [ \"bar1\", \"bar2\" ]}]");
        builder.setMIMEType("application/json");
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.initialize();
        connector.setExecutableSearchBuilder(builder);
        
        final ScriptedResponseMappingStrategy mapping =
                ScriptedResponseMappingStrategy.resourceScript(
                        ResourceHelper.of(new ClassPathResource((SCRIPT_PATH) + "test.js")));
        mapping.setLogPrefix(TEST_CONNECTOR_NAME + ":");
        mapping.setAcceptStatuses(Collections.singleton(HttpStatus.SC_OK));
        mapping.setAcceptTypes(Collections.singleton("application/json"));
        connector.setMappingStrategy(mapping);
        
        final TestCache cache = new TestCache();
        connector.setResultsCache(cache);
        
        connector.initialize();
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        assertTrue(cache.size() == 0);
        connector.resolve(context);
        assertTrue(cache.size() == 0);
    }

    public static TrustEngine<? super X509Credential> buildPKIXTrustEngine(String cert, String name, boolean nameCheckEnabled) throws URISyntaxException, CertificateException, IOException {
        final InputStream certStream = FileBackedHTTPMetadataResolver.class.getResourceAsStream((SCRIPT_PATH + cert));
        final X509Certificate rootCert = X509Support.decodeCertificate(ByteStreams.toByteArray(certStream));
        final PKIXValidationInformation info = new BasicPKIXValidationInformation(Collections.singletonList(rootCert), null, 5);
        final Set<String> trustedNames = name != null ? Collections.singleton(name) : Collections.emptySet();
        final StaticPKIXValidationInformationResolver resolver = new StaticPKIXValidationInformationResolver(Collections.singletonList(info), trustedNames);
        return new PKIXX509CredentialTrustEngine(resolver,
                new CertPathPKIXTrustEvaluator(),
                (nameCheckEnabled ? new BasicX509CredentialNameEvaluator() : null));
    }

    public static TrustEngine<? super X509Credential> buildExplicitKeyTrustEngine(String cert) throws URISyntaxException, CertificateException, IOException {
        
        final InputStream certStream = FileBackedHTTPMetadataResolver.class.getResourceAsStream(SCRIPT_PATH + cert);
        final X509Certificate entityCert = X509Support.decodeCertificate(ByteStreams.toByteArray(certStream));
        final X509Credential entityCredential = new BasicX509Credential(entityCert);
        return new ExplicitKeyTrustEngine(new StaticCredentialResolver(entityCredential));
        
    }

    public static LayeredConnectionSocketFactory buildSocketFactory() {
        return buildSocketFactory(true);
    }
    
    public static LayeredConnectionSocketFactory buildSocketFactory(boolean supportTrustEngine) {
        return SecurityEnhancedHttpClientSupport.buildTLSSocketFactory(supportTrustEngine, false);
    }
}