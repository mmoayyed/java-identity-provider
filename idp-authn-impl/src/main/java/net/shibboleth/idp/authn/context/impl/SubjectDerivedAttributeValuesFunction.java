/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.context.navigate.SubjectCanonicalizationContextSubjectLookupFunction;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * A Function which returns {@link IdPAttributeValue}s derived from the {@link java.security.Principal}s
 * associated with the request. The precise values are determined by an injected {@link Function}.
 */
public class SubjectDerivedAttributeValuesFunction extends AbstractIdentifiableInitializableComponent implements
        Function<ProfileRequestContext,List<IdPAttributeValue>> {

    /** Logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SubjectDerivedAttributeValuesFunction.class);

    /** Flag denoting whether plugin is being used for subject c14n or standard usage. */
    private boolean forCanonicalization;
    
    /** Strategy used to locate the {@link SubjectContext} to use. */
    @Nonnull private Function<ProfileRequestContext,SubjectContext> scLookupStrategy;

    /**
     * {@link Function} used to generate the values associated with a {@link Principal}
     * 
     * The {@link Function} returns null or an empty list if the {@link Principal} isn't relevant.
     */
    @NonnullAfterInit private Function<Principal,List<IdPAttributeValue>> attributeValuesFunction;

    /** Strategy used to locate the {@link Subject} to use. */
    @Nullable private Function<ProfileRequestContext,Subject> subjectLookupStrategy;

    /** Constructor. */
    public SubjectDerivedAttributeValuesFunction() {
        scLookupStrategy = new ChildContextLookup<>(SubjectContext.class);
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
     * Sets the function to extract attribute values from a {@link Principal}.
     * 
     * @param strategy strategy function
     */
    public void setAttributeValuesFunction(@Nonnull final Function<Principal,List<IdPAttributeValue>> strategy) {
        checkSetterPreconditions();
        attributeValuesFunction = Constraint.isNotNull(strategy, "Attribute value lookup strategy cannot be null");
    }

    /**
     * Sets the strategy used to locate a {@link Subject} associated with a given
     * {@link net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext}.
     * 
     * @param strategy strategy used to locate a {@link Subject} associated with a given
     *            {@link net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext}
     */
    public void setSubjectLookupStrategy(
            @Nullable final Function<ProfileRequestContext,Subject> strategy) {
        checkSetterPreconditions();
        subjectLookupStrategy = strategy;
    }
    
    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (attributeValuesFunction == null) {
            throw new ComponentInitializationException("Attribute value lookup strategy cannot be null");
        }
        
        if (forCanonicalization && subjectLookupStrategy == null) {
            log.debug("{} Marked for use during canonicalication, auto-installing Subject lookup strategy",
                    getLogPrefix());
            subjectLookupStrategy = new SubjectCanonicalizationContextSubjectLookupFunction().compose(
                    new ChildContextLookup<>(SubjectCanonicalizationContext.class));
        }
    }

    /** {@inheritDoc} */
    @Nullable @Unmodifiable @NotLive public List<IdPAttributeValue> apply(@Nullable final ProfileRequestContext prc) {
        
        Collection<Subject> subjects = CollectionSupport.emptyList();
        
        if (subjectLookupStrategy != null) {
            final Subject subject = subjectLookupStrategy.apply(prc);
            if (subject == null) {
                log.debug("{} No Subject returned from lookup, no attribute resolved", getLogPrefix());
                return null;
            }
            subjects = CollectionSupport.singletonList(subject);
        } else {
            final SubjectContext cs = scLookupStrategy.apply(prc);
            if (cs == null || cs.getSubjects().isEmpty()) {
                log.debug("{} No Subjects returned from SubjectContext lookup, no attribute resolved", getLogPrefix());
                return null;
            }
            subjects = cs.getSubjects();
        }
        
        final List<IdPAttributeValue> results = new ArrayList<>();

        for (final Subject subject : subjects) {
            for (final Principal principal : subject.getPrincipals()) {
                final List<IdPAttributeValue> values = attributeValuesFunction.apply(principal);
                if (null != values && !values.isEmpty()) {
                    results.addAll(values);
                }
            }
        }
        if (results.isEmpty()) {
            log.info("{} Generated no values, no attribute resolved", getLogPrefix());
            return null;
        }
        log.debug("{} Generated {} values", getLogPrefix(), results.size());
        log.trace("{} Values:", getLogPrefix(), results);
        return results;
    }

    /**
     * Produce a consistent log prefix.
     * 
     * @return a  consistent log prefix
     */
    private String getLogPrefix() {
        return "SubjectDerivedAttributeDefinition " + getId();
    }
    
}