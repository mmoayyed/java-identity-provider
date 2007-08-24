/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.config.attribute.resolver.attributeDefinition;

import java.util.List;

import org.apache.velocity.app.VelocityEngine;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.TemplateAttributeDefinition;

/**
 * Spring factory bean that produces {@link TemplateAttributeDefinition}s.
 */
public class TemplateAttributeDefinitionFactoryBean extends BaseAttributeDefinitionFactoryBean {

    /** Attribute template string. */
    private String attributeTemplate;

    /** IDs of source attributes. */
    private List<String> sourceAttributes;

    /** Velocity engine instance. */
    private VelocityEngine velocityEngine;

    /** {@inheritDoc} */
    public Class getObjectType() {
        return TemplateAttributeDefinition.class;
    }

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        TemplateAttributeDefinition definition = new TemplateAttributeDefinition(velocityEngine);
        populateAttributeDefinition(definition);

        definition.setAttributeTemplate(attributeTemplate);
        definition.setSourceAttributes(sourceAttributes);

        definition.initialize();

        return definition;
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

    /**
     * Get velocity engine instance.
     * 
     * @return velocity engine instance
     */
    public VelocityEngine getVelocityEngine() {
        return velocityEngine;
    }

    /**
     * Set velocity engine instance.
     * 
     * @param newVelocityEngine velocity engine instance
     */
    public void setVelocityEngine(VelocityEngine newVelocityEngine) {
        System.out.println("setting velocity engine " + newVelocityEngine);
        velocityEngine = newVelocityEngine;
    }

}