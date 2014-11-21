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
import java.util.HashMap;
import java.util.Map;

import net.shibboleth.idp.consent.ConsentTestingSupport;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for {@link StorageIndexSerializer}. */
public class StorageIndexSerializerTest {

    protected Map<String, StorageIndex> storageIndexes;

    protected StorageIndex storageIndex1;

    protected StorageIndex storageIndex2;

    protected StorageIndexSerializer serializer;

    @BeforeMethod public void setUp() {

        storageIndexes = ConsentTestingSupport.newStorageIndexMap();

        serializer = new StorageIndexSerializer();
    }

    @Test(expectedExceptions = ConstraintViolationException.class) public void testNull() throws Exception {
        serializer.initialize();
        serializer.serialize(null);
    }

    @Test(expectedExceptions = ConstraintViolationException.class) public void testEmpty() throws Exception {
        serializer.initialize();
        serializer.serialize(new HashMap<String, StorageIndex>());
    }

    @Test public void testSimple() throws IOException {

        final String serialized = serializer.serialize(storageIndexes);
        Assert.assertEquals(serialized,
                "[{\"ctx\":\"context2\",\"keys\":[\"key1\",\"key2\"]},{\"ctx\":\"context1\",\"keys\":[\"key1\"]}]");

        final Map<String, StorageIndex> deserialized = serializer.deserialize(-1, "context", "key", serialized, null);

        Assert.assertEquals(deserialized, storageIndexes);
    }
}
