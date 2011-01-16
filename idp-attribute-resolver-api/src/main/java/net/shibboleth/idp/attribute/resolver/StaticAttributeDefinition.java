/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
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

package net.shibboleth.idp.attribute.resolver;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;

/** An attribute definition that simply returns a static value. */
@ThreadSafe
public class StaticAttributeDefinition extends BaseAttributeDefinition {

    /** Static value returned by this definition. */
    private final Attribute<?> value;

    /**
     * Constructor.
     * 
     * @param id unique ID of this attribute definition
     * @param definitionValue static value returned by this definition
     */
    public StaticAttributeDefinition(final String id, final Attribute<?> definitionValue) {
        super(id);
        value = definitionValue;
    }

    /** {@inheritDoc} */
    protected Attribute<?> doAttributeDefinitionResolve(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        return value;
    }
}