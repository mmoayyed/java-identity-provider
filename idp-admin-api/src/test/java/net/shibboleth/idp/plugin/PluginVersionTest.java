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

package net.shibboleth.idp.plugin;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import org.testng.annotations.Test;

import net.shibboleth.profile.installablecomponent.InstallableComponentVersion;

/**
 * Tests for {@link InstallableComponentVersion}
 */
@SuppressWarnings("javadoc")
public final class PluginVersionTest {

    private void failParse(final String what) {
        try {
            new InstallableComponentVersion(what);
            fail("Invalid version parsed OK");
        } catch (final NumberFormatException e) {
            return;
        }
    }
    
    @Test public void parseTest() {
        InstallableComponentVersion ver = new InstallableComponentVersion("4.2.1");
        assertEquals(ver.getMajor(), 4);
        assertEquals(ver.getMinor(), 2);
        assertEquals(ver.getPatch(), 1);

        ver = new InstallableComponentVersion("3.4");
        assertEquals(ver.getMajor(), 3);
        assertEquals(ver.getMinor(), 4);
        assertEquals(ver.getPatch(), 0);
        
        ver = new InstallableComponentVersion("2");
        assertEquals(ver.getMajor(), 2);
        assertEquals(ver.getMinor(), 0);
        assertEquals(ver.getPatch(), 0);

        // Edge cases
        ver = new InstallableComponentVersion("2.-.");
        assertEquals(ver.getMajor(), 2);
        assertEquals(ver.getMinor(), 0);
        assertEquals(ver.getPatch(), 0);

        failParse("1.2.X");
        failParse("1.Y.");
        failParse(null);
        failParse("1.jo");
        failParse("");
        failParse("XXXX");
        failParse("-1");
        failParse("1.-1");
        failParse("1.1.-1");
        failParse("10001.99.0");

        try {
            new InstallableComponentVersion(1,2,-1);
            fail("Bad version not caught");
        } catch (NumberFormatException ex) {
            // OK
        }
        try {
            new InstallableComponentVersion(10000,2,0);
            fail("Bad version not caught");
        } catch (NumberFormatException ex) {
            // OK
        }
        try {
            new InstallableComponentVersion(1, 10000,2);
            fail("Bad version not caught");
        } catch (NumberFormatException ex) {
            // OK
        }
    }

    @Test public void compareTest() {
        // check direction
        assertTrue(Integer.valueOf(-1).compareTo(Integer.valueOf(0)) < 0);
        
        assertTrue(new InstallableComponentVersion("4.5.6").compareTo(new InstallableComponentVersion(4,5,6)) == 0); 
        assertTrue(new InstallableComponentVersion(4,0,0).compareTo(new InstallableComponentVersion(3,9,9)) > 0); 
        assertTrue(new InstallableComponentVersion(4,1,0).compareTo(new InstallableComponentVersion(4,2,1)) < 0);
        assertTrue(new InstallableComponentVersion(4,1,0).compareTo(new InstallableComponentVersion(4,1,1)) < 0);
    }
}
