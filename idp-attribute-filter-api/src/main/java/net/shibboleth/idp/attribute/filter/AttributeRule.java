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

package net.shibboleth.idp.attribute.filter;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractDestructableIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a value filtering rule for a particular attribute. <code>
     <element name="AttributeRule" type="afp:AttributeRuleType">
        <annotation>
            <documentation>A rule that describes how values of an attribute will be filtered.&lt;/documentation>
        &lt;/annotation>
    &lt;/element>
 </code>
 */
@ThreadSafe
public class AttributeRule extends AbstractDestructableIdentifiableInitializableComponent implements
        ValidatableComponent, UnmodifiableComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeRule.class);

    /**
     * Unique ID of the attribute this rule applies to. <code>
        <attribute name="attributeID" type="string" use="required">
            <annotation>
                <documentation>The ID of the attribute to which this rule applies.&lt;/documentation>
            &lt;/annotation>
        &lt;/attribute>
      </code>
     */
    private String attributeId;

    /**
     * Filter that permits the release of attribute values.
     */
    private Matcher permitValueRule;

    /**
     * Filter that denies the release of attribute values.
     */
    private Matcher denyValueRule;

    /** {@inheritDoc} */
    public synchronized void setId(@Nonnull @NotEmpty final String componentId) {
        super.setId(componentId);
    }

    /**
     * Gets the ID of the attribute to which this rule applies.
     * 
     * @return ID of the attribute to which this rule applies
     */
    @NonnullAfterInit public String getAttributeId() {
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
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
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        attributeId = StringSupport.trimOrNull(id);
    }

    /**
     * Gets the matcher used to determine permitted attribute values filtered by this rule.
     * 
     * @return matcher used to determine permitted attribute values filtered by this rule
     */
    @Nullable public Matcher getPermitRule() {
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        return permitValueRule;
    }

    /**
     * Sets the rule used to determine permitted attribute values filtered by this rule.
     * 
     * @param matcher matcher used to determine permitted attribute values filtered by this rule
     */
    public synchronized void setPermitRule(@Nonnull Matcher matcher) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        permitValueRule = Constraint.isNotNull(matcher, "Permit Rule can not be null");
    }

    /**
     * Gets the matcher used to determine denied attribute values filtered by this rule.
     * 
     * @return matcher used to determine denied attribute values filtered by this rule
     */
    @Nullable public Matcher getDenyRule() {
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        return denyValueRule;
    }

    /**
     * Sets the rule used to determine denied attribute values filtered by this rule.
     * 
     * @param matcher matcher used to determine denied attribute values filtered by this rule
     */
    public synchronized void setDenyRule(@Nonnull Matcher matcher) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        denyValueRule = Constraint.isNotNull(matcher, "Deny Rule can not be null");
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        ComponentSupport.validate(denyValueRule);
        ComponentSupport.validate(permitValueRule);
    }

    /**
     * Applies this rule to the respective attribute in the filter context.
     * 
     * @param attribute attribute whose values will be filtered by this policy
     * @param filterContext current filter context
     * 
     * @throws AttributeFilterException thrown if there is a problem applying this rule to the current filter context
     */
    public void apply(@Nonnull final Attribute attribute, @Nonnull final AttributeFilterContext filterContext)
            throws AttributeFilterException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        Constraint.isNotNull(attribute, "To-be-filtered attribute can not be null");
        Constraint.isNotNull(filterContext, "Attribute filter context can not be null");

        log.debug("Filtering values for attribute '{}' which currently contains {} values", getAttributeId(), attribute
                .getValues().size());

        if (permitValueRule != null) {
            final Set<AttributeValue> matchingValues = permitValueRule.getMatchingValues(attribute, filterContext);
            
            if (null == matchingValues) {
                log.warn("Filter failed.  Not attributes released for attribute '{}'", getAttributeId());
            } else {
                log.debug("Filter has permitted the release of {} values for attribute '{}'", matchingValues.size(),
                        attribute.getId());
                filterContext.addPermittedAttributeValues(attribute.getId(), matchingValues);
            }
        }
        if (denyValueRule != null) {
            final Set<AttributeValue> matchingValues = denyValueRule.getMatchingValues(attribute, filterContext);
            
            if (null == matchingValues) {
                log.warn("Filter failed.  all attributed denied for attribute '{}'", getAttributeId());
                filterContext.addDeniedAttributeValues(attribute.getId(), attribute.getValues());
            } else {
                log.debug("Filter has denied the release of {} values for attribute '{}'", matchingValues.size(),
                    attribute.getId());
                filterContext.addDeniedAttributeValues(attribute.getId(), matchingValues);
            }
        }
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        ComponentSupport.destroy(permitValueRule);
        ComponentSupport.destroy(denyValueRule);
        super.doDestroy();
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == getAttributeId()) {
            throw new ComponentInitializationException("Attribute Rule '" + getId()
                    + "': No attribute specified for this attribute value filter policy");
        }

        if (permitValueRule == null && denyValueRule == null) {
            throw new ComponentInitializationException("Attribute Rule '" + getId()
                    + "': must have a permit rule or a deny rule");
        }
        if (permitValueRule != null && denyValueRule != null) {
            throw new ComponentInitializationException("Attribute Rule '" + getId()
                    + "': must have a permit rule or a deny rule, but not both");
        }

        ComponentSupport.initialize(permitValueRule);
        ComponentSupport.initialize(denyValueRule);
    }
}