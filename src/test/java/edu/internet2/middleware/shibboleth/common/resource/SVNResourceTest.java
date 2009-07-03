/*
 * Copyright 2008 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.common.resource;

import java.io.File;

import junit.framework.TestCase;

import org.opensaml.xml.util.DatatypeHelper;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;

/** Unit test for {@link SVNResource }. */
public class SVNResourceTest extends TestCase{
    
    private File workingCopyDirectory;
    
    /** {@inheritDoc} */
    protected void setUp() throws Exception {
        super.setUp();
        
        workingCopyDirectory = new File(System.getProperty("java.io.tmpdir") + File.separator + "svntest");
    }
    
    /** {@inheritDoc} */
    protected void tearDown() throws Exception {
        workingCopyDirectory.delete();
    }
    
    public void test() throws Exception{
        String url = "https://svn.middleware.georgetown.edu/java-shib-common/branches/REL_1/src/test/resources/data/edu/internet2/middleware/shibboleth/common/attribute/filtering";
        SVNURL svnurl = SVNURL.parseURIDecoded(url);
        SVNClientManager manager = SVNClientManager.newInstance();
        
        // this will cause a checkout
        SVNResource resource = new SVNResource(manager, svnurl, workingCopyDirectory, 740, "policy1.xml");
        assertEquals(5, workingCopyDirectory.list().length);
        assertEquals(url + "/policy1.xml", resource.getLocation());
        assertTrue(DatatypeHelper.inputstreamToString(resource.getInputStream(), null).startsWith("<afp:AttributeFilterPolicyGroup id=\"PolicyExample1\""));
        
        // this will cause an update
        resource = new SVNResource(manager, svnurl, workingCopyDirectory, 740, "policy1.xml");
        assertEquals(5, workingCopyDirectory.list().length);
        assertEquals(url + "/policy1.xml", resource.getLocation());
        assertTrue(DatatypeHelper.inputstreamToString(resource.getInputStream(), null).startsWith("<afp:AttributeFilterPolicyGroup id=\"PolicyExample1\""));
    }
}