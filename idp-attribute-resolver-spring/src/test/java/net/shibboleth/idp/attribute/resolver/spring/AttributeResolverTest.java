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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.security.auth.Subject;
import javax.sql.DataSource;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.impl.NameIDBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;

import net.shibboleth.ext.spring.config.IdentifiableBeanPostProcessor;
import net.shibboleth.ext.spring.config.StringToDurationConverter;
import net.shibboleth.ext.spring.util.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.saml.authn.principal.NameIDPrincipal;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.idp.testing.DatabaseTestingSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceException;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

/** A work in progress to test the attribute resolver service. */
@SuppressWarnings("javadoc")
public class AttributeResolverTest extends OpenSAMLInitBaseTestCase {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeResolverTest.class);

    /* LDAP */
    private InMemoryDirectoryServer directoryServer;

    private static final String LDAP_INIT_FILE =
            "src/test/resources/net/shibboleth/idp/attribute/resolver/spring/ldapDataConnectorTest.ldif";

    /** DataBase initialise */
    private static final String DB_INIT_FILE = "/net/shibboleth/idp/attribute/resolver/spring/RdbmsStore.sql";

    /** DataBase Populate */
    private static final String DB_DATA_FILE = "/net/shibboleth/idp/attribute/resolver/spring/RdbmsData.sql";

    private DataSource datasource;
    
    private GenericApplicationContext pendingTeardownContext = null;
    
    @AfterMethod public void tearDownTestContext() {
        if (null == pendingTeardownContext ) {
            return;
        }
        pendingTeardownContext.close();
        pendingTeardownContext = null;
    }
    
    protected void setTestContext(final GenericApplicationContext context) {
        tearDownTestContext();
        pendingTeardownContext = context;
    }

    @BeforeTest public void setupDataConnectors() throws LDAPException {

        System.setProperty("org.ldaptive.provider", "org.ldaptive.provider.unboundid.UnboundIDProvider");
        
        // LDAP
        final InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=shibboleth,dc=net");
        config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", 10391));
        config.addAdditionalBindCredentials("cn=Directory Manager", "password");
        directoryServer = new InMemoryDirectoryServer(config);
        directoryServer.importFromLDIF(true, LDAP_INIT_FILE);
        directoryServer.startListening();

        // RDBMS
        datasource = DatabaseTestingSupport.GetMockDataSource(DB_INIT_FILE, "myTestDB");
        DatabaseTestingSupport.InitializeDataSourceFromFile(DB_DATA_FILE, datasource);

    }

    /**
     * Shutdown the in-memory directory server.
     */
    @AfterTest public void teardownDataConnectors() {
        directoryServer.shutDown(true);
        
        System.clearProperty("org.ldaptive.provider");
    }
    
    private ReloadableService<AttributeResolver> getResolver(final String file) {
        final GenericApplicationContext context = new GenericApplicationContext();
        context.getBeanFactory().addBeanPostProcessor(new IdentifiableBeanPostProcessor());
        setTestContext(context);
        context.setDisplayName("ApplicationContext: " + AttributeResolverTest.class);

        final ConversionServiceFactoryBean service = new ConversionServiceFactoryBean();
        context.setDisplayName("ApplicationContext: ");
        service.setConverters(new HashSet<>(Arrays.asList(new StringToDurationConverter())));
        service.afterPropertiesSet();

        context.getBeanFactory().setConversionService(service.getObject());
        
        final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions(file);
        context.refresh();

        return context.getBean(ReloadableService.class);

        
    }
    
    @Test public void service() throws ComponentInitializationException, ServiceException, ResolutionException {
        helper(true);
    }
    
    @Test public void serviceNullStrip() throws ComponentInitializationException, ServiceException, ResolutionException {
        helper(false);
    }

    private void helper(final boolean stripNulls) throws ComponentInitializationException, ServiceException, ResolutionException {

        final String inputFile;
        final int expectedEPAValues;
        if (stripNulls) {
            inputFile = "net/shibboleth/idp/attribute/resolver/spring/serviceNullStrip.xml";
            expectedEPAValues = 1;
        } else {
            inputFile ="net/shibboleth/idp/attribute/resolver/spring/service.xml";
            expectedEPAValues = 2;
        }

        final ReloadableService<AttributeResolver> attributeResolverService = getResolver(inputFile);

        attributeResolverService.initialize();

        ServiceableComponent<AttributeResolver> serviceableComponent = null;
        final AttributeResolutionContext resolutionContext =
                TestSources.createResolutionContext("PETER_THE_PRINCIPAL", "issuer", "recipient");

        try {
            serviceableComponent = attributeResolverService.getServiceableComponent();

            final AttributeResolver resolver = serviceableComponent.getComponent();
            assertEquals(resolver.getId(), "Shibboleth.Resolver");
            resolver.resolveAttributes(resolutionContext);
        } finally {
            if (null != serviceableComponent) {
                serviceableComponent.unpinComponent();
            }
        }

        final Map<String, IdPAttribute> resolvedAttributes = resolutionContext.getResolvedIdPAttributes();
        log.debug("resolved attributes '{}'", resolvedAttributes);

        assertEquals(resolvedAttributes.size(), 14);

        // Static
        IdPAttribute attribute = resolvedAttributes.get("eduPersonAffiliation");
        assertNotNull(attribute);
        List<IdPAttributeValue> values = attribute.getValues();
        
        assertEquals(values.size(), expectedEPAValues);
        assertTrue(values.contains(new StringAttributeValue("member")));
        
        // Broken (case 665)
        attribute =  resolvedAttributes.get("broken");
        assertEquals(attribute.getValues().size(), 2+expectedEPAValues);
        attribute =  resolvedAttributes.get("broken2");
        assertEquals(attribute.getValues().size(), 2+expectedEPAValues);
        

        // LDAP
        attribute = resolvedAttributes.get("uid");
        assertNotNull(attribute);
        values = attribute.getValues();
        assertEquals(values.size(), 1);
        assertTrue(values.contains(new StringAttributeValue("PETER_THE_PRINCIPAL")));

        attribute = resolvedAttributes.get("email");
        assertNotNull(attribute);
        values = attribute.getValues();
        assertEquals(values.size(), 2);
        assertTrue(values.contains(new StringAttributeValue("peterprincipal@shibboleth.net")));
        assertTrue(values.contains(new StringAttributeValue("peter.principal@shibboleth.net")));

        attribute = resolvedAttributes.get("surname");
        assertNotNull(attribute);
        values = attribute.getValues();
        assertEquals(values.size(), 1);
        assertTrue(values.contains(new StringAttributeValue("Principal")));

        attribute = resolvedAttributes.get("commonName");
        assertNotNull(attribute);
        values = attribute.getValues();
        assertEquals(values.size(), 3);
        assertTrue(values.contains(new StringAttributeValue("Peter Principal")));
        assertTrue(values.contains(new StringAttributeValue("Peter J Principal")));
        assertTrue(values.contains(new StringAttributeValue("pete principal")));

        attribute = resolvedAttributes.get("homePhone");
        assertNotNull(attribute);
        values = attribute.getValues();
        assertEquals(values.size(), 1);
        assertTrue(values.contains(new StringAttributeValue("555-111-2222")));

        // Computed
        attribute = resolvedAttributes.get("eduPersonTargetedID");
        assertNotNull(attribute);
        values = attribute.getValues();
        assertEquals(values.size(), 1);

        attribute = resolvedAttributes.get("pagerNumber");
        assertNotNull(attribute);
        values = attribute.getValues();
        assertEquals(values.size(), 1);
        assertTrue(values.contains(new StringAttributeValue("555-123-4567")));

        attribute = resolvedAttributes.get("mobileNumber");
        assertNotNull(attribute);
        values = attribute.getValues();
        assertEquals(values.size(), 1);
        assertTrue(values.contains(new StringAttributeValue("444-123-4567")));

        attribute = resolvedAttributes.get("street");
        assertNotNull(attribute);
        values = attribute.getValues();
        assertEquals(values.size(), 1);
        assertTrue(values.contains(new StringAttributeValue("TheStreet")));

        attribute = resolvedAttributes.get("title");
        assertNotNull(attribute);
        values = attribute.getValues();
        assertEquals(values.size(), 1);
        assertTrue(values.contains(new StringAttributeValue("Monsieur")));

        attribute = resolvedAttributes.get("departmentNumber");
        assertNotNull(attribute);
        values = attribute.getValues();
        assertEquals(values.size(), 1);
        assertTrue(values.contains(new StringAttributeValue("#4321")));

        final NameID nameId = new NameIDBuilder().buildObject();
        nameId.setFormat("urn:mace:shibboleth:1.0:nameIdentifier");
        nameId.setValue("MyHovercraftIsFullOfEels");
        final Subject subject = new Subject();
        subject.getPrincipals().add(new NameIDPrincipal(nameId));

        final SubjectCanonicalizationContext ctx = new SubjectCanonicalizationContext();
        ctx.setSubject(subject);
        ctx.setRequesterId("REQ");
        ctx.setResponderId("RES");

    }
    
    @Test public void mappedTemplate() throws Exception {
        final ReloadableService<AttributeResolver> attributeResolverService = getResolver("net/shibboleth/idp/attribute/resolver/spring/mappedTemplateService.xml");

        attributeResolverService.initialize();

        ServiceableComponent<AttributeResolver> serviceableComponent = null;
        final AttributeResolutionContext resolutionContext =
                TestSources.createResolutionContext("PETER_THE_PRINCIPAL", "issuer", "recipient");

        try {
            serviceableComponent = attributeResolverService.getServiceableComponent();

            final AttributeResolver resolver = serviceableComponent.getComponent();
            assertEquals(resolver.getId(), "Shibboleth.Resolver");
            resolver.resolveAttributes(resolutionContext);
        } finally {
            if (null != serviceableComponent) {
                serviceableComponent.unpinComponent();
            }
        }

        final Map<String, IdPAttribute> resolvedAttributes = resolutionContext.getResolvedIdPAttributes();
        log.debug("output {}", resolvedAttributes);
        assertEquals(resolvedAttributes.get("testing").getValues().size(), 2);
    }

    @Test public void id() throws ComponentInitializationException, ServiceException, ResolutionException {

        final ReloadableService<AttributeResolver> attributeResolverService = getResolver("net/shibboleth/idp/attribute/resolver/spring/service2.xml");

        attributeResolverService.initialize();

        ServiceableComponent<AttributeResolver> serviceableComponent = null;

        try {
            serviceableComponent = attributeResolverService.getServiceableComponent();

            final AttributeResolver resolver = serviceableComponent.getComponent();
            assertEquals(resolver.getId(), "TestID");
        } finally {
            if (null != serviceableComponent) {
                serviceableComponent.unpinComponent();
            }
        }

        final NameID nameId = new NameIDBuilder().buildObject();
        nameId.setFormat("urn:mace:shibboleth:1.0:nameIdentifier");
        nameId.setValue("MyHovercraftIsFullOfEels");
        final Subject subject = new Subject();
        subject.getPrincipals().add(new NameIDPrincipal(nameId));

        final SubjectCanonicalizationContext ctx = new SubjectCanonicalizationContext();
        ctx.setSubject(subject);
        ctx.setRequesterId("REQ");
        ctx.setResponderId("RES");

    }

    @Test public void selective() throws ResolutionException {
        final GenericApplicationContext context = new GenericApplicationContext();
        setTestContext(context);
        context.setDisplayName("ApplicationContext: " + AttributeResolverTest.class);

        final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions(new ClassPathResource(
                "net/shibboleth/idp/attribute/resolver/spring/attribute-resolver-selective.xml"),
                new ClassPathResource("net/shibboleth/idp/attribute/resolver/spring/predicates.xml"));
        context.refresh();

        final AttributeResolver resolver = BaseAttributeDefinitionParserTest.getResolver(context);
        AttributeResolutionContext resolutionContext =
                TestSources.createResolutionContext("PETER", "issuer", "recipient");

        resolver.resolveAttributes(resolutionContext);

        assertEquals(resolutionContext.getResolvedIdPAttributes().size(), 1);
        assertNotNull(resolutionContext.getResolvedIdPAttributes().get("EPA1"));

        resolutionContext = TestSources.createResolutionContext("PRINCIPAL", "ISSUER", "recipient");
        resolver.resolveAttributes(resolutionContext);
        assertEquals(resolutionContext.getResolvedIdPAttributes().size(), 1);
        assertNotNull(resolutionContext.getResolvedIdPAttributes().get("EPE"));

        resolutionContext = TestSources.createResolutionContext("OTHER", "issuer", "recipient");
        resolver.resolveAttributes(resolutionContext);
        assertTrue(resolutionContext.getResolvedIdPAttributes().isEmpty());
    }
    
    @Test public void preResolve() throws ResolutionException {
        final GenericApplicationContext context = new GenericApplicationContext();
        setTestContext(context);
        context.setDisplayName("ApplicationContext: " + AttributeResolverTest.class);

        final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions(new ClassPathResource(
                "net/shibboleth/idp/attribute/resolver/spring/attribute-resolver-preresolve.xml"));
        context.refresh();

        final AttributeResolver resolver = BaseAttributeDefinitionParserTest.getResolver(context);
        AttributeResolutionContext resolutionContext =
                TestSources.createResolutionContext("PETER", "issuer", "recipient");

        resolver.resolveAttributes(resolutionContext);
        assertEquals(resolutionContext.getResolvedIdPAttributes().size(), 2);
        final IdPAttribute pre =  resolutionContext.getResolvedIdPAttributes().get("pre");
        /* 
         * pre:
         * if (null == resolutionContext.getSubcontext("net.shibboleth.idp.attribute.context.AttributeContext", false))
         *      pre.addValue("preValueOnly");
         * else
            pre.addValue("postValueOnly") 
         *
         * it is preresolved so...
         */
        assertEquals(pre.getValues().size(), 1);
        assertEquals(pre.getValues().get(0).getDisplayValue(), "preValueOnly");
        final IdPAttribute postOnly =  resolutionContext.getResolvedIdPAttributes().get("postOnly");
        /*
         * PostOnly:
         *  ac = resolutionContext.getSubcontext("net.shibboleth.idp.attribute.context.AttributeContext", false);
         *  postOnly.getValues().addAll(ac.getIdPAttributes().get("preOnly").getValues());
         */
        assertEquals(postOnly.getValues().size(), 1);
        assertEquals(postOnly.getValues().get(0).getDisplayValue(), "preOnly");
    }

    @Test public void preResolve2() throws ResolutionException {
        final GenericApplicationContext context = new GenericApplicationContext();
        setTestContext(context);
        context.setDisplayName("ApplicationContext: " + AttributeResolverTest.class);

        final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions(new ClassPathResource(
                "net/shibboleth/idp/attribute/resolver/spring/attribute-resolver-preresolve2.xml"),
                new ClassPathResource("net/shibboleth/idp/attribute/resolver/spring/predicates.xml"));
        context.refresh();

        final AttributeResolver resolver = BaseAttributeDefinitionParserTest.getResolver(context);
        AttributeResolutionContext resolutionContext =
                TestSources.createResolutionContext("PETER", "issuer", "recipient");

        resolver.resolveAttributes(resolutionContext);
        assertEquals(resolutionContext.getResolvedIdPAttributes().size(), 1);
        final IdPAttribute pre =  resolutionContext.getResolvedIdPAttributes().get("EPE");
        assertEquals(pre.getValues().size(), 1);
        assertEquals(pre.getValues().get(0).getDisplayValue(), "urn:org:example:attribute");
    }

    @Test public void selectiveNavigate() throws ResolutionException {
        final GenericApplicationContext context = new GenericApplicationContext();
        setTestContext(context);
        context.setDisplayName("ApplicationContext: " + AttributeResolverTest.class);

        final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions(new ClassPathResource(
                "net/shibboleth/idp/attribute/resolver/spring/attribute-resolver-selective-navigate.xml"),
                new ClassPathResource("net/shibboleth/idp/attribute/resolver/spring/predicates-navigate.xml"));
        context.refresh();

        final AttributeResolver resolver =  BaseAttributeDefinitionParserTest.getResolver(context);
        AttributeResolutionContext resolutionContext =
                TestSources.createResolutionContext("PETER", "issuer", "recipient");
        resolver.resolveAttributes(resolutionContext);
        // this should fail since navigation failed.
        assertEquals(resolutionContext.getResolvedIdPAttributes().size(), 0);

        resolutionContext =
                TestSources.createResolutionContext("PETER", "issuer", "recipient");
        // add a child so we can navigate via that
        resolutionContext.getSubcontext(ProfileRequestContext.class, true);
        resolver.resolveAttributes(resolutionContext);
        assertEquals(resolutionContext.getResolvedIdPAttributes().size(), 1);
        assertNotNull(resolutionContext.getResolvedIdPAttributes().get("EPA1"));

        resolutionContext = TestSources.createResolutionContext("PRINCIPAL", "ISSUER", "recipient");
        resolutionContext.getSubcontext(ProfileRequestContext.class, true);
        resolver.resolveAttributes(resolutionContext);
        assertEquals(resolutionContext.getResolvedIdPAttributes().size(), 1);
        assertNotNull(resolutionContext.getResolvedIdPAttributes().get("EPE"));

        resolutionContext = TestSources.createResolutionContext("OTHER", "issuer", "recipient");
        resolver.resolveAttributes(resolutionContext);
        assertTrue(resolutionContext.getResolvedIdPAttributes().isEmpty());
    }
    
    @Test public void multiFile() throws ResolutionException {
        final ReloadableService<AttributeResolver> attributeResolverService = getResolver("net/shibboleth/idp/attribute/resolver/spring/multiFileService.xml");
        
        final AttributeResolutionContext resolutionContext =
                TestSources.createResolutionContext("PETER_THE_PRINCIPAL", "issuer", "recipient");

        ServiceableComponent<AttributeResolver> serviceableComponent = null;
        try {
            serviceableComponent = attributeResolverService.getServiceableComponent();

            final AttributeResolver resolver = serviceableComponent.getComponent();
            assertEquals(resolver.getId(), "MultiFileResolver");
            resolver.resolveAttributes(resolutionContext);
        } finally {
            if (null != serviceableComponent) {
                serviceableComponent.unpinComponent();
            }
        }
        
        assertNotNull(resolutionContext.getResolvedIdPAttributes().get("eduPersonAffiliation2"));

    }
    
    static class TestPredicate implements Predicate<ProfileRequestContext> {

        private final String value;
        
        private final Function<ProfileRequestContext, String> navigate; 

        public TestPredicate(final Function<ProfileRequestContext, String> profileFinder, final String compare) {
            value = Constraint.isNotNull(compare, "provided compare name must not be null");
            navigate = Constraint.isNotNull(profileFinder, "provided prinicpal locator must not be null");
        }

        /** {@inheritDoc} */
        public boolean test(@Nullable final ProfileRequestContext input) {
            return value.equals(navigate.apply(input));
        }
    }
}
