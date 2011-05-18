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

package net.shibboleth.idp.attribute.resolver.impl;

import java.util.Collection;
import java.util.Set;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.ScopedAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;

import org.opensaml.util.collections.LazySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An attribute definition that creates {@link ScopedAttributeValue}s by taking a source attribute value and applying a
 * static scope to each.
 */
public class ScopedAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private static Logger log = LoggerFactory.getLogger(ScopedAttributeDefinition.class);

    /** Scope value. */
    private String scope;

    /**
     * Constructor.
     * 
     * @param id the id of the definition.
     * @param newScope scope of the attribute
     */
    public ScopedAttributeDefinition(String id, String newScope) {
        super(id);
        this.scope = newScope;
    }

    /** {@inheritDoc} */
    protected Attribute<ScopedAttributeValue> doAttributeResolution(AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        Set<ResolverPluginDependency> depends = getDependencies();
        Attribute<ScopedAttributeValue> result = new Attribute<ScopedAttributeValue>(getId());
        Collection<ScopedAttributeValue> results = new LazySet<ScopedAttributeValue>();

        if (null == depends) {
            //
            // No input? No output
            //
            log.info("ScopedAttribute definition " + getId() + " had no dependencies");
            return null;
        }
        for (ResolverPluginDependency dep : depends) {
            Attribute<?> dependentAttribute = dep.getDependentAttribute(resolutionContext);
            Collection<?> values;
            if (null == dependentAttribute) {
                log.error("Dependency of ScopedAttribute " + getId() + " returned null dependent attribute");
                continue;
            }
            values = dependentAttribute.getValues();
            if (null == dependentAttribute.getValues()) {
                log.error("Dependency " + dependentAttribute.getId() + " of ScopedAttribute " + getId()
                        + "returned null value set");
                continue;
            }
            if (dependentAttribute.getValues().isEmpty()) {
                log.debug("Dependency " + dependentAttribute.getId() + " of ScopedAttribute " + getId()
                        + "returned no values, skipping");
                continue;
            }
            for (Object value : values) {
                results.add(new ScopedAttributeValue(value.toString(), scope));
            }
        }
        if (results.isEmpty()) {
            log.debug("Scoped definition " + getId() + " returned no values");
        }
        result.setValues(results);
        return result;
    }

    /**
     * Get scope value.
     * 
     * @return Returns the scope.
     */
    public String getScope() {
        return scope;
    }
}