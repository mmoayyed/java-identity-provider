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

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Map;

/**
 * Provides a date/time matching predicate that compares a date-based attribute value against
 * current system time with optional offset. By convention the predicate returns true if and only if
 * the date represented by the attribute value is after the current system time; false otherwise.
 * Thus the semantics are well-suited for cases such as evaluation of expiration dates.
 *
 * @author Marvin S. Addison
 */
public class DateAttributePredicate extends AbstractAttributePredicate {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(DateAttributePredicate.class);

    /** Name of attribute to query for. */
    @Nonnull @NotEmpty private final String attributeName;

    /** Formatter used to parse string-based date attribute values. */
    @Nonnull private final DateTimeFormatter dateTimeFormatter;

    /** Offset from system time used for date comparisons. */
    @Nullable private org.joda.time.Duration systemTimeOffset;
    
    /** Result of predicate if attribute is missing or has no values. */
    private boolean resultIfMissing;

    /**
     * Create a new instance that performs date comparisons against the given attribute
     * using ISO date/time format parser by default.
     *
     * @param attribute Attribute name that provides candidate date values to test.
     */
    public DateAttributePredicate(@Nonnull @NotEmpty @ParameterName(name="attribute") final String attribute) {
        this(attribute, ISODateTimeFormat.dateOptionalTimeParser());
    }

    /**
     * Create a new instance that performs date comparisons against the given attribute
     * using the given date parser.
     *
     * @param attribute Attribute name that provides candidate date values to test.
     * @param formatter Date/time parser.
     */
    @Deprecated
    public DateAttributePredicate(@Nonnull @NotEmpty @ParameterName(name="attribute") final String attribute,
            @Nonnull @ParameterName(name="formatter") final DateTimeFormatter formatter) {
        attributeName = Constraint.isNotNull(attribute, "Attribute cannot be null");
        dateTimeFormatter = Constraint.isNotNull(formatter, "Formatter cannot be null");
        resultIfMissing = false;
    }

    /**
     * Create a new instance that performs date comparisons against the given attribute
     * using the given date parser.
     *
     * @param attribute Attribute name that provides candidate date values to test.
     * @param formatString date/time parsing string, currently based on {@link DateTimeFormatter}
     */
    public DateAttributePredicate(@Nonnull @NotEmpty @ParameterName(name="attribute") final String attribute,
            @Nonnull @NotEmpty @ParameterName(name="formatString") final String formatString) {
        attributeName = Constraint.isNotNull(attribute, "Attribute cannot be null");
        dateTimeFormatter = DateTimeFormat.forPattern(
                Constraint.isNotNull(formatString, "Format string cannot be null"));
        resultIfMissing = false;
    }
    
    /**
     * Set the system time offset, which affects the reference date for comparisons.
     * By default all comparisons are against system time, i.e. zero offset.
     *
     * @param offset System time offset. A negative value decreases the target date (sooner);
     *                         a positive value increases the target date (later).
     */
    @Deprecated
    public void setSystemTimeOffset(@Nonnull final org.joda.time.Duration offset) {
        systemTimeOffset = Constraint.isNotNull(offset, "Offset cannot be null");
    }

    /**
     * Set the system time offset, which affects the reference date for comparisons.
     * By default all comparisons are against system time, i.e. zero offset.
     *
     * @param offset System time offset. A negative value decreases the target date (sooner);
     *                         a positive value increases the target date (later).
     */
    @Duration public void setOffset(@Duration final long offset) {
        systemTimeOffset = org.joda.time.Duration.millis(offset);
    }
    
    /**
     * Set the result to return if the attribute to check is missing or has no values.
     * 
     * @param flag  flag to set
     */
    public void setResultIfMissing(final boolean flag) {
        resultIfMissing = flag;
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean allowNullAttributeContext() {
        return resultIfMissing;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean hasMatch(@Nonnull @NonnullElements final Map<String, IdPAttribute> attributeMap) {
        
        final IdPAttribute attribute = attributeMap.get(attributeName);
        if (attribute == null) {
            log.debug("Attribute {} not found in context, returning {}", attributeName, resultIfMissing);
            return resultIfMissing;
        } else if (attribute.getValues().isEmpty()) {
            log.debug("Attribute {} has no values, returning {}", attributeName, resultIfMissing);
            return resultIfMissing;
        }
        
        String dateString;
        for (final IdPAttributeValue<?> value : attribute.getValues()) {
            if (value instanceof StringAttributeValue) {
                dateString = ((StringAttributeValue) value).getValue();
                try {
                    if (dateTimeFormatter.parseDateTime(dateString).plus(systemTimeOffset).isAfterNow()) {
                        return true;
                    }
                } catch (final RuntimeException e) {
                    log.warn("{} is not a valid date for the configured date parser", dateString, e);
                }
            }
        }
        return false;
    }
    
}