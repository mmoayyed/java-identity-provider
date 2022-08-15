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

package net.shibboleth.idp.attribute.resolver.spring.ad;

import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

import org.springframework.beans.factory.BeanCreationException;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.resolver.ad.impl.ScopedAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.spring.testing.BaseAttributeDefinitionParserTest;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * Test for {@link ScopedAttributeDefinitionParser}.
 */
@SuppressWarnings("javadoc")
public class ScopedAttributeDefinitionParserTest extends BaseAttributeDefinitionParserTest {

    @Test public void scope() {
        ScopedAttributeDefinition attrDef = getAttributeDefn("resolver/scoped.xml", ScopedAttributeDefinition.class);

        assertEquals(attrDef.getId(), "scoped");
        assertEquals(attrDef.getScope(), "mYsCoPe");
        assertNull(attrDef.getScopeSource());
    }

    @Test public void source() {
        ScopedAttributeDefinition attrDef = getAttributeDefn("resolver/scopedSource.xml", ScopedAttributeDefinition.class);

        assertEquals(attrDef.getId(), "scopedSource");
        assertEquals(attrDef.getScopeSource(), "TheScopeSourceAttribute");
        assertNull(attrDef.getScope());
    }

    @Test public void both() {
        try {
            getAttributeDefn("resolver/scopedBoth.xml", ScopedAttributeDefinition.class);
        } catch (final BeanCreationException e) {
            assertEquals(e.getRootCause().getClass(), ComponentInitializationException.class);
            return;
        }
        fail("Did not catch impossible setup");
    }

}
