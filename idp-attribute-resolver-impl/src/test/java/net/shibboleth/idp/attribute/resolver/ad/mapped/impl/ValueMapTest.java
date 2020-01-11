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

package net.shibboleth.idp.attribute.resolver.ad.mapped.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * Tests for {@link ValueMap}.
 */
@SuppressWarnings("javadoc")
public class ValueMapTest {
    
    
    @Test public void setterGetter() throws ComponentInitializationException {
        final SourceValue value = SourceValueTest.newSourceValue("value", true, true);
        
        final ValueMap map = new ValueMap();
        
        map.setSourceValues(Collections.singleton(value));
        map.setReturnValue("return");
        
        
        assertEquals(map.getReturnValue(), "return");
        assertEquals(map.getSourceValues().size(), 1);
        assertTrue(map.getSourceValues().contains(value));
    }
    
    @Test public void subString() throws ComponentInitializationException {
        final SourceValue value = SourceValueTest.newSourceValue("value", true, true);
        
        final ValueMap map = new ValueMap();
        
        map.setSourceValues(Collections.singleton(value));
        map.setReturnValue("return");
        
        Set<StringAttributeValue> result = map.apply("elephant");
        
        assertTrue(result.isEmpty());

        result = map.apply("elephantvaluegiraffe");
        assertEquals(result.size(), 1);
        assertTrue(result.contains(new StringAttributeValue("return")));
    }

    @Test public void regexp() throws ComponentInitializationException {
        final HashSet<SourceValue> sources = new HashSet<>(3);
        
        sources.add(SourceValueTest.newSourceValue("R(.+)", false, false));
        sources.add(SourceValueTest.newSourceValue("RE(.+)", true, false));
        final ValueMap map = new ValueMap();
        map.setSourceValues(sources);
        map.setReturnValue("foo$1");
        
        Set<StringAttributeValue> result = map.apply("elephant");
        assertTrue(result.isEmpty());

        result = map.apply("Recursion");
        assertEquals(result.size(), 2);
        assertTrue(result.contains(new StringAttributeValue("fooecursion")));
        assertTrue(result.contains(new StringAttributeValue("foocursion")));
        
    }

}
