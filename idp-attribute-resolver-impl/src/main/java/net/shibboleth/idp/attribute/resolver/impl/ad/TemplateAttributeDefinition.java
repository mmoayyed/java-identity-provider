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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

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
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.CollectionSupport;
import net.shibboleth.utilities.java.support.collection.LazyMap;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.velocity.Template;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;

/**
 * An attribute definition that constructs its values based on the values of its dependencies using the Velocity
 * Template Language. Dependencies may have multiple values, however multiples dependencies must have the same number of
 * values. In the case of multi-valued dependencies, the template will be evaluated multiples times, iterating over each
 * dependency. <br/>
 * The template is inserted into the engine with a unique name derived from this class and from the id supplied for this
 * attribute. <br/>
 * This is marked not thread safe since the constructor cannot do a safe check & insert of the template into the
 * repository.
 */
@NotThreadSafe
public class TemplateAttributeDefinition extends AbstractAttributeDefinition {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(TemplateAttributeDefinition.class);

    /** Template to be evaluated. */
    @NonnullAfterInit private Template template;

    /** Template (as Text) to be evaluated. */
    @NonnullAfterInit private String templateText;

    /** VelocityEngine. */
    @NonnullAfterInit private VelocityEngine engine;

    /** The names of the attributes we need. */
    @Nonnull private List<String> sourceAttributes = Collections.emptyList();

    /**
     * Get the source attribute IDs.
     * 
     * @return the source attribute IDs
     */
    @Nonnull @Unmodifiable @NonnullElements public List<String> getSourceAttributes() {
        return Collections.unmodifiableList(sourceAttributes);
    }

