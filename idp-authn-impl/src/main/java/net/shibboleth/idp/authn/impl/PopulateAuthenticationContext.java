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

package net.shibboleth.idp.authn.impl;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicateFactoryRegistry;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

/**
 * An action that populates an {@link AuthenticationContext} with the {@link AuthenticationFlowDescriptor}
 * objects configured into the IdP, and optionally a customized {@link PrincipalEvalPredicateFactoryRegistry}.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class, false) != null</pre>
 * @post The AuthenticationContext is modified as above.
 */
public class PopulateAuthenticationContext extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateAuthenticationContext.class);
    
    /** The flows to make available for possible use. */
    @Nonnull @NonnullElements private Collection<AuthenticationFlowDescriptor> availableFlows;
    
    /** The registry of predicate factories for custom principal evaluation. */
    @Nullable private PrincipalEvalPredicateFactoryRegistry evalRegistry;

    /** Constructor. */
    PopulateAuthenticationContext() {
        availableFlows = new ArrayList<>();
    }
    
    /**
     * Set the flows available for possible use.
     * 
     * @param flows the flows available for possible use
     */
    public void setAvailableFlows(@Nonnull @NonnullElements final Collection<AuthenticationFlowDescriptor> flows) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(flows, "Flow collection cannot be null");
        
        availableFlows.clear();
        availableFlows.addAll(Collections2.filter(flows, Predicates.notNull()));
    }
    
    /**
     * Get the registry of predicate factories for custom principal evaluation.
     * 
     * @return predicate factory registry
     */
    @Nonnull public PrincipalEvalPredicateFactoryRegistry getPrincipalPredicateFactoryEvalRegistry() {
        return evalRegistry;
    }
    
    /**
     * Set the registry of predicate factories for custom principal evaluation.
     * 
     * @param registry predicate factory registry
     */
    public void setPrincipalEvalPredicateFactoryRegistry(
            @Nonnull final PrincipalEvalPredicateFactoryRegistry registry) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        evalRegistry = Constraint.isNotNull(registry, "PrincipalEvalPredicateFactoryRegistry cannot be null");
    }
        
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {

        if (evalRegistry != null) {
            log.debug("{} Installing custom PrincipalEvalPredicateFactoryRegistry into AuthenticationContext",
                    getLogPrefix());
            authenticationContext.setPrincipalEvalPredicateFactoryRegistry(evalRegistry);
        }
        
        log.debug("{} Installing {} authentication flows into AuthenticationContext", getLogPrefix(),
                availableFlows.size());
        for (AuthenticationFlowDescriptor desc : availableFlows) {
            authenticationContext.getPotentialFlows().put(desc.getId(), desc);
        }
    }
    
}