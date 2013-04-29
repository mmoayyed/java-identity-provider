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

package net.shibboleth.idp.attribute.resolver.impl.ad;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.UnsupportedAttributeTypeException;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import com.google.common.base.Optional;

/**
 * An attribute definition that creates {@link ScopedStringAttributeValue}s by taking a source attribute value and
 * applying a static scope to each.
 */
@ThreadSafe
public class ScopedAttributeDefinition extends BaseAttributeDefinition {

    /** Scope value. */
    private String scope;

    /**
     * Get scope value.
     * 
     * @return Returns the scope.
     */
    @Nullable @NonnullAfterInit public String getScope() {
        return scope;
    }

    /**
     * Set the scope for this definition.
     * 
     * @param newScope what to set.
     */
    public synchronized void setScope(@Nonnull @NotEmpty final String newScope) {
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        scope = Constraint.isNotNull(StringSupport.trimOrNull(newScope), "Scope can not be null or empty");
    }

    /** {@inheritDoc} */
    @Nonnull protected Optional<Attribute> doAttributeDefinitionResolve(
            @Nonnull final AttributeResolutionContext resolutionContext) throws ResolutionException {

        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        final Attribute resultantAttribute = new Attribute(getId());

        final Set<AttributeValue> dependencyValues =
                PluginDependencySupport.getMergedAttributeValues(resolutionContext, getDependencies());

        for (AttributeValue dependencyValue : dependencyValues) {
            if (!(dependencyValue instanceof StringAttributeValue)) {
                throw new ResolutionException(new UnsupportedAttributeTypeException(
                        "This attribute definition only operates on attribute values of type "
                                + StringAttributeValue.class.getName()));
            }

            resultantAttribute.getValues().add(
                    new ScopedStringAttributeValue((String) dependencyValue.getValue(), scope));
        }

        return Optional.of(resultantAttribute);
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == scope) {
            throw new ComponentInitializationException("Attribute definition '" + getId()
                    + "': no scope was configured");
        }

        if (getDependencies().isEmpty()) {
            throw new ComponentInitializationException("Attribute definition '" + getId()
                    + "': no dependencies were configured");
        }
    }
}