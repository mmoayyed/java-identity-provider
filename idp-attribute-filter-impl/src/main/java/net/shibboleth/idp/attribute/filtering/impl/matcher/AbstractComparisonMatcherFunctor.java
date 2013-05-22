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

package net.shibboleth.idp.attribute.filtering.impl.matcher;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.attribute.filtering.MatchFunctor;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;

/**
 * This is the bases of all implementations of {@link MatchFunctor} which do some sort of comparison.<br/>
 * <br/>
 * 
 * PolicyRequirementRule implementations will set {@link #policyPredicate} and get a default result for
 * {@link #getMatchingValues(Attribute, AttributeFilterContext)} which states that if the predicate is true then we get
 * all values for the attribute otherwise none.
 * 
 * AttributeRule implementations will set {@link #valuePredicate} or and get a default for
 * {@link #evaluatePolicyRule(AttributeFilterContext)} which says that if this is true for one value then it is true for
 * all.
 */

public abstract class AbstractComparisonMatcherFunctor extends AbstractIdentifiableInitializableComponent implements
        MatchFunctor {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractComparisonMatcherFunctor.class);

    /** Predicate used to check attribute values. */
    private Predicate<AttributeValue> valuePredicate;

    /** Predicate used to check attributePolicy. */
    private Predicate<AttributeFilterContext> policyPredicate;

    /** The String used to prefix log message. */
    private String logPrefix;

    /**
     * Set the predicate we used to do AttributeValueFiltering.
     * 
     * @param newPredicate the predicate.
     */
    protected void setValuePredicate(@Nonnull Predicate<AttributeValue> newPredicate) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        valuePredicate = Constraint.isNotNull(newPredicate, "Value Predicate should not be Null");
    }

    /**
     * Set the predicate we used to do AttributeValueFiltering.
     * 
     * @param newPredicate the predicate.
     */
    protected void setPolicyPredicate(@Nonnull Predicate<AttributeFilterContext> newPredicate) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        policyPredicate = Constraint.isNotNull(newPredicate, "Value Predicate should not be Null");
    }

    /** {@inheritDoc} */
    public void setId(String id) {
        super.setId(id);
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        // Id is definitive, reset prefix
        logPrefix = null;

        if (null == valuePredicate && null == policyPredicate) {
            throw new ComponentInitializationException(getLogPrefix()
                    + "A ValuePredicate or PolicyPredicate should be present");
        }
        if (null != valuePredicate && null != policyPredicate) {
            throw new ComponentInitializationException(getLogPrefix()
                    + "A ValuePredicate or PolicyPredicate should be present, not both");
        }
    }

    /** {@inheritDoc} */
    public Set<AttributeValue> getMatchingValues(@Nonnull Attribute attribute,
            @Nonnull AttributeFilterContext filterContext) throws AttributeFilteringException {

        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        Constraint.isNotNull(attribute, "Attribute to be filtered can not be null");
        Constraint.isNotNull(filterContext, "Attribute filter context can not be null");

        if (null == valuePredicate) {
            //
            // This is a "PolicyRule" rule. So the rule is, if we are true then everything,
            // else nothing.
            //
            log.info("{} No value predicate present for attribute '{}'," + " applying the policy predicate",
                    getLogPrefix(), attribute.getId());
            if (policyPredicate.apply(filterContext)) {
                return attribute.getValues();
            } else {
                return Collections.EMPTY_SET;
            }
        }

        HashSet matchedValues = new HashSet();

        log.debug("{} Applying value predicate to all values of Attribute '{}'", getLogPrefix(), attribute.getId());

        for (AttributeValue value : attribute.getValues()) {
            if (valuePredicate.apply(value)) {
                matchedValues.add(value);
            }
        }

        return matchedValues;
    }

    /** {@inheritDoc} */
    public boolean evaluatePolicyRule(@Nonnull AttributeFilterContext context) throws AttributeFilteringException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        Constraint.isNotNull(context, "Attribute filter context can not be null");
        
        if (null != policyPredicate) {
            log.debug("{} Applying policy Predicate", getLogPrefix());
            return policyPredicate.apply(context);
        }
        
        log.info("{} Applying value predicate supplied as policy to all values of all attributes", getLogPrefix());

        for (Attribute attribute : context.getPrefilteredAttributes().values()) {
            for (AttributeValue value : attribute.getValues()) {
                if (valuePredicate.apply(value)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * return a string which is to be prepended to all log messages.
     * 
     * @return "Attribute Filter '<filterID>' :"
     */
    protected String getLogPrefix() {
        // local cache of cached entry to allow unsynchronised clearing.
        String prefix = logPrefix;
        if (null == prefix) {
            StringBuilder builder = new StringBuilder("Attribute Filter '").append(getId()).append("':");
            prefix = builder.toString();
            if (null == logPrefix) {
                logPrefix = prefix;
            }
        }
        return prefix;
    }
}