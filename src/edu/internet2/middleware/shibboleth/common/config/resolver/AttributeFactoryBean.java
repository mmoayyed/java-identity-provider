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

package edu.internet2.middleware.shibboleth.common.config.resolver;

import java.util.Collection;

import org.springframework.beans.factory.FactoryBean;

import edu.internet2.middleware.shibboleth.common.attribute.impl.BaseAttribute;

/**
 * Spring Bean Definition Parser for resolver attribute.
 */
public class AttributeFactoryBean implements FactoryBean {

    /** Attribute. */
    private BaseAttribute<String> attribute;

    /** Attribute values. */
    private Collection<String> values;

    /** {@inheritDoc} */
    public Object getObject() throws Exception {
        
        if (values != null && !values.isEmpty()) {
            for (String value : values) {
                attribute.getValues().add(value);
            }
        }

        return attribute;
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return BaseAttribute.class;
    }

    /** {@inheritDoc} */
    public boolean isSingleton() {
        return false;
    }

    /**
     * Set attribute.
     * 
     * @param newAttribute new attribute
     */
    public void setAttribute(BaseAttribute<String> newAttribute) {
        attribute = newAttribute;
    }

    /**
     * Set values.
     * 
     * @param newValues new values
     */
    public void setValues(Collection<String> newValues) {
        values = newValues;
    }
}
