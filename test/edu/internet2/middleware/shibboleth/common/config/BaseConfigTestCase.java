/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.config;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import edu.internet2.middleware.shibboleth.common.BaseTestCase;

/**
 * Base unit test case for Spring configuration tests.
 */
public class BaseConfigTestCase extends BaseTestCase {
    
    /**
     * Creates a Spring application context from the given configuration.
     * 
     * @param config spring configuration file to be located on the classpath
     * 
     * @return the configured spring context
     */
    protected ApplicationContext createSpringContext(String config){
        String[] configs = new String[1];
        configs[0] = config;
        return createSpringContext(configs);
    }
    
    /**
     * Creates a Spring application context from the given configurations.
     * 
     * @param configs spring configuration files to be located on the classpath
     * 
     * @return the configured spring context
     */
    protected ApplicationContext createSpringContext(String[] configs){
        GenericApplicationContext gContext = new GenericApplicationContext();
        
        XmlBeanDefinitionReader configReader = new XmlBeanDefinitionReader(gContext);
        configReader.setDocumentLoader(new SpringDocumentLoader());
        
        Resource[] configSources = new Resource[configs.length];
        for(int i = 0; i < configs.length; i++){
            configSources[i] = new ClassPathResource(configs[i]);
        }
        
        configReader.loadBeanDefinitions(configSources);
        
        return gContext;
    }
}