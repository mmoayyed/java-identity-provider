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

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;
import net.shibboleth.utilities.java.support.component.DestructableComponent;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;
import net.shibboleth.utilities.java.support.logic.Assert;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Represents a value filtering rule for a particular attribute. */
@ThreadSafe
public class AttributeValueFilterPolicy extends AbstractInitializableComponent implements DestructableComponent,
        ValidatableComponent, UnmodifiableComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeValueFilterPolicy.class);

    /** Whether this policy has been destroyed. */
    private boolean isDestroyed;

    /** Unique ID of the attribute this rule applies to. */
    private String attributeId;

    /**
     * Whether this attribute rule will treat values that its {@link AttributeValueMatcher} as values that are permitted
     * or denied. Default value: true
     */
    private boolean matchingPermittedValues = true;

    /** 
     * Filter that permits the release of attribute values. Default value: {@link AttributeValueMatcher#MATCHES_NONE} 
     */
    private AttributeValueMatcher valueMatchingRule;
    
    /** Constructor. */
    public AttributeValueFilterPolicy(){
        matchingPermittedValues = true;
        valueMatchingRule = AttributeValueMatcher.MATCHES_NONE;
    }

    /** {@inheritDoc} */
    public boolean isDestroyed() {
        return isDestroyed;
    }

    /** {@inheritDoc} */
    public synchronized void destroy() {
        ComponentSupport.destroy(valueMatchingRule);
    }

    /**
     * Gets the ID of the attribute to which this rule applies.
     * 
     * @return ID of the attribute to which this rule applies
     */
    @Nonnull public String getAttributeId() {
        return attributeId;
    }

    /**
     * Sets the ID of the attribute to which this rule applies.
     * 
     * This property may not be changed after this component has been initialized.
     * 
     * @param id ID of the attribute to which this rule applies
     */
    public synchronized void setAttributeId(@Nonnull @NotEmpty String id) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Value filter policy for attribute " + getAttributeId()
                    + " has already been initialized, its attribute ID can not be changed.");
        }

        if (isDestroyed()) {
            throw new DestroyedComponentException(this);
        }

        attributeId = Assert.isNotNull(StringSupport.trimOrNull(id), "Attribute ID can not be null or empty");
    }

    /**
     * Gets whether this attribute rule will treat values that its {@link AttributeValueMatcher} as values that are
     * permitted or denied.
     * 
     * @return true if matching attribute rules are permitted values, false if they are not
     */
    public boolean isMatchingPermittedValues() {
        return matchingPermittedValues;
    }

    /**
     * Sets whether this attribute rule will treat values that its {@link AttributeValueMatcher} as values that are
     * permitted or denied.
     * 
     * @param isMatchingPermittedValues whether this attribute rule will treat values that its
     *            {@link AttributeValueMatcher} as values that are permitted or denied
     */
    public synchronized void setMathingPermittedValues(boolean isMatchingPermittedValues) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Value filter policy for attribute " + getAttributeId()
                    + " has already been initialized, matching of permitted values can not be changed.");
        }

        if (isDestroyed()) {
            throw new DestroyedComponentException(this);
        }

        matchingPermittedValues = isMatchingPermittedValues;
    }

    /**
     * Gets the matcher used to determine attribute values filtered by this rule.
     * 
     * @return matcher used to determine attribute values filtered by this rule
     */
    @Nonnull public AttributeValueMatcher getValueMatcher() {
        return valueMatchingRule;
    }

    /**
     * Sets the matcher used to determine attribute values filtered by this rule.
     * 
     * @param matcher matcher used to determine attribute values filtered by this rule
     */
    public synchronized void setValueMatcher(@Nonnull AttributeValueMatcher matcher) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Value filter policy for attribute " + getAttributeId()
                    + " has already been initialized, its value matcher can not be changed.");
        }

        if (isDestroyed()) {
            throw new DestroyedComponentException(this);
        }

        valueMatchingRule = Assert.isNull(matcher, "Attribute value matcher can not be null");
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
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
    public void apply(@Nonnull final Attribute<?> attribute, @Nonnull final AttributeFilterContext filterContext)
            throws AttributeFilteringException {
        assert attribute != null : "To-be-filtered attribute can not be null";
        assert filterContext != null : "Attribute filter context can not be null";

        if (!isInitialized()) {
            throw new AttributeFilteringException("Value filter policy for attribute " + getAttributeId()
                    + " has not be initialized and can not yet be used");
        }

        if (isDestroyed()) {
            throw new DestroyedComponentException(this);
        }

        log.debug("Filtering values for attribute '{}' which currently contains {} values", getAttributeId(), attribute
                .getValues().size());

        Collection<?> matchingValues = valueMatchingRule.getMatchingValues(attribute, filterContext);
        if (matchingPermittedValues) {
            log.debug("Filter has permitted the release of {} values for attribute '{}'", matchingValues.size(),
                    getAttributeId());
            filterContext.addPermittedAttributeValues(getAttributeId(), matchingValues);
        } else {
            log.debug("Filter has denied the release of {} values for attribute '{}'", matchingValues.size(),
                    getAttributeId());
            filterContext.addDeniedAttributeValues(getAttributeId(), matchingValues);
        }
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (attributeId == null) {
            throw new ComponentInitializationException("No attribute specified for this attribute value filter policy");
        }

        if (valueMatchingRule == null) {
            throw new ComponentInitializationException("No value matching rule specified");
        }

        ComponentSupport.initialize(valueMatchingRule);
    }
}