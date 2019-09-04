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

package net.shibboleth.idp.attribute.resolver.spring.failfast;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.sql.DataSource;

import org.springframework.mock.env.MockPropertySource;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.ssl.KeyStoreKeyManager;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustStoreTrustManager;

import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.spring.dc.rdbms.RDBMSDataConnectorParserTest;
import net.shibboleth.idp.profile.spring.failfast.AbstractFailFastTest;
import net.shibboleth.idp.testing.DatabaseTestingSupport;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

@SuppressWarnings("unchecked")
public class AttributeResolverFailFastTest extends AbstractFailFastTest {
    
    protected String getPath() {
        return "/net/shibboleth/idp/attribute/resolver/failfast/";
    }
    
    private InMemoryDirectoryServer directoryServer;
    private DataSource datasource;

    @BeforeTest public void setupDirectoryServer() throws LDAPException, GeneralSecurityException {
        //
        // LDAP
        //
        System.setProperty("org.ldaptive.provider", "org.ldaptive.provider.unboundid.UnboundIDProvider");
        
        final InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=shibboleth,dc=net");
        final SSLUtil sslUtil =
                new SSLUtil(new KeyStoreKeyManager(
                        "src/test/resources/net/shibboleth/idp/attribute/resolver/spring/dc/ldap/server.keystore",
                        "changeit".toCharArray()), new TrustStoreTrustManager(
                        "src/test/resources/net/shibboleth/idp/attribute/resolver/spring/dc/ldap/client.keystore",
                        "changeit".toCharArray(), "JKS", false));
        config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", null, 10389,
                sslUtil.createSSLSocketFactory()));
        config.addAdditionalBindCredentials("cn=Directory Manager", "password");
        directoryServer = new InMemoryDirectoryServer(config);
        directoryServer.importFromLDIF(true,
                "src/test/resources/net/shibboleth/idp/attribute/resolver/spring/dc/ldap/ldapDataConnectorTest.ldif");
        directoryServer.startListening();
        
        //
        // RDBMS
        //
        datasource = DatabaseTestingSupport.GetMockDataSource(RDBMSDataConnectorParserTest.INIT_FILE, "RDBMSDataConnectorStore");
        DatabaseTestingSupport.InitializeDataSourceFromFile(RDBMSDataConnectorParserTest.DATA_FILE, datasource);

    }
    
    @AfterTest public void teardownDirectoryServer() {
        directoryServer.shutDown(true);
        System.clearProperty("org.ldaptive.provider");
    }
    
   public void workingAttributeResolver(String file) throws IOException {
        
        final Object bean = getBean(propertySource("ServiceConfiguration", makePath(file)), "attributeResolverBeansDefaultFF.xml");
        final ReloadableService<AttributeResolver > service = (ReloadableService<AttributeResolver>) bean;
        assertNotNull(service);
        final AttributeResolver resolver = service.getServiceableComponent().getComponent();
        assertNotNull(resolver);
    }

    @Test public void workingAttributeResolver() throws IOException {
        workingAttributeResolver("attributeResolverGood.xml");
    }

    private void badResolver(final Boolean failFast, String resolverFile) throws IOException {
        badResolver(new MockPropertySource("localProperties"), failFast, resolverFile);
    }
    
    private void badResolver(final MockPropertySource propertySource, final Boolean failFast, final String resolverFile) throws IOException {
        final String beanPath;
        if (failFast == null) {
            beanPath = "attributeResolverBeansDefaultFF.xml";
        } else {
            beanPath = "attributeResolverBeans.xml";
        }

        propertySource.setProperty("ServiceConfiguration", makePath(resolverFile));
        
        final Object bean = getBean(propertySource, failFast, beanPath);
        final ReloadableService<AttributeResolver > service = (ReloadableService<AttributeResolver>) bean;
        if (null != failFast && failFast) {
            assertNull(service);
            return;
        }
        assertNotNull(service);
        final ServiceableComponent<AttributeResolver> component = service.getServiceableComponent();
        assertNull(component);
    }
    
    @Test public void badAttributeFF()  throws IOException {
        badResolver(true, "attributeResolverBad.xml");        
    }
    
    @Test public void badAttributeDefaultFF() throws IOException {
        badResolver(null, "attributeResolverBad.xml");
    }

    @Test public void badAttributeNoFF()  throws IOException {
        badResolver(false, "attributeResolverBad.xml");        
    }

    @Test public void workingLDAPResolver() throws IOException {
        workingAttributeResolver("attributeResolverLDAP.xml");
    }

    private void failingLDAPResolver(final Boolean failFast) throws IOException {
        badResolver(propertySource("port", "10390"), failFast, "attributeResolverLDAP.xml");
    }
    
    @Test public void badLDAPFF() throws IOException {
        failingLDAPResolver(true);
    }
    
    @Test public void badLDAPDefault() throws IOException {
        failingLDAPResolver(null);
    }

    @Test public void badLDAPNoFF() throws IOException {
        failingLDAPResolver(false);
    }
    
    @Test public void workingRDBMSResolver() throws IOException {
        workingAttributeResolver("attributeResolverRDBMS.xml");
    }
    
    private void failingRDBMSResolver(final Boolean failFast) throws IOException {
        badResolver(propertySource("jdbcUserName", "SAD"), failFast, "attributeResolverRDBMS.xml");
    }

    @Test public void failingRDBMSResolverFF() throws IOException {
        failingRDBMSResolver(true);
    }

    @Test public void failingRDBMSResolverDefault() throws IOException {
        failingRDBMSResolver(null);
    }

    @Test public void failingRDBMSResolverNoFF() throws IOException {
        failingRDBMSResolver(false);
    }
}
