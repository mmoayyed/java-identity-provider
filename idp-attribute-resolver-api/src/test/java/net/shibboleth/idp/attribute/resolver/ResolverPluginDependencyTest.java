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

package net.shibboleth.idp.attribute.resolver;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link ResolverPluginDependency}. */
public class ResolverPluginDependencyTest {

    /** Tests the state of a newly instantiated object. */
    @Test public void testInstantiation() {
        ResolverPluginDependency dep = new ResolverPluginDependency(" foo ", " bar ");
        Assert.assertEquals(dep.getDependencyPluginId(), "foo");
        Assert.assertEquals(dep.getDependencyAttributeId(), "bar");

        dep = new ResolverPluginDependency("foo ", "");
        Assert.assertEquals(dep.getDependencyPluginId(), "foo");
        Assert.assertEquals(dep.getDependencyAttributeId(), null);

        dep = new ResolverPluginDependency("foo ", null);
        Assert.assertEquals(dep.getDependencyPluginId(), "foo");
        Assert.assertEquals(dep.getDependencyAttributeId(), null);

        try {
            dep = new ResolverPluginDependency(null, null);
            Assert.fail("able to set null dependency ID");
        } catch (IllegalArgumentException e) {
            // expected this
        }

        try {
            dep = new ResolverPluginDependency(" ", null);
            Assert.fail("able to set empty dependency ID");
        } catch (IllegalArgumentException e) {
            // expected this
        }
    }
}