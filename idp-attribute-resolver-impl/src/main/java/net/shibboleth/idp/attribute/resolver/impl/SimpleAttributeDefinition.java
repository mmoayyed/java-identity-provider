/*
 * Licensed to the University Corporation for Advanced Internet Development, Inc.
 * under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache 
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

package net.shibboleth.idp.attribute.resolver.impl;

import java.util.Collection;
import java.util.Set;

import org.opensaml.util.collections.CollectionSupport;
import org.opensaml.util.collections.LazySet;

import net.jcip.annotations.NotThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;

/** A Simple Attribute definition. */
@NotThreadSafe
public class SimpleAttributeDefinition extends BaseAttributeDefinition {

    /**
     * Constructor.
     * 
     * @param id the name of the attribute.
     */
    public SimpleAttributeDefinition(String id) {
        super(id);
    }

    /** {@inheritDoc} */
    protected Attribute<?> doAttributeResolution(AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        Set<ResolverPluginDependency> depends = getDependencies();
        Attribute<Object> result = new Attribute<Object>(getId());
        Collection<Object> results = new LazySet<Object>();

        if (null == depends) {
            //
            // No input? No output
            //
            return null;
        }
        for (ResolverPluginDependency dep : depends) {
            Attribute<?> dependentAttribute = dep.getDependentAttribute(resolutionContext);
            if (null != dependentAttribute) {
                CollectionSupport.addNonNull(dependentAttribute.getValues(), results);
            }
        }
        result.setValues(results);
        return result;
    }

}
