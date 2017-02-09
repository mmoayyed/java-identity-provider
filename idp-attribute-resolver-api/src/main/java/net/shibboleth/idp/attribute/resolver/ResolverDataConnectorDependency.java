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

package net.shibboleth.idp.attribute.resolver;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

/**
 *
 */
public final class ResolverDataConnectorDependency extends ResolverPluginDependency {

    private boolean allAttributes = false;
    
    private Set<String> attributeNames = Collections.EMPTY_SET;
    
    /**
     * Constructor.
     *
     * @param pluginId
     */
    public ResolverDataConnectorDependency(final String pluginId) {
        super(pluginId);
    }

    /**
     * @return Returns the allAttributes.
     */
    public boolean isAllAttributes() {
        return allAttributes;
    }

    /**
     * @param all The allAttributes to set.
     */
    public void setAllAttributes(final boolean all) {
        allAttributes = all;
    }

    /**
     * @return Returns the attributeNames.
     */
    public Collection<String> getAttributeNames() {
        return attributeNames;
    }

    /**
     * @param names The attributeNames to set.
     */
    public void setAttributeNames(final Collection<String> names) {
        attributeNames = new HashSet(names);
    }
   
    /**
     * {@inheritDoc}
     * @deprecated("Used SetAttributeNames instead")
     */
    @Override @Deprecated public void setDependencyAttributeId(@Nullable final String attributeId) {
        super.setDependencyAttributeId(attributeId);
    }


}
