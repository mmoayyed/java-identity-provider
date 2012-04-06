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

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import com.google.common.base.Function;
import com.google.common.base.Optional;

/**
 * An attribute definition that populates the generated attribute with information extracted from the current
 * {@link AttributeResolutionContext}.
 */
public class ResolutionContextDataAttributeDefinition extends BaseAttributeDefinition {

    /** Function used to extract attribute values from the current resolution context. */
    private Function<AttributeResolutionContext, Collection<? extends AttributeValue>> dataExtractionStrategy;

    /**
     * Gets the function used to extract attribute values from the current {@link AttributeResolutionContext}.
     * 
     * @return function used to extract attribute values from the current {@link AttributeResolutionContext}
     */
    @Nullable public Function<AttributeResolutionContext, Collection<? extends AttributeValue>>
            getDataExtractionStrategy() {
        return dataExtractionStrategy;
    }

    /**
     * Sets the function used to extract attribute values from the current {@link AttributeResolutionContext}.
     * 
     * @param strategy function used to extract attribute values from the current {@link AttributeResolutionContext}
     */
    public synchronized void setDataExtractionStrategy(
            @Nonnull final Function<AttributeResolutionContext, Collection<? extends AttributeValue>> strategy) {
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        dataExtractionStrategy = Constraint.isNotNull(strategy, "Data extraction strategy can not be null");
    }

    /** {@inheritDoc} */
    @Nonnull protected Optional<Attribute> doAttributeDefinitionResolve(
            @Nonnull AttributeResolutionContext resolutionContext) throws AttributeResolutionException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        Constraint.isNotNull(resolutionContext, "Resolution Context cannot be null");

        final Attribute attribute = new Attribute(getId());

        Collection<? extends AttributeValue> values = dataExtractionStrategy.apply(resolutionContext);
        if (values != null && !values.isEmpty()) {
            for (AttributeValue value : values) {
                if (value != null) {
                    attribute.getValues().add(value);
                }
            }
        }

        return Optional.of(attribute);
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        dataExtractionStrategy = null;
        super.doDestroy();
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (dataExtractionStrategy == null) {
            throw new ComponentInitializationException("No data extraction strategy has been given");
        }
    }
}