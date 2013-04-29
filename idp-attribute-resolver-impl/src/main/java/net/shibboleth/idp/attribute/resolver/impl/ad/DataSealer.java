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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import net.shibboleth.utilities.java.support.codec.Base64Support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//TODO This needs to find a better home 

/**
 * Applies a MAC to time-limited information and encrypts with a symmetric key.
 * 
 * @author Scott Cantor
 * @author Walter Hoehn
 * @author Derek Morr
 */
public class DataSealer {

    /** Class logger. */
    private Logger log = LoggerFactory.getLogger(DataSealer.class);

    /** Key used for encryption. */
    private SecretKey cipherKey;

    /** Key used for MAC. */
    private SecretKey macKey;

    /** Source of secure random data. */
    private SecureRandom random;

    /** Type of keystore to use for access to keys. */
    private String keystoreType = "JCEKS";

    /** Path to keystore. */
    private String keystorePath;

    /** Password for keystore. */
    private String keystorePassword;

    /** Keystore alias for the encryption key. */
    private String cipherKeyAlias;

    /** Password for encryption key. */
    private String cipherKeyPassword;

    /** Encryption algorithm to use. */
    private String cipherAlgorithm = "AES/CBC/PKCS5Padding";

    /** Keystore alias for the MAC key. */
    private String macKeyAlias;

    /** Password for MAC key. */
    private String macKeyPassword;

    /** MAC algorithm to use. */
    private String macAlgorithm = "HmacSHA256";

    /**
     * Initialization method used after setting all relevant bean properties.
     * 
     * @throws DataSealerException if initialization fails
     */
    public void init() throws DataSealerException {
        try {
            if (cipherKey == null) {
                if (keystoreType == null || keystorePath == null || keystorePassword == null || cipherKeyAlias == null
                        || cipherKeyPassword == null) {
                    throw new IllegalArgumentException("Missing a required configuration property.");
                }
            }

            if (random == null) {
                random = new SecureRandom();
            }

            loadKeys();

            // Before we finish initialization, make sure that things are working.
            testEncryption();

        } catch (GeneralSecurityException e) {
            log.error(e.getMessage());
            throw new DataSealerException("Caught NoSuchAlgorithmException loading the java keystore.", e);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new DataSealerException("Caught IOException loading the java keystore.", e);
        }
    }

    /**
     * Returns the encryption key.
     * 
     * @return the encryption key
     */
    @Nullable public SecretKey getCipherKey() {
        return cipherKey;
    }

    /**
     * Returns the MAC key, if different from the encryption key.
     * 
     * @return the MAC key
     */
    @Nullable public SecretKey getMacKey() {
        return macKey;
    }

    /**
     * Returns the pseudorandom generator.
     * 
     * @return the pseudorandom generator
     */
    @Nullable public SecureRandom getRandom() {
        return random;
    }

    /**
     * Returns the keystore type.
     * 
     * @return the keystore type.
     */
    @Nullable public String getKeystoreType() {
        return keystoreType;
    }

    /**
     * Returns the keystore path.
     * 
     * @return the keystore path
     */
    @Nullable public String getKeystorePath() {
        return keystorePath;
    }

    /**
     * Returns the keystore password.
     * 
     * @return the keystore password
     */
    @Nullable public String getKeystorePassword() {
        return keystorePassword;
    }

    /**
     * Returns the encryption key alias.
     * 
     * @return the encryption key alias
     */
    @Nullable public String getCipherKeyAlias() {
        return cipherKeyAlias;
    }

    /**
     * Returns the encryption key password.
     * 
     * @return the encryption key password
     */
    @Nullable public String getCipherKeyPassword() {
        return cipherKeyPassword;
    }

    /**
     * Returns the encryption algorithm.
     * 
     * @return the encryption algorithm
     */
    @Nullable public String getCipherAlgorithm() {
        return cipherAlgorithm;
    }

    /**
     * Returns the MAC key alias.
     * 
     * @return the MAC key alias
     */
    @Nullable public String getMacKeyAlias() {
        return macKeyAlias;
    }

