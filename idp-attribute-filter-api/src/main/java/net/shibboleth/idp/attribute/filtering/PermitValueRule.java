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

package net.shibboleth.idp.attribute.filtering;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.utilities.java.support.component.AbstractDestructableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Deny value filtering rule.<br/>
 * <code>
    <element name="PermitValueRule" type="afp:MatchFunctorType">
        <annotation>
            <documentation>
                A filter for attribute values. If the filter evaluates to true the value is permitted to be released.
            &lt;/documentation>
        &lt;/annotation>
    &lt;/element>
 </code>
 * */
@ThreadSafe
public class PermitValueRule extends AbstractDestructableInitializableComponent implements ValidatableComponent,
        UnmodifiableComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(PermitValueRule.class);

    /**
     * Filter that permits the release of attribute values. Default value: {@link MatchFunctor#MATCHES_NONE}
     */
    private MatchFunctor valueMatchingRule;

    /** Constructor. */
    public PermitValueRule() {
        valueMatchingRule = MatchFunctor.MATCHES_NONE;
    }

    /**
     * Gets the matcher used to determine attribute values filtered by this rule.
     * 
     * @return matcher used to determine attribute values filtered by this rule
     */
    @Nonnull public MatchFunctor getValueMatcher() {
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        return valueMatchingRule;
    }

    /**
     * Sets the matcher used to determine attribute values filtered by this rule.
     * 
     * @param matcher matcher used to determine attribute values filtered by this rule
     */
    public synchronized void setValueMatcher(@Nonnull MatchFunctor matcher) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        valueMatchingRule = Constraint.isNotNull(matcher, "Attribute value matcher can not be null");
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        ComponentSupport.validate(valueMatchingRule);
    }

    /**
     * Applies this rule to the respective attribute in the filter context.
     * 
     * @param attribute attribute whose values will be filtered by this policy
     * @param filterContext current filter context
     * 
     * @throws AttributeFilteringException thrown if there is a problem applying this rule to the current filter context
     */
    public void apply(@Nonnull final Attribute attribute, @Nonnull final AttributeFilterContext filterContext)
            throws AttributeFilteringException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        Constraint.isNotNull(attribute, "To-be-filtered attribute can not be null");
        Constraint.isNotNull(filterContext, "Attribute filter context can not be null");

        log.debug("Filtering values for attribute '{}' which currently contains {} values", attribute.getId(),
                attribute.getValues().size());

        Collection matchingValues = valueMatchingRule.getMatchingValues(attribute, filterContext);
        log.debug("Filter has permitted the release of {} values for attribute '{}'", matchingValues.size(),
                attribute.getId());
        filterContext.addPermittedAttributeValues(attribute.getId(), matchingValues);
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        ComponentSupport.destroy(valueMatchingRule);
        super.doDestroy();
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (valueMatchingRule == null) {
            // Will never happen unless we change the constructor
            throw new ComponentInitializationException("No value matching rule specified");
        }

        ComponentSupport.initialize(valueMatchingRule);
    }
}