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

package net.shibboleth.idp.authn.context;

import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.OperatingSystem;
import org.testng.annotations.Test;

import org.testng.Assert;

/**
 * Unit test for {@link UserAgentContext}.
 */
public class UserAgentContextTest {

    @Test
    public void testIsInstanceOfBrowser() throws Exception {
        final UserAgentContext ie9Win7Ctx = new UserAgentContext();
        ie9Win7Ctx.setIdentifier("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Win64; x64; Trident/5.0)");
        Assert.assertNotNull(ie9Win7Ctx.getUserAgent(), "Expected non-null user agent");
        Assert.assertTrue(ie9Win7Ctx.isInstance(Browser.IE), "Expected instance of IE");
        Assert.assertTrue(ie9Win7Ctx.isInstance(Browser.IE9), "Expected instance of IE9");
        Assert.assertFalse(ie9Win7Ctx.isInstance(Browser.FIREFOX), "Unexpected instance of Firefox");
    }

    @Test
    public void testIsInstanceOfOperatingSystem() throws Exception {
        final UserAgentContext chrome41Win7 = new UserAgentContext();
        chrome41Win7.setIdentifier(
                "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");
        Assert.assertNotNull(chrome41Win7.getUserAgent(), "Expected non-null user agent");
        Assert.assertTrue(chrome41Win7.isInstance(OperatingSystem.WINDOWS), "Expected instance of Windows");
        Assert.assertTrue(chrome41Win7.isInstance(OperatingSystem.WINDOWS_7), "Expected instance of Windows 7");
        Assert.assertFalse(chrome41Win7.isInstance(OperatingSystem.MAC_OS), "Unexpected instance of Mac OS");
    }
}