    /**
     * Returns the MAC key password.
     * 
     * @return the MAC key password
     */
    @Nullable public String getMacKeyPassword() {
        return macKeyPassword;
    }

    /**
     * Returns the MAC algorithm.
     * 
     * @return the MAC algorithm
     */
    @Nullable public String getMacAlgorithm() {
        return macAlgorithm;
    }

    /**
     * Sets the encryption key.
     * 
     * @param key the encryption key to set
     */
    public void setCipherKey(SecretKey key) {
        cipherKey = key;
    }

    /**
     * Sets the MAC key.
     * 
     * @param key the MAC key to set
     */
    public void setMacKey(SecretKey key) {
        macKey = key;
    }

    /**
     * Sets the pseudorandom generator.
     * 
     * @param r the pseudorandom generator to set
     */
    public void setRandom(SecureRandom r) {
        random = r;
    }

    /**
     * Sets the keystore type.
     * 
     * @param type the keystore type to set
     */
    public void setKeystoreType(String type) {
        keystoreType = type;
    }

    /**
     * Sets the keystore path.
     * 
     * @param path the keystore path to set
     */
    public void setKeystorePath(String path) {
        keystorePath = path;
    }

    /**
     * Sets the keystore password.
     * 
     * @param password the keystore password to set
     */
    public void setKeystorePassword(String password) {
        keystorePassword = password;
    }

    /**
     * Sets the encryption key alias.
     * 
     * @param alias the encryption key alias to set
     */
    public void setCipherKeyAlias(String alias) {
        cipherKeyAlias = alias;
    }

    /**
     * Sets the encryption key password.
     * 
     * @param password the encryption key password to set
     */
    public void setCipherKeyPassword(@Nullable String password) {
        cipherKeyPassword = password;
    }

    /**
     * Sets the encryption algorithm.
     * 
     * @param alg the encryption algorithm to set
     */
    public void setCipherAlgorithm(@Nullable String alg) {
        cipherAlgorithm = alg;
    }

    /**
     * Sets the MAC key alias.
     * 
     * @param alias the MAC key alias to set
     */
    public void setMacKeyAlias(@Nullable String alias) {
        macKeyAlias = alias;
    }

    /**
     * Sets the MAC key password.
     * 
     * @param password the the MAC key password to set
     */
    public void setMacKeyPassword(@Nullable String password) {
        macKeyPassword = password;
    }

    /**
     * Sets the MAC key algorithm.
     * 
     * @param alg the MAC algorithm to set
     */
    public void setMacAlgorithm(@Nullable String alg) {
        macAlgorithm = alg;
    }

