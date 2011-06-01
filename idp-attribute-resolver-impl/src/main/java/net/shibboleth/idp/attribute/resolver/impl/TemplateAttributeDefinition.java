/*
 * Licensed to the University Corporation for Advanced Internet Development, Inc.
 * under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache 
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.NotThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.opensaml.util.collections.LazyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An attribute definition that constructs its values based on the values of its dependencies using the Velocity
 * Template Language. Dependencies may have multiple values, however multiples dependencies must have the same number of
 * values. In the case of multi-valued dependencies, the template will be evaluated multiples times, iterating over each
 * dependency.
 */
@NotThreadSafe
public class TemplateAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private static Logger log = LoggerFactory.getLogger(TemplateAttributeDefinition.class);

    /** Velocity engine to use to render attribute values. */
    private final VelocityEngine velocity;

    /** Name the attribute template is registered under within the template engine. */
    private final String templateName;

    /** Template that produces the attribute value. */
    private final String attributeTemplate;

    /**
     * IDs of the attributes used in this composite.
     * 
     * This is checked against dependencies
     */
    private final Set<String> sourceAttributes;

    /**
     * Constructor.
     * 
     * @param id the identifier for this definition.
     * @param engine velocity engine used to parse template.
     * @param template the template we use.
     * @param attributes the attributes.
     */
    public TemplateAttributeDefinition(final String id, final VelocityEngine engine, final String template,
            final List<String> attributes) {
        super(id);
        velocity = engine;
        templateName = "shibboleth.resolver.ad." + id;
        sourceAttributes = new HashSet<String>(attributes);
        attributeTemplate = template;

        //
        // Register the template
        //
        StringResourceRepository repository = StringResourceLoader.getRepository();
        repository.putStringResource(templateName, attributeTemplate.trim());
    }

    /**
     * Set up a map which can be used to populate the template. The key is the attribute name and the value is the
     * iterator to give all the names. We also return how deep the iteration will be and throw an exception if there is
     * a mismatch.
     * 
     * Finally the names of the source attributes is checked against the dependency attributes and if there is a
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
        Set<ResolverPluginDependency> depends = getDependencies();
        Set<String> unresolvedAttributes = new HashSet<String>(sourceAttributes);

        for (ResolverPluginDependency dep : depends) {
            Attribute<?> dependentAttribute = dep.getDependentAttribute(resolutionContext);
            Collection<?> values;
            if (null == dependentAttribute) {
                log.warn("Dependency of TemplateAttribute " + getId() + " returned null dependent attribute");
                continue;
            }
            if (!sourceAttributes.contains(dependentAttribute.getId())) {
                log.info("Dependent atribute " + dependentAttribute.getId()
                        + " not a source attribute for template attribute definition " + getId());
                continue;
            }
            //
            // Take from the input list
            //
            unresolvedAttributes.remove(dependentAttribute.getId());

            //
            // And add the values to the output list
            //
            values = dependentAttribute.getValues();
            if (null == values) {
                log.warn("Dependency " + dependentAttribute.getId() + " of TemplateAttribute " + getId()
                        + "returned null value set");
                continue;
            }
            if (valueCount == -1) {
                valueCount = values.size();
            } else if (valueCount != values.size()) {
                log.error("All attributes used in TemplateAttributeDefinition " + getId()
                        + " must have the same number of values.");
                throw new AttributeResolutionException("All attributes used in TemplateAttributeDefinition " + getId()
                        + " must have the same number of values.");
            }

            sourceValues.put(dependentAttribute.getId(), values.iterator());
        }
        if (!unresolvedAttributes.isEmpty()) {
            log.warn("Dependant attributes {} not used by TemplateAttributeDefinition {}",
                    unresolvedAttributes.toArray(), getId());
        }
        return valueCount;
    }

    /** {@inheritDoc} */

    protected Attribute<?> doAttributeResolution(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {

        Map<String, Iterator> sourceValues = new LazyMap<String, Iterator>();
        Attribute<String> result = new Attribute<String>(getId());
        int valueCount = countAndSetupSourceValues(resolutionContext, sourceValues);

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
                vCtx.put(attributeId, sourceValues.get(attributeId).next());
            }

            try {
                log.debug("Populating the following {} template", templateName);

                StringWriter output = new StringWriter();
                Template template;
                template = velocity.getTemplate(templateName);
                template.merge(vCtx, output);
                result.addValue(output.toString());

            } catch (Exception e) {
                //
                // Yup, Velocity throws Exception....
                //
                log.error("Unable to populate " + templateName + " template", e);
                throw new AttributeResolutionException("Unable to evaluate template", e);
            }
        }

        return result;
    }
}