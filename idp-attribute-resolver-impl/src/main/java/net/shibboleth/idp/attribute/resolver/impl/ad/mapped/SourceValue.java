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

package net.shibboleth.idp.attribute.resolver.impl.ad.mapped;

import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.primitive.StringSupport;

import com.google.common.base.Objects;

/**
 * Represents incoming attribute values and rules used for matching them. The value may include regular expressions.
 */
public class SourceValue {

    /**
     * Value string. This may contain regular expressions.
     */
    private final String value;

    /**
     * Whether case should be ignored when matching.
     */
    private final boolean ignoreCase;

    /**
     * Whether partial matches should be allowed.
     */
    private final boolean partialMatch;

    /**
     * Constructor.
     * 
     * @param theValue value string
     * @param theIgnoreCase whether case should be ignored when matching
     * @param thePartialMatch whether partial matches should be allowed
     */
    public SourceValue(@Nullable String theValue, boolean theIgnoreCase, boolean thePartialMatch) {
        value = StringSupport.trimOrNull(theValue);
        ignoreCase = theIgnoreCase;
        partialMatch = thePartialMatch;
    }

    /**
     * Gets whether case should be ignored when matching.
     * 
     * @return whether case should be ignored when matching
     */
    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    /**
     * Gets whether partial matches should be allowed.
     * 
     * @return whether partial matches should be allowed
     */
    public boolean isPartialMatch() {
        return partialMatch;
    }

    /**
     * Gets the value string.
     * 
     * @return the value string.
     */
    @Nullable public String getValue() {
        return value;
    }

    /** {@inheritDoc} */
    public String toString() {
        return Objects.toStringHelper(this).add("value", value).add("IsIgnoreCase", isIgnoreCase())
                .add("isPartialMatch", isPartialMatch()).toString();
    }

}