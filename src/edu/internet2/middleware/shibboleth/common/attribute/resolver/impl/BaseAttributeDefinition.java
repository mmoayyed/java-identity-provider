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

import java.util.List;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.AttributeEncoder;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeDefinition;

/**
 * Base class for Attribute Definition PlugIns.
 * 
 * @author Will Norris (wnorris@usc.edu)
 */
public abstract class BaseAttributeDefinition extends BaseResolutionPlugIn<Attribute> implements AttributeDefinition {

    private List<AttributeEncoder> encoders;

    /** {@inheritDoc} */
    public List<AttributeEncoder> getAttributeEncoders() {
        return encoders;
    }

}