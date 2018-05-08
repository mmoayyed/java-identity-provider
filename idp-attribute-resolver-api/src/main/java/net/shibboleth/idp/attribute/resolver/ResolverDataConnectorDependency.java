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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * A Dependency that references to an Data Connector.
 */
public final class ResolverDataConnectorDependency extends ResolverPluginDependency {

    /** Whether to depend on all the connector's attributes. */
    private boolean allAttributes;
    
    /** Names of attributes to import via dependency. */
    @Nonnull @NotEmpty private Set<String> attributeNames;
    
    /**
     * Constructor.
     *
     * @param pluginId ID of dependency
     */
    public ResolverDataConnectorDependency(@ParameterName(name="pluginId") final String pluginId) {
        super(pluginId);
        
        allAttributes = false;
        attributeNames = Collections.emptySet();
    }

    /**
     * Get whether all the connector's attributes are part of the dependency.
     * 
     * @return whether all the connector's attributes are part of the dependency
     */
    public boolean isAllAttributes() {
        return allAttributes;
    }

    /**
     * Set whether all the connector's attributes are part of the dependency.
     * 
     * @param all flag to set
     */
    public void setAllAttributes(final boolean all) {
        allAttributes = all;
    }

    /**
     * Get the names of the connector's attributes that make up the dependency.
     * 
     * @return attribute names
     */
    @Nonnull @NonnullElements public Collection<String> getAttributeNames() {
        return attributeNames;
    }

    /**
     * Set the names of the connector's attributes that make up the dependency.
     * 
     * @param names attribute names
     */
    public void setAttributeNames(@Nonnull @NotEmpty final Collection<String> names) {
        attributeNames = new HashSet<>(StringSupport.normalizeStringCollection(names));
    }
   
    /**
     * {@inheritDoc}
     *
     * @deprecated Use {@link #setAttributeNames} instead
     * @see        #setAttributeNames
     */
    @Override @Deprecated public void setDependencyAttributeId(@Nullable final String attributeId) {
        DeprecationSupport.warn(ObjectType.METHOD,
                "ResolverDataConnectorDependency#setDependencyAttributeId(String)",
                null, "#setAttributeNames(Collection<String>)");
        super.setDependencyAttributeId(attributeId);
    }

}