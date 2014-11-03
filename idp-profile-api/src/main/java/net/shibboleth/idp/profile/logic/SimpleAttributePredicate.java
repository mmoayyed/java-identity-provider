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

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * Predicate that evaluates an {@link AttributeContext} and checks for particular attribute/value pairs.
 * 
 * <p>A map to a collection of strings is used to represent the attribute(s) and value(s) to evaluate.
 * The values are evaluated as a disjunction (OR) and the attributes are evaluated as a conjunction (AND).</p>
 * 
 * <p>This handles only simple string-valued data.</p>
 * 
 * <p>For the special case of checking for an attribute's presence, regardless of values, the '*' value is
 * supported.</p>
 */
public class SimpleAttributePredicate implements Predicate<ProfileRequestContext> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SimpleAttributePredicate.class);

    /** Strategy function to lookup {@link AttributeContext}. */
    @Nonnull private Function<ProfileRequestContext,AttributeContext> attributeContextLookupStrategy;

    /** Map of attribute IDs to values. */
    @Nonnull @NonnullElements private ListMultimap<String,String> attributeValueMap;
    
    /** Constructor. */
    public SimpleAttributePredicate() {
        attributeContextLookupStrategy = Functions.compose(new ChildContextLookup<>(AttributeContext.class),
                new ChildContextLookup<ProfileRequestContext,RelyingPartyContext>(RelyingPartyContext.class));
        
        attributeValueMap = ArrayListMultimap.create();
    }

    /**
     * Set the lookup strategy to use to locate the {@link AttributeContext}.
     * 
     * @param strategy lookup function to use
     */
    public void setAttributeContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,AttributeContext> strategy) {

        attributeContextLookupStrategy =
                Constraint.isNotNull(strategy, "AttributeContext lookup strategy cannot be null");
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

    /** {@inheritDoc} */
    @Override
    public boolean apply(@Nullable final ProfileRequestContext input) {
        
        final AttributeContext attributeCtx = attributeContextLookupStrategy.apply(input);
        if (attributeCtx == null) {
            log.warn("No AttributeContext located for evaluation");
            return attributeValueMap.isEmpty();
        }
        
        for (final String id : attributeValueMap.keySet()) {
            log.debug("Checking for attribute: {}", id);
            
            final IdPAttribute attribute = attributeCtx.getIdPAttributes().get(id);
            if (attribute == null) {
                log.info("Attribute {} not found in context", id);
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
                log.info("Attribute {} values not matched", id);
                return false;
            }
        }
        
        log.debug("Context satisfied requirements");
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
    private boolean findMatch(@Nonnull @NotEmpty final String toMatch, @Nonnull final IdPAttribute attribute) {
        
        if ("*".equals(toMatch)) {
            log.debug("Wildcard (*) value rule for attribute {}", attribute.getId());
            return true;
        } else {
            for (final IdPAttributeValue<?> value : attribute.getValues()) {
                if (value instanceof StringAttributeValue) {
                    if (toMatch.equals(value.getValue())) {
                        log.debug("Found matching value ({}) in attribute {}", toMatch, attribute.getId());
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
}