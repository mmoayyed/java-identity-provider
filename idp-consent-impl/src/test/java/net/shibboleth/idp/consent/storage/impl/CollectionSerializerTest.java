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

package net.shibboleth.idp.consent.storage.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.ConstraintViolationException;

/** Unit tests for {@link CollectionSerializer}. */
@SuppressWarnings("javadoc")
public class CollectionSerializerTest {

    protected CollectionSerializer serializer;
    private Object nullObj;

    @BeforeMethod public void setUp() throws Exception {
        serializer = new CollectionSerializer();
        serializer.initialize();
    }

    @SuppressWarnings({ "null", "unchecked" })
    @Test(expectedExceptions = ConstraintViolationException.class) public void testNull() throws Exception {
        serializer.serialize((Collection<String>) nullObj);
    }

    @Test public void testEmpty() throws Exception {
        Assert.assertEquals(serializer.serialize(new ArrayList<String>()), "[]");
        Assert.assertEquals(serializer.deserialize(-1, "context", "key", "[]", null), CollectionSupport.emptyList());
    }

    @SuppressWarnings("null")
    @Test public void testNullValue() throws Exception {
        Assert.assertEquals(serializer.serialize(CollectionSupport.<String> singletonList((String)nullObj)), "[]");
        Assert.assertEquals(serializer.deserialize(-1, "context", "key", "[null]", null), CollectionSupport.emptyList());
    }

    @Test public void testSimple() throws IOException {
        final Collection<String> collection = CollectionSupport.listOf("element1", "element2", "element3");
        final String serialized = serializer.serialize(collection);
        Assert.assertEquals(serialized, "[\"element1\",\"element2\",\"element3\"]");
        final Collection<String> deserialized = serializer.deserialize(-1, "context", "key", serialized, null);
        Assert.assertEquals(deserialized, collection);
    }

}