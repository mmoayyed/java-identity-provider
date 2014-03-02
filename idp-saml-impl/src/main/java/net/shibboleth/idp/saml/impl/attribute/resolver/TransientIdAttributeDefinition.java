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

import java.io.IOException;
import java.util.Collections;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AbstractAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.idp.saml.impl.nameid.StoredTransientIdGenerator;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;

import org.opensaml.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * An attribute definition that generates random identifiers useful for transient subject IDs.
 * 
 * <p>Information about the created IDs are stored within a provided {@link StorageService}. The identifier
 * itself is the record key, and the value combines the principal name with the identifier of the recipient.</p>
 */
public class TransientIdAttributeDefinition extends AbstractAttributeDefinition {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(TransientIdAttributeDefinition.class);

    /** The actual implementation of the transient generation process. */
    @Nonnull private final StoredTransientIdGenerator idGenerator;
    
    /** Constructor. */
    public TransientIdAttributeDefinition() {
        idGenerator = new StoredTransientIdGenerator();
    }

    /**
     * Set the ID store we should use.
     * 
     * @param store the store to use.
     */
    public void setIdStore(@Nonnull final StorageService store) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        idGenerator.setIdStore(store);
    }

    /**
     * Set the ID generator we should use.
     * 
     * @param generator identifier generation strategy to use
     */
    public void setIdGenerator(@Nonnull final IdentifierGenerationStrategy generator) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        idGenerator.setIdGenerator(generator);
    }
    
    /**
     * Get the size, in bytes, of the id.
     * 
     * @return  id size, in bytes
     */
    public int getIdSize() {
        return idGenerator.getIdSize();
    }
    
    /**
     * Set the size, in bytes, of the id.
     * 
     * @param size size, in bytes, of the id
     */
    public void setIdSize(final int size) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        idGenerator.setIdSize(size);
    }

    
    /**
     * Get the time, in milliseconds, ids are valid.
     * 
     * @return  time, in milliseconds, ids are valid
     */
    public long getIdLifetime() {
        return idGenerator.getIdLifetime();
    }
    
    /**
     * Set the time, in milliseconds, ids are valid.
     * 
     * @param lifetime time, in milliseconds, ids are valid
     */
    public void setIdLifetime(final long lifetime) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        idGenerator.setIdLifetime(lifetime);
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        idGenerator.setId(getId() + " Transient Generator");
        idGenerator.initialize();
    }

    /** {@inheritDoc} */
    @Override protected void doDestroy() {
        idGenerator.destroy();
        super.doDestroy();
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

    /** {@inheritDoc} */
    @Override @Nonnull protected IdPAttribute doAttributeDefinitionResolve(
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
        } catch (final IOException except) {
            throw new ResolutionException(except);
        }
    }

}