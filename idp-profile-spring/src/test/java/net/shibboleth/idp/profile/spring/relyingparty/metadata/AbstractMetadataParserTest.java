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

package net.shibboleth.idp.profile.spring.relyingparty.metadata;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockPropertySource;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import net.shibboleth.ext.spring.config.StringToDurationConverter;
import net.shibboleth.ext.spring.config.StringToIPRangeConverter;
import net.shibboleth.ext.spring.config.StringToResourceConverter;
import net.shibboleth.ext.spring.context.FilesystemGenericApplicationContext;
import net.shibboleth.ext.spring.util.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.saml.metadata.RelyingPartyMetadataProvider;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;

/**
 * Base class for testing metadata providers.
 */
public class AbstractMetadataParserTest extends OpenSAMLInitBaseTestCase {

    private static final String PATH = "/net/shibboleth/idp/profile/spring/relyingparty/metadata/";

    protected static final String SP_ID = "https://sp.example.org/sp/shibboleth";

    protected static final String IDP_ID = "https://idp.example.org/idp/shibboleth";

    static private String workspaceDirName;

    static private File tempDir;

    static private String tempDirName;
    
    static List<GenericApplicationContext> contexts;

    protected Object parserPool;

    @BeforeSuite public void setupDirs() throws IOException {
        final Path p = Files.createTempDirectory("MetadataProviderTest");
        tempDir = p.toFile();
        tempDirName = tempDir.getAbsolutePath();

        final ClassPathResource resource =
                new ClassPathResource("/net/shibboleth/idp/profile/spring/relyingparty/metadata");
        workspaceDirName = resource.getFile().getAbsolutePath();
        contexts = new ArrayList<>();
    }

    private void emptyDir(final File dir) {
        for (final File f : dir.listFiles()) {
            if (f.isDirectory()) {
                emptyDir(f);
            }
            f.delete();
        }
    }
    
    protected void registerContext(final GenericApplicationContext context) {
        synchronized(contexts) {
            contexts.add(context);
        }        
    }

    @AfterSuite public void deleteTmpDir() {
        emptyDir(tempDir);
        tempDir.delete();
        tempDir = null;
    }
    
    @AfterSuite public void tearDownContexts() {
        final Iterator<GenericApplicationContext> contextIterator = contexts.iterator(); 
        while (contextIterator.hasNext()) {
            final GenericApplicationContext context;
            synchronized (contexts) {
                context = contextIterator.next();
            }
            context.close();
        }
    }

    /**
     * Set up a property placeholder called DIR which points to the test directory this makes the test location
     * insensitive but able to look at the local filesystem.
     * 
     * @param context the context
     * @throws IOException
     */
    protected void setDirectoryPlaceholder(final GenericApplicationContext context) throws IOException {
        final PropertySourcesPlaceholderConfigurer placeholderConfig = new PropertySourcesPlaceholderConfigurer();
        placeholderConfig.setPlaceholderPrefix("%{");
        placeholderConfig.setPlaceholderSuffix("}");

        final MutablePropertySources propertySources = context.getEnvironment().getPropertySources();
        final MockPropertySource mockEnvVars = new MockPropertySource();
        mockEnvVars.setProperty("DIR", workspaceDirName);
        mockEnvVars.setProperty("TMPDIR", tempDirName);

        propertySources.replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, mockEnvVars);
        placeholderConfig.setPropertySources(propertySources);

        context.addBeanFactoryPostProcessor(placeholderConfig);

    }
    
    protected ApplicationContext getApplicationContext(final String contextName, final PropertySource propSource, final String... files) throws IOException {
        final Resource[] resources = new Resource[files.length];

        for (int i = 0; i < files.length; i++) {
            resources[i] = new ClassPathResource(PATH + files[i]);
        }

        final GenericApplicationContext context = new FilesystemGenericApplicationContext();
        registerContext(context);
        
        if (propSource != null) {
            context.getEnvironment().getPropertySources().addFirst(propSource);
        }
        
        setDirectoryPlaceholder(context);

        final ConversionServiceFactoryBean service = new ConversionServiceFactoryBean();
        context.setDisplayName("ApplicationContext: " + contextName);
        service.setConverters(new HashSet<>(Arrays.asList(
                new StringToDurationConverter(),
                new StringToIPRangeConverter(),
                new StringToResourceConverter())));
        service.afterPropertiesSet();

        context.getBeanFactory().setConversionService(service.getObject());

        final SchemaTypeAwareXMLBeanDefinitionReader configReader = new SchemaTypeAwareXMLBeanDefinitionReader(context);

        configReader.setValidating(true);

        configReader.loadBeanDefinitions(resources);
        context.refresh();
        
        return context;
    }
    protected ApplicationContext getApplicationContext(final String contextName, final String... files) throws IOException {
        return getApplicationContext(contextName, null, files);
    }

    protected <T> T getBean(final Class<T> claz, final PropertySource propSource, final String... files) throws IOException {
        final ApplicationContext context = getApplicationContext(claz.getCanonicalName(), propSource, files);

        if (context.containsBean("shibboleth.ParserPool")) {
            parserPool = context.getBean("shibboleth.ParserPool");
        } else {
            parserPool = null;
        }

        final T result = SpringSupport.getBean(context, claz);
        if (result != null) {
            return result;
        }

        final RelyingPartyMetadataProvider rpProvider = context.getBean(RelyingPartyMetadataProvider.class);

        return claz.cast(rpProvider.getEmbeddedResolver());
    }
    
    protected <T> T getBean(final Class<T> claz, final String... files) throws IOException {
        return getBean(claz, null, files);
    }
        
    protected MockPropertySource singletonPropertySource(final String name, final String value) {
        MockPropertySource propSource = new MockPropertySource("localProperties");
        propSource.setProperty(name, value);
        return propSource;
    }
   
    static public CriteriaSet criteriaFor(final String entityId) {
        final EntityIdCriterion criterion = new EntityIdCriterion(entityId);
        return new CriteriaSet(criterion);
    }

}
