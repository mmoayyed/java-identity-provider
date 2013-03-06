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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.zip.GZIPOutputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import net.shibboleth.utilities.java.support.codec.Base64Support;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test for {@link DataSealer}.
 */
public class DataSealerTest {

    String keyStorePath;

    final private String THE_DATA = "THIS IS SOME TEST DATA";

    final private long THE_DELAY = 500;

    /**
     * Copy the JKS from the classpath into the filesystem and get the name.
     * 
     * @throws IOException
     */
    @BeforeClass public void setJKSFileName() throws IOException {
        final File out = File.createTempFile("testDataSeal", "file");

        final InputStream inStream = DataSealerTest.class.getResourceAsStream("/data/SealerKeyStore.jks");

        keyStorePath = out.getAbsolutePath();

        final OutputStream outStream = new FileOutputStream(out, false);

        final byte buffer[] = new byte[1024];

        final int bytesRead = inStream.read(buffer);
        outStream.write(buffer, 0, bytesRead);
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

        Assert.assertEquals(sealer.getKeystoreType(), KEYSTORE_TYPE);
        Assert.assertEquals(sealer.getKeystorePath(), KEYSTORE_PATH);
        Assert.assertEquals(sealer.getKeystorePassword(), KEYSTORE_PASSWORD);

        Assert.assertEquals(sealer.getCipherAlgorithm(), CIPHER_ALGORITHM);
        Assert.assertEquals(sealer.getCipherKey(), new MyKey(CIPHER_KEY));
        Assert.assertEquals(sealer.getCipherKeyAlias(), CIPHER_KEY_ALIIAS);
        Assert.assertEquals(sealer.getCipherKeyPassword(), CIPHER_KEY_PASSWORD);

        Assert.assertEquals(sealer.getMacAlgorithm(), MAC_ALGORITHM);
        Assert.assertEquals(sealer.getMacKey(), new MyKey(MAC_KEY));
        Assert.assertEquals(sealer.getMacKeyAlias(), MAC_KEY_ALIIAS);
        Assert.assertEquals(sealer.getMacKeyPassword(), MAC_KEY_PASSWORD);

        Assert.assertEquals(sealer.getRandom(), random);

    }

    private DataSealer createDataSealer() throws DataSealerException {
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

    @Test public void testEncodeDecode() throws DataSealerException {
        final DataSealer sealer = createDataSealer();

        String encoded = sealer.wrap(THE_DATA, System.currentTimeMillis() + 50000);
        Assert.assertEquals(sealer.unwrap(encoded), THE_DATA);
    }

    @Test public void testWithTimeOut() throws DataSealerException, InterruptedException {
        final DataSealer sealer = createDataSealer();

        String encoded = sealer.wrap(THE_DATA, System.currentTimeMillis() + THE_DELAY);
        Thread.sleep(THE_DELAY + 1);
        try {
            sealer.unwrap(encoded);
            Assert.fail("Should have timed out");
        } catch (DataExpiredException ex) {
            // OK
        }
    }

    @Test public void testBadValues() throws DataSealerException {
        DataSealer sealer = new DataSealer();

        try {
            sealer.init();
            Assert.fail("no keys");
        } catch (IllegalArgumentException e) {

        }
        sealer.setCipherKeyAlias("secret");
        sealer.setCipherKeyPassword("kpassword");

        sealer.setKeystorePassword("password");
        sealer.setKeystorePath(keyStorePath);

        sealer.init();

        try {
            sealer.unwrap("");
            Assert.fail("no keys");
        } catch (DataSealerException e) {
            // OK
        }

        try {
            sealer.unwrap("RandomGarbage");
            Assert.fail("no keys");
        } catch (DataSealerException e) {
            // OK
        }

        final String wrapped = sealer.wrap(THE_DATA, 3600 * 1000);

        final String corrupted = wrapped.substring(0, 10) + "A" + wrapped.substring(12);

        try {
            sealer.unwrap(corrupted);
            Assert.fail("no keys");
        } catch (DataSealerException e) {
            // OK
        }

        try {
            sealer.wrap(null, 10);
            Assert.fail("no keys");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    private String badEncrypt(DataSealer sealer, boolean shortMac, boolean badMac) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IOException,
            DataSealerException, IllegalBlockSizeException, BadPaddingException {

        long exp = System.currentTimeMillis() + THE_DELAY;
        //
        // This code pretty much taken from DataSealer. But we inject badness
        //
        Cipher cipher = Cipher.getInstance(sealer.getCipherAlgorithm());
        byte[] iv = new byte[cipher.getBlockSize()];
        sealer.getRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, sealer.getCipherKey(), ivSpec);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        GZIPOutputStream compressedStream = new GZIPOutputStream(byteStream);
        DataOutputStream dataStream = new DataOutputStream(compressedStream);

        if (shortMac) {
            dataStream.write(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9,});
        } else if (badMac) {
            dataStream.write(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22,
                    23, 24, 25, 26, 27, 28, 29, 30, 31, 32});
        } else {
            Mac mac = Mac.getInstance(sealer.getMacAlgorithm());
            mac.init(sealer.getMacKey());
            dataStream.write(DataSealer.getMAC(mac, THE_DATA, exp));
        }
        if (!shortMac) {
            dataStream.writeLong(exp);
            dataStream.writeUTF(THE_DATA);
        }
        dataStream.flush();
        compressedStream.flush();
        compressedStream.finish();
        byteStream.flush();

        byte[] encryptedData = cipher.doFinal(byteStream.toByteArray());
        byte[] handleBytes = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, handleBytes, 0, iv.length);
        System.arraycopy(encryptedData, 0, handleBytes, iv.length, encryptedData.length);

        return Base64Support.encode(handleBytes, false);
    }

    @Test public void testTamper() throws DataSealerException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException, IOException, IllegalBlockSizeException,
            BadPaddingException {

        DataSealer sealer = createDataSealer();

        //
        // First up test that the test code works
        //
        Assert.assertEquals(sealer.unwrap(badEncrypt(sealer, false, false)), THE_DATA);

        try {
            sealer.unwrap(badEncrypt(sealer, true, false));
            Assert.fail("Invalid MAC");
        } catch (DataSealerException e) {
            // OK
        }

        try {
            sealer.unwrap(badEncrypt(sealer, false, true));
            Assert.fail("short MAC");
        } catch (DataSealerException e) {
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
