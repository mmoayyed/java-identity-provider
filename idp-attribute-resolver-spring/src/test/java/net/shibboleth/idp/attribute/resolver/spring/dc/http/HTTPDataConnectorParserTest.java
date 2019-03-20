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

package net.shibboleth.idp.attribute.resolver.spring.dc.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockPropertySource;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.google.common.collect.Collections2;

import net.shibboleth.ext.spring.resource.PreferFileSystemResourceLoader;
import net.shibboleth.ext.spring.util.ApplicationContextBuilder;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.dc.http.impl.HTTPDataConnector;
import net.shibboleth.idp.attribute.resolver.spring.dc.http.impl.HTTPDataConnectorParser;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.utilities.java.support.repository.RepositorySupport;
import net.shibboleth.utilities.java.support.testing.TestSupport;

/** Test for {@link HTTPDataConnectorParser}. */
public class HTTPDataConnectorParserTest {

    private static final String TEST_URL =
            RepositorySupport.buildHTTPSResourceURL("java-identity-provider",
                    "idp-attribute-resolver-impl/src/test/resources/net/shibboleth/idp/attribute/resolver/impl/dc/http/test.json");

    private static final String SCRIPT_PATH = "/net/shibboleth/idp/attribute/resolver/impl/dc/http/";
    
    private static final String SCRIPT_PATH_V8 = "/net/shibboleth/idp/attribute/resolver/impl/dc/http/v8/";

    private GenericApplicationContext pendingTeardownContext = null;
    
    @AfterMethod public void tearDownTestContext() {
        if (null == pendingTeardownContext ) {
            return;
        }
        pendingTeardownContext.close();
        pendingTeardownContext = null;
    }
    
    private void setTestContext(final GenericApplicationContext context) {
        tearDownTestContext();
        pendingTeardownContext = context;
    }
    
    @Test public void v2Config() throws Exception {
        
        final MockPropertySource propSource = singletonPropertySource("serviceURL", TEST_URL);
        propSource.setProperty("scriptPath", (TestSupport.isJavaV8OrLater() ? SCRIPT_PATH_V8 : SCRIPT_PATH) + "test.js");
        propSource.setProperty("userAgent", "disguised/1.0.0 hidden/3.4.5");
        propSource.setProperty("certificateAuthority", "/org/opensaml/saml/metadata/resolver/impl/repo-rootCA.crt");
        
        final HTTPDataConnector connector =
                getDataConnector(propSource,
                        "net/shibboleth/idp/attribute/resolver/spring/dc/http/http-attribute-resolver-v2.xml");
        Assert.assertNotNull(connector);
        
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
        
        Assert.assertTrue(connector.getResultsCache().size() == 1);
    }

    @Test(expectedExceptions=ResolutionException.class) public void v2BadProtocol() throws Exception {
        
        final MockPropertySource propSource = singletonPropertySource("serviceURL", TEST_URL);
        propSource.setProperty("scriptPath", (TestSupport.isJavaV8OrLater() ? SCRIPT_PATH_V8 : SCRIPT_PATH) + "test.js");
        propSource.setProperty("userAgent", "disguised/1.0.0 hidden/3.4.5");
        
        final HTTPDataConnector connector =
                getDataConnector(propSource,
                        "net/shibboleth/idp/attribute/resolver/spring/dc/http/http-attribute-resolver-v2-badprotocol.xml");
        Assert.assertNotNull(connector);
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        connector.resolve(context);
    }

    @Test(expectedExceptions=ResolutionException.class) public void v2Size() throws Exception {
        
        final MockPropertySource propSource = singletonPropertySource("serviceURL", TEST_URL);
        propSource.setProperty("scriptPath", (TestSupport.isJavaV8OrLater() ? SCRIPT_PATH_V8 : SCRIPT_PATH) + "testsize.js");
        propSource.setProperty("userAgent", "disguised/1.0.0 hidden/3.4.5");
        propSource.setProperty("certificateAuthority", "/org/opensaml/saml/metadata/resolver/impl/repo-rootCA.crt");

        
        final HTTPDataConnector connector =
                getDataConnector(propSource,
                        "net/shibboleth/idp/attribute/resolver/spring/dc/http/http-attribute-resolver-v2.xml");
        Assert.assertNotNull(connector);
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        connector.resolve(context);
    }

