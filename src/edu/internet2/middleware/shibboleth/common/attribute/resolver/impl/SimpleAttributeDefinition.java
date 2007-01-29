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

import org.apache.log4j.Logger;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.impl.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionContext;

/**
 * Simple attribute definition.
 */
public class SimpleAttributeDefinition extends BaseAttributeDefinition {

    /** Log4j logger. */
    private static Logger log = Logger.getLogger(SimpleAttributeDefinition.class);

    /** {@inheritDoc} */
    public Attribute resolve(ResolutionContext resolutionContext) throws AttributeResolutionException {
        log.debug("Resolving attribute: (" + getId() + ")");

        BaseAttribute<Object> attribute = new BaseAttribute<Object>();
        for (Object o : getValuesFromAllDependencies(resolutionContext)) {
            attribute.getValues().add(o);
        }

        return attribute;
    }

}