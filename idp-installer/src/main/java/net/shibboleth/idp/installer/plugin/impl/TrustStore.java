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

package net.shibboleth.idp.installer.plugin.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.bouncycastle.bcpg.ArmoredOutputStream;
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
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;

import net.shibboleth.idp.installer.impl.InstallationLogger;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

/**
 * Code to handle (load, update, check) the trust store for an individual plugin.
 * a thin shim on BC.
 */
@NotThreadSafe public final class TrustStore extends AbstractInitializableComponent {

    /** logger. */
    @Nonnull private final Logger log = InstallationLogger.getLogger(TrustStore.class);
    
    /** Where the IdP is installed.  */
    @NonnullAfterInit private Path idpHome;
    
    /** Explicit path to trust store.  */
    @NonnullAfterInit private String explicitTrustStore;

    /** The plugin this is the trust store for. */
    @NonnullAfterInit private String pluginId;

    /** The key store. */
    @NonnullAfterInit private Path store;

    /** The key store backup. */
    @NonnullAfterInit private Path backup;

    /** KeyRing. */
    @NonnullAfterInit private PGPPublicKeyRingCollection keyRings;

    /** Set the pluginId.
     *
     * @param what to set.
     */
    public void setPluginId(final String what) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        pluginId = what;
    }

    /** Set IdPHome.
     *
     * @param what The idpHome to set.
     */
    public void setIdpHome(final Path what) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        idpHome = what;
    }

    /** Set explicitTrustStore.
    * @param what The value to set.
    */
   public void setTrustStore(@Nullable final String what) {
       ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
       explicitTrustStore = what;
   }

    /** Return a store loaded from the supplied stream.
     *
     * @param in the stream
     * @return a suitable store
     * @throws IOException from {@link Files#newInputStream(Path, java.nio.file.OpenOption...)} and from
     * {@link PGPPublicKeyRingCollection#PGPPublicKeyRingCollection(InputStream,
     *   org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator)}
     */
    private static PGPPublicKeyRingCollection loadStoreFrom(final InputStream in) throws IOException {
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

    /** Load the store from its designated location.
     *
     * @throws IOException from {@link Files#newInputStream(Path, java.nio.file.OpenOption...)} and from
     * {@link PGPPublicKeyRingCollection#PGPPublicKeyRingCollection(InputStream, 
     *   org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator)} 
     */
    protected void loadStore() throws IOException {
        try (final InputStream in = Files.newInputStream(store)) {
            keyRings = loadStoreFrom(in);
        }
    }

    /** Create an empty store and save to new location.
     *
     * @throws IOException from {@link #saveStore()} and in the unlikely event that
     *  {@link PGPPublicKeyRingCollection#PGPPublicKeyRingCollection(java.util.Collection)}
     *  has problems.
     */
    protected void createNewStore() throws IOException {
        try {        
            keyRings = new PGPPublicKeyRingCollection(Collections.emptyList());
        } catch (final PGPException e) {
            throw new IOException("Bad keystore", e);
        }
        saveStoreInternal();
    }

    /** Save the store to its designated location.
     *
     * @throws IOException from {@link Files#newOutputStream(Path, java.nio.file.OpenOption...)} and
     * from {@link PGPPublicKeyRingCollection#encode(OutputStream)}
     */
    public void saveStore() throws IOException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        saveStoreInternal();
    }

    /** Save the store to its designated location.
     *
     * @throws IOException from {@link Files#newOutputStream(Path, java.nio.file.OpenOption...)} and
     * from {@link PGPPublicKeyRingCollection#encode(OutputStream)}
     */
    public void saveStoreInternal() throws IOException {
        if (Files.exists(store)) {
            Files.copy(store, backup, StandardCopyOption.REPLACE_EXISTING);
        }
        try (final OutputStream outStream = Files.newOutputStream(store)) {
            final Iterator<PGPPublicKeyRing> kit = keyRings.getKeyRings();
            while (kit.hasNext()) {
                final PGPPublicKey kr = kit.next().getPublicKey();

                final StringBuffer comment = new StringBuffer().append("\n\r");
                final Iterator<String> sit = kr.getUserIDs();
                if (sit.hasNext()) {
                    comment .append(sit.next()).append('\t');
                }
                comment.append("id\t").append(String.format("%X", (int) kr.getKeyID())).append("\n\r");
                outStream.write(comment.toString().getBytes());
                try (OutputStream armed = new ArmoredOutputStream(outStream)) {
                    kr.encode(armed);
                }
            }
        }
    }
    
    /** Load up the provided store and if the key is found and the
     * Predicate allows it add it to the store which we will then save.
     *
     * @param sigForKey the signature we are looking for a key for.
     * @param keyStream where to load the key from
     * @param accept whether we actually want to install this key
     * @throws IOException if the load or save fails
     */
    public void importKeyFromStream(final Signature sigForKey,
                            final InputStream keyStream,
                            final Predicate<String> accept) throws IOException {
        final PGPPublicKeyRingCollection providedStore = loadStoreFrom(keyStream);

        try {
            final PGPPublicKey key = providedStore.getPublicKey(sigForKey.getSignature().getKeyID());
            if (key == null) {
                log.info("Provided key stream did not contain a key for {}", sigForKey);
                return;
            }
            final StringBuilder builder = new StringBuilder("Signature:\t").
                    append(sigForKey.toString()).
                    append("\nFingerPrint:\t").
                    append((new String(Hex.encode(key.getFingerprint()))).toUpperCase());
            final Iterator<String> namesIterator = key.getUserIDs();
            while (namesIterator.hasNext()) {
                builder.append("\nUsername:\t").append(namesIterator.next());
            }
            builder.append('\n');
            final String keyInfo = builder.toString();
            log.debug("Asking to import key\n{}", keyInfo);
            if (!accept.test(keyInfo)) {
                log.info("Key import barred by user");
                return;
            }
            keyRings = PGPPublicKeyRingCollection.addPublicKeyRing(
                    keyRings,
                    new PGPPublicKeyRing(Collections.singletonList(key)));
            saveStoreInternal();
        } catch (final PGPException e) {
            log.warn("Couldn't locate key", e);
        }
    }

    /** Provide an opaque signature object from an input stream.
     * @param stream what to read.
     * @return the Signature.
     * @throws IOException if there is a problem reading the file of it it doesn't represent a signature
     */
    public static Signature signatureOf(final InputStream stream) throws IOException {
        return new Signature(stream);
    }

    /** Does the key that made this signature exist in our keyrings?
     * @param signature what to ask about
     * @return whether it is there
     */
    public boolean contains(final Signature signature) {

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
    public boolean checkSignature(final InputStream input, final Signature signature) throws IOException {
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
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (pluginId == null) {
            throw new ComponentInitializationException("Plugin Id not set up");
        }

        if (explicitTrustStore != null) {
            store = Path.of(explicitTrustStore);
            if (!Files.exists(store)) {
                log.error("Trust store {} does not exist", explicitTrustStore);
                throw new ComponentInitializationException("Supplied trust store does not exist.");
            }
            backup = Path.of(explicitTrustStore + ".backup");
            log.debug("Plugin {}: Loading explicit truststore {}", pluginId, explicitTrustStore);
            try {
                loadStore();
            } catch (final IOException e) {
                log.error("Plugin {}: Could not load explicit trust store {}", pluginId, explicitTrustStore, e);
                throw new ComponentInitializationException(e);
            }
        } else {
            if (idpHome == null) {
                throw new ComponentInitializationException("IdP home not set up");
            }

            if (!Files.exists(idpHome)) {
                throw new ComponentInitializationException("IdP home '" + idpHome + "' does not exist");
            }

            try {
                final Path parent = idpHome.resolve("credentials").resolve(pluginId);
                if (!Files.exists(parent)) {
                    log.info("Plugin {}: Trust store folder does not exist, creating", pluginId);
                    Files.createDirectories(parent);
                }
                store = parent.resolve("truststore.asc");
                backup = parent.resolve("truststore.asc.backup");
                if (!Files.exists(store)) {
                    log.info("Plugin {}: Trust store does not exist, creating", pluginId);
                    createNewStore();
                } else {
                    log.debug("Plugin {}: Trust store exists, loading", pluginId);
                    loadStore();
                }
            } catch (final IOException e) {
                throw new ComponentInitializationException(e);
            }
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
            keyId = String.format("0X%X", signature.getKeyID());
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
