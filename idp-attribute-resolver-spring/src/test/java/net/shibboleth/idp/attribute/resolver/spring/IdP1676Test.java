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

package net.shibboleth.idp.attribute.resolver.spring;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockPropertySource;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;

import net.shibboleth.ext.spring.config.IdentifiableBeanPostProcessor;
import net.shibboleth.ext.spring.config.StringToDurationConverter;
import net.shibboleth.ext.spring.util.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.saml.impl.testing.TestSources;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

@SuppressWarnings("javadoc")
public class IdP1676Test extends OpenSAMLInitBaseTestCase {

    /* LDAP */
    private InMemoryDirectoryServer directoryServer;

    private static final String LDAP_INIT_FILE =
            "src/test/resources/net/shibboleth/idp/attribute/resolver/spring/ldapDataConnectorTest.ldif";
    
    private GenericApplicationContext pendingTeardownContext = null;
    
    @AfterMethod public void tearDownTestContext() {
        if (null == pendingTeardownContext ) {
            return;
        }
        pendingTeardownContext.close();
        pendingTeardownContext = null;
    }
    
    @AfterClass public void teardown() {
        connectorOff();
    }

    protected void setTestContext(final GenericApplicationContext context) {
        tearDownTestContext();
        pendingTeardownContext = context;
    }

    protected synchronized void connectorOn() throws LDAPException {
        if (directoryServer == null) {
            // LDAP
            final InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=shibboleth,dc=net");
            config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", 10391));
            config.addAdditionalBindCredentials("cn=Directory Manager", "password");
            directoryServer = new InMemoryDirectoryServer(config);
            directoryServer.importFromLDIF(true, LDAP_INIT_FILE);
            directoryServer.startListening();
        }
    }
    
    /**
     * Shutdown the in-memory directory server.
     */
   public synchronized void connectorOff() {
       if (directoryServer != null) {
           directoryServer.shutDown(true);
       }
       directoryServer = null;
    }
    
    private ReloadableService<AttributeResolver> getResolver(final boolean failFast, final boolean propagateResolutionExceptions) throws ComponentInitializationException {
        final GenericApplicationContext context = new GenericApplicationContext();
        context.getBeanFactory().addBeanPostProcessor(new IdentifiableBeanPostProcessor());
        
        final Pair<String, String> pRE = new Pair<>("propagateResolutionExceptions", propagateResolutionExceptions ? "true": "false");
        final Pair<String, String> fF = new Pair<>("failfast", failFast ? "true": "false");
        final List<Pair<String, String>> properties = List.of(pRE,fF);
        
        final MutablePropertySources propertySources = context.getEnvironment().getPropertySources();
        final MockPropertySource mockEnvVars = new MockPropertySource();
        for (final Pair<String, String> p :properties) {
            mockEnvVars.setProperty(p.getFirst(), p.getSecond());
        }
        propertySources.replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, mockEnvVars);
        
        setTestContext(context);
        context.setDisplayName("ApplicationContext: " + IdP1676Test.class);

        final ConversionServiceFactoryBean service = new ConversionServiceFactoryBean();
        context.setDisplayName("ApplicationContext: ");
        service.setConverters(new HashSet<>(Arrays.asList(new StringToDurationConverter())));
        service.afterPropertiesSet();

        context.getBeanFactory().setConversionService(service.getObject());
        
        final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions("net/shibboleth/idp/attribute/resolver/spring/idp-1676-service.xml");
        context.refresh();

        final ReloadableService<AttributeResolver> result =  context.getBean(ReloadableService.class);
        result.initialize();
        return result;
    }
    
    private void testResolve(ReloadableService<AttributeResolver> resolverService, int attributeCount) throws ResolutionException {
        final AttributeResolutionContext resolutionContext =
                TestSources.createResolutionContext("PETER_THE_PRINCIPAL", "issuer", "recipient");
        ServiceableComponent<AttributeResolver> serviceableComponent = null;
        try {
            serviceableComponent = resolverService.getServiceableComponent();
            serviceableComponent.getComponent().resolveAttributes(resolutionContext);
        } finally {
            if (null != serviceableComponent) {
                serviceableComponent.unpinComponent();
            } 
        }
        assertEquals(resolutionContext.getResolvedIdPAttributes().size(), attributeCount);
        
    }


    @Test public void failFast() throws LDAPException, ComponentInitializationException, ResolutionException {
        connectorOff();
        ReloadableService<AttributeResolver> resolverService = getResolver(true, true);
        assertNull(resolverService.getServiceableComponent());
        connectorOn();
        assertNull(resolverService.getServiceableComponent());
        resolverService = getResolver(true, true);
        testResolve(resolverService, 7);
        connectorOff();
        try {
            testResolve(resolverService, 2);
            fail("Expected an Exception");
        } catch (final ResolutionException ex) {
            //expected that
        }
    }
    
    @Test public void failFastNoPE() throws LDAPException, ComponentInitializationException, ResolutionException {
        connectorOff();
        ReloadableService<AttributeResolver> resolverService = getResolver(true, false);
        assertNull(resolverService.getServiceableComponent());
        connectorOn();
        assertNull(resolverService.getServiceableComponent());
        resolverService = getResolver(true, false);
        testResolve(resolverService, 7);
        connectorOff();
        testResolve(resolverService, 2);
        connectorOff();
    }

    @Test public void normal() throws LDAPException, ComponentInitializationException, ResolutionException {
        connectorOn();
        ReloadableService<AttributeResolver> resolverService = getResolver(false, true);
        testResolve(resolverService, 7);
        connectorOff();
        try {
            testResolve(resolverService, 2);
            fail("Expected an Exception");
        } catch (final ResolutionException ex) {
            //expected that
        }
        resolverService = getResolver(false, true);
        try {
            testResolve(resolverService, 2);
            fail("Expected an Exception");
        } catch (final ResolutionException ex) {
            //expected that
        }
        connectorOn();
        testResolve(resolverService, 7);        
    }
    
    @Test public void normalNoPE() throws LDAPException, ComponentInitializationException, ResolutionException {
        connectorOn();
        ReloadableService<AttributeResolver> resolverService = getResolver(false, false);
        testResolve(resolverService, 7);
        connectorOff();
        testResolve(resolverService, 2);
        resolverService = getResolver(false, false);
        testResolve(resolverService, 2);
        connectorOn();
        testResolve(resolverService, 7);        
    }

}