    @Test(expectedExceptions=ResolutionException.class) public void v2Missing() throws Exception {
        
        final MockPropertySource propSource = singletonPropertySource("serviceURL", "https://build.shibboleth.net/test.json");
        propSource.setProperty("scriptPath", (TestSupport.isJavaV8OrLater() ? SCRIPT_PATH_V8 : SCRIPT_PATH) + "test.js");
        propSource.setProperty("userAgent", "disguised/1.0.0 hidden/3.4.5");
        propSource.setProperty("certificateAuthority", "/org/opensaml/saml/metadata/resolver/impl/repo-rootCA.crt");

        
        final HTTPDataConnector connector =
                getDataConnector(propSource,
                        "net/shibboleth/idp/attribute/resolver/spring/dc/http/http-attribute-resolver-v2.xml");
        Assert.assertNotNull(connector);
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        connector.resolve(context);
    }
    
    @Test public void v2MissingOk() throws Exception {
        
        final MockPropertySource propSource = singletonPropertySource("serviceURL", "https://build.shibboleth.net/test.json");
        propSource.setProperty("scriptPath", (TestSupport.isJavaV8OrLater() ? SCRIPT_PATH_V8 : SCRIPT_PATH) + "test.js");
        propSource.setProperty("userAgent", "disguised/1.0.0 hidden/3.4.5");
        
        final HTTPDataConnector connector =
                getDataConnector(propSource,
                        "net/shibboleth/idp/attribute/resolver/spring/dc/http/http-attribute-resolver-v2-missingok.xml");
        Assert.assertNotNull(connector);
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        final Map<String,IdPAttribute> attrs = connector.resolve(context);

        Assert.assertTrue(attrs == null || attrs.isEmpty());
    }

    @Test public void v2Certificate() throws Exception {
        
        final MockPropertySource propSource = singletonPropertySource("serviceURL", TEST_URL);
        propSource.setProperty("scriptPath", (TestSupport.isJavaV8OrLater() ? SCRIPT_PATH_V8 : SCRIPT_PATH) + "test.js");
        propSource.setProperty("userAgent", "disguised/1.0.0 hidden/3.4.5");
        propSource.setProperty("certificate", "/org/opensaml/saml/metadata/resolver/impl/repo-entity.crt");
        
        final HTTPDataConnector connector =
                getDataConnector(propSource,
                        "net/shibboleth/idp/attribute/resolver/spring/dc/http/http-attribute-resolver-v2-certificate.xml");
        Assert.assertNotNull(connector);
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        connector.resolve(context);
        
        final Map<String,IdPAttribute> attrs = connector.resolve(context);
        
        Assert.assertEquals(attrs.size(), 2);
        
        Assert.assertEquals(attrs.get("foo").getValues().size(), 1);
        Assert.assertEquals(attrs.get("foo").getValues().get(0).getValue(), "foo1");
        
        Assert.assertEquals(attrs.get("bar").getValues().size(), 2);
        Assert.assertEquals(attrs.get("bar").getValues().get(0).getValue(), "bar1");
        Assert.assertEquals(attrs.get("bar").getValues().get(1).getValue(), "bar2");
    }

    @Test(expectedExceptions=ResolutionException.class) public void v2BadCertificate() throws Exception {
        
        final MockPropertySource propSource = singletonPropertySource("serviceURL", TEST_URL);
        propSource.setProperty("scriptPath", (TestSupport.isJavaV8OrLater() ? SCRIPT_PATH_V8 : SCRIPT_PATH) + "test.js");
        propSource.setProperty("userAgent", "disguised/1.0.0 hidden/3.4.5");
        propSource.setProperty("certificate", "/org/opensaml/saml/metadata/resolver/impl/badKey.crt");
        
        final HTTPDataConnector connector =
                getDataConnector(propSource,
                        "net/shibboleth/idp/attribute/resolver/spring/dc/http/http-attribute-resolver-v2-certificate.xml");
        Assert.assertNotNull(connector);
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        connector.resolve(context);
    }

    @Test(expectedExceptions=ResolutionException.class) public void v2BadCA() throws Exception {
        
        final MockPropertySource propSource = singletonPropertySource("serviceURL", TEST_URL);
        propSource.setProperty("scriptPath", (TestSupport.isJavaV8OrLater() ? SCRIPT_PATH_V8 : SCRIPT_PATH) + "test.js");
        propSource.setProperty("userAgent", "disguised/1.0.0 hidden/3.4.5");
        propSource.setProperty("certificateAuthority", "/org/opensaml/saml/metadata/resolver/impl/badCA.crt");
        
        final HTTPDataConnector connector =
                getDataConnector(propSource,
                        "net/shibboleth/idp/attribute/resolver/spring/dc/http/http-attribute-resolver-v2-ca.xml");
        Assert.assertNotNull(connector);
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        
        connector.resolve(context);
    }
    
