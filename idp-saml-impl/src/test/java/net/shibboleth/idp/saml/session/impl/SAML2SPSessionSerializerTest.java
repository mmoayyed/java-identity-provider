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

package net.shibboleth.idp.saml.session.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;

import net.shibboleth.idp.saml.session.SAML2SPSession;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.NameID;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link SAML2SPSessionSerializer} unit test. */
@SuppressWarnings({"javadoc", "null",})
public class SAML2SPSessionSerializerTest extends OpenSAMLInitBaseTestCase {

    private static final String DATAPATH = "/net/shibboleth/idp/saml/impl/session/";
    
    private static final Instant INSTANT = Instant.ofEpochMilli(1378827849463L);
    
    private static final String SESSION_INDEX = "1234567890";
    
    private static final String CONTEXT = "context";
    
    private static final String KEY = "key";
    
    private SAML2SPSessionSerializer serializer;
    
    @BeforeMethod public void setUp() {
        serializer = new SAML2SPSessionSerializer(Duration.ofMillis(0));
    }

    @Test public void testInvalid() throws Exception {
        try {
            serializer.deserialize(1, CONTEXT, KEY, fileToString(DATAPATH + "noNameID.json"), INSTANT.toEpochMilli());
            Assert.fail();
        } catch (IOException e) {
            
        }
        
        try {
            serializer.deserialize(1, CONTEXT, KEY, fileToString(DATAPATH + "noSessionIndex.json"), INSTANT.toEpochMilli());
            Assert.fail();
        } catch (IOException e) {
            
        }

        try {
            serializer.deserialize(1, CONTEXT, KEY, fileToString(DATAPATH + "invalidXML.json"), INSTANT.toEpochMilli());
            Assert.fail();
        } catch (IOException e) {
            
        }

        try {
            serializer.deserialize(1, CONTEXT, KEY, fileToString(DATAPATH + "invalidNameID.json"), INSTANT.toEpochMilli());
            Assert.fail();
        } catch (IOException e) {
            
        }
    }
    
    @Test public void testValid() throws Exception {
        final Instant exp = INSTANT.plusSeconds(60);
        
        NameID nameID = (NameID) XMLObjectSupport.buildXMLObject(NameID.DEFAULT_ELEMENT_NAME);
        nameID.setValue("joe@example.org");
        
        SAML2SPSession session = new SAML2SPSession("test", INSTANT, exp, nameID, SESSION_INDEX, "https://sp.example.org/acs", false);
        
        String s = serializer.serialize(session);
        String s2 = fileToString(DATAPATH + "saml2SPSession.json");
        Assert.assertEquals(s, s2);
        
        SAML2SPSession session2 = (SAML2SPSession) serializer.deserialize(1, CONTEXT, KEY, s2, exp.toEpochMilli());

        Assert.assertEquals(session.getId(), session2.getId());
        Assert.assertEquals(session.getCreationInstant(), session2.getCreationInstant());
        Assert.assertEquals(session.getExpirationInstant(), session2.getExpirationInstant());
        Assert.assertEquals(session.getNameID().getValue(), session2.getNameID().getValue());
        Assert.assertEquals(session.getSessionIndex(), session2.getSessionIndex());
        Assert.assertFalse(session2.supportsLogoutPropagation());
    }
    
    private String fileToString(String pathname) throws URISyntaxException, IOException {
        try (FileInputStream stream = new FileInputStream(
                new File(SAML2SPSessionSerializerTest.class.getResource(pathname).toURI()))) {
            int avail = stream.available();
            byte[] data = new byte[avail];
            int numRead = 0;
            int pos = 0;
            do {
              if (pos + avail > data.length) {
                byte[] newData = new byte[pos + avail];
                System.arraycopy(data, 0, newData, 0, pos);
                data = newData;
              }
              numRead = stream.read(data, pos, avail);
              if (numRead >= 0) {
                pos += numRead;
              }
              avail = stream.available();
            } while (avail > 0 && numRead >= 0);
            return new String(data, 0, pos, "UTF-8");
        }
    }
}