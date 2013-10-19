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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Largely boilerplate test for {@link ResolvedDataConnector}
 * 
 */
public class ResolvedDataConnectorTest {

    private Map<String, IdPAttribute> resolvedData = Collections.EMPTY_MAP;

    @Test public void init() {
        MockStaticDataConnector dc = new MockStaticDataConnector();

        try {
            new ResolvedDataConnector(null, resolvedData);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // OK
        }

        try {
            new ResolvedDataConnector(dc, null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // OK
        }

        try {
            new ResolvedDataConnector(dc, resolvedData);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // OK
        }

    }

    @Test public void equalsHashToString() throws ComponentInitializationException {
        MockStaticDataConnector dc = new MockStaticDataConnector();
        dc.setValues(Arrays.asList(new IdPAttribute("attr")));
        dc.setId("Defn");
        dc.initialize();
        ResolvedDataConnector resolvedDataConnector = new ResolvedDataConnector(dc, resolvedData);

        resolvedDataConnector.toString();

        ResolvedDataConnector otherDc;

        MockStaticDataConnector otherDef = new MockStaticDataConnector();
        otherDef.setId("OtherDefn");
        otherDef.setValues(Arrays.asList(new IdPAttribute("otherAttr")));
        otherDef.initialize();
        otherDc = new ResolvedDataConnector(otherDef, resolvedData);

        Assert.assertFalse(resolvedDataConnector.equals(null));
        Assert.assertFalse(resolvedDataConnector.equals(this));
        Assert.assertFalse(resolvedDataConnector.equals(otherDc));
        Assert.assertTrue(resolvedDataConnector.equals(resolvedDataConnector));
        Assert.assertTrue(resolvedDataConnector.equals(dc));

        Assert.assertNotSame(resolvedDataConnector.hashCode(), otherDc.hashCode());
        Assert.assertEquals(resolvedDataConnector.hashCode(), dc.hashCode());

    }

    @Test public void noops() throws ComponentInitializationException, ComponentValidationException,
            ResolutionException {
        MockStaticDataConnector dc = new MockStaticDataConnector();
        dc.setValues(Arrays.asList(new IdPAttribute("attr")));
        dc.setId("Defn");
        ResolverPluginDependency dep = new ResolverPluginDependency("doo");
        dep.setDependencyAttributeId("foo");
        dc.setDependencies(Collections.singleton(dep));
        dc.setPropagateResolutionExceptions(false);
        dc.initialize();
        ResolvedDataConnector resolvedDataConnector = new ResolvedDataConnector(dc, resolvedData);

        Assert.assertEquals(resolvedDataConnector.doDataConnectorResolve(new AttributeResolutionContext()),
                resolvedData);
        Assert.assertNull(resolvedDataConnector.getFailoverDataConnectorId());
        Assert.assertEquals(resolvedDataConnector.getResolvedConnector(), dc);
        Assert.assertTrue(resolvedDataConnector.isInitialized());

        Assert.assertEquals(resolvedDataConnector.getDependencies(), dc.getDependencies());
        Assert.assertTrue(resolvedDataConnector.getActivationCriteria().apply(null));
        Assert.assertFalse(resolvedDataConnector.isPropagateResolutionExceptions());

        //
        // TODO - do we want to do more about seeing that these are indeed noops?
        //
        resolvedDataConnector.setFailoverDataConnectorId("otherthing");

        resolvedDataConnector.setPropagateResolutionExceptions(true);
        Assert.assertFalse(resolvedDataConnector.isPropagateResolutionExceptions());

        resolvedDataConnector.doValidate();
    }
}
