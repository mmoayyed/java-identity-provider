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

package net.shibboleth.idp.attribute.resolver.ad.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.UnsupportedAttributeTypeException;
import net.shibboleth.idp.attribute.resolver.AbstractAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.ThreadSafeAfterInit;
import net.shibboleth.utilities.java.support.collection.LazyMap;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.velocity.Template;

/**
 * An attribute definition that constructs its values based on the values of its dependencies using the Velocity
 * Template Language. Dependencies may have multiple values, however multiple dependencies must have the same number of
 * values. In the case of multi-valued dependencies, the template will be evaluated multiples times, iterating over each
 * dependency.
 * 
 * <p>The template is inserted into the engine with a unique name derived from this class and from the id supplied for
 * this attribute.</p>
 */
@ThreadSafeAfterInit
public class TemplateAttributeDefinition extends AbstractAttributeDefinition {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(TemplateAttributeDefinition.class);

    /** Template to be evaluated. */
    @NonnullAfterInit private Template template;

    /** Template (as Text) to be evaluated. */
    @NonnullAfterInit private String templateText;

    /** VelocityEngine. */
    @NonnullAfterInit private VelocityEngine engine;

    /**
     * Get the template text to be evaluated.
     * 
     * @return the template
     */
    @NonnullAfterInit public Template getTemplate() {
        return template;
    }

    /**
     * Get the template text to be evaluated.
     * 
     * @return the template
     */
    @NonnullAfterInit public String getTemplateText() {
        return templateText;
    }

