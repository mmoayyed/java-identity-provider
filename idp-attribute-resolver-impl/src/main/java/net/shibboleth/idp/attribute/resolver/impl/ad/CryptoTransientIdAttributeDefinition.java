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

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeRecipientContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An attribute definition that generates integrity protected, encrypted identifiers useful for stateless transient
 * subject IDs.
 */
public class CryptoTransientIdAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private Logger log = LoggerFactory.getLogger(CryptoTransientIdAttributeDefinition.class);

    /** Object used to protect and encrypt the data. */
    private DataSealer dataSealer;

    /** Length, in milliseconds, tokens are valid. */
    private long idLifetime;

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        if (0 == idLifetime) {
            log.debug("set default lifetime of 4 hours.", getLogPrefix());
            idLifetime = 1000 * 60 * 60 * 4;
        }
        if (null == dataSealer) {
            throw new ComponentInitializationException(getLogPrefix() + " data sealer was null or unspecified");
        }

    }

    /** {@inheritDoc} */
    @Nonnull protected Attribute doAttributeDefinitionResolve(@Nonnull AttributeResolutionContext resolutionContext)
            throws ResolutionException {

        final AttributeRecipientContext attributeRecipientContext =
                resolutionContext.getSubcontext(AttributeRecipientContext.class);

        if (null == attributeRecipientContext) {
            throw new ResolutionException(getLogPrefix() + " no attribute recipient context provided ");
        }

        final String attributeIssuerID = attributeRecipientContext.getAttributeIssuerID();
        if (null == attributeIssuerID) {
            throw new ResolutionException(getLogPrefix() + " provided attribute issuer ID was empty");
        }

        final String attributeRecipientID = attributeRecipientContext.getAttributeRecipientID();
        if (null == attributeRecipientID) {
            throw new ResolutionException(getLogPrefix() + " provided attribute recipient ID was empty");
        }

        final String principalName = attributeRecipientContext.getPrincipal();
        if (null == principalName) {
            throw new ResolutionException(getLogPrefix() + " provided prinicipal name was empty");
        }

        final Attribute result = new Attribute(getId());

        log.debug("{} Building crypto transient ID for recipient: '{}', issuer: {}, principal identifer: {}",
                new Object[] {getLogPrefix(), attributeRecipientID, attributeIssuerID, principalName,});

        StringBuilder principalTokenIdBuilder = new StringBuilder();
        principalTokenIdBuilder.append(attributeIssuerID).append("!").append(attributeRecipientID).append("!")
                .append(principalName);

        try {
            String transientId =
                    dataSealer.wrap(principalTokenIdBuilder.toString(), System.currentTimeMillis() + idLifetime);
            Set<AttributeValue> vals = Collections.singleton((AttributeValue) new StringAttributeValue(transientId));
            result.setValues(vals);
        } catch (DataSealerException e) {
            throw new ResolutionException(getLogPrefix() + " Caught exception wrapping principal identifier. {}", e);
        }

        return result;
    }

    /**
     * Gets the time, in milliseconds, that ids are valid for.
     * 
     * @return time, in milliseconds, that ids are valid for.
     */
    public long getIdLifetime() {
        return idLifetime;
    }

    /**
     * Sets the time, in milliseconds, that ids are valid for.
     * 
     * @param lifetime time, in milliseconds, that ids are valid for.
     */
    public void setIdLifetime(long lifetime) {
        idLifetime = lifetime;
    }

    /**
     * Sets the Sealer.
     * 
     * @param sealer object used to protect and encrypt the data
     */
    public void setDataSealer(@Nonnull DataSealer sealer) {
        dataSealer = Constraint.isNotNull(sealer, "DataSealer may not be null");
    }

    /**
     * Gets the Sealer.
     * 
     * @return sealer object used to protect and encrypt the data
     */
    @Nullable @NonnullAfterInit public DataSealer getDataSealer() {
        return dataSealer;
    }

}
