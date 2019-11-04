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

package net.shibboleth.idp.attribute.resolver.dc.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.security.auth.Subject;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.AbstractDataConnector;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.context.navigate.SubjectCanonicalizationContextSubjectLookupFunction;
import net.shibboleth.idp.authn.principal.IdPAttributePrincipal;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * A {@link net.shibboleth.idp.attribute.resolver.DataConnector} that extracts all
 * {@link IdPAttributePrincipal} objects from the {@link Subject} objects associated
 * with the request.
 * 
 * @since 4.0.0
 */
@ThreadSafe
public class SubjectDataConnector extends AbstractDataConnector {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SubjectDataConnector.class);
    
    /** Flag denoting whether plugin is being used for subject c14n or standard usage. */
    private boolean forCanonicalization;

    /** Strategy used to locate the {@link SubjectContext} to use. */
    @Nonnull private Function<ProfileRequestContext,SubjectContext> scLookupStrategy;
    
    /** Strategy used to locate the {@link Subject} to use. */
    @Nullable private Function<ProfileRequestContext,Subject> subjectLookupStrategy;
    
    /** Controls handling of empty results. */
    private boolean noResultIsError;
    
    /** Constructor. */
    public SubjectDataConnector() {
        scLookupStrategy = new ChildContextLookup<>(SubjectContext.class);
    }
    
    /**
     * Gets whether the connector is being used during Subject Canonicalization, causing
     * auto-installation of an alternate Subject lookup strategy.
     * 
     * @return whether connector is being used during c14n
     */
    public boolean isForCanonicalization() {
        return forCanonicalization;
    }
    
    /**
     * Sets whether the connector is being used during Subject Canonicalization, causing
     * auto-installation of an alternate Subject lookup strategy.
     * 
     * <p>Defaults to false.</p>
     * 
     * @param flag flag to set
     */
    public void setForCanonicalization(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        forCanonicalization = flag;
    }
    
    /**
     * Sets the strategy used to locate the {@link SubjectContext} associated with a given
     * {@link AttributeResolutionContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setSubjectContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SubjectContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        scLookupStrategy = Constraint.isNotNull(strategy, "SubjectContext lookup strategy cannot be null");
    }
    
    /**
     * Sets the strategy used to locate a {@link Subject} associated with a given
     * {@link AttributeResolutionContext}.
     * 
     * @param strategy strategy used to locate a {@link Subject} associated with a given
     *            {@link AttributeResolutionContext}
     */
    public void setSubjectLookupStrategy(@Nullable final Function<ProfileRequestContext,Subject> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        subjectLookupStrategy = strategy;
    }
    
    /**
     * Gets whether obtaining no results should be treated as an error.
     * 
     * @return whether obtaining no results should be treated as an error
     */
    public boolean isNoResultIsError() {
        return noResultIsError;
    }
    
    /**
     * Sets whether obtaining no results should be treated as an error.
     * 
     * <p>Defaults to false.</p>
     * 
     * @param flag flag to set
     */
    public void setNoResultIsError(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        noResultIsError = flag;
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
    @Override
    @Nonnull protected Map<String,IdPAttribute> doDataConnectorResolve(
            @Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        
        Collection<Subject> subjects = Collections.emptyList();
        
        if (subjectLookupStrategy != null) {
            final Subject subject = subjectLookupStrategy.apply(getProfileContextStrategy().apply(resolutionContext));
            if (subject == null) {
                log.debug("{} No Subject returned from lookup, no attributes resolved", getLogPrefix());
                return Collections.emptyMap();
            }
            subjects = Collections.singletonList(subject);
        } else {
            final SubjectContext cs = scLookupStrategy.apply(getProfileContextStrategy().apply(resolutionContext));
            if (cs == null || cs.getSubjects().isEmpty()) {
                log.debug("{} No Subjects returned from SubjectContext lookup, no attributes resolved", getLogPrefix());
                return Collections.emptyMap();
            }
            subjects = cs.getSubjects();
        }
        
        final Map<String,IdPAttribute> results = new HashMap<>();
        
        for (final Subject subject : subjects) {
            for (final IdPAttributePrincipal principal : subject.getPrincipals(IdPAttributePrincipal.class)) {
                results.put(principal.getName(), principal.getAttribute());
            }
        }

        if (results.isEmpty()) {
            if (noResultIsError) {
                throw new ResolutionException("No IdPAttributePrincipal objects found");
            }
            log.debug("{} Obtained no attributes from Subjects for principal '{}'", getLogPrefix(),
                    resolutionContext.getPrincipal());
            return Collections.emptyMap();
        }
        
        log.debug("{} Extracted {} IdPAttribute(s)", getLogPrefix(), results.size());
        log.trace("{} Extracted atttribute IDs: {}", getLogPrefix(), results.keySet());
        
        return results;
    }
    
}