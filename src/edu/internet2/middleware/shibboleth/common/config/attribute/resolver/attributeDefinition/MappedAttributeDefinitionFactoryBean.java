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

import edu.internet2.middleware.shibboleth.common.attribute.encoding.AttributeEncoder;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.MappedAttributeDefinition;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.ScopedAttributeDefinition;

/**
 * Spring factory bean that produces {@link ScopedAttributeDefinition}s.
 */
public class MappedAttributeDefinitionFactoryBean extends BaseAttributeDefinitionFactoryBean {

    /** Regex string to match the source attribute value with. */
    private String regex;

    /** The replacement string to replace the matched groups in the pattern with. */
    private String replacement;

    /** Allow regex to match a substring within the attribute value. */
    private boolean partialMatch;
    
    /** Perform case-insensitve match. */
    private boolean ignoreCase;
    

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        MappedAttributeDefinition definition = new MappedAttributeDefinition();
        definition.setId(getPluginId());
        definition.setSourceAttributeID(getSourceAttributeId());
        definition.setRegex(getRegex());
        definition.setReplacement(getReplacement());
        definition.setPartialMatch(isPartialMatch());
        definition.setIgnoreCase(isIgnoreCase());
        
        if(getAttributeDefinitionDependencyIds() != null) {
            definition.getAttributeDefinitionDependencyIds().addAll(getAttributeDefinitionDependencyIds());
        }
        
        if(getDataConnectorDependencyIds() != null) {
            definition.getDataConnectorDependencyIds().addAll(getDataConnectorDependencyIds());
        }
        
        List<AttributeEncoder> encoders = getAttributeEncoders();
        if (encoders != null && encoders.size() > 0) {
            for (AttributeEncoder encoder : encoders) {
                definition.getAttributeEncoders().put(encoder.getEncoderCategory(), encoder);
            }
        }
        
        return definition;
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return MappedAttributeDefinition.class;
    }

    
    /**
     * Get regex string.
     * 
     * @return teh regex string
     */
    public String getRegex() {
        return regex;
    }

    /**
     * Get replacement string.
     * 
     * @return the replacement string
     */
    public String getReplacement() {
        return replacement;
    }

    /**
     * Get if search is case insensitive.
     * 
     * @return true if search is case insensitive
     */
    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    /**
     * Get if search should allow partial matches.
     * @return true if search should allow partial matches.
     */
    public boolean isPartialMatch() {
        return partialMatch;
    }

    /**
     * Set if search should be case insensitive.
     * 
     * @param newIgnoreCase should search be case insensitive
     */
    public void setIgnoreCase(boolean newIgnoreCase) {
        ignoreCase = newIgnoreCase;
    }

    /**
     * Set if search should allow partial matches.
     * 
     * @param newPartialMatch should search allow partial matches
     */
    public void setPartialMatch(boolean newPartialMatch) {
        partialMatch = newPartialMatch;
    }

    /**
     * Set regex string.
     * 
     * @param newRegex new regex string
     */
    public void setRegex(String newRegex) {
        regex = newRegex;
    }

    /**
     * Set replacement string.
     * 
     * @param newReplacement new replacement string
     */
    public void setReplacement(String newReplacement) {
        replacement = newReplacement;
    }
    
}