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

package net.shibboleth.idp.attribute;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.TransformedInputMapBuilder;
import net.shibboleth.utilities.java.support.logic.TrimOrNullStringFunction;

import org.opensaml.messaging.context.AbstractSubcontext;
import org.opensaml.messaging.context.SubcontextContainer;

/**
 * Subcontext that tracks a set of attributes. Usually the tracked attributes are about a particular user and associated
 * with a particular service request.
 */
public class AttributeSubcontext extends AbstractSubcontext {

    /** The attributes tracked by this context. */
    private Map<String, Attribute<?>> attributes;

    /**
     * Constructor.
     * 
     * @param parent the owner of this subcontext
     */
    public AttributeSubcontext(@Nullable final SubcontextContainer parent) {
        super(parent);
        attributes = Collections.emptyMap();
    }

    /**
     * Gets the collection of attributes, indexed by attribute ID, tracked by this context.
     * 
     * @return the collection of attributes indexed by attribute ID
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<String, Attribute<?>> getAttributes() {
        return attributes;
    }

    /**
     * Sets the attributes tracked by this context.
     * 
     * @param newAttributes the attributes
     */
    public void setAttributes(@Nullable @NullableElements Collection<Attribute<?>> newAttributes) {
        if (newAttributes == null) {
            attributes = Collections.emptyMap();
            return;
        }

        TransformedInputMapBuilder<String, Attribute<?>> mapBuilder =
                new TransformedInputMapBuilder<String, Attribute<?>>()
                        .keyPreprocessor(TrimOrNullStringFunction.INSTANCE);
        for (Attribute attribute : newAttributes) {
            if (attribute == null) {
                continue;
            }

            mapBuilder.put(attribute.getId(), attribute);
        }

        attributes = mapBuilder.buildImmutableMap();
    }
}