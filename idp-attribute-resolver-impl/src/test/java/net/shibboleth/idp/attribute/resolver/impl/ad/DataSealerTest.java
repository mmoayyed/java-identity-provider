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

package net.shibboleth.idp.attribute.resolver.impl.ad;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;

import javax.crypto.SecretKey;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test for {@link DataSealer}. 
 */
public class DataSealerTest {
     
    String keyStorePath; 
    
    /**
     * Copy the JKS from the classpath tpo the filesystem and get the name.
     * @throws IOException 
     */
    @BeforeClass public void setJKSFileName() throws IOException
    {
        final File out = File.createTempFile("testDataSeal", "file");
        
        final InputStream inStream = DataSealerTest.class.getResourceAsStream("/data/SealerKeyStore.jks");
        
        keyStorePath = out.getAbsolutePath();
        
        final OutputStream outStream = new FileOutputStream(out, false);
        
        final byte buffer[] = new byte[1024];
        
        final int bytesRead = inStream.read(buffer);
        outStream.write(buffer,0, bytesRead);
        outStream.close();
    }
    
    @Test public void testDefaults() {
        DataSealer sealer = new DataSealer();
        Assert.assertEquals(sealer.getKeystoreType(), "JCEKS");
        Assert.assertEquals(sealer.getCipherAlgorithm(), "AES/CBC/PKCS5Padding");
        Assert.assertEquals(sealer.getMacAlgorithm(), "HmacSHA256");
    }
    
    @Test public void testSetterGetters() {
        final SecureRandom random = new SecureRandom();
        final String CIPHER_ALGORITHM = "CipherAlgo";
        final String CIPHER_KEY_ALIIAS = "CipherAlias";
        final String CIPHER_KEY = "CipherKey";
        final String CIPHER_KEY_PASSWORD = "Cpassword";
        final String KEYSTORE_PASSWORD = "Kpassword";
        final String KEYSTORE_TYPE = "KType";
        final String KEYSTORE_PATH = "Kpath";
        final String MAC_ALGORITHM = "MacAlgo";
        final String MAC_KEY_ALIIAS = "MacAlias";
        final String MAC_KEY = "MaxKey";
        final String MAC_KEY_PASSWORD = "Mpassword";
        
        DataSealer sealer = new DataSealer();
        
        sealer.setKeystoreType(KEYSTORE_TYPE);
        sealer.setKeystorePath(KEYSTORE_PATH);
        sealer.setKeystorePassword(KEYSTORE_PASSWORD);
        
        sealer.setCipherAlgorithm(CIPHER_ALGORITHM);
        sealer.setCipherKey(new MyKey(CIPHER_KEY));
        sealer.setCipherKeyAlias(CIPHER_KEY_ALIIAS);
        sealer.setCipherKeyPassword(CIPHER_KEY_PASSWORD);
        
        sealer.setMacAlgorithm(MAC_ALGORITHM);
        sealer.setMacKey(new MyKey(MAC_KEY));
        sealer.setMacKeyAlias(MAC_KEY_ALIIAS);
        sealer.setMacKeyPassword(MAC_KEY_PASSWORD);
        
        sealer.setRandom(random);
        
        Assert.assertEquals(sealer.getKeystoreType(),KEYSTORE_TYPE);
        Assert.assertEquals(sealer.getKeystorePath(),KEYSTORE_PATH);
        Assert.assertEquals(sealer.getKeystorePassword(),KEYSTORE_PASSWORD);
        
        Assert.assertEquals(sealer.getCipherAlgorithm(),CIPHER_ALGORITHM);
        Assert.assertEquals(sealer.getCipherKey(),new MyKey(CIPHER_KEY));
        Assert.assertEquals(sealer.getCipherKeyAlias(),CIPHER_KEY_ALIIAS);
        Assert.assertEquals(sealer.getCipherKeyPassword(),CIPHER_KEY_PASSWORD);
        
        Assert.assertEquals(sealer.getMacAlgorithm(),MAC_ALGORITHM);
        Assert.assertEquals(sealer.getMacKey(),new MyKey(MAC_KEY));
        Assert.assertEquals(sealer.getMacKeyAlias(),MAC_KEY_ALIIAS);
        Assert.assertEquals(sealer.getMacKeyPassword(),MAC_KEY_PASSWORD);
        
        Assert.assertEquals(sealer.getRandom(),random);
        
    }
    
    private DataSealer createDataSealer() throws DataSealerException{
        DataSealer sealer = new DataSealer();
        sealer.setCipherKeyAlias("secret");
        sealer.setCipherKeyPassword("kpassword");
        
        sealer.setMacKeyAlias("secret");
        sealer.setMacKeyPassword("kpassword");

        sealer.setKeystorePassword("password");
        sealer.setKeystorePath(keyStorePath);
        
        sealer.init();

        return sealer;
    }

    @Test public void testEncodeDecode() throws DataSealerException  {
        final DataSealer sealer = createDataSealer();
        
        final String THE_DATA = "THIS IS SOME TEST DATA";
        
        String encoded = sealer.wrap(THE_DATA, System.currentTimeMillis() + 50000);
        
        Assert.assertEquals(sealer.unwrap(encoded), THE_DATA);
    }
    
    @Test public void testWithTimeOut() throws DataSealerException, InterruptedException  {
        final DataSealer sealer = createDataSealer();
        final String THE_DATA = "THIS IS SOME TEST DATA";
        final int timeout = 500;
        
        String encoded = sealer.wrap(THE_DATA, System.currentTimeMillis() + timeout);
        Thread.sleep(timeout+1);
        try {
            sealer.unwrap(encoded);
            Assert.fail("Should have timed out");
        } catch (DataExpiredException ex) {
            // OK
        }
    }
    
    private class MyKey implements SecretKey {
        
        /**
         * serialVersionUID.
         */
        private static final long serialVersionUID = 4581864892463472837L;
        final String val;
        
        MyKey(String string) {
            val = string;
        }
        
        public boolean equals(Object obj) {
            if (null == obj) {
                return false;
            }
            if (obj instanceof MyKey) {
                MyKey other = (MyKey) obj;
                return val.equals(other.getVal());
            }
            return false;
        }

        private String getVal() {
            return val;
        }

        /** {@inheritDoc} */
        public String getAlgorithm() {
            return null;
        }

        /** {@inheritDoc} */
        public byte[] getEncoded() {
            return null;
        }

        /** {@inheritDoc} */
        public String getFormat() {
            return null;
        }
        
    }
}
