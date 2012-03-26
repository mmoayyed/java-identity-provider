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

package net.shibboleth.idp.authn;

import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link UsernamePasswordContext} unit test. */
public class UsernamePasswordContextTest {

    /** Tests that everything is properly initialized during object construction. */
    @Test public void testInstantiation() {
        UsernamePasswordContext ctx = new UsernamePasswordContext("bob", "test");
        Assert.assertEquals(ctx.getUsername(), "bob");
        Assert.assertEquals(ctx.getPassword(), "test");

        try {
            new UsernamePasswordContext(null, "test");
            Assert.fail();
        } catch (AssertionError e) {

        }

        try {
            new UsernamePasswordContext("", "test");
            Assert.fail();
        } catch (AssertionError e) {

        }

        try {
            new UsernamePasswordContext("  ", "test");
            Assert.fail();
        } catch (AssertionError e) {

        }

        try {
            new UsernamePasswordContext("bob", null);
            Assert.fail();
        } catch (AssertionError e) {

        }

        try {
            new UsernamePasswordContext("bob", "");
            Assert.fail();
        } catch (AssertionError e) {

        }

        try {
            new UsernamePasswordContext("bob", "  ");
            Assert.fail();
        } catch (AssertionError e) {

        }
    }
}