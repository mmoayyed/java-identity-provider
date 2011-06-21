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

package net.shibboleth.idp.attribute.resolver.impl;

import java.util.Collection;
import java.util.Set;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.ComponentValidationException;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.ScopedAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;

import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;
import org.opensaml.util.collections.LazySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An attribute definition that creates {@link ScopedAttributeValue}s by taking a source attribute value splitting it at
 * a delimiter. The first atom becomes the attribute value and the second value becomes the scope.
 */
@ThreadSafe
public class PrescopedAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(PrescopedAttributeDefinition.class);

    /** Delimiter between value and scope. */
    private final String scopeDelimiter;

    /**
     * Constructor.
     * 
     * @param id the id of the object
     * @param delimiter scope of the attribute
     */
    public PrescopedAttributeDefinition(final String id, final String delimiter) {
        super(id);
        scopeDelimiter = delimiter;
        Assert.isFalse(StringSupport.isNullOrEmpty(scopeDelimiter), "Scope delimiter must be non null and non empty");
    }

    /**
     * Access the scope delimiter we were initialised with.
     * 
     * @return the delimiter
     */
    public String getScopeDelimiter() {
        return scopeDelimiter;
    }

    /** {@inheritDoc} */
    protected Attribute<ScopedAttributeValue> doAttributeResolution(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        
        final Set<ResolverPluginDependency> depends = getDependencies();
        if (null == depends) {
            log.info("PrescopedAttribute definition " + getId() + " had no dependencies");
            return null;
        }
        
        final Collection<ScopedAttributeValue> resultingValues = new LazySet<ScopedAttributeValue>();
        for (ResolverPluginDependency dep : depends) {
            final Attribute<?> dependentAttribute = dep.getDependentAttribute(resolutionContext);
            if (null == dependentAttribute) {
                log.error("Dependency of PrescopedAttribute " + getId() + " returned null dependent attribute");
                continue;
            }

            final Collection<?> values = dependentAttribute.getValues();
            if (null == dependentAttribute.getValues()) {
                log.error("Dependency " + dependentAttribute.getId() + " of PrescopedAttribute " + getId()
                        + "returned null value set");
                continue;
            }
            if (dependentAttribute.getValues().isEmpty()) {
                log.debug("Dependency " + dependentAttribute.getId() + " of PrescopedAttribute " + getId()
                        + "returned no values, skipping");
                continue;
            }
            
            for (Object value : values) {
                if (!(value instanceof String)) {
                    log.debug("Skipping non string value " + value.toString());
                    continue;
                }
                final String[] stringValues = ((String) value).split(scopeDelimiter);
                if (stringValues.length < 2) {
                    log.error("Input attribute value {} does not contain delimited {} and can not be split", value,
                            scopeDelimiter);
                    throw new AttributeResolutionException("Input attribute value can not be split.");
                }
                resultingValues.add(new ScopedAttributeValue(stringValues[0], stringValues[1]));
            }
        }
        if (resultingValues.isEmpty()) {
            log.debug("Prescoped definition " + getId() + " returned no values");
        }
        
        final Attribute<ScopedAttributeValue> resultantAttribute = new Attribute<ScopedAttributeValue>(getId());
        resultantAttribute.setValues(resultingValues);
        return resultantAttribute;
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {

        if (StringSupport.isNullOrEmpty(scopeDelimiter)) {
            final String msg =
                    "PrescopedAtributeDefinition (" + getId() + ") Should have a valid delimiter.  None provided.";
            log.error(msg);
            throw new ComponentValidationException(msg);
        }
    }
}