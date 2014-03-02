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

package net.shibboleth.idp.saml.impl.attribute.resolver;

import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AbstractAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.idp.saml.impl.nameid.CryptoTransientIdGenerationStrategy;
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.security.DataSealer;

import org.opensaml.profile.ProfileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * An attribute definition that generates integrity protected, encrypted identifiers useful for stateless transient
 * subject IDs.
 */
public class CryptoTransientIdAttributeDefinition extends AbstractAttributeDefinition {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(CryptoTransientIdAttributeDefinition.class);

    /** The actual implementation of the transient generation process. */
    @Nonnull private final CryptoTransientIdGenerationStrategy idGenerator;

    /** Constructor. */
    public CryptoTransientIdAttributeDefinition() {
        idGenerator = new CryptoTransientIdGenerationStrategy();
    }

    /**
     * Set the data sealer to use.
     * 
     * @param sealer object used to protect and encrypt the data
     */
    public void setDataSealer(@Nonnull final DataSealer sealer) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        idGenerator.setDataSealer(sealer);
    }

    /**
     * Get the time, in milliseconds, ids are valid.
     * 
     * @return  time, in milliseconds, ids are valid
     */
    @Positive public long getIdLifetime() {
        return idGenerator.getIdLifetime();
    }
    
    /**
     * Set the time, in milliseconds, ids are valid.
     * 
     * @param lifetime time, in milliseconds, ids are valid
     */
    public void setIdLifetime(@Duration @Positive final long lifetime) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        idGenerator.setIdLifetime(lifetime);
    }
    
    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        idGenerator.setId(getId() + " Transient Generator");
        idGenerator.initialize();
    }
    
    /** {@inheritDoc} */
    @Override protected void doDestroy() {
        idGenerator.destroy();
        super.doDestroy();
    }

    /** {@inheritDoc} */
    @Override @Nullable protected IdPAttribute doAttributeDefinitionResolve(
            @Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        final String attributeRecipientID = getAttributeRecipientID(resolutionContext);
        
        final String principalName = getPrincipal(resolutionContext);
    
        try {
            final String transientId = idGenerator.generate(attributeRecipientID, principalName);
            log.debug("{} creating new transient ID '{}' for request '{}'",
                    new Object[] {getLogPrefix(), transientId, resolutionContext.getId(),});
            
            final IdPAttribute result = new IdPAttribute(getId());
            result.setValues(Collections.singleton(new StringAttributeValue(transientId)));
            return result;
        } catch (final ProfileException e) {
            throw new ResolutionException(e);
        }
    }

    /**
     * Police and get the AttributeRecipientID.
     * 
     * @param resolutionContext where to look
     * @return the AttributeRecipientID
     * @throws ResolutionException if it was non null
     */
    @Nonnull @NotEmpty private String getAttributeRecipientID(
            @Nonnull final AttributeResolutionContext resolutionContext) throws ResolutionException {
        final String attributeRecipientID = resolutionContext.getAttributeRecipientID();
        if (Strings.isNullOrEmpty(attributeRecipientID)) {
            throw new ResolutionException(getLogPrefix() + " provided attribute recipient ID was empty");
        }
        return attributeRecipientID;
    }

    /**
     * Police and get the Principal.
     * 
     * @param context where to look
     * @return the Principal
     * @throws ResolutionException if it was non null
     */
    @Nonnull @NotEmpty private String getPrincipal(@Nonnull final AttributeResolutionContext context)
            throws ResolutionException {
        final String principalName = context.getPrincipal();
        if (Strings.isNullOrEmpty(principalName)) {
            throw new ResolutionException(getLogPrefix() + " provided prinicipal name was empty");
        }

        return principalName;
    }

}