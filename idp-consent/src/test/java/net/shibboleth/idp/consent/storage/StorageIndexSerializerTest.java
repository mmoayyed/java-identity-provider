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

package net.shibboleth.idp.consent.storage;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for {@link StorageIndexSerializer}. */
// TODO incomplete
public class StorageIndexSerializerTest {

    /** Class logger. */
    @Nonnull protected final Logger log = LoggerFactory.getLogger(StorageIndexSerializerTest.class);

    protected Map<String, StorageIndex> storageIndexes;

    protected StorageIndex storageIndex1;

    protected StorageIndex storageIndex2;

    protected StorageIndexSerializer serializer;

    @BeforeMethod public void setUp() {

        storageIndex1 = new StorageIndex();
        storageIndex1.setContext("context1");
        storageIndex1.getKeys().add("key1");

        storageIndex2 = new StorageIndex();
        storageIndex2.setContext("context2");
        storageIndex2.getKeys().add("key1");
        storageIndex2.getKeys().add("key2");

        storageIndexes = new LinkedHashMap<>();
        storageIndexes.put(storageIndex1.getContext(), storageIndex1);
        storageIndexes.put(storageIndex2.getContext(), storageIndex2);

        serializer = new StorageIndexSerializer();
    }

    @Test public void testSimple() throws IOException {

        final String serialized = serializer.serialize(storageIndexes);

        final Map<String, StorageIndex> deserialized = serializer.deserialize(-1, "context", "key", serialized, null);

        Assert.assertEquals(deserialized, storageIndexes);
    }
}