    /**
     * Set the literal text of the template to be evaluated.
     * 
     * @param velocityTemplate template to be evaluated
     */
    public void setTemplateText(@Nullable final String velocityTemplate) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        templateText = StringSupport.trimOrNull(velocityTemplate);
    }

    /**
     * Get the {@link VelocityEngine} to be used.
     * 
     * @return the template
     */
    @NonnullAfterInit public VelocityEngine getVelocityEngine() {
        return engine;
    }

    /**
     * Set the {@link VelocityEngine} to be used.
     * 
     * @param velocityEngine engine to be used
     */
    public void setVelocityEngine(@Nonnull final VelocityEngine velocityEngine) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        engine = Constraint.isNotNull(velocityEngine, "VelocityEngine cannot be null");
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
    
        if (getAttributeDependencies().isEmpty() && getDataConnectorDependencies().isEmpty()) {
            throw new ComponentInitializationException(getLogPrefix() + " no dependencies were configured");
        }
    
        if (null == engine) {
            throw new ComponentInitializationException(getLogPrefix() + " no velocity engine was configured");
        }
    
        if (null == templateText) {
            throw new ComponentInitializationException(getLogPrefix() + " no template provided");
        }
    
        template = Template.fromTemplate(engine, templateText);
    }

    /** {@inheritDoc} */
    @Override @Nonnull protected IdPAttribute doAttributeDefinitionResolve(
            @Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException {

        final IdPAttribute resultantAttribute = new IdPAttribute(getId());

        final Map<String,Iterator<IdPAttributeValue>> sourceValues = new LazyMap<>();
        final int valueCount = setupSourceValues(workContext, sourceValues);

        final List<IdPAttributeValue> valueList = new ArrayList<>(valueCount);

        for (int i = 0; i < valueCount; i++) {
            log.debug("{} Determining value {}", getLogPrefix(), i + 1);
            final VelocityContext velocityContext = new VelocityContext();

            // Build velocity context.
            for (final String attributeId : sourceValues.keySet()) {
                final IdPAttributeValue value = sourceValues.get(attributeId).next();
                final Object velocityValue;
                if (value instanceof EmptyAttributeValue) {
                    switch (((EmptyAttributeValue) value).getValue()) {
                        case NULL_VALUE:
                            velocityValue = null;
                            break;
                        case ZERO_LENGTH_VALUE:
                            velocityValue = "";
                            break;
                        default:
                            throw new ResolutionException(new UnsupportedAttributeTypeException(getLogPrefix()
                                    + "Unknown empty attribute value type " + value.getClass()));
                    }
                } else if (value instanceof StringAttributeValue) {
                    velocityValue = ((StringAttributeValue) value).getValue();
                } else {
                    log.debug("{} Adding non string Attribute value : {}", getLogPrefix(), value.getNativeValue());
                    velocityValue =value.getNativeValue(); 
                }
                log.debug("{} Adding value '{}' for attribute '{}' to the template context", new Object[] {
                        getLogPrefix(), velocityValue, attributeId,});
                velocityContext.put(attributeId, velocityValue);
            }

            // Evaluate the context.
            try {
                log.debug("{} Evaluating template", getLogPrefix());
                final String templateResult = template.merge(velocityContext);
                log.debug("{} Result of template evaluating was '{}'", getLogPrefix(), templateResult);
                valueList.add(StringAttributeValue.valueOf(templateResult));
            } catch (final VelocityException e) {
                // uncovered path
                log.error("{} Unable to evaluate velocity template: {}", getLogPrefix(), e.getMessage());
                throw new ResolutionException("Unable to evaluate template", e);
            }
        }
        
        resultantAttribute.setValues(valueList);
        return resultantAttribute;
    }
    
    /** Add values for a given attribute to the source Map. 
     *  Helper function for {@link #setupSourceValues(AttributeResolverWorkContext, Map)}
     * @param attributeName the attribute name under consideration
     * @param attributeValues The values to add.
     * @param sourceValues the Map to add to
     * @param curValueCount how many values to expect.  0 means never set
     * @return the number of values to expect, 0 means still not set.
     * @throws ResolutionException if there is a mismatched count of attributes
     */
    private int addAttributeValues(@Nonnull final String attributeName,
            @Nullable final List<IdPAttributeValue> attributeValues,  
            @Nonnull @NonnullElements final Map<String,Iterator<IdPAttributeValue>> sourceValues,
            final int curValueCount) throws ResolutionException {
        
        int valueCount = curValueCount;
        if (null == attributeValues || 0 == attributeValues.size()) {
            log.debug("{} Ignoring input attribute '{}' with no values", getLogPrefix(), attributeName);
            return valueCount;
        }

        if (valueCount <= 0) {
            valueCount = attributeValues.size();
        } else if (attributeValues.size() != valueCount) {
            final String msg = getLogPrefix() + " All source attributes used in"
                    + " TemplateAttributeDefinition must have the same number of values: '" + attributeName  + "'" ;
            log.error("{} {}", getLogPrefix(), msg);
            throw new ResolutionException(msg);
        }

        sourceValues.put(attributeName, attributeValues.iterator());
        return valueCount;
    }
    /**
     * Set up a map which can be used to populate the template. The key is the attribute name and the value is the
     * iterator to give all the names. We also return how deep the iteration will be and throw an exception if there is
     * a mismatch in number of elements in any attribute.
     * 
     * @param workContext source for dependencies
     * @param sourceValues to populate with the attribute iterators
     * 
     * @return how many values in the attributes
     * @throws ResolutionException if there is a mismatched count of attributes
     */
    private int setupSourceValues(@Nonnull final AttributeResolverWorkContext workContext,
            @Nonnull @NonnullElements final Map<String,Iterator<IdPAttributeValue>> sourceValues)
                    throws ResolutionException {

        final Map<String, List<IdPAttributeValue>> dependencyAttributes = 
                PluginDependencySupport.getAllAttributeValues(workContext,
                                                              getAttributeDependencies(), 
                                                              getDataConnectorDependencies());

        int valueCount = 0;

        for (final Entry<String, List<IdPAttributeValue>> entry : dependencyAttributes.entrySet() ) {
            valueCount = addAttributeValues(entry.getKey(), entry.getValue(), sourceValues, valueCount);
        }

        return valueCount;
    }
    
}
