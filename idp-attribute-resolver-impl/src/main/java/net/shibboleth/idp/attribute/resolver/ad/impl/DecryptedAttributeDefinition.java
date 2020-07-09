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

package net.shibboleth.idp.attribute.resolver.ad.impl;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.resolver.AbstractAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.security.DataSealer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An {@link AttributeDefinition} that creates an attribute whose values are the
 * decrypted values of its dependencies.
 * 
 * Empty values are copied through and non-string values are ignored.
 * 
 * @since 4.1.0
 */
@ThreadSafe
public class DecryptedAttributeDefinition extends AbstractAttributeDefinition {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(DecryptedAttributeDefinition.class);

    /** The DataSealer that we'll use to decrypt the attribute. */
    @NonnullAfterInit private DataSealer sealer;

    /**
     * Set the DataSealer (sealer) for this Definition.
     * 
     * @param newSealer what to set
     */
    public void setDataSealer(@Nonnull final DataSealer newSealer) {
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        sealer = Constraint.isNotNull(newSealer, "DataSealer cannot be null");
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
    
        if (getDataConnectorDependencies().isEmpty() && getAttributeDependencies().isEmpty()) {
            throw new ComponentInitializationException(getLogPrefix() + " no dependencies were configured");
        } else if (sealer == null) {
            throw new ComponentInitializationException("DataSealer cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override @Nonnull protected IdPAttribute doAttributeDefinitionResolve(
            @Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException {
        Constraint.isNotNull(workContext, "AttributeResolverWorkContext cannot be null");

        final List<IdPAttributeValue> results =
                PluginDependencySupport.getMergedAttributeValues(workContext, getAttributeDependencies(), 
                        getDataConnectorDependencies(), getId());

        final Collection<IdPAttributeValue> decryptedValues = new ArrayList<>(results.size());

        for (final IdPAttributeValue value : results) {
            
            if (value instanceof EmptyAttributeValue) {
                log.trace("{} Passing through EmptyAttributeValue", getLogPrefix());
                decryptedValues.add(value);
                continue;
            } else if (!(value instanceof StringAttributeValue)) {
                log.warn("{} Ignoring non-string-valued IdPAttributeValue type {}", getLogPrefix(),
                        value.getClass().getSimpleName());
                continue;
            }
            
            log.trace("{} Encrypted attribute value: {}", getLogPrefix(), ((StringAttributeValue) value).getValue());
        
            try{
                final String decrypted = sealer.unwrap(((StringAttributeValue) value).getValue());
                log.trace("{}: Decrypted attribute value: {}", getLogPrefix(), decrypted);
                decryptedValues.add(new StringAttributeValue(decrypted));
                
            } catch(final Exception e){
                log.warn("{}: Error decrypting attribute: {}", getLogPrefix(), e);
            }
        }
    
        final IdPAttribute decryptedAttribute = new IdPAttribute(getId());
        decryptedAttribute.setValues(decryptedValues);

        return decryptedAttribute;
    }
  
}