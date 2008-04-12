/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs many to one mapping of source values to a return value. SourceValue strings may include regular expressions
 * and the ReturnValue may include back references to capturing groups as supported by {@link java.util.regex.Pattern}.
 */
public class ValueMap {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ValueMap.class);

    /** Return value. */
    private String returnValue;

    /** Source values. */
    private Collection<SourceValue> sourceValues;

    /** Constructor. */
    public ValueMap() {
        sourceValues = new HashSet<SourceValue>();
    }

    /**
     * Gets the return value.
     * 
     * @return the return value
     */
    public String getReturnValue() {
        return returnValue;
    }

    /**
     * Sets the return value.
     * 
     * @param newReturnValue the return value
     */
    public void setReturnValue(String newReturnValue) {
        returnValue = newReturnValue;
    }

    /**
     * Gets the collection of source values.
     * 
     * @return the collection of source values
     */
    public Collection<SourceValue> getSourceValues() {
        return sourceValues;
    }

    /**
     * Evaluate an incoming attribute value against this value map.
     * 
     * @param sourceValue incoming attribute value
     * @return set of new values the incoming value mapped to
     */
    public Set<String> evaluate(String sourceValue) {
        Set<String> mappedValues = new HashSet<String>();
        Matcher m;

        for (SourceValue vmv : sourceValues) {
            int flags = vmv.isIgnoreCase() ? Pattern.CASE_INSENSITIVE : 0;

            try {
                m = Pattern.compile(vmv.getValue(), flags).matcher(sourceValue);

                if (vmv.isPartialMatch() || m.matches()) {
                    String newValue = m.replaceAll(returnValue);
                    if (!DatatypeHelper.isEmpty(newValue)) {
                        mappedValues.add(newValue);
                    }
                }
            } catch (PatternSyntaxException e) {
                log.debug("MappedAttributeDefinition caught an exception when trying to match value {}.  Skipping this value.", sourceValue);
            }
        }

        return mappedValues;
    }

    /**
     * Represents incoming attribute values and rules used for matching them. The value may include regular expressions.
     */
    public class SourceValue {

        /**
         * Value string. This may contain regular expressions.
         */
        private String value;

        /**
         * Whether case should be ignored when matching.
         */
        private boolean ignoreCase;

        /**
         * Whether partial matches should be allowed.
         */
        private boolean partialMatch;

        /**
         * Constructor.
         * 
         * @param newValue value string
         * @param newIgnoreCase whether case should be ignored when matching
         * @param newPartialMatch whether partial matches should be allowed
         */
        public SourceValue(String newValue, boolean newIgnoreCase, boolean newPartialMatch) {
            value = newValue;
            ignoreCase = newIgnoreCase;
            partialMatch = newPartialMatch;
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
        public String getValue() {
            return value;
        }

        /** {@inheritDoc} */
        public String toString() {
            return getValue();
        }

    }
}