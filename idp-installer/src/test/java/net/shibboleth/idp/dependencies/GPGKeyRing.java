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

package net.shibboleth.idp.dependencies;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.slf4j.Logger;

import net.shibboleth.idp.installer.impl.InstallationLogger;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;

/**
 * Code to handle (load, update, check) the keyrings for each maven group.
 */
@NotThreadSafe public final class GPGKeyRing {

    /** logger. */
    @Nonnull private final Logger log = InstallationLogger.getLogger(GPGKeyRing.class);
    
    /** The key store backup. */
    @Nullable private Path backup;

    /** KeyRing. */
    @NonnullAfterInit private PGPPublicKeyRingCollection keyRings;
    
    /** Constructor.
     * Locate and load the keyring for the provided group,  First look for the keyring
     * and then for an asc file.
     * @param group the group to look for
     * @throws Exception under various error conditions.
     */
    public GPGKeyRing(final String group) throws Exception {
        try (final InputStream armoredKeys = getClass().getResourceAsStream("/net/shibboleth/idp/dependencies/stores/"+group);
            final InputStream keyRingStream = getClass().getResourceAsStream("/net/shibboleth/idp/dependencies/stores/"+group+".gpg")) {
            if (keyRingStream != null) {
                log.debug("Loading keyring for {}", group);
                keyRings = new PGPPublicKeyRingCollection(keyRingStream, new JcaKeyFingerprintCalculator());
            } else if (armoredKeys != null) {
                log.debug("Loading asci keys for {}", group);
                keyRings = loadRingFromAsc(armoredKeys);
            } else {
                log.warn("No asc of keyring found for {}", group);
                throw new FileNotFoundException("Could not locate keyring");
            }
        } catch (final Exception e) {
            log.error("Could not load explicit trust store for {} from stream", group, e);
            throw e;
        }
    }

    /** Return a store loaded from the supplied stream.
     *
     * @param in the stream
     * @return a suitable store
     * @throws IOException from {@link Files#newInputStream(Path, java.nio.file.OpenOption...)} and from
     * {@link PGPPublicKeyRingCollection#PGPPublicKeyRingCollection(InputStream,
     *   org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator)}
     */
    private static PGPPublicKeyRingCollection loadRingFromAsc(final InputStream in) throws IOException {
        try (final InputStream decoded = PGPUtil.getDecoderStream(in)) {
           final ArrayList<PGPPublicKeyRing> listr = new ArrayList<>();

           PGPObjectFactory pgpFact = new PGPObjectFactory(decoded, new JcaKeyFingerprintCalculator());
           Object obj;
           while ((obj = pgpFact.nextObject()) != null) {
               // Inner loop - when new factories return nothing we are done
               do {
                   if (!(obj instanceof PGPPublicKeyRing)) {
                       throw new IOException(obj.getClass().getName() + " found where PGPPublicKeyRing expected");
                   }
                   listr.add((PGPPublicKeyRing) obj);
                   obj = pgpFact.nextObject();
               } while (obj != null);
               pgpFact = new PGPObjectFactory(decoded, new JcaKeyFingerprintCalculator());
           }
           return new PGPPublicKeyRingCollection(listr);
       } catch (final PGPException e) {
           throw new IOException("Error reading key ring", e);
       }
    }

    /** Lookup and return the key information for this key (and any parent).
     * @param sigForKey the signature to lookup
     * @return the string in a normalized form.
     */
    protected String getKeyInfo(final Signature sigForKey) {
        final PGPPublicKeyRing keyRing;
        try {
            keyRing = keyRings.getPublicKeyRing(sigForKey.getSignature().getKeyID());
        } catch (final PGPException e) {
                log.warn("Couldn't locate key", e);
                return null;
        }
        if (keyRing == null) {
            log.info("Provided key stream did not contain a key for {}", sigForKey);
            return null;
        }
        final StringBuilder builder = new StringBuilder("KeyId: ").append(sigForKey.toString());
        final Iterator<PGPPublicKey> keyIterator = keyRing.getPublicKeys();
        final Set<String> seenNames = new HashSet<>();
        while (keyIterator.hasNext()) {
            final PGPPublicKey key = keyIterator.next();
            final Iterator<String> namesIterator = key.getUserIDs();
            while (namesIterator.hasNext()) {
                final String name =  namesIterator.next();
                if (seenNames.add(name)) {
                    builder.append("\tUsername:\t").append(name);
                }
            }
        }
        return builder.toString();
    }

    /** Provide an opaque signature object from an input stream.
     * @param stream what to read.
     * @return the Signature.
     * @throws IOException if there is a problem reading the file of it it doesn't represent a signature
     */
    protected static Signature signatureOf(final InputStream stream) throws IOException {
        return new Signature(stream);
    }

    /** Does the key that made this signature exist in our keyrings?
     * @param signature what to ask about
     * @return whether it is there
     */
    protected boolean contains(final Signature signature) {

        final PGPSignature sig = signature.getSignature();

        log.debug("Looking for key with Id {}", signature);

        try {
            return keyRings.getPublicKey(sig.getKeyID()) != null;
        } catch (final PGPException e) {
            log.warn("Error looking for key {}", signature, e);
            return false;
        }
    }

    /** Run a signature check over the streams.
     * @param input what to check
     * @param signature what to check with
     * @return whether it passed or not
     * @throws IOException if we get an error reading the stream
     */
    protected boolean checkSignature(final InputStream input, final Signature signature) throws IOException {
        try {
            final PGPSignature pgpSignature = signature.getSignature();
            final PGPPublicKey pubKey = keyRings.getPublicKey(pgpSignature.getKeyID());
            pgpSignature.init(new JcaPGPContentVerifierBuilderProvider().setProvider("BC"), pubKey);

            final byte[] buffer = new byte[1024];
            int count = input.read(buffer);
            while (count > 0) {
                pgpSignature.update(buffer, 0, count);
                count = input.read(buffer);
            }
            final boolean result = pgpSignature.verify();
            if (result) {
                log.debug("Signature Check Succeeded");
            } else {
                log.debug("Signature Check Failed");
            }
            return result;
        } catch (final PGPException e) {
            log.warn("Error thrown during signature check", e);
            return false;
        }
    }
    

    /**
     * An opaque handle around a {@link PGPSignature}.
     */
    public static final class Signature {
        
        /** What we are hiding. */
        @Nonnull private PGPSignature signature;

        /** printable key. */
        @Nonnull private String keyId;

        /**
         * Constructor.
         *
         * @param input input data
         * 
         * @throws IOException if an error occurs
         */
        protected Signature(final @Nonnull InputStream input) throws IOException {
            try (final InputStream sigStream =  PGPUtil.getDecoderStream(input)) {
                final JcaPGPObjectFactory factory = new JcaPGPObjectFactory(sigStream);
                final Object first = factory.nextObject();
                if (first instanceof PGPSignatureList) {
                    final PGPSignatureList list = (PGPSignatureList) first;
                    signature = list.get(0);
                } else {
                    throw new IOException("Provided file was not a signature");
                }
            }
            keyId = String.format("0X%016X", signature.getKeyID());
        }

        /**
         * Get signature.
         * 
         * @return the signature
         */
        protected PGPSignature getSignature() {
            return signature;
        }

        /** {@inheritDoc} */
        public String toString() {
            return keyId;
        }         
    }
}
