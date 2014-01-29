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

package net.shibboleth.idp.saml.impl.nameid;

import java.io.IOException;

import net.shibboleth.idp.authn.SubjectCanonicalizationException;
import net.shibboleth.idp.saml.nameid.TransientIdParameters;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.profile.ProfileException;
import org.opensaml.storage.StorageService;
import org.opensaml.storage.impl.MemoryStorageService;
import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link TransientDecoder} unit test. */
public class TransientDecoderTest {

    private static final String RECIPIENT="TheRecipient";
    private static final String PRINCIPAL="ThePrincipalName";

    @Test public void testSucess() throws ProfileException, ComponentInitializationException, IOException {

        final StorageService store = new MemoryStorageService();
        store.initialize();

        final String principalTokenId;
        principalTokenId = new TransientIdParameters(RECIPIENT, PRINCIPAL).encode();

        final String id = "THE_ID";

        final long expiration = System.currentTimeMillis() + 50000;

        Assert.assertTrue(store.create(TransientIdParameters.CONTEXT, id, principalTokenId, expiration),
                "initial store");

        TransientDecoder decoder = new TransientDecoder();
        decoder.setId("decoder");
        decoder.setIdStore(store);
        decoder.initialize();

        Assert.assertEquals(decoder.decode(id, "ME", RECIPIENT), PRINCIPAL);

    }

    @Test(expectedExceptions={SubjectCanonicalizationException.class,}) public void testExpired() throws ProfileException, ComponentInitializationException, IOException {

        final StorageService store = new MemoryStorageService();
        store.initialize();

        final String principalTokenId;
        principalTokenId = new TransientIdParameters(RECIPIENT, PRINCIPAL).encode();

        final String id = "THE_ID";

        final long expiration = System.currentTimeMillis() - 50000;

        Assert.assertTrue(store.create(TransientIdParameters.CONTEXT, id, principalTokenId, expiration),
                "initial store");

        TransientDecoder decoder = new TransientDecoder();
        decoder.setId("decoder");
        decoder.setIdStore(store);
        decoder.initialize();

        decoder.decode(id, "ME", RECIPIENT);

    }


    @Test(expectedExceptions={SubjectCanonicalizationException.class,})  public void testNotFound() throws ProfileException, ComponentInitializationException, IOException {

        final StorageService store = new MemoryStorageService();
        store.initialize();

        TransientDecoder decoder = new TransientDecoder();
        decoder.setId("decoder");
        decoder.setIdStore(store);
        decoder.initialize();

        decoder.decode("THE_ID", "ME", RECIPIENT);

    }


    @Test(expectedExceptions={SubjectCanonicalizationException.class,}) public void testBadRecipient() throws ProfileException, ComponentInitializationException, IOException {

        final StorageService store = new MemoryStorageService();
        store.initialize();

        final String principalTokenId;
        principalTokenId = new TransientIdParameters(RECIPIENT, PRINCIPAL).encode();

        final String id = "THE_ID";

        final long expiration = System.currentTimeMillis() + 50000;

        Assert.assertTrue(store.create(TransientIdParameters.CONTEXT, id, principalTokenId, expiration),
                "initial store");

        TransientDecoder decoder = new TransientDecoder();
        decoder.setId("decoder");
        decoder.setIdStore(store);
        decoder.initialize();

        decoder.decode(id, "ME", PRINCIPAL);

    }


}