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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockPropertySource;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import net.shibboleth.ext.spring.config.DurationToLongConverter;
import net.shibboleth.ext.spring.config.StringToIPRangeConverter;
import net.shibboleth.ext.spring.config.StringToResourceConverter;
import net.shibboleth.ext.spring.util.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.dc.http.impl.HTTPDataConnector;
import net.shibboleth.idp.attribute.resolver.spring.dc.http.impl.HTTPDataConnectorParser;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.utilities.java.support.repository.RepositorySupport;

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
        propSource.setProperty("scriptPath", (isV8() ? SCRIPT_PATH_V8 : SCRIPT_PATH) + "test.js");
        
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
        propSource.setProperty("scriptPath", (isV8() ? SCRIPT_PATH_V8 : SCRIPT_PATH) + "test.js");
        
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
        propSource.setProperty("scriptPath", (isV8() ? SCRIPT_PATH_V8 : SCRIPT_PATH) + "testsize.js");
        
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
        
        final MockPropertySource propSource = singletonPropertySource("serviceURL", "http://shibboleth.net/test.json");
        propSource.setProperty("scriptPath", (isV8() ? SCRIPT_PATH_V8 : SCRIPT_PATH) + "test.js");
        
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
        
        final MockPropertySource propSource = singletonPropertySource("serviceURL", "http://build.shibboleth.net/test.json");
        propSource.setProperty("scriptPath", (isV8() ? SCRIPT_PATH_V8 : SCRIPT_PATH) + "test.js");
        
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
        propSource.setProperty("scriptPath", (isV8() ? SCRIPT_PATH_V8 : SCRIPT_PATH) + "test.js");
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
        propSource.setProperty("scriptPath", (isV8() ? SCRIPT_PATH_V8 : SCRIPT_PATH) + "test.js");
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

    @Test public void v2CA() throws Exception {
        
        final MockPropertySource propSource = singletonPropertySource("serviceURL", TEST_URL);
        propSource.setProperty("scriptPath", (isV8() ? SCRIPT_PATH_V8 : SCRIPT_PATH) + "test.js");
        propSource.setProperty("certificateAuthority", "/org/opensaml/saml/metadata/resolver/impl/repo-rootCA.crt");
        
        final HTTPDataConnector connector =
                getDataConnector(propSource,
                        "net/shibboleth/idp/attribute/resolver/spring/dc/http/http-attribute-resolver-v2-ca.xml");
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
    
    @Test(expectedExceptions=ResolutionException.class) public void v2BadCA() throws Exception {
        
        final MockPropertySource propSource = singletonPropertySource("serviceURL", TEST_URL);
        propSource.setProperty("scriptPath", (isV8() ? SCRIPT_PATH_V8 : SCRIPT_PATH) + "test.js");
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
    
    @Test public void hybridConfig() throws Exception {
        final MockPropertySource propSource = singletonPropertySource("serviceURL", TEST_URL);
        propSource.setProperty("scriptPath", (isV8() ? SCRIPT_PATH_V8 : SCRIPT_PATH) + "test.js");
        
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

    private HTTPDataConnector getDataConnector(final PropertySource propSource, final String... beanDefinitions)
            throws IOException {
        final GenericApplicationContext context = new GenericApplicationContext();
        setTestContext(context);
        context.setDisplayName("ApplicationContext: " + HTTPDataConnectorParserTest.class);

        final ConversionServiceFactoryBean service = new ConversionServiceFactoryBean();
        service.setConverters(new HashSet<>(Arrays.asList(new DurationToLongConverter(), new StringToIPRangeConverter(),
                new StringToResourceConverter())));
        service.afterPropertiesSet();

        context.getBeanFactory().setConversionService(service.getObject());

        if (propSource != null) {
            context.getEnvironment().getPropertySources().addFirst(propSource);
        }
        
        final XmlBeanDefinitionReader configReader = new SchemaTypeAwareXMLBeanDefinitionReader(context);

        configReader.loadBeanDefinitions("net/shibboleth/idp/attribute/resolver/spring/dc/http/spring-beans.xml");
        
        final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.setValidating(true);
        beanDefinitionReader.loadBeanDefinitions(beanDefinitions);
        context.refresh();

        return (HTTPDataConnector) context.getBean("myHTTP");
    }

    private boolean isV8() {
        final String ver = System.getProperty("java.version");
        return ver.startsWith("1.8");
    }
    
    private MockPropertySource singletonPropertySource(final String name, final String value) {
        final MockPropertySource propSource = new MockPropertySource("localProperties");
        propSource.setProperty(name, value);
        return propSource;
    }

}