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

import java.io.StringWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.NotThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.utilities.java.support.collection.LazyMap;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
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

    /** Velocity engine to use to render attribute values. */
    private VelocityEngine velocity;

    /** Name the attribute template is registered under within the template engine. */
    private String templateName;

    /** Template that produces the attribute value. */
    private String templateSource;

    /**
     * Set the engine to use.
     * 
     * @param engine velocity engine used to parse template.
     */
    public synchronized void setVelocityEngine(final VelocityEngine engine) {
        ifInitializedThrowUnmodifiabledComponentException(getId());
        ifDestroyedThrowDestroyedComponentException(getId());

        velocity = engine;
    }

    /**
     * Access the velocity engine being used.
     * 
     * @return the velocity engine. Not null after initialization.
     */
    public VelocityEngine getVelocityEngine() {
        return velocity;
    }

    /**
     * Access the name by which the template is used. This name is uniqueified by the implemeting class and the id of
     * the attribute resolver.
     * 
     * @return the template name. Only available after initialization.
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * Set the source to use.
     * 
     * @param template the template we use.
     */
    public synchronized void setTemplateSource(final String template) {
        ifInitializedThrowUnmodifiabledComponentException(getId());
        ifDestroyedThrowDestroyedComponentException(getId());

        templateSource = StringSupport.trimOrNull(template);
    }

    /**
     * Access the source used in this template.
     * 
     * @return the source used by this template. Not null after initialization.
     */
    public String getTemplateSource() {
        return templateSource;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        templateName = "shibboleth.resolver.ad." + this.getClass().getName() + getId();

        if (null == templateSource) {
            throw new ComponentInitializationException("TemplateAttribute definition " + getId()
                    + " is being initialized without any source");
        }
        if (null == velocity) {
            throw new ComponentInitializationException("TemplateAttribute definition " + getId()
                    + " is being initialized without and engine being supplied");
        }

        // Register the template
        final StringResourceRepository repository = StringResourceLoader.getRepository();
        if (null != repository.getStringResource(templateName)) {
            throw new ComponentInitializationException("TemplateAttribute definition " + getId() + ". emplate named "
                    + templateName + "has already been declared");
        }
        repository.putStringResource(templateName, templateSource);
    }

    /** {@inheritDoc} */
    protected Optional<Attribute> doAttributeResolution(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {

        final Attribute resultantAttribute = new Attribute(getId());
        final Map<String, Iterator> sourceValues = new LazyMap<String, Iterator>();
        final int valueCount = countAndSetupSourceValues(resolutionContext, sourceValues);

        if (null == getDependencies()) {
            log.info("TemplateAttribute definition " + getId() + " had no dependencies");
            return null;
        }

        // build velocity context
        VelocityContext vCtx = new VelocityContext();
        vCtx.put("requestContext", resolutionContext);
        for (int i = 0; i < valueCount; i++) {
            // Setup the attributes for this time around
            for (String attributeId : sourceValues.keySet()) {
                final Object value = sourceValues.get(attributeId).next();
                log.debug("TemplateAttribute definition {} iteration {}; attribute {} has value {}", new Object[] {
                        getId(), i + 1, attributeId, value.toString(),});
                vCtx.put(attributeId, value);
            }

            try {
                log.debug("Populating the following {} template", templateName);

                StringWriter output = new StringWriter();
                Template template;
                template = velocity.getTemplate(templateName);
                template.merge(vCtx, output);
                resultantAttribute.addValue(output.toString());

            } catch (Exception e) {
                //
                // Yup, Velocity throws Exception....
                //
                log.error("Unable to populate " + templateName + " template", e);
                throw new AttributeResolutionException("Unable to evaluate template", e);
            }
        }

        return Optional.of(resultantAttribute);
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
            Map<String, Iterator> sourceValues) throws AttributeResolutionException {
        int valueCount = -1;
        final Set<ResolverPluginDependency> depends = getDependencies();
        final Set<String> unresolvedAttributes = new HashSet<String>(sourceAttributes);

        for (ResolverPluginDependency dep : depends) {
            final Attribute dependentAttribute = dep.getDependentAttribute(resolutionContext);
            if (null == dependentAttribute) {
                log.warn("Dependency of TemplateAttribute " + getId() + " returned null dependent attribute");
                continue;
            }
            if (!sourceAttributes.contains(dependentAttribute.getId())) {
                log.info("Dependent atribute " + dependentAttribute.getId()
                        + " not a source attribute for template attribute definition " + getId());
                continue;
            }

            // Take from the input list
            unresolvedAttributes.remove(dependentAttribute.getId());

            // And add the values to the output list
            final Collection<?> values = dependentAttribute.getValues();
            if (null == values) {
                log.warn("Dependency " + dependentAttribute.getId() + " of TemplateAttribute " + getId()
                        + "returned null value set");
                continue;
            }
            if (valueCount == -1) {
                valueCount = values.size();
            } else if (valueCount != values.size()) {
                final String msg =
                        "All attributes used in TemplateAttributeDefinition " + getId()
                                + " must have the same number of values.";
                log.error(msg);
                throw new AttributeResolutionException(msg);
            }

            sourceValues.put(dependentAttribute.getId(), values.iterator());
        }
        if (!unresolvedAttributes.isEmpty()) {
            log.warn("Dependant attributes {} not used by TemplateAttributeDefinition {}",
                    unresolvedAttributes.toArray(), getId());
        }
        return valueCount;
    }
}