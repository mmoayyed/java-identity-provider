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

package net.shibboleth.idp.profile.logic;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Predicate that evaluates an {@link net.shibboleth.idp.attribute.context.AttributeContext} and checks
 * for particular attribute/value pairs.
 * 
 * <p>A map to a collection of strings is used to represent the attribute(s) and value(s) to evaluate.
 * The values are evaluated as a disjunction (OR) and the attributes are evaluated as a conjunction (AND).</p>
 * 
 * <p>This handles only simple string-valued data, or if scope is supplied, requires scoped values.</p>
 * 
 * <p>For the special case of checking for an attribute's presence, regardless of values, the '*' value is
 * supported. Note that this does NOT exclude pathological cases such as empty or null values. A more
 * advanced predicate should be used to deal with such cases.</p>
 */
public class SimpleAttributePredicate extends AbstractAttributePredicate {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SimpleAttributePredicate.class);

    /** Map of attribute IDs to values. */
    @Nonnull @NonnullElements private ListMultimap<String,String> attributeValueMap;
    
    /** Optional scope to check. */
    @Nullable @NotEmpty private String scope;
    
    /** Constructor. */
    public SimpleAttributePredicate() {
        attributeValueMap = ArrayListMultimap.create();
    }

    /**
     * Set the map of attribute/value pairs (as a map of string collections) to check for.
     * 
     * @param map   map of attribute/value pairs
     */
    public void setAttributeValueMap(@Nonnull @NonnullElements final Map<String,Collection<String>> map) {
        Constraint.isNotNull(map, "Attribute/value map cannot be null");
        
        attributeValueMap.clear();
        for (final Map.Entry<String,Collection<String>> entry : map.entrySet()) {
            final String attributeId = StringSupport.trimOrNull(entry.getKey());
            attributeValueMap.putAll(attributeId, StringSupport.normalizeStringCollection(entry.getValue()));
        }
    }
    
    /**
     * Set a scope to check for.
     * 
     * <p>If set, values that "match" must be scoped with this value. A "*" will match any scope, but
     * one must exist.</p>
     * 
     * @param s scope to check for
     * 
     * @since 4.2.0
     */
    public void setScope(@Nullable @NotEmpty final String s) {
        scope = StringSupport.trimOrNull(s);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean allowNullAttributeContext() {
        return attributeValueMap.isEmpty() && scope == null;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean hasMatch(final Map<String, IdPAttribute> attributeMap) {
        for (final String id : attributeValueMap.keySet()) {
            log.debug("Checking for attribute: {}", id);

            final IdPAttribute attribute = attributeMap.get(id);
            if (attribute == null) {
                log.debug("Attribute {} not found in context", id);
                return false;
            }

            boolean matched = false;

            for (final String value : attributeValueMap.get(id)) {
                if (findMatch(value, attribute)) {
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                log.debug("Attribute {} values not matched", id);
                return false;
            }
        }
        return true;
    }

    /**
     * Look for a matching value in an attribute.
     * 
     * @param toMatch   value to look for
     * @param attribute attribute to check
     * 
     * @return true iff the value is one of the attribute's values
     */
// Checkstyle: CyclomaticComplexity OFF
    protected boolean findMatch(@Nonnull @NotEmpty final String toMatch, @Nonnull final IdPAttribute attribute) {
        
        if ("*".equals(toMatch) && scope == null) {
            log.debug("Wildcard (*) value rule for attribute {}", attribute.getId());
            return true;
        }
        
        for (final IdPAttributeValue value : attribute.getValues()) {
            if (scope != null && value instanceof ScopedStringAttributeValue) {
                final String scopeCopy = scope;
                if ("*".equals(toMatch) || toMatch.equals(((ScopedStringAttributeValue) value).getValue())) {
                    if ("*".equals(scopeCopy) || scopeCopy.equals(((ScopedStringAttributeValue) value).getScope())) {
                        log.debug("Found matching value ({}) and scope ({}) in attribute {}", toMatch, scopeCopy,
                                attribute.getId());
                        return true;
                    }
                }
            } else if (scope == null && value instanceof StringAttributeValue) {
                if (toMatch.equals(((StringAttributeValue) value).getValue())) {
                    log.debug("Found matching value ({}) in attribute {}", toMatch, attribute.getId());
                    return true;
                }
            }
        }
        
        return false;
    }
// Checkstyle: CyclomaticComplexity ON

}