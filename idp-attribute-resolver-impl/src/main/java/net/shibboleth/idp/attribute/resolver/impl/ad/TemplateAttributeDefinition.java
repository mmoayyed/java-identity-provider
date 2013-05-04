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

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.UnsupportedAttributeTypeException;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
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
    
    /** Template (as Text) to be evaluated. */
    private String templateText;

    /** VelocityEngine. */
    private VelocityEngine engine;

    /** The names of the attributes we need. */
    private List<String> sourceAttributes = Collections.EMPTY_LIST;

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
    public void setSourceAttributes(@Nonnull @NullableElements List<String> newSourceAttributes) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        ArrayList<String> checkedSourceAttrs = new ArrayList<String>(newSourceAttributes.size());
        CollectionSupport.addIf(checkedSourceAttrs, newSourceAttributes, Predicates.notNull());
        sourceAttributes = Collections.unmodifiableList(checkedSourceAttrs);
    }

    /**
     * Gets the template text to be evaluated.
     * 
     * @return the template
     */
    @Nullable @NonnullAfterInit public Template getTemplate() {
        return template;
    }

    /**
     * Gets the template text to be evaluated.
     * 
     * @return the template
     */
    @Nullable @NonnullAfterInit public String getTemplateText() {
        return templateText;
    }

    /**
     * Sets the template to be evaluated.
     * 
     * @param velocityTemplate template to be evaluated
     */
    public synchronized void setTemplateText(@Nullable String velocityTemplate) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        templateText = velocityTemplate;
    }
    
    /**
     * Gets the {@link VelocityEngine} to be used.
     * 
     * @return the template
     */
    @Nullable @NonnullAfterInit public VelocityEngine getVelocityEngine() {
        return engine;
    }

    /**
     * Sets the {@link VelocityEngine} to be used.
     * 
     * @param velocityEngine engine to be used
     */
    public synchronized void setVelocityEngine(VelocityEngine velocityEngine) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        engine = velocityEngine;
    }

    /** {@inheritDoc} */
    @Nonnull protected Attribute doAttributeDefinitionResolve(final AttributeResolutionContext resolutionContext)
            throws ResolutionException {

        final Attribute resultantAttribute = new Attribute(getId());

        final Map<String, Iterator<AttributeValue>> sourceValues = new LazyMap<String, Iterator<AttributeValue>>();
        final int valueCount = setupSourceValues(resolutionContext, sourceValues);

        // build velocity context
        VelocityContext velocityContext;
        String templateResult;

        for (int i = 0; i < valueCount; i++) {
            log.debug("Attribute definition '{}': determing value {}", i + 1);
            velocityContext = new VelocityContext();

            for (String attributeId : sourceValues.keySet()) {
                final AttributeValue value = sourceValues.get(attributeId).next();
                if (!(value instanceof StringAttributeValue)) {
                    throw new ResolutionException(new UnsupportedAttributeTypeException(
                            "This attribute definition only supports attribute value types of "
                                    + StringAttributeValue.class.getName() + " not values of type "
                                    + value.getClass().getName()));
                }

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
                // TODO (rdw) uncovered path
                log.error("Attribute definition '" + getId() + "': unable to evaluate velocity template", e);
                throw new ResolutionException("Unable to evaluate template", e);
            }
        }

        return resultantAttribute;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (getDependencies().isEmpty()) {
            throw new ComponentInitializationException("Attribute definition '" + getId()
                    + "': no dependencies were configured");
        }

        if (null == engine) {
            throw new ComponentInitializationException("Attribute definition '" + getId()
                    + "': no velocity engine was configured" );
        }
        
        if (sourceAttributes.isEmpty()) {
            log.info("Attribute Definition '{}': No Source Attributes supplied, was this intended?");
        }

        templateText = StringSupport.trimOrNull(templateText);

        if (null == templateText) {
            StringBuffer defaultTemplate = new StringBuffer();
            for (String id : sourceAttributes) {
                defaultTemplate.append("${").append(id).append("} ");
            }
            if (defaultTemplate.length() > 0) {
                templateText = defaultTemplate.toString();
            } else {
                throw new ComponentInitializationException("Attribute definition '" + getId()
                        + "': no template and no source attributes were configured");                
            }
            log.info("Attribute definition '{}' no template supplied default generated '{}'", getId(), templateText);
        }
        
        template = Template.fromTemplate(engine, templateText);
        
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
     * @throws ResolutionException if there is a mismatched count of attributes
     */
    private int setupSourceValues(final AttributeResolutionContext resolutionContext,
            Map<String, Iterator<AttributeValue>> sourceValues) throws ResolutionException {

        final Map<String, Set<AttributeValue>> dependencyAttributes =
                PluginDependencySupport.getAllAttributeValues(resolutionContext, getDependencies());

        int valueCount = 0;
        boolean valueCountSet = false;

        for (String attributeName : sourceAttributes) {

            final Set<AttributeValue> attributeValues = dependencyAttributes.get(attributeName);
            if (null == attributeValues) {
                throw new ResolutionException("Attribute Resolution '" + getId()
                        + "' : no values found for attribute named '" + attributeName + "'");
            }

            if (!valueCountSet) {
                valueCount = attributeValues.size();
                valueCountSet = true;
            } else if (attributeValues.size() != valueCount) {
                final String msg =
                        "All attributes used in TemplateAttributeDefinition " + getId()
                                + " must have the same number of values.";
                log.error(msg);
                throw new ResolutionException(msg);
            }

            sourceValues.put(attributeName, attributeValues.iterator());
        }

        return valueCount;
    }
}