    /**
     * Set the source attribute IDs.
     * 
     * @param newSourceAttributes the source attribute IDs
     */
    public void setSourceAttributes(@Nonnull @NullableElements final List<String> newSourceAttributes) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        final ArrayList<String> checkedSourceAttrs = new ArrayList<String>(newSourceAttributes.size());
        CollectionSupport.addIf(checkedSourceAttrs, newSourceAttributes, Predicates.notNull());
        sourceAttributes = Collections.unmodifiableList(checkedSourceAttrs);
    }

    /**
     * Gets the template text to be evaluated.
     * 
     * @return the template
     */
    @NonnullAfterInit public Template getTemplate() {
        return template;
    }

    /**
     * Gets the template text to be evaluated.
     * 
     * @return the template
     */
    @NonnullAfterInit public String getTemplateText() {
        return templateText;
    }

    /**
     * Sets the template to be evaluated.
     * 
     * @param velocityTemplate template to be evaluated
     */
    public synchronized void setTemplateText(@Nullable final String velocityTemplate) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        templateText = velocityTemplate;
    }

    /**
     * Gets the {@link VelocityEngine} to be used.
     * 
     * @return the template
     */
    @NonnullAfterInit public VelocityEngine getVelocityEngine() {
        return engine;
    }

    /**
     * Sets the {@link VelocityEngine} to be used.
     * 
     * @param velocityEngine engine to be used
     */
    public synchronized void setVelocityEngine(@Nullable final VelocityEngine velocityEngine) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        engine = velocityEngine;
    }

    /** {@inheritDoc} */
    @Override @Nonnull protected IdPAttribute doAttributeDefinitionResolve(
            @Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException {

        final IdPAttribute resultantAttribute = new IdPAttribute(getId());

        final Map<String, Iterator<IdPAttributeValue<?>>> sourceValues = new LazyMap<>();
        final int valueCount = setupSourceValues(workContext, sourceValues);

        // build velocity context
        VelocityContext velocityContext;
        String templateResult;

        for (int i = 0; i < valueCount; i++) {
            log.debug("{} determing value {}", getLogPrefix(), i + 1);
            velocityContext = new VelocityContext();

            for (final String attributeId : sourceValues.keySet()) {
                final IdPAttributeValue value = sourceValues.get(attributeId).next();
                if (!(value instanceof StringAttributeValue)) {
                    throw new ResolutionException(new UnsupportedAttributeTypeException(getLogPrefix()
                            + "This attribute definition only supports attribute value types of "
                            + StringAttributeValue.class.getName() + " not values of type "
                            + value.getClass().getName()));
                }

                log.debug("{} adding value '{}' for attribute '{}' to the template context", new Object[] {
                        getLogPrefix(), value.getValue(), attributeId,});
                velocityContext.put(attributeId, value.getValue());
            }

            try {
                log.debug("{} evaluating template", getLogPrefix());
                templateResult = template.merge(velocityContext);
                log.debug("{} result of template evaluating was '{}'", getLogPrefix(), templateResult);
                resultantAttribute.getValues().add(new StringAttributeValue(templateResult));
            } catch (final VelocityException e) {
                // uncovered path
                log.error(getLogPrefix() + " unable to evaluate velocity template", e);
                throw new ResolutionException("Unable to evaluate template", e);
            }
        }

        return resultantAttribute;
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (getDependencies().isEmpty()) {
            throw new ComponentInitializationException(getLogPrefix() + " no dependencies were configured");
        }

        if (null == engine) {
            throw new ComponentInitializationException(getLogPrefix() + " no velocity engine was configured");
        }

        if (sourceAttributes.isEmpty()) {
            log.info("{} no Source Attributes supplied, was this intended?", getLogPrefix());
        }

        templateText = StringSupport.trimOrNull(templateText);

        if (null == templateText) {
            // V2 compatibility - define our own template
            final StringBuffer defaultTemplate = new StringBuffer();
            for (final String id : sourceAttributes) {
                defaultTemplate.append("${").append(id).append("} ");
            }
            if (defaultTemplate.length() > 0) {
                templateText = defaultTemplate.toString();
            } else {
                throw new ComponentInitializationException(getLogPrefix()
                        + " no template and no source attributes were configured");
            }
            log.info("{} no template supplied. Default generated was '{}'", getLogPrefix(), templateText);
        }

        template = Template.fromTemplate(engine, templateText);
    }

    /**
     * Set up a map which can be used to populate the template. The key is the attribute name and the value is the
     * iterator to give all the names. We also return how deep the iteration will be and throw an exception if there is
     * a mismatch in number of elements in any attribute. <br/>
     * Finally, the names of the source attributes is checked against the dependency attributes and if there is a
     * mismatch then a warning is emitted.
     * 
     * @param workContext source for dependencies
     * @param sourceValues to populate with the attribute iterators
     * 
     * @return how many values in the attributes
     * @throws ResolutionException if there is a mismatched count of attributes
     */
    private int setupSourceValues(@Nonnull final AttributeResolverWorkContext workContext,
            final Map<String, Iterator<IdPAttributeValue<?>>> sourceValues) throws ResolutionException {

        final Map<String, Set<IdPAttributeValue<?>>> dependencyAttributes =
                PluginDependencySupport.getAllAttributeValues(workContext, getDependencies());

        int valueCount = 0;
        boolean valueCountSet = false;

        for (final String attributeName : sourceAttributes) {

            final Set<IdPAttributeValue<?>> attributeValues = dependencyAttributes.get(attributeName);
            if (null == attributeValues) {
                throw new ResolutionException(getLogPrefix() + " no values found for attribute named '" + attributeName
                        + "'");
            }

            if (!valueCountSet) {
                valueCount = attributeValues.size();
                valueCountSet = true;
            } else if (attributeValues.size() != valueCount) {
                final String msg =
                        getLogPrefix() + " all attributes used in"
                                + " TemplateAttributeDefinition must have the same number of values.";
                log.error(msg);
                throw new ResolutionException(msg);
            }

            sourceValues.put(attributeName, attributeValues.iterator());
        }

        return valueCount;
    }
    
}