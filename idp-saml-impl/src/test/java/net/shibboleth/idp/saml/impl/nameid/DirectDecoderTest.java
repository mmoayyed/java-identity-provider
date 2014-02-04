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

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;

import org.opensaml.profile.ProfileException;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.opensaml.saml.saml1.core.impl.NameIdentifierBuilder;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.impl.NameIDBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link DirectNameIDDecoder} unit test. */
public class DirectDecoderTest {

    private static final String PRINCIPAL="ThePrincipalName";

    @Test public void testSucess() throws ProfileException, ComponentInitializationException, IOException {
        DirectNameIDDecoder decode = new DirectNameIDDecoder();
        decode.setId("Decoder");
        decode.initialize();
        
        NameID nameId = new NameIDBuilder().buildObject();
        nameId.setValue(PRINCIPAL);
        
        Assert.assertEquals(decode.decode(nameId, null, null), PRINCIPAL);

    }

    @Test(expectedExceptions={UninitializedComponentException.class,}) public void testNoinit() throws ProfileException, ComponentInitializationException, IOException {

        DirectNameIDDecoder decode = new DirectNameIDDecoder();
        decode.setId("Decoder");
        NameID nameId = new NameIDBuilder().buildObject();
        nameId.setValue(PRINCIPAL);
        decode.decode(nameId, null, null);
    }

    @Test public void testSAML1Sucess() throws ProfileException, ComponentInitializationException, IOException {
        DirectNameIdentifierDecoder decode = new DirectNameIdentifierDecoder();
        decode.setId("Decoder");
        decode.initialize();
        
        NameIdentifier nameIdentifier = new NameIdentifierBuilder().buildObject();
        nameIdentifier.setNameIdentifier(PRINCIPAL);
        
        Assert.assertEquals(decode.decode(nameIdentifier, null, null), PRINCIPAL);

    }

    @Test(expectedExceptions={UninitializedComponentException.class,}) public void testSAML1Noinit() throws ProfileException, ComponentInitializationException, IOException {

        DirectNameIdentifierDecoder decode = new DirectNameIdentifierDecoder();
        decode.setId("Decoder");
        NameIdentifier nameIdentifier = new NameIdentifierBuilder().buildObject();
        nameIdentifier.setNameIdentifier(PRINCIPAL);
        decode.decode(nameIdentifier, null, null);
    }

}