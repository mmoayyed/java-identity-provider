/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.util.StringResourceLoader;

/**
 * An {@link AttributeDefinition} that constructs its values based on the values of its dependencies using the Velocity
 * Template Language. Dependencies may have multiple values, however multiples dependencies must have the same number of
 * values. In the case of multi-valued dependencies, the template will be evaluated multiples times, iterating over each
 * dependency.
 */
public class TemplateAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(TemplateAttributeDefinition.class);

    /** Velocity engine to use to render attribute values. */
    private VelocityEngine velocity;

    /** Name the attribute template is registered under within the template engine. */
    private String templateName;

    /** Template that produces the attribute value. */
    private String attributeTemplate;

    /**
     * IDs of the attributes used in this composite.
     */
    private List<String> sourceAttributes;

    /**
     * Constructor.
     * 
     * @param newVelocityEngine velocity engine used to parse template.
     */
    public TemplateAttributeDefinition(VelocityEngine newVelocityEngine) {
        velocity = newVelocityEngine;
        sourceAttributes = new ArrayList<String>();
    }

    /** {@inheritDoc} */
    protected BaseAttribute doResolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        Map<String, Iterator> sourceValues = new HashMap<String, Iterator>();
        BasicAttribute<Object> attribute = new BasicAttribute<Object>();
        attribute.setId(getId());

        int valueCount = -1;

        // get source attributes values
        for (String attributeId : sourceAttributes) {
            Collection values = getValuesFromAllDependencies(resolutionContext, attributeId);

            if (valueCount == -1) {
                valueCount = values.size();
            } else if (valueCount != values.size()) {
                log.error("All attributes used in TemplateAttributeDefinition " + getId()
                        + " must have the same number of values.");
                throw new AttributeResolutionException("All attributes used in TemplateAttributeDefinition " + getId()
                        + " must have the same number of values.");
            }

            sourceValues.put(attributeId, values.iterator());
        }

        // build velocity context
        VelocityContext vCtx = new VelocityContext();
        vCtx.put("requestContext", resolutionContext.getAttributeRequestContext());
        for (int i = 0; i < valueCount; i++) {
            for (String attributeId : sourceValues.keySet()) {
                vCtx.put(attributeId, sourceValues.get(attributeId).next());
            }

            try {
                log.debug("Populating the following {} template", templateName);

                StringWriter output = new StringWriter();
                Template template = velocity.getTemplate(templateName);
                template.merge(vCtx, output);
                attribute.getValues().add(output.toString());
            } catch (Exception e) {
                log.error("Unable to populate " + templateName + " template", e);
                throw new AttributeResolutionException("Unable to evaluate template", e);
            }
        }

        return attribute;
    }

    /**
     * Initialize the attribute definition and prepare it for use.
     * 
     * @throws Exception if unable to initialize attribute definition
     */
    public void initialize() throws Exception {
        if (DatatypeHelper.isEmpty(attributeTemplate)) {
            StringBuffer defaultTemplate = new StringBuffer();
            for (String id : sourceAttributes) {
                defaultTemplate.append("${").append(id).append("} ");
            }
            attributeTemplate = defaultTemplate.toString();
        }

        registerTemplate();
    }

    /**
     * Registers the template with template engine.
     */
    protected void registerTemplate() {
        StringResourceRepository repository = StringResourceLoader.getRepository();
        templateName = "shibboleth.resolver.ad." + getId();
        repository.putStringResource(templateName, attributeTemplate.trim());
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        // do nothing
    }

    /**
     * Get the attribute template.
     * 
     * @return the attribute template
     */
    public String getAttributeTemplate() {
        return attributeTemplate;
    }

    /**
     * Set the attribute template.
     * 
     * @param newAttributeTemplate the attribute template
     */
    public void setAttributeTemplate(String newAttributeTemplate) {
        attributeTemplate = newAttributeTemplate;
    }

    /**
     * Get the source attribute IDs.
     * 
     * @return the source attribute IDs
     */
    public List<String> getSourceAttributes() {
        return sourceAttributes;
    }

    /**
     * Set the source attribute IDs.
     * 
     * @param newSourceAttributes the source attribute IDs
     */
    public void setSourceAttributes(List<String> newSourceAttributes) {
        sourceAttributes = newSourceAttributes;
    }
}