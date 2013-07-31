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

package net.shibboleth.idp.authn.impl;

import java.nio.charset.Charset;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.velocity.Template;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.profile.action.EventIds;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link DisplayUsernamePasswordPage} unit test. */
public class DisplayUsernamePasswordPageTest extends InitializeAuthenticationContextTest {
    
    private Template template;
    private DisplayUsernamePasswordPage action; 
    
    @BeforeClass public void init() throws InitializationException {
        // Initialize OpenSAML for ESAPI config.
        InitializationService.initialize();
    }
    
    @BeforeMethod public void setUp() throws Exception {
        super.setUp();

        final VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");
        velocityEngine.setProperty(RuntimeConstants.OUTPUT_ENCODING, "UTF-8");
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocityEngine.setProperty("classpath.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        velocityEngine.init();

        template = Template.fromTemplateName(velocityEngine, "/templates/login.vt", Charset.forName("UTF-8"));
        action = new DisplayUsernamePasswordPage();
    }

    @Test public void testNoTemplate() {
        try {
            action.initialize();
            Assert.fail();
        } catch (ComponentInitializationException e) {
            
        }
    }

    @Test public void testNoServlet() throws Exception {
        action.setTemplate(template);
        action.initialize();
        action.execute(prc);
        
        ActionTestingSupport.assertEvent(prc, EventIds.INVALID_PROFILE_CTX);
    }

    @Test public void testDefault() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/idp");
        request.setServletPath("/authn");
        prc.setHttpRequest(request);
        MockHttpServletResponse response = new MockHttpServletResponse();
        prc.setHttpResponse(response);
        action.setTemplate(template);
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertEquals(response.getContentType(), "text/html", "Unexpected content type");
        Assert.assertEquals("UTF-8", response.getCharacterEncoding(), "Unexpected character encoding");
        Assert.assertEquals(response.getHeader("Cache-control"), "no-cache, no-store", "Unexpected cache controls");
        Assert.assertEquals(response.getContentAsString().hashCode(), 375748888L);
    }
}