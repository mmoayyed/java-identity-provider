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

package net.shibboleth.idp.session.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import net.shibboleth.idp.session.BasicServiceSession;
import net.shibboleth.idp.session.ServiceSession;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link BasicServiceSessionSerializer} unit test. */
public class BasicServiceSessionSerializerTest {

    private static final String DATAPATH = "/data/net/shibboleth/idp/session/impl/";
    
    private static final long INSTANT = 1378827849463L;
    
    private BasicServiceSessionSerializer serializer;
    
    @BeforeMethod public void setUp() {
        serializer = new BasicServiceSessionSerializer();
    }

    @Test public void testInvalid() throws Exception {
        try {
            serializer.deserialize(fileToString(DATAPATH + "invalid.json"), null, null, null);
            Assert.fail();
        } catch (IOException e) {
            
        }

        try {
            serializer.deserialize(fileToString(DATAPATH + "noServiceId.json"), null, null, null);
            Assert.fail();
        } catch (IOException e) {
            
        }
        
        try {
            serializer.deserialize(fileToString(DATAPATH + "noFlowId.json"), null, null, null);
            Assert.fail();
        } catch (IOException e) {
            
        }

        try {
            serializer.deserialize(fileToString(DATAPATH + "noInstant.json"), null, null, null);
            Assert.fail();
        } catch (IOException e) {
            
        }

        try {
            // Tests expiration being null.
            serializer.deserialize(fileToString(DATAPATH + "basicServiceSession.json"), null, null, null);
            Assert.fail();
        } catch (IOException e) {
            
        }
    }
    
    @Test public void testBasic() throws Exception {
        long exp = INSTANT + 60000L;
        
        BasicServiceSession session = new BasicServiceSession("test", "foo", INSTANT, exp);
        
        String s = serializer.serialize(session);
        String s2 = fileToString(DATAPATH + "basicServiceSession.json");
        Assert.assertEquals(s, s2);
        
        ServiceSession session2 = serializer.deserialize(s2, null, null, exp);

        Assert.assertEquals(session.getId(), session2.getId());
        Assert.assertEquals(session.getAuthenticationFlowId(), session2.getAuthenticationFlowId());
        Assert.assertEquals(session.getCreationInstant(), session2.getCreationInstant());
        Assert.assertEquals(session.getExpirationInstant(), session2.getExpirationInstant());
    }
    
    private String fileToString(String pathname) throws URISyntaxException, IOException {
        try (FileInputStream stream = new FileInputStream(
                new File(BasicServiceSessionSerializerTest.class.getResource(pathname).toURI()))) {
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