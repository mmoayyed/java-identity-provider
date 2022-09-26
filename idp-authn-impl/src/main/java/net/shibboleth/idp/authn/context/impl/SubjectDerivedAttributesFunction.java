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

package net.shibboleth.idp.authn.context.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.context.navigate.SubjectCanonicalizationContextSubjectLookupFunction;
import net.shibboleth.idp.authn.principal.IdPAttributePrincipal;
import net.shibboleth.shared.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;

/**
 * A Function which returns {@link IdPAttribute}s derived from the {@link java.security.Principal}s
 * associated with the request.
 */
public class SubjectDerivedAttributesFunction extends AbstractIdentifiableInitializableComponent implements
        Function<ProfileRequestContext,List<IdPAttribute>> {

    /** Logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SubjectDerivedAttributesFunction.class);

    /** Flag denoting whether plugin is being used for subject c14n or standard usage. */
    private boolean forCanonicalization;
    
    /** Strategy used to locate the {@link SubjectContext} to use. */
    @Nonnull private Function<ProfileRequestContext,SubjectContext> scLookupStrategy;

    /** Strategy used to locate the {@link Subject} to use. */
    @Nullable private Function<ProfileRequestContext,Subject> subjectLookupStrategy;

    /** Constructor. */
    public SubjectDerivedAttributesFunction() {
        scLookupStrategy = new ChildContextLookup<>(SubjectContext.class);
    }
    
    /**
     * Gets whether the definition is being used during Subject Canonicalization, causing
     * auto-installation of an alternate Subject lookup strategy.
     * 
     * @return whether the definition is being used during Subject Canonicalization
     */
    public boolean isForCanonicalization() {
        return forCanonicalization;
    }
    
    /**
     * Sets whether the definition is being used during Subject Canonicalization, causing
     * auto-installation of an alternate Subject lookup strategy.
     * 
     * @param flag flag to set
     */
    public void setForCanonicalization(final boolean flag) {
        checkSetterPreconditions();
        forCanonicalization = flag;
    }

    /**
     * Sets the strategy used to locate the {@link SubjectContext} associated with a given
     * {@link net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext}.
     * 
     * @param strategy strategy used to locate the {@link SubjectContext} associated with a given
     *            {@link net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext}
     */
    public void setSubjectContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SubjectContext> strategy) {
        checkSetterPreconditions();
        scLookupStrategy = Constraint.isNotNull(strategy, "SubjectContext lookup strategy cannot be null");
    }

    /**
     * Sets the strategy used to locate a {@link Subject} associated with a given
     * {@link net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext}.
     * 
     * @param strategy strategy used to locate a {@link Subject} associated with a given
     *            {@link net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext}
     */
    public void setSubjectLookupStrategy(@Nullable final Function<ProfileRequestContext,Subject> strategy) {
        checkSetterPreconditions();
        subjectLookupStrategy = strategy;
    }
    
    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (forCanonicalization && subjectLookupStrategy == null) {
            log.debug("{} Marked for use during canonicalication, auto-installing Subject lookup strategy",
                    getLogPrefix());
            subjectLookupStrategy = new SubjectCanonicalizationContextSubjectLookupFunction().compose(
                    new ChildContextLookup<>(SubjectCanonicalizationContext.class));
        }
    }

    /** {@inheritDoc} */
    @Nullable public List<IdPAttribute> apply(@Nullable final ProfileRequestContext prc) {
        
        Collection<Subject> subjects = Collections.emptyList();
        
        if (subjectLookupStrategy != null) {
            final Subject subject = subjectLookupStrategy.apply(prc);
            if (subject == null) {
                log.debug("{} No Subject returned from lookup, no attribute resolved", getLogPrefix());
                return null;
            }
            subjects = Collections.singletonList(subject);
        } else {
            final SubjectContext cs = scLookupStrategy.apply(prc);
            if (cs == null || cs.getSubjects().isEmpty()) {
                log.debug("{} No Subjects returned from SubjectContext lookup, no attribute resolved", getLogPrefix());
                return null;
            }
            subjects = cs.getSubjects();
        }
        
        final List<IdPAttribute> results = new ArrayList<>();

        for (final Subject subject : subjects) {
            results.addAll(subject.getPrincipals(IdPAttributePrincipal.class).stream()
                    .map(IdPAttributePrincipal::getAttribute)
                    .collect(Collectors.toUnmodifiableList()));
        }
        
        if (results.isEmpty()) {
            log.info("{} No attributes resolved", getLogPrefix());
            return null;
        }
        log.debug("{} Generated {} attributes", getLogPrefix(), results.size());
        
        return results;
    }

    /**
     * Produce a consistent log prefix.
     * 
     * @return a  consistent log prefix
     */
    private String getLogPrefix() {
        return "SubjectDerivedDataConnector " + getId();
    }
    
}