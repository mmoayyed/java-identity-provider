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

package net.shibboleth.idp.saml.attribute.mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.saml.saml2.core.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * The class contains the mechanics to go from a list of {@link Attribute}s (or derived) to a {@link Multimap} of
 * {@link String},{@link IdPAttribute} (or derived, or null). The representation as a {@link Multimap} is useful for
 * filtering situations and is exploited by AttributeInMetadata filter.
 * 
 * @param <InType> the type which is to be inspected and mapped
 * @param <OutType> some sort of representation of an IdP attribute
 */

public abstract class AbstractSAMLAttributesMapper<InType extends Attribute, OutType extends IdPAttribute> extends
        AbstractIdentifiableInitializableComponent implements AttributesMapper<InType, OutType> {

    /** Log. */
    private final Logger log = LoggerFactory.getLogger(AbstractSAMLAttributesMapper.class);

    /** The mappers we can apply. */
    private Collection<AttributeMapper<InType, OutType>> mappers = Collections.EMPTY_LIST;

    /** The String used to prefix log message. */
    private String logPrefix;

    /**
     * Get the mappers.
     * 
     * @return Returns the mappers.
     */
    @Nonnull public Collection<AttributeMapper<InType, OutType>> getMappers() {
        return mappers;
    }

    /**
     * Set the attribute mappers into the lookup map.
     * 
     * @param theMappers The mappers to set.
     */
    public void setMappers(@Nonnull Collection<AttributeMapper<InType, OutType>> theMappers) {
        mappers = Constraint.isNotNull(theMappers, "mappers list must be non null");
    }

    /** {@inheritDoc} */
    public void setId(@Nullable String id) {
        super.setId(id);
    }

    /**
     * Map the SAML attributes into IdP attributes.
     * 
     * @param prototypes the SAML attributes
     * @return a map from IdP AttributeId to RequestedAttributes (or NULL).
     */
    public Multimap<String, OutType> mapAttributes(@Nonnull @NonnullElements List<InType> prototypes) {

        Multimap<String, OutType> result = ArrayListMultimap.create();

        for (InType prototype : prototypes) {
            for (AttributeMapper<InType, OutType> mapper : mappers) {

                Map<String, OutType> mappedAttributes = mapper.mapAttribute(prototype);

                log.debug("{} SAML attribute '{}' mapped to {} attributes by mapper '{}'", getLogPrefix(),
                        prototype.getName(), mappedAttributes.size(), mapper.getId());

                for (Entry<String, OutType> entry : mappedAttributes.entrySet()) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        logPrefix = null;
        for (AttributeMapper mapper : mappers) {
            ComponentSupport.initialize(mapper);
        }
    }

    /**
     * Return a string which is to be prepended to all log messages.
     * 
     * @return "Attribute Mappers '<ID>' :"
     */
    private Object getLogPrefix() {
        String s = logPrefix;
        if (null == s) {
            s = new StringBuilder("Attribute Mappers : '").append(getId()).append("':").toString();
            logPrefix = s;
        }
        return s;
    }
}
