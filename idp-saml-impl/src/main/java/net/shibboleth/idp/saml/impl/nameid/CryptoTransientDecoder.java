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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.SubjectCanonicalizationException;
import net.shibboleth.idp.saml.nameid.NameDecoderException;
import net.shibboleth.idp.saml.nameid.NameIdentifierDecoder;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.security.DataExpiredException;
import net.shibboleth.utilities.java.support.security.DataSealer;
import net.shibboleth.utilities.java.support.security.DataSealerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract action which contains the logic to do crypto transient decoding matching. This reverses the work done by
 * {@link net.shibboleth.idp.attribute.resolver.impl.ad.CryptoTransientIdAttributeDefinition}
 */
public class CryptoTransientDecoder extends AbstractIdentifiableInitializableComponent implements
        NameIdentifierDecoder {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(CryptoTransientDecoder.class);

    /** Object used to protect and encrypt the data. */
    @NonnullAfterInit private DataSealer dataSealer;

    /** cache for the log prefix - to save multiple recalculations. */
    @Nullable private String logPrefix;

    /**
     * Gets the Data Sealer we are using.
     * 
     * @return the Data Sealer we are using.
     */
    @NonnullAfterInit public DataSealer getDataSealer() {
        return dataSealer;
    }

    /**
     * Sets the Data Sealer we should use.
     * 
     * @param sealer the Data Sealer to use.
     */
    public void setDataSealer(@Nonnull final DataSealer sealer) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        dataSealer = Constraint.isNotNull(sealer, "DataSealer cannot be null");
    }

    /** {@inheritDoc} */
    @Override public synchronized void setId(String componentId) {
        super.setId(componentId);
    }

    /**
     * Convert the transient Id into the principal.
     * 
     * @param transientId the encrypted transientID
     * @param issuerId The issuer (not used)
     * @param requesterId the requested (SP)
     * @return the decoded entity.
     * @throws SubjectCanonicalizationException if a mismatch occurrs
     * @throws NameDecoderException if a decode error occurs.
     */
    /** {@inheritDoc} */
    @Override @Nonnull public String decode(@Nonnull String transientId, @Nullable String issuerId,
            @Nullable String requesterId) throws SubjectCanonicalizationException, NameDecoderException {
        Constraint.isNotNull(requesterId, "Supplied requested should be null");
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        if (null == transientId) {
            throw new NameDecoderException(getLogPrefix() + " transient Identifier was null");
        }

        final String decodedId;
        try {
            decodedId = dataSealer.unwrap(transientId);
        } catch (DataExpiredException e) {
            throw new NameDecoderException(getLogPrefix() + " Principal identifier has expired.");
        } catch (DataSealerException e) {
            throw new NameDecoderException(getLogPrefix()
                    + " Caught exception unwrapping principal identifier.", e);
        }

        if (decodedId == null) {
            throw new NameDecoderException(getLogPrefix()
                    + " Unable to recover principal from transient identifier: " + transientId);
        }

        // Split the identifier.
        String[] parts = decodedId.split("!");
        if (parts.length != 3) {
            throw new SubjectCanonicalizationException(getLogPrefix() + " Decoded principal information was invalid: "
                    + decodedId);
        }

        if (issuerId != null && !issuerId.equals(parts[0])) {
            throw new SubjectCanonicalizationException(getLogPrefix() + " Issuer (" + issuerId
                    + ") does not match supplied value (" + parts[0] + ").");
        } else if (requesterId != null && !requesterId.equals(parts[1])) {
            throw new SubjectCanonicalizationException(getLogPrefix() + " Requested (" + requesterId
                    + ") does not match supplied value (" + parts[1] + ").");
        }

        return parts[2];
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == dataSealer) {
            throw new ComponentInitializationException(getLogPrefix() + " no data sealer set");
        }
    }

    /**
     * Return a prefix for logging messages for this component.
     * 
     * @return a string for insertion at the beginning of any log messages
     */
    @Nonnull protected String getLogPrefix() {
        // local cache of cached entry to allow unsynchronised clearing.
        String prefix = logPrefix;
        if (null == prefix) {
            StringBuilder builder = new StringBuilder("Crypto Transient Decoder '").append(getId()).append("':");
            prefix = builder.toString();
            if (null == logPrefix) {
                logPrefix = prefix;
            }
        }
        return prefix;
    }

}
