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

import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.impl.DatabaseTestingSupport;
import net.shibboleth.idp.attribute.resolver.impl.TestSources;
import net.shibboleth.idp.service.ReloadableService;
import net.shibboleth.idp.service.ServiceException;
import net.shibboleth.idp.service.ServiceableComponent;
import net.shibboleth.idp.spring.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;

/** A work in progress to test the attribute resolver service. */
// TODO incomplete
public class AttributeResolverTest extends OpenSAMLInitBaseTestCase {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeResolverTest.class);
    
    /* LDAP */
    private InMemoryDirectoryServer directoryServer;
    
    private static final String LDAP_INIT_FILE = "src/test/resources/net/shibboleth/idp/attribute/resolver/spring/ldapDataConnectorTest.ldif";
    
    /** DataBase initialise */
    private static final String DB_INIT_FILE = "/net/shibboleth/idp/attribute/resolver/spring/RdbmsStore.sql";

    /** DataBase Populate */
    private static final String DB_DATA_FILE = "/net/shibboleth/idp/attribute/resolver/spring/RdbmsData.sql";

    private DataSource datasource;

    @BeforeTest public void setupDataConnectors() throws LDAPException {

        // LDAP
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=shibboleth,dc=net");
        config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", 10391));
        config.addAdditionalBindCredentials("cn=Directory Manager", "password");
        directoryServer = new InMemoryDirectoryServer(config);
        directoryServer.importFromLDIF(true,LDAP_INIT_FILE);
        directoryServer.startListening();
        
        //RDBMS
        datasource = DatabaseTestingSupport.GetMockDataSource(DB_INIT_FILE, "myTestDB");
        DatabaseTestingSupport.InitializeDataSourceFromFile(DB_DATA_FILE, datasource);

    }
    
    /**
     * Shutdown the in-memory directory server.
     */
    @AfterTest public void teardownDataConnectors() {
        directoryServer.shutDown(true);
    }



    // stub test
    @Test public void one() throws ComponentInitializationException, ServiceException, ResolutionException {

        GenericApplicationContext context = new GenericApplicationContext();
        context.setDisplayName("ApplicationContext: " + AttributeResolverTest.class);
        
        SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions("net/shibboleth/idp/attribute/resolver/spring/service.xml");
        context.refresh();

        final ReloadableService<AttributeResolver> attributeResolverService = context.getBean(ReloadableService.class);
        
        attributeResolverService.start();

        ServiceableComponent<AttributeResolver> serviceableComponent = null;
        final AttributeResolutionContext resolutionContext = TestSources.createResolutionContext("PETER_THE_PRINCIPAL", "issuer", "recipient");

        try {
            serviceableComponent = attributeResolverService.getServiceableComponent();
        
            
            final AttributeResolver resolver = serviceableComponent.getComponent();
            resolver.resolveAttributes(resolutionContext);
        } finally {
            if (null != serviceableComponent) {
                serviceableComponent.unpinComponent();
            }
        }

        Map<String, IdPAttribute> resolvedAttributes = resolutionContext.getResolvedIdPAttributes();
        log.debug("resolved attributes '{}'", resolvedAttributes);

        Assert.assertEquals(resolvedAttributes.size(), 12);

        // Static
        IdPAttribute attribute = resolvedAttributes.get("eduPersonAffiliation");
        Assert.assertNotNull(attribute);
        Set<IdPAttributeValue<?>> values = attribute.getValues();
        Assert.assertEquals(values.size(), 1);
        Assert.assertTrue(values.contains(new StringAttributeValue("member")));

        // LDAP
        attribute = resolvedAttributes.get("uid");
        Assert.assertNotNull(attribute);
        values = attribute.getValues();
        Assert.assertEquals(values.size(), 1);
        Assert.assertTrue(values.contains(new StringAttributeValue("PETER_THE_PRINCIPAL")));

        attribute = resolvedAttributes.get("email");
        Assert.assertNotNull(attribute);
        values = attribute.getValues();
        Assert.assertEquals(values.size(), 2);
        Assert.assertTrue(values.contains(new StringAttributeValue("peterprincipal@shibboleth.net")));
        Assert.assertTrue(values.contains(new StringAttributeValue("peter.principal@shibboleth.net")));

        attribute = resolvedAttributes.get("surname");
        Assert.assertNotNull(attribute);
        values = attribute.getValues();
        Assert.assertEquals(values.size(), 1);
        Assert.assertTrue(values.contains(new StringAttributeValue("Principal")));

        attribute = resolvedAttributes.get("commonName");
        Assert.assertNotNull(attribute);
        values = attribute.getValues();
        Assert.assertEquals(values.size(), 3);
        Assert.assertTrue(values.contains(new StringAttributeValue("Peter Principal")));
        Assert.assertTrue(values.contains(new StringAttributeValue("Peter J Principal")));
        Assert.assertTrue(values.contains(new StringAttributeValue("pete principal")));

        attribute = resolvedAttributes.get("homePhone");
        Assert.assertNotNull(attribute);
        values = attribute.getValues();
        Assert.assertEquals(values.size(), 1);
        Assert.assertTrue(values.contains(new StringAttributeValue("555-111-2222")));

        // Computed
        attribute = resolvedAttributes.get("eduPersonTargetedID");
        Assert.assertNotNull(attribute);
        values = attribute.getValues();
        Assert.assertEquals(values.size(), 1);

        // RDBMS TODO wire in the template
        attribute = resolvedAttributes.get("pagerNumber");
        Assert.assertNotNull(attribute);
        values = attribute.getValues();
        Assert.assertEquals(values.size(), 1);
        Assert.assertTrue(values.contains(new StringAttributeValue("555-123-4567")));

        attribute = resolvedAttributes.get("mobileNumber");
        Assert.assertNotNull(attribute);
        values = attribute.getValues();
        Assert.assertEquals(values.size(), 1);
        Assert.assertTrue(values.contains(new StringAttributeValue("444-123-4567")));

        attribute = resolvedAttributes.get("street");
        Assert.assertNotNull(attribute);
        values = attribute.getValues();
        Assert.assertEquals(values.size(), 1);
        Assert.assertTrue(values.contains(new StringAttributeValue("TheStreet")));

        attribute = resolvedAttributes.get("title");
        Assert.assertNotNull(attribute);
        values = attribute.getValues();
        Assert.assertEquals(values.size(), 1);
        Assert.assertTrue(values.contains(new StringAttributeValue("Monsieur")));

        attribute = resolvedAttributes.get("departmentNumber");
        Assert.assertNotNull(attribute);
        values = attribute.getValues();
        Assert.assertEquals(values.size(), 1);
        Assert.assertTrue(values.contains(new StringAttributeValue("#4321")));

    }
}
