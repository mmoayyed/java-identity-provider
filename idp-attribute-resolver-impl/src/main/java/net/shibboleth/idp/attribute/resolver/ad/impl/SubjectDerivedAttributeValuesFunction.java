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

package net.shibboleth.idp.attribute.resolver.ad.impl;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Function which returns {@link IdPAttributeValue}s derived from the {@link java.security.Principal}s
 * associated with the request.  The precise values are determined by an injected {@link Function}.
 */
public class SubjectDerivedAttributeValuesFunction extends AbstractIdentifiableInitializableComponent implements
        Function<ProfileRequestContext,List<IdPAttributeValue<?>>> {

    /** Logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ContextDerivedAttributeDefinition.class);

    /** Strategy used to locate the {@link SubjectContext} to use. */
    @Nonnull private Function<ProfileRequestContext,SubjectContext> scLookupStrategy;

    /**
     * {@link Function} used to generate the values associated with a {@link Principal}
     * 
     * The {@link Function} returns null or an empty list if the {@link Principal} isn't relevant.
     */
    @Nonnull private Function<Principal,List<IdPAttributeValue<?>>> attributesValueFunction;

    /** Constructor. */
    public SubjectDerivedAttributeValuesFunction() {
        scLookupStrategy = new ChildContextLookup<ProfileRequestContext,SubjectContext>(SubjectContext.class);
    }

    /**
     * Set the strategy used to locate the {@link SubjectContext} associated with a given
     * {@link net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext}.
     * 
     * @param strategy strategy used to locate the {@link SubjectContext} associated with a given
     *            {@link net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext}
     */
    public void
            setSubjectContextLookupStrategy(@Nonnull final Function<ProfileRequestContext,SubjectContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        scLookupStrategy = Constraint.isNotNull(strategy, "SubjectContext lookup strategy cannot be null");
    }

    /**
     * Sets the attribute value function.
     * 
     * @param engine what to set.
     */
    public void setAttributeValuesFunction(@Nonnull final Function<Principal,List<IdPAttributeValue<?>>> engine) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        attributesValueFunction = Constraint.isNotNull(engine, "Attribute Engine cannot be null");
    }

    /** {@inheritDoc} */
    @Nullable public List<IdPAttributeValue<?>> apply(@Nullable final ProfileRequestContext prc) {
        final SubjectContext cs = scLookupStrategy.apply(prc);
        final List<IdPAttributeValue<?>> results = new ArrayList<>(1);

        for (final Subject subject : cs.getSubjects()) {
            for (final Principal principal : subject.getPrincipals()) {
                final List<IdPAttributeValue<?>> values = attributesValueFunction.apply(principal);
                if ((null != values) && !values.isEmpty()) {
                    results.addAll(values);
                }
            }
        }
        if (results.isEmpty()) {
            log.info("{} generated no values, attribute no resolved.", getLogPrefix());
            return null;
        }
        log.debug("{} Generated {} values.", getLogPrefix(), results.size());
        log.trace("{} Values:", getLogPrefix(), results);
        return results;
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        Constraint.isNotNull(scLookupStrategy, "SubjectContext lookup strategy cannot be null");
        Constraint.isNotNull(attributesValueFunction, "Attribute Engine cannot be null");

        super.doInitialize();
    }

    /** Produce a consistent log prefix.
     * @return a  consistent log prefix
     */
    private String getLogPrefix() {
        return "SubjectDerivedAttributeDefinition" + getId();
    }
}