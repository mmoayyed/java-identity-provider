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

package net.shibboleth.idp.attribute.resolver.dc.storage.impl;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;

import org.opensaml.storage.StorageRecord;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.dc.MappingStrategy;
import net.shibboleth.idp.attribute.resolver.dc.storage.StorageMappingStrategy;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * {@link MappingStrategy} for pulling data out of {@link StorageRecord}.
 * 
 * @since 4.1.0
 */
public class SimpleStorageMappingStrategy implements StorageMappingStrategy {

    /** ID of the attribute to create. */
    @Nonnull @NotEmpty private final String attributeId;
    
    /**
     * Constructor.
     *
     * @param id attribute ID to create
     */
    public SimpleStorageMappingStrategy(@Nonnull @NotEmpty final String id) {
        attributeId = Constraint.isNotNull(StringSupport.trimOrNull(id), "Attribute ID cannot be null or empty");
    }
    
    /** {@inheritDoc} */
    @Nonnull @NonnullElements public Map<String,IdPAttribute> map(
            @Nonnull final StorageRecord<?> results) throws ResolutionException {
        
        final IdPAttribute attribute = new IdPAttribute(attributeId);
        attribute.setValues(Collections.singleton(StringAttributeValue.valueOf(results.getValue())));
        
        return Collections.singletonMap(attributeId, attribute);
    }

}