    @Test public void v2ClientCertificate() throws Exception {
        
        // Could use a better test for this end to end.
        
        final MockPropertySource propSource = singletonPropertySource("serviceURL", TEST_URL);
        propSource.setProperty("scriptPath", (TestSupport.isJavaV8OrLater() ? SCRIPT_PATH_V8 : SCRIPT_PATH) + "test.js");
        propSource.setProperty("userAgent", "disguised/1.0.0 hidden/3.4.5");
        propSource.setProperty("key", "net/shibboleth/idp/attribute/resolver/spring/dc/http/client.key");
        propSource.setProperty("certificate", "net/shibboleth/idp/attribute/resolver/spring/dc/http/client.crt");
        propSource.setProperty("certificateAuthority", "/org/opensaml/saml/metadata/resolver/impl/repo-rootCA.crt");

        
        final HTTPDataConnector connector =
                getDataConnector(propSource,
                        "net/shibboleth/idp/attribute/resolver/spring/dc/http/http-attribute-resolver-v2-clientcert.xml");
        Assert.assertNotNull(connector);
        
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
    
    @Test public void hybridConfig() throws Exception {
        final MockPropertySource propSource = singletonPropertySource("serviceURL", TEST_URL);
        propSource.setProperty("scriptPath", (TestSupport.isJavaV8OrLater() ? SCRIPT_PATH_V8 : SCRIPT_PATH) + "test.js");
        propSource.setProperty("userAgent", "disguised/1.0.0 hidden/3.4.5");
        propSource.setProperty("certificateAuthority", "/org/opensaml/saml/metadata/resolver/impl/repo-rootCA.crt");
        
        final HTTPDataConnector connector =
                getDataConnector(propSource,
                        "net/shibboleth/idp/attribute/resolver/spring/dc/http/http-attribute-resolver-v2-hybrid.xml",
                        "net/shibboleth/idp/attribute/resolver/spring/dc/http/http-attribute-resolver-spring-context.xml");
        Assert.assertNotNull(connector);
        
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
        
        Assert.assertTrue(connector.getResultsCache().size() == 1);    
    }

    @Test(enabled=false) public void v2ConfigPOST() throws Exception {
        
        final MockPropertySource propSource = singletonPropertySource("serviceURL", "https://shibboleth.net/cgi-bin/_frobnitz.cgi");
        propSource.setProperty("serviceBody",
                "[{\"name\" : \"foo\",\"values\" : [ \"foo1\" ]},{\"name\" : \"bar\",\"values\" : [ \"bar1\", \"bar2\" ]}]");
        propSource.setProperty("scriptPath", (TestSupport.isJavaV8OrLater() ? SCRIPT_PATH_V8 : SCRIPT_PATH) + "test.js");
        propSource.setProperty("userAgent", "disguised/1.0.0 hidden/3.4.5");
        
        final HTTPDataConnector connector =
                getDataConnector(propSource,
                        "net/shibboleth/idp/attribute/resolver/spring/dc/http/http-attribute-resolver-v2-body.xml");
        Assert.assertNotNull(connector);
        
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
    
    private HTTPDataConnector getDataConnector(final PropertySource propSource, final String... beanDefinitions)
            throws IOException {

        final ResourceLoader loader = new PreferFileSystemResourceLoader();
        
        final ApplicationContextBuilder builder = new ApplicationContextBuilder();
        builder.setName("ApplicationContext: " + HTTPDataConnectorParserTest.class);

        final Collection<String> defs = new ArrayList<>(Arrays.asList(beanDefinitions));
        defs.add("net/shibboleth/idp/attribute/resolver/spring/dc/http/spring-beans.xml");

        builder.setServiceConfigurations(Collections2.transform(defs, s -> loader.getResource(s)));

        if (propSource != null) {
            builder.setPropertySources(Collections.singletonList(propSource));
        }
        
        final GenericApplicationContext context = builder.build();

        setTestContext(context);
                
        return (HTTPDataConnector) context.getBean("myHTTP");
    }

    private MockPropertySource singletonPropertySource(final String name, final String value) {
        final MockPropertySource propSource = new MockPropertySource("localProperties");
        propSource.setProperty(name, value);
        return propSource;
    }

}