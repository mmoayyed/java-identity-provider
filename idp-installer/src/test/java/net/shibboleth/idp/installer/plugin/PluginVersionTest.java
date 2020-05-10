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

package net.shibboleth.idp.installer.plugin;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import org.testng.annotations.Test;

/**
 * Tests for {@link PluginVersion}
 */
@SuppressWarnings("javadoc")
public final class PluginVersionTest {

    private void failParse(final String what) {
        try {
            new PluginVersion(what);
            fail("Invalid version parsed OK");
        } catch (final NumberFormatException e) {
            return;
        }
    }
    
    @Test public void parseTest() {
        PluginVersion ver = new PluginVersion("4.2.1");
        assertEquals(ver.getMajor(), 4);
        assertEquals(ver.getMinor(), 2);
        assertEquals(ver.getPatch(), 1);

        ver = new PluginVersion("3.4");
        assertEquals(ver.getMajor(), 3);
        assertEquals(ver.getMinor(), 4);
        assertEquals(ver.getPatch(), 0);
        
        ver = new PluginVersion("2");
        assertEquals(ver.getMajor(), 2);
        assertEquals(ver.getMinor(), 0);
        assertEquals(ver.getPatch(), 0);

        // Edge cases
        ver = new PluginVersion("2.-.");
        assertEquals(ver.getMajor(), 2);
        assertEquals(ver.getMinor(), 0);
        assertEquals(ver.getPatch(), 0);

        failParse("1.2.X");
        failParse("1.Y.");
        failParse(null);
        failParse("1.jo");
        failParse("");
        failParse("XXXX");
    }

    @Test public void compareTest() {
        // check direction
        assertTrue(Integer.valueOf(-1).compareTo(Integer.valueOf(0)) < 0);
        
        assertTrue(new PluginVersion("4.5.6").compareTo(new PluginVersion(4,5,6)) == 0); 
        assertTrue(new PluginVersion(4,0,0).compareTo(new PluginVersion(3,9,9)) > 0); 
        assertTrue(new PluginVersion(4,1,0).compareTo(new PluginVersion(4,2,1)) < 0);
        assertTrue(new PluginVersion(4,1,0).compareTo(new PluginVersion(4,1,1)) < 0);
    }

}
