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

package net.shibboleth.idp.attribute.filter.impl.policyrule.filtercontext;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.impl.policyrule.AbstractPolicyRule;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * A policy rule that checks if the given attribute has more than the minimum number of values but less than the
 * maximum.
 */
public class NumOfAttributeValuesPolicyRule extends AbstractPolicyRule {
    
    /** The logger. */
    private final Logger log = LoggerFactory.getLogger(NumOfAttributeValuesPolicyRule.class);

    /** ID of the attribute that will be checked. */
    private String attributeId;

    /** Minimum allowed number of attribute values. */
    private int minimumValues = -1;

    /** Maximum allowed number of attribute values. */
    private int maximumValues = -1;

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (null == attributeId) {
            throw new ComponentInitializationException(getLogPrefix() + " no AttributeID specified");
        }
        if (minimumValues < 0) {
            throw new ComponentInitializationException(getLogPrefix() + " a minimum value >= 0 must be specified");
        }
        if (maximumValues <= 0) {
            throw new ComponentInitializationException(getLogPrefix() + " a maximum value > 0 must be specified");
        }
    }

    /** {@inheritDoc} */
    @Nonnull public Tristate matches(@Nonnull AttributeFilterContext filterContext) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        final Attribute attribute = filterContext.getPrefilteredAttributes().get(attributeId);
        
        if (null == attribute) {
            log.warn("{} Attribute {} not found", getLogPrefix(), attributeId);
            return Tristate.FALSE;
        }
        
        final int numOfValues = attribute.getValues().size();
        boolean result = (numOfValues >= minimumValues) && (numOfValues <= maximumValues);
        log.debug("{} Attribute {} has {} values, returning {}", getLogPrefix(), attributeId, numOfValues, result);
        
        if (result) {
            return Tristate.TRUE;
        }
        return Tristate.FALSE;
    }

    /**
     * Return the attribute whose value count is under question.
     * 
     * @return Returns the attributeId.
     */
    public String getAttributeId() {
        return attributeId;
    }

    /**
     * Set the attribute whose value count is under question.
     * 
     * @param attribute The attributeId to set.
     */
    public void setAttributeId(@Nonnull @NotEmpty String attribute) {
        attributeId = Constraint.isNotNull(StringSupport.trimOrNull(attribute), "attributeID must be nonempty");
    }

    /**
     * Return the minimum allowed number of attribute values.
     * 
     * @return Returns the minimumValues.
     */
    public int getMinimumValues() {
        return minimumValues;
    }

    /**
     * Set the minimum allowed number of attribute values.
     * 
     * @param minValues The minimumValues to set.
     */
    public void setMinimumValues(int minValues) {
        minimumValues =  (int) Constraint.isGreaterThanOrEqual(0, minValues, "min value must be >= 0");
    }

    /**
     * Return the maximum allowed number of attribute values.
     * 
     * @return Returns the maximumValues.
     */
    public int getMaximumValues() {
        return maximumValues;
    }

    /**
     * Set the maximum allowed number of attribute values.
     * 
     * @param maxValues The maximumValues to set.
     */
    public void setMaximumValues(int maxValues) {
        maximumValues = (int) Constraint.isGreaterThan(0, maxValues, "max value must be > 0");
    }

}
