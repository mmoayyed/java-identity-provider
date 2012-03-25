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

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;

import com.google.common.base.Function;
import com.google.common.base.Optional;

/**
 * An attribute definition that populates the generated attribute with information extracted from the current
 * {@link AttributeResolutionContext}.
 */
public class ResolutionContextDataAttributeDefinition extends BaseAttributeDefinition {

    /** Function used to extract attribute values from the current resolution context. */
    private Function<AttributeResolutionContext, Collection<? extends AttributeValue>> dataExtractionStrategy;

    /** {@inheritDoc} */
    protected Optional<Attribute> doAttributeDefinitionResolve(AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        Attribute attribute = new Attribute(getId());

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
}