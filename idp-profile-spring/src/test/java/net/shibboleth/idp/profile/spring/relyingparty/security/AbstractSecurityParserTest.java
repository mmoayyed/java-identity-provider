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

package net.shibboleth.idp.profile.spring.relyingparty.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import net.shibboleth.ext.spring.util.ApplicationContextBuilder;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockPropertySource;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeSuite;

/**
 * Base mechanics for Security parser tests
 */
public class AbstractSecurityParserTest {

    private static final String PATH = "/net/shibboleth/idp/profile/spring/relyingparty/security/";
    
    protected static final String SP_ID = "https://sp.example.org/sp/shibboleth"; 
    protected static final String IDP_ID = "https://idp.example.org/idp/shibboleth";
    
    static private String workspaceDirName;

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
   
    @BeforeSuite public void setupDirs() throws IOException {
        final ClassPathResource resource = new ClassPathResource(PATH);
        workspaceDirName = resource.getFile().getAbsolutePath();
    }

    protected <T> T getBean(final Class<T> claz, final String... files) throws IOException{
        return getBean(null, claz, files);
    }

    protected <T> T getBean(final String name, final Class<T> claz, final String... files) throws IOException{
        final Resource[] resources = new Resource[files.length + 1];
        
        for (int i = 0; i < files.length; i++) {
            resources[i] = new ClassPathResource(PATH + files[i]);
        }
        
        final ApplicationContextBuilder builder = new ApplicationContextBuilder();
        
        builder.setName("ApplicationContext: " + claz);
        
        final MockPropertySource mockEnvVars = new MockPropertySource();
        mockEnvVars.setProperty("DIR", workspaceDirName);
        builder.setPropertySources(Collections.singletonList(mockEnvVars));
        
        builder.setServiceConfigurations(Arrays.asList(resources));

        final GenericApplicationContext context = builder.build();
        
        setTestContext(context);
        
        if (name != null) {
            return context.getBean(name, claz);
        }
        return context.getBean(claz);
    }
}