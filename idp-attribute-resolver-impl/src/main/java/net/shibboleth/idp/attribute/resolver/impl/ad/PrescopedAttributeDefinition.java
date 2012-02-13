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
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.UnsupportedAttributeTypeException;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Assert;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * An attribute definition that creates {@link ScopedStringAttributeValue}s by taking a source attribute value splitting
 * it at a delimiter. The first atom becomes the attribute value and the second value becomes the scope.
 */
@ThreadSafe
public class PrescopedAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(PrescopedAttributeDefinition.class);

    /** Delimiter between value and scope. Default value: @ */
    private String scopeDelimiter = "@";

    /**
     * Get delimiter between value and scope.
     * 
     * @return delimiter between value and scope
     */
    @Nonnull public String getScopeDelimiter() {
        return scopeDelimiter;
    }

    /**
     * Set the delimiter between value and scope.
     * 
     * @param newScopeDelimiter delimiter between value and scope
     */
    public synchronized void setScopeDelimiter(@Nonnull @NotEmpty final String newScopeDelimiter) {
        ifInitializedThrowUnmodifiabledComponentException(getId());
        ifDestroyedThrowDestroyedComponentException(getId());

        scopeDelimiter =
                Assert.isNotNull(StringSupport.trimOrNull(newScopeDelimiter),
                        "Scope delimiter can not be null or empty");
    }

    /** {@inheritDoc} */
    @Nonnull protected Optional<Attribute> doAttributeDefinitionResolve(
            @Nonnull final AttributeResolutionContext resolutionContext) throws AttributeResolutionException {
        assert resolutionContext != null : "Attribute resolution context can not be null";

        final Attribute resultantAttribute = new Attribute(getId());

        final Set<AttributeValue> dependencyValues =
                PluginDependencySupport.getMergedAttributeValues(resolutionContext, getDependencies());
        log.debug("Attribute definition '{}': Dependencies {} provided unmapped values of {}", new Object[] {getId(),
                getDependencies(), dependencyValues,});

        for (AttributeValue dependencyValue : dependencyValues) {
            if (!(dependencyValue instanceof StringAttributeValue)) {
                throw new AttributeResolutionException(new UnsupportedAttributeTypeException(
                        "This attribute definition only operates on attribute values of type "
                                + StringAttributeValue.class.getName()));
            }

            resultantAttribute.getValues().add(buildScopedStringAttributeValue((StringAttributeValue) dependencyValue));
        }

        return Optional.of(resultantAttribute);
    }

    /**
     * Builds a {@link ScopedStringAttributeValue} from a {@link StringAttributeValue} whose value contains a delimited
     * value.
     * 
     * @param value the original attribute value
     * 
     * @return the scoped attribute value
     * 
     * @throws AttributeResolutionException thrown if the given attribute value does not contain a delimited value
     */
    @Nonnull private ScopedStringAttributeValue buildScopedStringAttributeValue(@Nonnull StringAttributeValue value)
            throws AttributeResolutionException {
        assert value != null : "Attribute value can not be null";

        final String[] stringValues = value.getValue().split(scopeDelimiter);
        if (stringValues.length < 2) {
            log.error(
                    "Attribute definition '{}': Input attribute value {} does not contain delimiter {} and can not be split",
                    new Object[] {getId(), value.getValue(), scopeDelimiter,});
            throw new AttributeResolutionException("Input attribute value can not be split.");
        }

        log.debug("Attribute definition '{}': Value '{}' was split into {} at scope delimiter '{}'", new Object[] {
                getId(), value.getValue(), stringValues, scopeDelimiter,});
        return new ScopedStringAttributeValue(stringValues[0], stringValues[1]);
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (getDependencies().isEmpty()) {
            throw new ComponentInitializationException("Attribute definition '" + getId()
                    + "': no dependencies were configured");
        }
    }
}