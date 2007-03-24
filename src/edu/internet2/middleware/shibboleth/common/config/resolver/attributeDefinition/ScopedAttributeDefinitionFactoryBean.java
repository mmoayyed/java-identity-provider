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

package edu.internet2.middleware.shibboleth.common.config.resolver.attributeDefinition;

import java.util.Set;

import org.springframework.beans.factory.FactoryBean;

import edu.internet2.middleware.shibboleth.common.attribute.AttributeEncoder;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.ScopedAttributeDefinition;

/**
 * Factory bean for creating scoped attribute definitions.
 */
public class ScopedAttributeDefinitionFactoryBean implements FactoryBean {

    /** Attribute definition. */
    private ScopedAttributeDefinition attributeDefinition;
    
    /** Constructor. */
    public ScopedAttributeDefinitionFactoryBean() {
        attributeDefinition = new ScopedAttributeDefinition();
    }
    
    /** {@inheritDoc} */
    public Object getObject() throws Exception {
        return attributeDefinition;
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return ScopedAttributeDefinition.class;
    }

    /** {@inheritDoc} */
    public boolean isSingleton() {
        return false;
    }
    
    /**
     * Set id.
     * @param id new id
     */
    public void setId(String id) {
        attributeDefinition.setId(id);
    }
    
    /**
     * Set propagate errors flag.
     * @param propagateErrors new value for propagate errors flag
     */
    public void setPropagateErrors(boolean propagateErrors) {
        attributeDefinition.setPropagateErrors(propagateErrors);
    }
    
    /**
     * Set attribute definition dependency ids.
     * @param newIds new ids
     */
    public void setAttributeDefinitionDependencyIds(Set<String> newIds) {
        attributeDefinition.setAttributeDefinitionDependencyIds(newIds);
    }

    /**
     * Set data connector dependency ids.
     * @param newIds new ids
     */
    public void setDataConnectorDependencyIds(Set<String> newIds) {
        attributeDefinition.setDataConnectorDependencyIds(newIds);
    }
    
    /**
     * Set source attribute ID.
     * @param newSourceAttributeID new source attribute id
     */
    public void setSourceAttributeID(String newSourceAttributeID) {
        attributeDefinition.setSourceAttributeID(newSourceAttributeID);
    }
    
    /**
     * Set dependency only flag.
     * @param isDependencyOnly new dependency only value
     */
    public void setDependencyOnly(boolean isDependencyOnly) {
        attributeDefinition.setDependencyOnly(isDependencyOnly);
    }
    
    /**
     * Set attribute encoders.
     * @param newAttributeEncoders new attribute encoders
     */
    public void setAttributeEncoders(Set<AttributeEncoder> newAttributeEncoders) {
        for(AttributeEncoder encoder : newAttributeEncoders) {
            attributeDefinition.getAttributeEncoders().put(encoder.getEncoderCategory(), encoder);
        }
    }
    
    /**
     * Set scope.
     * @param newScope new scope
     */
    public void setScopeType(String newScope) {
        attributeDefinition.setScope(newScope);
    }

}
