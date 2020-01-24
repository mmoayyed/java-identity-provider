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

package net.shibboleth.idp.attribute.impl;

import java.io.IOException;
import java.util.Collections;

import net.shibboleth.idp.attribute.PairwiseId;
import net.shibboleth.idp.attribute.impl.ComputedPairwiseIdStore.Encoding;
import net.shibboleth.idp.testing.DatabaseTestingSupport;
import net.shibboleth.utilities.java.support.codec.Base64Support;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for {@link ComputedPairwiseIdStore}.
 */
@SuppressWarnings("javadoc")
public class ComputedPairwiseIdStoreTest {

    /** Value calculated using V2 version. DO NOT CHANGE WITHOUT TESTING AGAINST 2.0 */
    private static final String RESULT = "Vl6z6K70iLc4AuBoNeb59Dj1rGw=";

    private static final String RESULT2 = "kLyH1uEvYigEvg1ZLh/QXeW1VAs=";

    private static final String B32RESULT = "KZPLH2FO6SELOOAC4BUDLZXZ6Q4PLLDM";

    private static final byte salt[] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

    private static final String salt2 = "thisisaspecialsalt";
    
    private static final String INVALID_BASE64_SALT="AB==";
        
    public static final String COMMON_ATTRIBUTE_VALUE_STRING = "at1-Data";
    
    @Test(expectedExceptions = ComponentInitializationException.class)
    public void testInvalidConfig() throws ComponentInitializationException {
        final ComputedPairwiseIdStore store = new ComputedPairwiseIdStore();
        store.initialize();
    }

    @Test
    public void testSaltSetters() throws ComponentInitializationException {
        final ComputedPairwiseIdStore store = new ComputedPairwiseIdStore();
        store.setSalt(salt);
        Assert.assertEquals(salt, store.getSalt());
        
        store.setEncodedSalt(Base64Support.encode(salt, false));
        Assert.assertEquals(salt, store.getSalt());
    }
    
    /* test that an invalid base64 string which can not be decoded throws an illegal argument exception*/ 
    @Test(expectedExceptions = IllegalArgumentException.class) public void testInvalidBase64SaltString() {
        final ComputedPairwiseIdStore store = new ComputedPairwiseIdStore();
        store.setEncodedSalt(INVALID_BASE64_SALT);
    }

    @Test(expectedExceptions=IOException.class)
    public void testRevoked() throws Exception {
        final ComputedPairwiseIdStore store = new ComputedPairwiseIdStore();
        store.setSalt(salt);
        store.setExceptionMap(Collections.singletonMap("foo",
                Collections.<String,String>singletonMap(DatabaseTestingSupport.SP_ENTITY_ID, null)));
        store.initialize();
        
        PairwiseId pid = new PairwiseId();
        pid.setRecipientEntityID(DatabaseTestingSupport.SP_ENTITY_ID);
        pid.setPrincipalName("foo");
        pid.setSourceSystemId(COMMON_ATTRIBUTE_VALUE_STRING);
        
        store.getBySourceValue(pid, true);
    }
    
    @Test(expectedExceptions=IOException.class)
    public void testRevokedWildcardRP() throws Exception {
        final ComputedPairwiseIdStore store = new ComputedPairwiseIdStore();
        store.setSalt(salt);
        store.setExceptionMap(Collections.singletonMap("foo",
                Collections.<String,String>singletonMap(ComputedPairwiseIdStore.WILDCARD_OVERRIDE, null)));
        store.initialize();

        PairwiseId pid = new PairwiseId();
        pid.setRecipientEntityID(DatabaseTestingSupport.SP_ENTITY_ID);
        pid.setPrincipalName("foo");
        pid.setSourceSystemId(COMMON_ATTRIBUTE_VALUE_STRING);
        
        store.getBySourceValue(pid, true);
    }
    
    @Test(expectedExceptions=IOException.class)
    public void testRevokedWildcardUser() throws Exception {
        final ComputedPairwiseIdStore store = new ComputedPairwiseIdStore();
        store.setSalt(salt);
        store.setExceptionMap(Collections.singletonMap(ComputedPairwiseIdStore.WILDCARD_OVERRIDE,
                Collections.<String,String>singletonMap(DatabaseTestingSupport.SP_ENTITY_ID, null)));
        store.initialize();

        PairwiseId pid = new PairwiseId();
        pid.setRecipientEntityID(DatabaseTestingSupport.SP_ENTITY_ID);
        pid.setPrincipalName("foo");
        pid.setSourceSystemId(COMMON_ATTRIBUTE_VALUE_STRING);
        
        store.getBySourceValue(pid, true);
    }
    
    @Test
    public void testComputedId() throws Exception {
        final ComputedPairwiseIdStore store = new ComputedPairwiseIdStore();
        store.setSalt(salt);
        store.initialize();

        PairwiseId pid = new PairwiseId();
        pid.setRecipientEntityID(DatabaseTestingSupport.SP_ENTITY_ID);
        pid.setPrincipalName("foo");
        pid.setSourceSystemId(COMMON_ATTRIBUTE_VALUE_STRING);
        
        pid = store.getBySourceValue(pid, true);

        Assert.assertNotNull(pid);
        Assert.assertEquals(pid.getPairwiseId(), RESULT);
    }
    
    @Test
    public void testComputedIdOverride() throws Exception {
        final ComputedPairwiseIdStore store = new ComputedPairwiseIdStore();
        store.setSalt(salt);
        store.setExceptionMap(Collections.singletonMap(ComputedPairwiseIdStore.WILDCARD_OVERRIDE,
                Collections.<String,String>singletonMap(DatabaseTestingSupport.SP_ENTITY_ID, salt2)));
        store.initialize();

        PairwiseId pid = new PairwiseId();
        pid.setRecipientEntityID(DatabaseTestingSupport.SP_ENTITY_ID);
        pid.setPrincipalName("foo");
        pid.setSourceSystemId(COMMON_ATTRIBUTE_VALUE_STRING);
        
        pid = store.getBySourceValue(pid, true);

        Assert.assertNotNull(pid);
        Assert.assertEquals(pid.getPairwiseId(), RESULT2);
    }

    @Test
    public void testBase32ComputedId() throws Exception {
        final ComputedPairwiseIdStore store = new ComputedPairwiseIdStore();
        store.setSalt(salt);
        store.setEncoding(Encoding.BASE32);
        store.initialize();

        PairwiseId pid = new PairwiseId();
        pid.setRecipientEntityID(DatabaseTestingSupport.SP_ENTITY_ID);
        pid.setPrincipalName("foo");
        pid.setSourceSystemId(COMMON_ATTRIBUTE_VALUE_STRING);
        
        pid = store.getBySourceValue(pid, true);

        Assert.assertNotNull(pid);
        Assert.assertEquals(pid.getPairwiseId(), B32RESULT);
    }
   
}