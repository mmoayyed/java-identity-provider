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

package net.shibboleth.idp.attribute.mapper.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.opensaml.saml.saml2.metadata.RequestedAttribute;

import net.shibboleth.idp.attribute.AttributeEncoder;
import net.shibboleth.idp.attribute.IdPRequestedAttribute;
import net.shibboleth.idp.attribute.mapper.AbstractSAMLAttributeMapper;
import net.shibboleth.idp.attribute.mapper.AbstractSAMLAttributesMapper;
import net.shibboleth.idp.attribute.mapper.AttributeMapper;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.saml.attribute.encoding.AttributeMapperFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * This class conceptually represents the content of a attribute-map file, hence it describes (and then does) the
 * mappings from a {@link java.util.List} of SAML2 {@link RequestedAttribute} into a {@link Multimap} going from (SAML2)
 * attributeId to idp {@link IdPRequestedAttribute}s (or null if the mapping failed for type reasons).
 * 
 */
public class RequestedAttributesMapper extends
        AbstractSAMLAttributesMapper<org.opensaml.saml.saml2.metadata.RequestedAttribute, IdPRequestedAttribute> {

    /**
     * Constructor to create the mapping from an existing resolver. <br/>
     * This code inverts the {@link AttributeEncoder} (internal attribute -> SAML Attributes) into
     * {@link AttributeMapper} (SAML [RequestedAttributes] -> internal [Requested] Attributes). <br/>
     * to generate the {@link AbstractSAMLAttributeMapper} (with no
     * {@link AbstractSAMLAttributeMapper#getAttributeIds(). These are accumulated into a {@link Multimap}, where the
     * key is the {@link AbstractSAMLAttributeMapper} and the values are the (IdP) attribute names. The collection of
     * {@link AttributeMapper}s can then be extracted from the map, and the appropriate internal names added (these
     * being the value of the {@link Multimap})
     * 
     * @param resolver The resolver
     */
    public RequestedAttributesMapper(AttributeResolver resolver) {

        super();
        setId(resolver.getId());

        final Multimap<AbstractSAMLAttributeMapper<RequestedAttribute, IdPRequestedAttribute>, String> theMappers;

        theMappers = HashMultimap.create();

        for (AttributeDefinition attributeDef : resolver.getAttributeDefinitions().values()) {
            for (AttributeEncoder encode : attributeDef.getAttributeEncoders()) {
                if (encode instanceof AttributeMapperFactory) {
                    // There is an appropriate reverse mappers
                    AttributeMapperFactory factory = (AttributeMapperFactory) encode;
                    AbstractSAMLAttributeMapper<RequestedAttribute, IdPRequestedAttribute> mapper =
                            factory.getRequestedMapper();

                    theMappers.put(mapper, attributeDef.getId());
                }
            }
        }

        final List<AttributeMapper<RequestedAttribute, IdPRequestedAttribute>> mappers =
                new ArrayList<AttributeMapper<RequestedAttribute, IdPRequestedAttribute>>(theMappers.values().size());

        for (Entry<AbstractSAMLAttributeMapper<RequestedAttribute, IdPRequestedAttribute>, Collection<String>> entry : 
                 theMappers.asMap().entrySet()) {

            AbstractSAMLAttributeMapper<RequestedAttribute, IdPRequestedAttribute> mapper = entry.getKey();
            mapper.setAttributeIds(new ArrayList<String>(entry.getValue()));
            mappers.add(mapper);
        }

        setMappers(mappers);
    }

    /**
     * Constructor.
     * 
     */
    public RequestedAttributesMapper() {
        super();
    }
}