    /**
     * Decrypts and verifies an encrypted bundle of MAC'd data, and returns it.
     * 
     * @param wrapped the encoded blob
     * @return the decrypted data, if it's unexpired
     * @throws DataSealerException if the data cannot be unwrapped and verified
     */
    @Nonnull public String unwrap(@Nonnull String wrapped) throws DataSealerException {

        try {
            byte[] in = Base64Support.decode(wrapped);

            Cipher cipher = Cipher.getInstance(cipherAlgorithm);
            int ivSize = cipher.getBlockSize();
            byte[] iv = new byte[ivSize];


            if (in.length < ivSize) {
                log.error("Wrapped data is malformed (not enough bytes).");
                throw new DataSealerException("Wrapped data is malformed (not enough bytes).");
            }

            // extract the IV, setup the cipher and extract the encrypted handle
            System.arraycopy(in, 0, iv, 0, ivSize);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, cipherKey, ivSpec);

            byte[] encryptedHandle = new byte[in.length - iv.length];
            System.arraycopy(in, ivSize, encryptedHandle, 0, in.length - iv.length);

            // decrypt the rest of the data and setup the streams
            byte[] decryptedBytes = cipher.doFinal(encryptedHandle);
            return extractAndCheckDecryptedData(decryptedBytes);

        } catch (GeneralSecurityException e) {
            log.error(e.getMessage());
            throw new DataSealerException("Caught GeneralSecurityException unwrapping data.", e);
        }
    }

    /**
     * Extract the components from the provided data stream decode them and test them prior to returning the value.
     * 
     * @param decryptedBytes the data we are looking at.
     * @return the decoded data if it is valid and unexpired.
     * @throws DataSealerException if the data cannot be unwrapped and verified
     */
    @Nonnull private String extractAndCheckDecryptedData(@Nonnull byte[] decryptedBytes)
            throws DataSealerException {
        
        try {
            Mac mac = Mac.getInstance(macAlgorithm);
            mac.init(macKey);
            int macSize = mac.getMacLength();
            
            ByteArrayInputStream byteStream = new ByteArrayInputStream(decryptedBytes);
            GZIPInputStream compressedData = new GZIPInputStream(byteStream);
            DataInputStream dataInputStream = new DataInputStream(compressedData);

            // extract the components
            byte[] decodedMac = new byte[macSize];
            int bytesRead;

            bytesRead = dataInputStream.read(decodedMac);
            if (bytesRead != macSize) {
                log.error("Error parsing unwrapped data, unable to extract HMAC.");
                throw new DataSealerException("Error parsing unwrapped data, unable to extract HMAC.");
            }
            long decodedExpirationTime = dataInputStream.readLong();
            String decodedData = dataInputStream.readUTF();

            if (System.currentTimeMillis() > decodedExpirationTime) {
                log.info("Unwrapped data has expired.");
                throw new DataExpiredException("Unwrapped data has expired.");
            }

            byte[] generatedMac = getMAC(mac, decodedData, decodedExpirationTime);

            if (!Arrays.equals(decodedMac, generatedMac)) {
                log.warn("Unwrapped data failed integrity check.");
                throw new DataSealerException("Unwrapped data failed integrity check.");
            }

            log.debug("Unwrapped data verified.");
            return decodedData;
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new DataSealerException("Caught IOException unwrapping data.", e);
        } catch (GeneralSecurityException e) {
            log.error(e.getMessage());
            throw new DataSealerException("Caught GeneralSecurityException unwrapping data.", e);
        }

    }

    /**
     * Encodes data into a cryptographic blob: [IV][HMAC][exp][data] where: [IV] = the Initialization Vector; byte-array
     * [HMAC] = the HMAC; byte array [exp] = expiration time of the data; 8 bytes; Big-endian [data] = the principal; a
     * UTF-8-encoded string The bytes are then GZIP'd. The IV is pre-pended to this byte stream, and the result is
     * Base32-encoded. We don't need to encode the IV or MAC's lengths. They can be obtained from Cipher.getBlockSize()
     * and Mac.getMacLength(), respectively.
     * 
     * @param data the data to wrap
     * @param exp expiration time
     * @return the encoded blob
     * @throws DataSealerException if the wrapping operation fails
     */
    @Nonnull public String wrap(@Nonnull String data, long exp) throws DataSealerException {

        if (data == null) {
            throw new IllegalArgumentException("Data must be supplied for the wrapping operation.");
        }

        try {
            Mac mac = Mac.getInstance(macAlgorithm);
            mac.init(macKey);

            Cipher cipher = Cipher.getInstance(cipherAlgorithm);
            byte[] iv = new byte[cipher.getBlockSize()];
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, cipherKey, ivSpec);

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            GZIPOutputStream compressedStream = new GZIPOutputStream(byteStream);
            DataOutputStream dataStream = new DataOutputStream(compressedStream);

            dataStream.write(getMAC(mac, data, exp));
            dataStream.writeLong(exp);
            dataStream.writeUTF(data);

            dataStream.flush();
            compressedStream.flush();
            compressedStream.finish();
            byteStream.flush();

            byte[] encryptedData = cipher.doFinal(byteStream.toByteArray());

            byte[] handleBytes = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, handleBytes, 0, iv.length);
            System.arraycopy(encryptedData, 0, handleBytes, iv.length, encryptedData.length);

            return Base64Support.encode(handleBytes, false);

        } catch (KeyException e) {
            log.error(e.getMessage());
            throw new DataSealerException("Caught KeyException wrapping data.", e);
        } catch (GeneralSecurityException e) {
            log.error(e.getMessage());
            throw new DataSealerException("Caught GeneralSecurityException wrapping data.", e);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new DataSealerException("Caught IOException wrapping data.", e);
        }

    }

    /**
     * Run a test over the configured bean properties.
     * 
     * @throws DataSealerException if the test fails
     */
    private void testEncryption() throws DataSealerException {

        String decrypted;
        try {
            Cipher cipher = Cipher.getInstance(cipherAlgorithm);
            byte[] iv = new byte[cipher.getBlockSize()];
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, cipherKey, ivSpec);
            byte[] cipherText = cipher.doFinal("test".getBytes());
            cipher = Cipher.getInstance(cipherAlgorithm);
            cipher.init(Cipher.DECRYPT_MODE, cipherKey, ivSpec);
            decrypted = new String(cipher.doFinal(cipherText));
        } catch (GeneralSecurityException e) {
            log.error("Round trip encryption/decryption test unsuccessful: " + e);
            throw new DataSealerException("Round trip encryption/decryption test unsuccessful.", e);
        }

        if (decrypted == null || !"test".equals(decrypted)) {
            log.error("Round trip encryption/decryption test unsuccessful. Decrypted text did not match.");
            throw new DataSealerException("Round trip encryption/decryption test unsuccessful.");
        }

        byte[] code;
        try {
            Mac mac = Mac.getInstance(macAlgorithm);
            mac.init(macKey);
            mac.update("foo".getBytes());
            code = mac.doFinal();
        } catch (GeneralSecurityException e) {
            log.error("Message Authentication test unsuccessful: " + e);
            throw new DataSealerException("Message Authentication test unsuccessful.", e);
        }

        if (code == null) {
            log.error("Message Authentication test unsuccessful.");
            throw new DataSealerException("Message Authentication test unsuccessful.");
        }
    }

    /**
     * Compute a MAC over a string, prefixed by an expiration time.
     * 
     * @param mac MAC object to use
     * @param data data to hash
     * @param exp timestamp to prefix the data with
     * @return the resulting MAC
     */
    @Nonnull protected static byte[] getMAC(@Nonnull Mac mac, @Nonnull String data, long exp) {
        mac.update(getLongBytes(exp));
        mac.update(data.getBytes());
        return mac.doFinal();
    }

    /**
     * Convert a long value into a byte array.
     * 
     * @param longValue value to convert
     * @return a byte array
     */
    @Nonnull protected static byte[] getLongBytes(long longValue) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(byteStream);

            dataStream.writeLong(longValue);
            dataStream.flush();
            byteStream.flush();

            return byteStream.toByteArray();
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * Load keys based on bean properties.
     * 
     * @throws GeneralSecurityException if the keys fail due to a security-related issue
     * @throws IOException if the load process fails
     */
    private void loadKeys() throws GeneralSecurityException, IOException {
        if (cipherKey == null || macKey == null) {
            KeyStore ks = KeyStore.getInstance(keystoreType);
            FileInputStream fis = null;
            try {
                fis = new java.io.FileInputStream(keystorePath);
                ks.load(fis, keystorePassword.toCharArray());
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }

            Key loadedKey;
            if (cipherKey == null) {
                loadedKey = ks.getKey(cipherKeyAlias, cipherKeyPassword.toCharArray());
                if (!(loadedKey instanceof SecretKey)) {
                    log.error("Cipher key {} is not a symmetric key.", cipherKeyAlias);
                }
                cipherKey = (SecretKey) loadedKey;
            }

            if (macKey == null && macKeyAlias != null) {
                loadedKey = ks.getKey(macKeyAlias, macKeyPassword.toCharArray());
                if (!(loadedKey instanceof SecretKey)) {
                    log.error("MAC key {} is not a symmetric key.", macKeyAlias);
                }
                macKey = (SecretKey) loadedKey;
            } else if (macKey == null) {
                macKey = cipherKey;
            }
        }
    }
}
