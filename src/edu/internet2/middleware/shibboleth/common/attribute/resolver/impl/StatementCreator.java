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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.impl;

import java.io.StringWriter;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeDefinition;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.DataConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionContext;

/**
 * Create a statement from a give template by replacing its macro's with information within the resolution context.
 */
public class StatementCreator {

    /** Class logger. */
    private static Logger log = Logger.getLogger(StatementCreator.class);
    
    /**
     * Create a statement from a give template by replacing it's macro's with information within the resolution context.
     * 
     * @param template statement template
     * @param resolutionContext the current resolution context
     * @param dataConnectors the list of data connectors that will provider attributes
     * @param attributeDefinitions the list of attribute definitions that will provider attributes
     * 
     * @return constructed statement
     * 
     * @throws AttributeResolutionException thrown if the given template can not be populated because it is malformed or
     *             the given data connectors or attribute definitions error out during resolution
     */
    public String createStatement(String template, ResolutionContext resolutionContext, Set<String> dataConnectors,
            Set<String> attributeDefinitions) throws AttributeResolutionException {
        VelocityContext vContext = createVelocityContext(resolutionContext, dataConnectors, attributeDefinitions);

        try {
            if(log.isDebugEnabled()){
                log.debug("Populating the following template: " + template);
            }
            
            StringWriter output = new StringWriter();
            Velocity.evaluate(vContext, output, "StatementCreator", template);
            return output.toString();
        } catch (Exception e) {
            log.error("Unable to populate template " + template, e);
            throw new AttributeResolutionException("Unable to evaluate template", e);
        }
    }

    /**
     * Creates the velocity context from the given resolution context.
     * 
     * @param resolutionContext the resolution context containing the currently resolved attribute information
     * @param dataConnectors data connectors that will provide attributes to the velocity context
     * @param attributeDefinitions attribute definitions that will provide attributes to the velocity context
     * 
     * @return the velocity context to use when evaluating the template
     * 
     * @throws AttributeResolutionException thrown if a resolution plugin errors out while resolving its attributes
     */
    protected VelocityContext createVelocityContext(ResolutionContext resolutionContext, Set<String> dataConnectors,
            Set<String> attributeDefinitions) throws AttributeResolutionException {
        if(log.isDebugEnabled()){
            log.debug("Populating velocity context");
        }
        VelocityContext vCtx = new VelocityContext();
        vCtx.put("principal", resolutionContext.getPrincipalName());

        List<Attribute> attributes;
        DataConnector dataConnector;
        for (String connectorId : dataConnectors) {
            dataConnector = resolutionContext.getResolvedDataConnectors().get(connectorId);
            if(log.isDebugEnabled()){
                log.debug("Resolving attributes from data connector " + connectorId);
            }
            attributes = dataConnector.resolve(resolutionContext);
            for (Attribute attribute : attributes) {
                vCtx.put(attribute.getID(), attribute.getValues());
            }
        }

        Attribute attribute;
        AttributeDefinition attributeDefinition;
        for (String definitionId : attributeDefinitions) {
            attributeDefinition = resolutionContext.getResolvedAttributeDefinitions().get(definitionId);
            if(log.isDebugEnabled()){
                log.debug("Resolving attributes from attribute definition " + definitionId);
            }
            attribute = attributeDefinition.resolve(resolutionContext);
            vCtx.put(attribute.getID(), attribute.getValues());
        }

        return vCtx;
    }
}