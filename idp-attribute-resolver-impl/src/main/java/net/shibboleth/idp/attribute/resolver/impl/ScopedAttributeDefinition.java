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

package net.shibboleth.idp.attribute.resolver.impl;

import java.util.Collection;
import java.util.Set;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.ScopedAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An attribute definition that creates {@link ScopedAttributeValue}s by taking a source attribute value and applying a
 * static scope to each.
 */
@ThreadSafe
public class ScopedAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ScopedAttributeDefinition.class);

    /** Scope value. */
    private String scope;

    /**
     * Set the scope for this definition.
     * 
     * @param newScope what to set.
     */
    public synchronized void setScope(final String newScope) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Scoped Attribute definition " + getId()
                    + " has already been initialized, scope can not be changed.");
        }
        scope = StringSupport.trimOrNull(newScope);
    }

    /**
     * Get scope value.
     * 
     * @return Returns the scope.
     */
    public String getScope() {
        return scope;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == scope) {
            throw new ComponentInitializationException("Scoped Attribute definition " + getId()
                    + " does not have valid scope set up.");
        }
    }

    /** {@inheritDoc} */
    protected Attribute<ScopedAttributeValue> doAttributeResolution(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {

        final Set<ResolverPluginDependency> depends = getDependencies();
        if (null == depends) {
            log.info("ScopedAttribute definition " + getId() + " had no dependencies");
            return null;
        }

        final Collection<ScopedAttributeValue> resultingValues = new LazySet<ScopedAttributeValue>();
        for (ResolverPluginDependency dep : depends) {

            final Attribute<?> dependentAttribute = dep.getDependentAttribute(resolutionContext);
            if (null == dependentAttribute) {
                log.error("Dependency of ScopedAttribute " + getId() + " returned null dependent attribute");
                continue;
            }

            final Collection<?> values = dependentAttribute.getValues();
            if (null == values) {
                log.error("Dependency " + dependentAttribute.getId() + " of ScopedAttribute " + getId()
                        + "returned null value set");
                continue;
            }
            if (values.isEmpty()) {
                log.debug("Dependency " + dependentAttribute.getId() + " of ScopedAttribute " + getId()
                        + "returned no values, skipping");
                continue;
            }
            for (Object value : values) {
                resultingValues.add(new ScopedAttributeValue(value.toString(), scope));
            }
        }
        if (resultingValues.isEmpty()) {
            log.debug("Scoped definition " + getId() + " returned no values");
        }

        final Attribute<ScopedAttributeValue> resultantAttribute = new Attribute<ScopedAttributeValue>(getId());
        resultantAttribute.setValues(resultingValues);
        return resultantAttribute;
    }
}