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

package net.shibboleth.idp.attribute;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.opensaml.messaging.context.AbstractSubcontext;
import org.opensaml.messaging.context.SubcontextContainer;

/**
 * Subcontext that tracks a set of attributes. Usually the tracked attributes are about a particular user and associated
 * with a particular service request.
 */
public class AttributeSubcontext extends AbstractSubcontext {

    /** The attributes tracked by this context. */
    private Map<String, Attribute<?>> attributes;

    /**
     * Constructor.
     * 
     * @param parent the owner of this subcontext, may be null
     */
    public AttributeSubcontext(SubcontextContainer parent) {
        super(parent);
        attributes = Collections.emptyMap();
    }

    /**
     * Gets the unmodifiable collection of attributes, indexed by attribute ID, tracked by this context.
     * 
     * @return the collection of attributes indexed by attribute ID; never null nor containing null elements
     */
    public Map<String, Attribute<?>> getAttributes() {
        return attributes;
    }

    /**
     * Sets the attributes tracked by this context.
     * 
     * @param newAttributes the attributes; may be null or contain null elements
     */
    public void setAttributes(Collection<Attribute<?>> newAttributes) {
        if (newAttributes == null || newAttributes.isEmpty()) {
            attributes = Collections.emptyMap();
            return;
        }

        HashMap<String, Attribute<?>> checkedAttributes = new HashMap<String, Attribute<?>>();
        for (Attribute attribute : newAttributes) {
            if (attribute == null) {
                continue;
            }

            checkedAttributes.put(attribute.getId(), attribute);
        }

        if (checkedAttributes.isEmpty()) {
            attributes = Collections.emptyMap();
        } else {
            attributes = Collections.unmodifiableMap(checkedAttributes);
        }
    }
}