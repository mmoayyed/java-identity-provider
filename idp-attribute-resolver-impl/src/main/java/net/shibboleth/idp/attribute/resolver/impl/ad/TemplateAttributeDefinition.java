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

package net.shibboleth.idp.attribute.resolver.impl.ad;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.UnsupportedAttributeTypeException;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;
import net.shibboleth.utilities.java.support.collection.LazyMap;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.velocity.Template;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.VelocityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * An attribute definition that constructs its values based on the values of its dependencies using the Velocity
 * Template Language. Dependencies may have multiple values, however multiples dependencies must have the same number of
 * values. In the case of multi-valued dependencies, the template will be evaluated multiples times, iterating over each
 * dependency.
 * 
 * The template is inserted into the engine with a unique name derived from this class and from the id supplied for this
 * attribute.
 * 
 * This is marked not thread safe since the constructor cannot do a safe check & insert of the template into the
 * repository.
 */
@NotThreadSafe
public class TemplateAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(TemplateAttributeDefinition.class);

    /** Template to be evaluated. */
    private Template template;

    /**
     * Gets the template to be evaluated.
     * 
     * @return the template
     */
    @Nullable public Template getTemplate() {
        return template;
    }

    /**
     * Sets the template to be evaluated.
     * 
     * @param velocityTemplate template to be evaluated
     */
    public synchronized void setTemplate(@Nonnull Template velocityTemplate) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        template = Constraint.isNotNull(velocityTemplate, "Template can not be null");
    }

    /** {@inheritDoc} */
    protected Optional<Attribute> doAttributeDefinitionResolve(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {

        final Attribute resultantAttribute = new Attribute(getId());

        final Map<String, Iterator<AttributeValue>> sourceValues = new LazyMap<String, Iterator<AttributeValue>>();
        final int valueCount = countAndSetupSourceValues(resolutionContext, sourceValues);

        // build velocity context
        VelocityContext velocityContext;
        String templateResult;

        for (int i = 0; i < valueCount; i++) {
            log.debug("Attribute definition '{}': determing value {}", i + 1);
            velocityContext = new VelocityContext();

            for (String attributeId : sourceValues.keySet()) {
                final AttributeValue value = sourceValues.get(attributeId).next();
                log.debug("Attribute definition '{}': adding value '{}' for attribute '{}' to the template context",
                        new Object[] {getId(), value.toString(), attributeId,});
                velocityContext.put(attributeId, value.getValue());
            }

            try {
                log.debug("Attribute definition '{}': evaluating template", getId());
                templateResult = template.merge(velocityContext);
                log.debug("Attribute definition '{}': result of template evaluating was '{}'", getId(), templateResult);
                resultantAttribute.getValues().add(new StringAttributeValue(templateResult));
            } catch (VelocityException e) {
                log.error("Attribute definition '" + getId() + "': unable to evaluate velocity template", e);
                throw new AttributeResolutionException("Unable to evaluate template", e);
            }
        }

        return Optional.of(resultantAttribute);
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (getDependencies().isEmpty()) {
            throw new ComponentInitializationException("Attribute definition '" + getId()
                    + "': no dependencies were configured");
        }

        if (null == template) {
            throw new ComponentInitializationException("Attribute definition '" + getId()
                    + "': no template was configured");
        }
    }

    /**
     * Set up a map which can be used to populate the template. The key is the attribute name and the value is the
     * iterator to give all the names. We also return how deep the iteration will be and throw an exception if there is
     * a mismatch in number of elements in any attribute.
     * 
     * Finally, the names of the source attributes is checked against the dependency attributes and if there is a
     * mismatch then a warning is emitted.
     * 
     * @param resolutionContext to look for dependencies in.
     * @param sourceValues to populate with the attribute iterators
     * @return how many values in the attributes
     * @throws AttributeResolutionException if there is a mismatched count of attributes
     */
    private int countAndSetupSourceValues(final AttributeResolutionContext resolutionContext,
            Map<String, Iterator<AttributeValue>> sourceValues) throws AttributeResolutionException {

        final Map<String, Set<AttributeValue>> dependencyAttributes =
                PluginDependencySupport.getAllAttributeValues(resolutionContext, getDependencies());

        int valueCount = 0;
        boolean valueCountSet = false;

        HashSet<String> attributeValues;
        for (Entry<String, Set<AttributeValue>> dependencyAttribute : dependencyAttributes.entrySet()) {
            attributeValues = new HashSet<String>();
            for (AttributeValue value : dependencyAttribute.getValue()) {
                if (value instanceof StringAttributeValue) {
                    attributeValues.add((String) value.getValue());
                } else {
                    throw new AttributeResolutionException(new UnsupportedAttributeTypeException(
                            "This attribute definition only supports attribute value types of "
                                    + StringAttributeValue.class.getName() + " not values of type "
                                    + value.getClass().getName()));
                }
            }
            
            if (!valueCountSet) {
                valueCount = attributeValues.size();
                valueCountSet = true;
            } else if (attributeValues.size() != valueCount) {
                final String msg =
                        "All attributes used in TemplateAttributeDefinition " + getId()
                                + " must have the same number of values.";
                log.error(msg);
                throw new AttributeResolutionException(msg);
            }

            sourceValues.put(dependencyAttribute.getKey(), dependencyAttribute.getValue().iterator());
        }

        return valueCount;
    }
}