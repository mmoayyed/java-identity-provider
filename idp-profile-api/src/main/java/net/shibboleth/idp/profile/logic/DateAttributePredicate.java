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

import java.time.format.DateTimeFormatter;

import javax.annotation.Nonnull;

import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.primitive.DeprecationSupport;
import net.shibboleth.shared.primitive.DeprecationSupport.ObjectType;

/**
 * Deprecated stub for relocated class.
 * 
 * @deprecated
 */
@Deprecated(since="5.0.0", forRemoval=true)
@SuppressWarnings("null")
public class DateAttributePredicate extends net.shibboleth.profile.context.logic.DateAttributePredicate {

    /**
     * Create a new instance that performs date comparisons against the given attribute.
     *
     * @param attribute Attribute name that provides candidate date values to test.
     */
    public DateAttributePredicate(@Nonnull @NotEmpty @ParameterName(name="attribute") final String attribute) {
        super(attribute);
        DeprecationSupport.warn(ObjectType.CLASS, getClass().getName(), null,
                "Parent bean 'shibboleth.Conditions.DateAttribute'");
    }

    /**
     * Create a new instance that performs date comparisons against the given attribute
     * using the given date parser.
     *
     * @param attribute Attribute name that provides candidate date values to test.
     * @param formatter Date/time parser.
     */
    public DateAttributePredicate(@Nonnull @NotEmpty @ParameterName(name="attribute") final String attribute,
            @Nonnull @ParameterName(name="formatter") final DateTimeFormatter formatter) {
        super(attribute, formatter);
        DeprecationSupport.warn(ObjectType.CLASS, getClass().getName(), null,
                "Parent bean 'shibboleth.Conditions.DateAttribute'");
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
        super(attribute, formatString);
        DeprecationSupport.warn(ObjectType.CLASS, getClass().getName(), null,
                "Parent bean 'shibboleth.Conditions.DateAttribute'");
    }
    
}