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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ResolutionPlugIn;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.AttributeDefinition;

/**
 * A velocity based template engine that pulls information from a resolution context for use within the template.
 */
public class TemplateEngine {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(TemplateEngine.class);

    /** Velocity engine to use to render templates. */
    private VelocityEngine velocity;

    /**
     * Constructor.
     * 
     * @param engine velocity engine used to evaluate templates
     */
    public TemplateEngine(VelocityEngine engine) {
        velocity = engine;
    }

    /**
     * Registers a template under a given name.
     * 
     * @param templateName name to register the template under
     * @param template template to register
     */
    public void registerTemplate(String templateName, String template) {
        StringResourceRepository repository = StringResourceLoader.getRepository();
        repository.putStringResource(templateName, template);
    }

    /**
     * Create a statement from a give template by replacing it's macro's with information within the resolution context.
     * 
     * @param templateName name of the template
     * @param resolutionContext the current resolution context
     * @param dependencies the list of resolution plug-in dependencies that will provider attributes
     * @param escapingStrategy strategy used to escape values, may be null if no escaping is necessary
     * 
     * @return constructed statement
     * 
     * @throws AttributeResolutionException thrown if the given template can not be populated because it is malformed or
     *             the given data connectors or attribute definitions error out during resolution
     */
    public String createStatement(String templateName, ShibbolethResolutionContext resolutionContext,
            List<String> dependencies, CharacterEscapingStrategy escapingStrategy) throws AttributeResolutionException {
        VelocityContext vContext = createVelocityContext(resolutionContext, dependencies, escapingStrategy);

        try {
            log.trace("Populating the following {} template", templateName);

            StringWriter output = new StringWriter();
            Template template = velocity.getTemplate(templateName);
            template.merge(vContext, output);
            return output.toString();
        } catch (Exception e) {
            log.error("Unable to populate " + templateName + " template", e);
            throw new AttributeResolutionException("Unable to evaluate template", e);
        }
    }

    /**
     * Creates the velocity context from the given resolution context.
     * 
     * @param resolutionContext the resolution context containing the currently resolved attribute information
     * @param dependencies resolution plug-in dependencies that will provide attributes to the velocity context
     * @param escapingStrategy strategy used to escape values
     * 
     * @return the velocity context to use when evaluating the template
     * 
     * @throws AttributeResolutionException thrown if a resolution plugin errors out while resolving its attributes
     */
    @SuppressWarnings("unchecked")
    protected VelocityContext createVelocityContext(ShibbolethResolutionContext resolutionContext,
            List<String> dependencies, CharacterEscapingStrategy escapingStrategy) throws AttributeResolutionException {
        log.trace("Populating velocity context");
        VelocityContext vCtx = new VelocityContext();
        vCtx.put("requestContext", resolutionContext.getAttributeRequestContext());

        ResolutionPlugIn plugin;
        Map<String, BaseAttribute> attributes;
        BaseAttribute attribute;
        for (String dependencyId : dependencies) {
            plugin = resolutionContext.getResolvedPlugins().get(dependencyId);
            if (plugin instanceof DataConnector) {
                log.trace("Resolving attributes from data connector {}", dependencyId);
                attributes = ((DataConnector) plugin).resolve(resolutionContext);

                for (String attributeId : attributes.keySet()) {
                    vCtx.put(attributeId, prepareAttributeValues(attributes.get(attributeId), escapingStrategy));
                }
            } else if (plugin instanceof AttributeDefinition) {
                log.trace("Resolving attributes from attribute definition {}", dependencyId);
                attribute = ((AttributeDefinition) plugin).resolve(resolutionContext);
                if (!vCtx.containsKey(attribute.getId())) {
                    vCtx.put(attribute.getId(), new ArrayList<String>());
                }
                ((List<String>) vCtx.get(attribute.getId())).addAll(attribute.getValues());
            } else {
                log.trace("Unable to locate resolution plugin {}", dependencyId);
            }
        }

        return vCtx;
    }

    /**
     * Prepares an attributes values for use within a template.
     * 
     * @param attribute attribute whose values are to be prepared
     * @param escapingStrategy character escaping strategy to be sued
     * 
     * @return prepared values
     */
    protected List<Object> prepareAttributeValues(BaseAttribute attribute, CharacterEscapingStrategy escapingStrategy) {
        ArrayList<Object> preparedValues = new ArrayList<Object>();

        if (attribute == null || attribute.getValues() == null || attribute.getValues().isEmpty()) {
            return preparedValues;
        }

        if (escapingStrategy == null) {
            preparedValues.addAll(attribute.getValues());
        } else {
            for (Object value : attribute.getValues()) {
                if(value instanceof String){
                    preparedValues.add(escapingStrategy.escape(value.toString()));
                }else{
                    preparedValues.add(value);
                }
            }
        }

        return preparedValues;
    }

    /**
     * Represents a domain specific strategy for escaping values used within a template.
     */
    public interface CharacterEscapingStrategy {

        /**
         * Creates a new string with necessary escaping rules.
         * 
         * @param value the value to be escaped
         * 
         * @return the escaped string
         */
        public String escape(String value);
    }
}