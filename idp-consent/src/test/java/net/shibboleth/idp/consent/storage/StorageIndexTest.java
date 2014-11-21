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

import java.util.HashSet;
import java.util.Set;

import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Sets;

/** {@link StorageIndex} unit test. */
public class StorageIndexTest {

    private StorageIndex storageIndex;

    @Test public void testInstantiation() {
        storageIndex = new StorageIndex();
        Assert.assertNull(storageIndex.getContext());
        Assert.assertTrue(storageIndex.getKeys().isEmpty());

        storageIndex.setContext("context");
        storageIndex.setKeys(Sets.newHashSet("key1", "key2"));
        Assert.assertEquals(storageIndex.getContext(), "context");
        Assert.assertEquals(storageIndex.getKeys(), Sets.newHashSet("key1", "key2"));
    }

    @Test(expectedExceptions = ConstraintViolationException.class) public void testEmptyContext() {
        storageIndex = new StorageIndex();
        storageIndex.setContext("");
    }

    @Test(expectedExceptions = ConstraintViolationException.class) public void testNullContext() {
        storageIndex = new StorageIndex();
        storageIndex.setContext(null);
    }

    @Test(expectedExceptions = ConstraintViolationException.class) public void testNullKeys() {
        storageIndex = new StorageIndex();
        storageIndex.setKeys(null);
    }

    @Test public void testNullKeysElements() {
        final Set<String> keys = new HashSet<>();
        keys.add(null);

        storageIndex = new StorageIndex();
        storageIndex.setKeys(keys);
        Assert.assertTrue(storageIndex.getKeys().isEmpty());
    }
    
    @Test public void testEqualityAndHashCode() {
        final StorageIndex index1 = new StorageIndex();
        final StorageIndex index2 = new StorageIndex();
        Assert.assertEquals(index1, index1);
        Assert.assertNotEquals(index1, null);
        Assert.assertEquals(index1, index2);
        Assert.assertTrue(index1.hashCode() == index2.hashCode());
        
        index1.setContext("context1");
        Assert.assertNotEquals(index1, index2);
        Assert.assertFalse(index1.hashCode() == index2.hashCode());
        index2.setContext("context1");
        Assert.assertEquals(index1, index2);
        Assert.assertTrue(index1.hashCode() == index2.hashCode());
        
        index1.setKeys(Sets.newHashSet("key1", "key2"));
        Assert.assertNotEquals(index1, index2);
        Assert.assertFalse(index1.hashCode() == index2.hashCode());
        index2.setKeys(Sets.newHashSet("key1", "key2"));
        Assert.assertEquals(index1, index2);
        Assert.assertTrue(index1.hashCode() == index2.hashCode());
    }
}
