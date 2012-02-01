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

import java.util.HashSet;
import java.util.Set;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * An attribute definition that creates {@link ScopedStringAttributeValue}s by taking a source attribute value and
 * applying a static scope to each.
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
        ifInitializedThrowUnmodifiabledComponentException(getId());
        ifDestroyedThrowDestroyedComponentException(getId());

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
    protected Optional<Attribute> doAttributeResolution(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {

        final Set<ResolverPluginDependency> depends = getDependencies();
        if (null == depends) {
            log.info("ScopedAttribute definition " + getId() + " had no dependencies");
            return null;
        }
        final Set<AttributeValue> dependencyValues =
                PluginDependencySupport.getMergedAttributeValues(resolutionContext, getDependencies());

        final Set<ScopedStringAttributeValue> resultingValues = new HashSet<ScopedStringAttributeValue>();
        for (AttributeValue dependencyValue : dependencyValues) {
            if (!(dependencyValue instanceof StringAttributeValue)) {
                throw new AttributeResolutionException(
                        "This attribute definition only operates on attribute values of type "
                                + StringAttributeValue.class.getName());
            }

            resultingValues.add(new ScopedStringAttributeValue((String) dependencyValue.getValue(), scope));
        }

        if (resultingValues.isEmpty()) {
            log.debug("Scoped definition " + getId() + " returned no values");
        }

        final Attribute resultantAttribute = new Attribute(getId());
        resultantAttribute.setValues(resultingValues);
        return Optional.of(resultantAttribute);
    }
}