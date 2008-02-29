/*
 * Copyright 2008 University Corporation for Advanced Internet Development, Inc.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ScopedAttributeValue;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;

/**
 * An attribute definition that creates {@link ScopedAttributeValue}s by taking a source attribute value splitting it
 * at a delimiter. The first atom becomes the attribute value and the second value becomes the scope.
 */
public class PrescopedAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(PrescopedAttributeDefinition.class);

    /** Delimiter between value and scope. */
    private String scopeDelimiter;

    /**
     * Constructor.
     * 
     * @param delimiter scope of the attribute
     */
    public PrescopedAttributeDefinition(String delimiter) {
        scopeDelimiter = delimiter;
    }

    /** {@inheritDoc} */
    public BaseAttribute<ScopedAttributeValue> doResolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        BasicAttribute<ScopedAttributeValue> attribute = new BasicAttribute<ScopedAttributeValue>();
        attribute.setId(getId());

        Collection<?> values = getValuesFromAllDependencies(resolutionContext);
        if (values == null || values.isEmpty()) {
            return attribute;
        }

        String[] stringValues;
        for (Object value : values) {
            if (value instanceof String && value != null) {
                continue;
            }

            stringValues = ((String) value).split(scopeDelimiter);
            if (stringValues.length < 2) {
                log.error("Input attribute value {} does not contain delimited {} and can not be split", value,
                        scopeDelimiter);
                throw new AttributeResolutionException("Input attribute value can not be split.");
            }
            attribute.getValues().add(new ScopedAttributeValue(stringValues[0], stringValues[1]));
        }

        return attribute;
    }

    /**
     * Get delimiter between value and scope.
     * 
     * @return delimiter between value and scope
     */
    public String getScopeDelimited() {
        return scopeDelimiter;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        // do nothing
    }
}