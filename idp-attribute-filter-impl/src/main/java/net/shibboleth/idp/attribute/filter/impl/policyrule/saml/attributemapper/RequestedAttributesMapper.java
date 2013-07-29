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

package net.shibboleth.idp.attribute.filter.impl.policyrule.saml.attributemapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.filter.RequestedAttribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * This class conceptually represents the content of a attribute-map file, hence it describes (and then does) the
 * mappings from a {@link List} of SAML2 {@link org.opensaml.saml.saml2.metadata.RequestedAttribute} into a multimap
 * going from (SAML2) attributeId to idp {@link RequestedAttribute}s (or null if the mapping failed for type reasons).
 * 
 * TODO Make more general &lt;Foo extends IdPAttribute, Bar extents SAMLAttribute&gt;
 */
public class RequestedAttributesMapper extends AbstractIdentifiableInitializableComponent {

    /** Log. */
    private final Logger log = LoggerFactory.getLogger(RequestedAttributesMapper.class);

    /** The mappers we can apply. */
    private List<RequestedAttributeMapper> mappers = Collections.EMPTY_LIST;

    /** The String used to prefix log message. */
    private String logPrefix;

    /**
     * Get the mappers.
     * 
     * @return Returns the mappers.
     */
    @Nonnull public List<RequestedAttributeMapper> getMappers() {
        return mappers;
    }

    /**
     * Set the attribute mappers into the lookup map.
     * 
     * @param theMappers The mappers to set.
     */
    public void setMappers(@Nonnull List<RequestedAttributeMapper> theMappers) {
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
    public Multimap<String, RequestedAttribute> mapAttributes(
            @Nonnull @NonnullElements List<org.opensaml.saml.saml2.metadata.RequestedAttribute> prototypes) {

        Multimap<String, RequestedAttribute> result = ArrayListMultimap.create();

        for (org.opensaml.saml.saml2.metadata.RequestedAttribute prototype : prototypes) {
            for (RequestedAttributeMapper mapper : mappers) {

                Map<String, RequestedAttribute> mappedAttributes = mapper.mapAttribute(prototype);

                log.debug("{} SAML attribute '{}' mapped to {} attributes by mapper '{}'", getLogPrefix(),
                        prototype.getName(), mappedAttributes.size(), mapper.getId());

                for (Entry<String, RequestedAttribute> entry : mappedAttributes.entrySet()) {
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
        for (RequestedAttributeMapper mapper : mappers) {
            mapper.initialize();
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
