/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.profile.support;

import org.springframework.mock.web.MockHttpServletRequest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.shared.collection.CollectionSupport;

/** {@link PathInfoSupportingFlowUrlHandler} unit test. */
@SuppressWarnings("javadoc")
public class PathInfoSupportingFlowUrlHandlerTest {

    MockHttpServletRequest request;
    PathInfoSupportingFlowUrlHandler handler;
    
    @BeforeMethod public void setUp() {
        request = new MockHttpServletRequest();
        handler = new PathInfoSupportingFlowUrlHandler();
    }
    
    @Test public void testNoPathInfo() {
        // No path info
        Assert.assertNull(handler.getFlowId(request));
    }

    @Test public void testNoneSupported() {
        request.setPathInfo("/admin/foo/bar/baz");
        Assert.assertEquals("admin/foo/bar/baz", handler.getFlowId(request));
    }

    @Test public void testNoMatch() {
        request.setPathInfo("/admin/foo/bar/baz");
        
        handler.setSupportedFlows(CollectionSupport.singletonList("admit/fop"));
        Assert.assertEquals("admin/foo/bar/baz", handler.getFlowId(request));
    }

    @Test public void testSuported() {
        request.setPathInfo("/admin/foo/bar/baz");
        
        handler.setSupportedFlows(CollectionSupport.singletonList("admin/foo/bar"));
        Assert.assertEquals("admin/foo/bar", handler.getFlowId(request));
        
        handler.setSupportedFlows(CollectionSupport.singletonList("admin/foo"));
        Assert.assertEquals("admin/foo", handler.getFlowId(request));
    }

    @Test public void testOverlapping() {
        request.setPathInfo("/admin/foo/bar/baz");
        
        handler.setSupportedFlows(CollectionSupport.arrayAsList("admin/foo/bar", "admin/foo", "admin"));
        Assert.assertEquals("admin/foo/bar", handler.getFlowId(request));

        handler.setSupportedFlows(CollectionSupport.arrayAsList("admin", "admin/foo", "admin/foo/bar"));
        Assert.assertEquals("admin", handler.getFlowId(request));
    }
    
}