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

package net.shibboleth.idp.attribute.resolver.impl.dc.ldap;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO(lajoie): do we want something that can map data types too like the RDBMS equivalent?
//TODO(lajoie): want some settings to control what happens if there is more than one LdapEntry

/**
 * A simple {@link SearchResultMappingStrategy} that iterates over all result entries and includes all attribute values
 * as strings.
 */
public class StringAttributeValueMappingStrategy implements SearchResultMappingStrategy {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(StringAttributeValueMappingStrategy.class);

    /** {@inheritDoc} */
    @Override @Nullable public Map<String, IdPAttribute> map(@Nonnull final SearchResult results)
            throws ResolutionException {
        Constraint.isNotNull(results, "Results can not be null");

        final Map<String, IdPAttribute> attributes = new HashMap<String, IdPAttribute>();
        for (LdapEntry entry : results.getEntries()) {
            for (LdapAttribute attr : entry.getAttributes()) {
                final IdPAttribute attribute = new IdPAttribute(attr.getName());
                final LinkedHashSet<StringAttributeValue> hs = new LinkedHashSet<>(attr.getStringValues().size());

                for (String value : attr.getStringValues()) {
                    hs.add(new StringAttributeValue(value));
                }
                attribute.setValues(hs);
                attributes.put(attribute.getId(), attribute);
            }
        }
        log.trace("Mapping strategy mapped {} to {}", results, attributes);
        if (attributes.isEmpty()) {
            return null;
        } else {
            return attributes;
        }
    }
}