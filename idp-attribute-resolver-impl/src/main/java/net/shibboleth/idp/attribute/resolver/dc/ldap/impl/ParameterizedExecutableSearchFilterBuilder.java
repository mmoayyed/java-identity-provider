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

package net.shibboleth.idp.attribute.resolver.dc.ldap.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.ldaptive.SearchFilter;
import org.slf4j.LoggerFactory;

/**
 * An {@link net.shibboleth.idp.attribute.resolver.dc.impl.ExecutableSearchBuilder} that generates the search filter to
 * be executed by evaluating a parameterized filter string against the currently resolved attributes within a
 * {@link AttributeResolutionContext}.
 */
public class ParameterizedExecutableSearchFilterBuilder extends AbstractExecutableSearchFilterBuilder {

    /** LDAP search filter. */
    @NonnullAfterInit private String searchFilter;

    /**
     * Constructor.
     * 
     * @param filter used for the LDAP search
     * @deprecated - use the property setters
     */
    @Deprecated public ParameterizedExecutableSearchFilterBuilder(@Nonnull final String filter) {
        LoggerFactory.getLogger(ParameterizedExecutableSearchFilterBuilder.class).warn("Using Deprecated Constructor");
        setSearchFilter(filter);
        try {
            initialize();
        } catch (final ComponentInitializationException e) {
            throw new ConstraintViolationException("Invalid parameterization to deprecated structure");
        }
    }
    
    /**
     * Constructor.  
     *
     */
    public ParameterizedExecutableSearchFilterBuilder() {
        
    }
    
    /** Set the filter used for the LDAP search.
     * @param filter used for the LDAP search
     */
    public void setSearchFilter(@Nonnull final String filter) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        searchFilter = Constraint.isNotNull(filter, "Search filter can not be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        Constraint.isNotNull(searchFilter, "Search filter can not be null");
        super.doInitialize();
    }

    /** {@inheritDoc} */
    @Override public ExecutableSearchFilter build(@Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final Map<String, List<IdPAttributeValue<?>>> dependencyAttributes) throws ResolutionException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        final SearchFilter sf = new SearchFilter(searchFilter);
        sf.setParameter("principalName", resolutionContext.getPrincipal());
        if (dependencyAttributes != null && !dependencyAttributes.isEmpty()) {
            for (final Map.Entry<String, List<IdPAttributeValue<?>>> entry : dependencyAttributes.entrySet()) {
                int i = 0;
                for (final IdPAttributeValue<?> value : entry.getValue()) {
                    if (i == 0) {
                        sf.setParameter(String.format("%s", entry.getKey(), i), value.getValue());
                    }
                    sf.setParameter(String.format("%s[%s]", entry.getKey(), i++), value.getValue());
                }
            }
        }
        return super.build(sf);
    }
    
}