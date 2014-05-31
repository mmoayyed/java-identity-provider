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

import net.shibboleth.ext.spring.config.DurationToLongConverter;
import net.shibboleth.ext.spring.config.StringToIPRangeConverter;
import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.saml.metadata.impl.RelyingPartyMetadataProvider;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockPropertySource;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import com.google.common.collect.Sets;

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
    
    protected Object parserPool;
    
    @BeforeSuite public void setupDirs() throws IOException {
        final Path p = Files.createTempDirectory("MetadataProviderTest");
        tempDir = p.toFile();
        tempDirName = tempDir.getAbsolutePath();
        
        final ClassPathResource resource = new ClassPathResource("/net/shibboleth/idp/profile/spring/relyingparty/metadata");
        workspaceDirName = resource.getFile().getAbsolutePath();
    }
    
    private void emptyDir(File dir) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                emptyDir(f);
            }
            f.delete();
        }
    }
    
    @AfterSuite public void deleteTmpDir() {
        emptyDir(tempDir);
        tempDir.delete();
        tempDir = null;
    }
    
    /**
     * Set up a property placeholder called DIR which points to the test directory
     * this makes the test location insensitive but able to look at the local
     * filesystem.
     * @param context the context
     * @throws IOException 
     */
    protected void setDirectoryPlaceholder(GenericApplicationContext context) throws IOException {
        PropertySourcesPlaceholderConfigurer placeholderConfig = new PropertySourcesPlaceholderConfigurer();
        
        MutablePropertySources propertySources = context.getEnvironment().getPropertySources();
        MockPropertySource mockEnvVars = new MockPropertySource();
        mockEnvVars.setProperty("DIR", workspaceDirName);
        mockEnvVars.setProperty("TMPDIR", tempDirName);
        
        propertySources.replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, mockEnvVars);
        placeholderConfig.setPropertySources(propertySources);
        
        context.addBeanFactoryPostProcessor(placeholderConfig);
        
    }
    
    protected <T extends MetadataResolver> T getBean(Class<T> claz,  boolean validating, String... files) throws IOException{
        final Resource[] resources = new Resource[files.length];
       
        for (int i = 0; i < files.length; i++) {
            resources[i] = new ClassPathResource(PATH + files[i]);
        }
        
        final GenericApplicationContext context = new GenericApplicationContext();

        setDirectoryPlaceholder(context);
        
        ConversionServiceFactoryBean service = new ConversionServiceFactoryBean();
        context.setDisplayName("ApplicationContext: " + claz);
        service.setConverters(Sets.newHashSet(new DurationToLongConverter(), new StringToIPRangeConverter()));
        service.afterPropertiesSet();

        context.getBeanFactory().setConversionService(service.getObject());

        final XmlBeanDefinitionReader configReader = new XmlBeanDefinitionReader(context);


        configReader.setValidating(validating);
        
        configReader.loadBeanDefinitions(resources);
        context.refresh();
        
        if (context.containsBean("shibboleth.ParserPool")) {
            parserPool = context.getBean("shibboleth.ParserPool");
        } else {
            parserPool = null;
        }
        
        T result = SpringSupport.getBean(context, claz);
        if (result != null) {
            return result;
        }
        
        RelyingPartyMetadataProvider rpProvider = context.getBean(RelyingPartyMetadataProvider.class);

        return claz.cast(rpProvider.getEmbeddedResolver());
    }
    
    static public CriteriaSet criteriaFor(String entityId) {
        EntityIdCriterion criterion = new EntityIdCriterion(entityId);
        return  new CriteriaSet(criterion);
    }
    
 }
