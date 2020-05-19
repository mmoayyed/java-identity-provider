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
import java.util.Collections;

import javax.annotation.Nonnull;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * Code to handle (load, update, check) the trust store for an individual plugin.
 * a thin shim on BC.
 */
public final class TrustStore extends AbstractInitializableComponent {

    /** logger. */
    @NonnullAfterInit private final Logger log = LoggerFactory.getLogger(TrustStore.class);
    
    /** Where the IdP is installed.  */
    @NonnullAfterInit private String idpHome;
    
    /** The plugin this is the trust store for. */
    @NonnullAfterInit private String pluginId;
    
    /** The key store. */
    @NonnullAfterInit private Path store;

    /** The key store backup. */
    @NonnullAfterInit private Path backup;

    /** KeyRing. */
    @NonnullAfterInit private PGPPublicKeyRingCollection keyRings;

    /** Set the pluginId. 
     * @param what The id to set.
     */
    public void setPluginId(final String what) {
        pluginId = what;
    }
    
    /** Set the IdPHome.
     * @param what The idpHome to set.
     */
    public void setIdpHome(final String what) {
        idpHome = what;
    }

    /** Load the store from its designated location.
     *
     * @throws IOException from {@link Files#newInputStream(Path, java.nio.file.OpenOption...)} and from
     * {@link PGPPublicKeyRingCollection#PGPPublicKeyRingCollection(InputStream, 
     *   org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator)} 
     */
    protected void loadStore() throws IOException {
        try (final InputStream in = Files.newInputStream(store);
             final InputStream in2 = PGPUtil.getDecoderStream(in)) {
            keyRings = new PGPPublicKeyRingCollection(in2, new JcaKeyFingerprintCalculator());
            } catch (final PGPException e) {
                throw new IOException("Bad keystore", e);
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
        saveStore();
    }
    
    /** Save the store to its designated location.
     *  
     * @throws IOException from {@link Files#newOutputStream(Path, java.nio.file.OpenOption...)} and
     * from {@link PGPPublicKeyRingCollection#encode(OutputStream)}
     */
    public void saveStore() throws IOException {
        if (Files.exists(store)) {
            Files.copy(store, backup, StandardCopyOption.REPLACE_EXISTING);
        }
        try (final OutputStream out = Files.newOutputStream(store)) {
            keyRings.encode(out);            
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
        
        for (final PGPPublicKeyRing keyRing : keyRings) {
            for (final PGPPublicKey key : keyRing) {
                if (sig.getKeyID() == key.getKeyID()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
                
        if (idpHome == null) {
            throw new ComponentInitializationException("IdP home not set up");
        }
        
        if (pluginId == null) {
            throw new ComponentInitializationException("Plugin IN not set up");
        }
        
        final Path home = Path.of(idpHome);
        if (!Files.exists(home)) {
            throw new ComponentInitializationException("IdP home '" + idpHome + "' does not exist");
        }

        try {
            final Path parent = home.resolve("credentials").resolve(pluginId);
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
    
    /**
     * An opaque handle around a {@link PGPSignature}.
     */
    public static final class Signature {
        
        /** What we are hiding. */
        @Nonnull private PGPSignature signature;
        
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
        }

        protected PGPSignature getSignature() {
            return signature;
        }
    }
}
