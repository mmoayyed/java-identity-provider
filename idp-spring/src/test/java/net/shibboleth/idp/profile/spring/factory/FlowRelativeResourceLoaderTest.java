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

package net.shibboleth.idp.profile.spring.factory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for the {@link FlowRelativeResourceLoader}.
 */
public class FlowRelativeResourceLoaderTest {
    
    /** The resource loader to test.*/
    private FlowRelativeResourceLoader loader;
    
    
    /**
     * Test the getResourcesByPath method runs and correctly produces a classpath resource
     * for a wildcard classpath. Without the fix in IDP-1833, on Windows, this would throw 
     * a runtime exception (e.g. an InvalidFilePathException). 
     */
    @Test
    public void testClasspathWildcard_GetResourceByPath_IDP1833() {
        
        //the 'flow' resource from which other lookups are relative.
        Resource baseFlowResource = 
                new ClassPathResource("classpath:/net/shibboleth/idp/profile/spring/factory/idp1833-flow-example.xml");
        loader = new FlowRelativeResourceLoader(baseFlowResource);
        //there is only one of these resources, but we just need to check it does not throw an RT exception.
        final Resource resolved = 
                loader.getResourceByPath("classpath*://net/shibboleth/idp/profile/spring/factory/idp1833-beans.xml");
        //should result in a classpath resource
        Assert.assertTrue(resolved instanceof ClassPathResource);
    }
    
    /**
     * Test the getResources method runs and correctly produces a classpath resource
     * for a wildcard classpath. Without the fix in IDP-1833, on Windows, this would throw 
     * a runtime exception (e.g. an InvalidFilePathException). 
     */
    @Test
    public void testClasspathWildcard_GetResource_IDP1833() {
        
        //the 'flow' resource from which other lookups are relative.
        Resource baseFlowResource = 
                new ClassPathResource("classpath:/net/shibboleth/idp/profile/spring/factory/idp1833-flow-example.xml");
        loader = new FlowRelativeResourceLoader(baseFlowResource);
        //there is only one of these resources, but we just need to check it does not throw an RT exception.
        final Resource resolved = 
                loader.getResource("classpath*://net/shibboleth/idp/profile/spring/factory/idp1833-beans.xml");
        //should result in a classpath resource
        Assert.assertTrue(resolved instanceof ClassPathResource);
    }

}
