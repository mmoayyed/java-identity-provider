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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Predicate that evaluates an {@link net.shibboleth.idp.attribute.context.AttributeContext} and checks
 * a specific attribute for value(s) that match a regular expression.
 * 
 * <p>This handles only simple string-valued data.</p>
 */
public class RegexAttributePredicate extends AbstractAttributePredicate {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(RegexAttributePredicate.class);

    /** The attribute to evaluate. */
    @Nullable @NotEmpty private String attributeId;

    /** Regular expression. */
    @Nullable private Pattern pattern;

    /**
     * Get the attribute ID to check.
     * 
     * @return the attribute ID to check
     */
    @Nullable @NotEmpty public String getAttributeId() {
        return attributeId;
    }

    /**
     * Set the attribute ID to check.
     * 
     * @param id the attribute ID to check
     */
    public void setAttributeId(@Nonnull @NotEmpty final String id) {
        attributeId = Constraint.isNotNull(StringSupport.trimOrNull(id), "Attribute ID cannot be null or empty");
    }

    /**
     * Get the pattern to match the attribute values against.
     * 
     * @return the pattern to match the attribute values against
     */
    @Nullable public Pattern getPattern() {
        return pattern;
    }

    /**
     * Set the pattern to match the attribute values against.
     * 
     * @param p the pattern to match the attribute values against
     */
    public void setPattern(@Nonnull final Pattern p) {
        pattern = p;
    }

    /**
     * Set the pattern to match the attribute values against.
     * 
     * @param s the pattern to match the attribute values against
     */
    public void setPattern(@Nonnull @NotEmpty final String s) {
        pattern = Pattern.compile(s);
    }

    /** {@inheritDoc} */
    @Override protected boolean hasMatch(@Nonnull @NonnullElements final Map<String, IdPAttribute> attributeMap) {

        if (attributeId == null || pattern == null) {
            log.warn("Attribute ID or regular expression were not set");
            return false;
        }
        
        final IdPAttribute attribute = attributeMap.get(attributeId);
        if (attribute == null) {
            log.debug("Attribute '{}' not found in context", attributeId);
            return false;
        }
        
        for (final IdPAttributeValue value : attribute.getValues()) {
            if (value instanceof StringAttributeValue) {
                assert pattern != null;
                final Matcher m = pattern.matcher(((StringAttributeValue)value).getValue());
                if (m.matches()) {
                    log.debug("Found matching value '{}' in attribute '{}'", m.group(), attributeId);
                    return true;
                }
            }
        }
        log.debug("Attribute '{}' values not matched", attributeId);
        return false;
    }

}
