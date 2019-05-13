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

import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.ListMultimap;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/**
 * Predicate over an {@link AttributeContext} that derives the value(s) to match based
 * on one or more supplied Functions instead of static values.
 * 
 * @since 3.4.0
 */
public class DynamicAttributePredicate extends AbstractAttributePredicate {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(DynamicAttributePredicate.class);

    /** Map of attribute IDs to functions. */
    @Nonnull @NonnullElements private ListMultimap<String,Function<ProfileRequestContext,String>> attributeFunctionMap;
    
    /** Constructor. */
    public DynamicAttributePredicate() {
        attributeFunctionMap = ArrayListMultimap.create();
    }
    
    /**
     * Set the map of attribute/function pairs (as a map of function collections) to check for.
     * 
     * @param map   map of attribute/function pairs
     */
    public void setAttributeFunctionMap(
            @Nonnull @NonnullElements final Map<String,Collection<Function<ProfileRequestContext,String>>> map) {
        Constraint.isNotNull(map, "Attribute/value map cannot be null");
        
        attributeFunctionMap.clear();
        for (final Map.Entry<String,Collection<Function<ProfileRequestContext,String>>> entry : map.entrySet()) {
            final String attributeId = StringSupport.trimOrNull(entry.getKey());
            attributeFunctionMap.putAll(attributeId, Collections2.filter(entry.getValue(), Predicates.notNull()));
        }
    }

    /** {@inheritDoc} */
    public boolean test(@Nullable final ProfileRequestContext input) {
        
        final AttributeContext attributeCtx = getAttributeContextLookupStrategy().apply(input);
        if (attributeCtx == null) {
            log.warn("No AttributeContext located for evaluation");
            return allowNullAttributeContext();
        }
        
        final Map<String,IdPAttribute> attributes = isUseUnfilteredAttributes()
                ? attributeCtx.getUnfilteredIdPAttributes()
                : attributeCtx.getIdPAttributes();

        if (hasMatch(input, attributes)) {
            log.debug("Context satisfied requirements");
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean hasMatch(@Nonnull @NonnullElements final Map<String,IdPAttribute> attributeMap) {
        log.error("Method should never be called");
        return false;
    }

    /**
     * Implementation of the condition to evaluate.
     * 
     * @param profileRequestContext current profile request context
     * @param attributeMap  the attributes to evaluate
     * 
     * @return the condition result
     */
    protected boolean hasMatch(@Nullable final ProfileRequestContext profileRequestContext,
            @Nonnull @NonnullElements final Map<String,IdPAttribute> attributeMap) {
        
        for (final String id : attributeFunctionMap.keySet()) {
            log.debug("Checking for attribute: {}", id);

            final IdPAttribute attribute = attributeMap.get(id);
            if (attribute == null) {
                log.debug("Attribute {} not found in context", id);
                return false;
            }

            boolean matched = false;

            for (final Function<ProfileRequestContext,String> fn : attributeFunctionMap.get(id)) {
                if (findMatch(fn.apply(profileRequestContext), attribute)) {
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
    protected boolean findMatch(@Nonnull @NotEmpty final String toMatch, @Nonnull final IdPAttribute attribute) {
        
        if ("*".equals(toMatch)) {
            log.debug("Wildcard (*) value rule for attribute {}", attribute.getId());
            return true;
        } else {
            for (final IdPAttributeValue value : attribute.getValues()) {
                if (value instanceof StringAttributeValue) {
                    if (toMatch.equals(((StringAttributeValue)value).getValue())) {
                        log.debug("Found matching value ({}) in attribute {}", toMatch, attribute.getId());
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

}
