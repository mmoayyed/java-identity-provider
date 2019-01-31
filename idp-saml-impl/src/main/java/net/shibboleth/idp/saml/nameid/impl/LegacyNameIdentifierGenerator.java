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

package net.shibboleth.idp.saml.nameid.impl;

import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLException;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.profile.NameIdentifierGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;

import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.saml.nameid.NameIdentifierAttributeEncoder;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;

/**
 * Legacy generator of name identifier objects that relies on resolved attributes having
 * {@link NameIdentifierAttributeEncoder}s attached.
 * 
 * <p>Provided for compatibility with V2 configurations.</p>
 * 
 * <p>While in principle this generator could be configured in the V3 manner by mapping Format(s)
 * to instances of the class, this would require extra configuration to guarantee compatibility,
 * so by design it works by relying on the Format value supplied at generation time to decide
 * which attribute encoders use, in the manner the V2 IdP does.</p>
 * 
 * @param <NameIdType> type of identifier object
 * 
 * @deprecated
 */
public class LegacyNameIdentifierGenerator<NameIdType extends SAMLObject>
        extends AbstractIdentifiableInitializableComponent implements NameIdentifierGenerator<NameIdType> {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(LegacyNameIdentifierGenerator.class);

    /** A predicate indicating whether the component applies to a request. */
    @Nonnull private Predicate<ProfileRequestContext> activationCondition;
    
    /** Lookup strategy for {@link AttributeContext}. */
    @Nonnull private Function<ProfileRequestContext, AttributeContext> attributeContextLookupStrategy;
    
    
    /**
     * Constructor.
     * 
     */
    protected LegacyNameIdentifierGenerator() {
        activationCondition = Predicates.alwaysTrue();

        // ProfileRequestContext -> RelyingPartyContext -> AttributeContext
        attributeContextLookupStrategy = new ChildContextLookup<>(AttributeContext.class).compose(
                new ChildContextLookup<>(RelyingPartyContext.class));
    }

    /**
     * Set an activation condition that determines whether to run or not.
     * 
     * @param condition an activation condition
     */
    public void setActivationCondition(@Nonnull final Predicate<ProfileRequestContext> condition) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        activationCondition = Constraint.isNotNull(condition, "Predicate cannot be null");
    }
    
    /**
     * Set the lookup strategy to locate the {@link AttributeContext} to pull from.
     * 
     * @param strategy lookup strategy
     */
    public void setAttributeContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, AttributeContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        attributeContextLookupStrategy = Constraint.isNotNull(strategy,
                "AttributeContext lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        DeprecationSupport.warn(ObjectType.CLASS, getClass().getName(), null, null);
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public NameIdType generate(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull @NotEmpty final String format) throws SAMLException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        Constraint.isNotNull(format, "Format cannot be null or empty");
        log.error("Legacy encoding as NameID not supported");
        return null;
    }
}