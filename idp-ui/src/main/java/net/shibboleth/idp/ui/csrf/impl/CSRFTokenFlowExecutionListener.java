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

package net.shibboleth.idp.ui.csrf.impl;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.definition.StateDefinition;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.FlowExecutionListener;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.View;

import net.shibboleth.idp.ui.csrf.CSRFToken;
import net.shibboleth.idp.ui.csrf.CSRFTokenManager;
import net.shibboleth.idp.ui.csrf.InvalidCSRFTokenException;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;


/**
 * A flow execution lifecycle listener that, if enabled:
 * <ul>
 *      <li>Sets an anti-CSRF token into the view-scope map on rendering of a suitable view-state</li>
 *      <li>Checks the CSRF token in a HTTP request matches that stored in the view-scope map when a suitable 
 *       view-state event occurs.</li>
 * </ul>
 */
public class CSRFTokenFlowExecutionListener extends AbstractInitializableComponent implements FlowExecutionListener {
    
    /** The name of the view scope parameter that holds the CSRF token. */
    @Nonnull public static final String CSRF_TOKEN_VIEWSCOPE_NAME = "csrfToken";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(CSRFTokenFlowExecutionListener.class);    
 
    /** Should the request context and event be checked for a valid (matching) CSRF token? */
    @NonnullAfterInit private BiPredicate<RequestContext,Event> eventRequiresCSRFTokenValidationPredicate;
    
    /** Does the view being rendered require a CSRF token to be set.*/
    @NonnullAfterInit private Predicate<RequestContext> viewRequiresCSRFTokenPredicate;
    
    /** Is this listener enabled? */
    @Nonnull private boolean enabled;

    /** The CSRF token manager for getting and validating tokens. */    
    @NonnullAfterInit private CSRFTokenManager csrfTokenManager;
    
    
    /** Constructor. */
    public CSRFTokenFlowExecutionListener() {
        enabled = false;
    }
    
    /**
     * Set whether CSRF protection is globally enabled or disabled. 
     * 
     * @param enable enabled/disable CSRF protection (default is {@literal false}).
     */
    public void setEnabled(@Nonnull final boolean enable) {
        throwSetterPreconditionExceptions();
        enabled = enable;
    }
    
    /**
     *  Sets the request context condition to determine if a CSRF token should be added to the view-scope.
     *  
     * @param condition the condition to apply.
     */
    public void setViewRequiresCSRFTokenPredicate(@Nonnull final Predicate<RequestContext> condition) {
        throwSetterPreconditionExceptions();
        viewRequiresCSRFTokenPredicate = Constraint.isNotNull(condition, 
                        "Does view require CSRF token predicate can not be null");
    }
    
    /**
     * Set the request context and event condition to determine if a CSRF token should be validated.
     *  
     * @param condition the condition to apply
     */
    public void setEventRequiresCSRFTokenValidationPredicate(
            @Nonnull final BiPredicate<RequestContext,Event> condition) {
        throwSetterPreconditionExceptions();
        eventRequiresCSRFTokenValidationPredicate = Constraint.isNotNull(condition, 
                "Validate CSRF token condition cannot be null");
    }
    

    /**
     * Sets the CSRF token manager.
     * 
     * @param tokenManager the CSRF token manager.
     */
    public void setCsrfTokenManager(@Nonnull final CSRFTokenManager tokenManager) {    
        throwSetterPreconditionExceptions();
        csrfTokenManager = Constraint.isNotNull(tokenManager, "CSRF Token manager can not be null");
    }


    /**
     * Generates a CSRF token and adds it to the request context view scope, overwriting any existing token. 
     * 
     * {@inheritDoc}
     */
    @Override
    public void viewRendering(@Nonnull final RequestContext context, @Nonnull final View view, 
            @Nonnull final StateDefinition viewState) {        
      
        //state here should always be a view-state, but guard anyway.
        if (enabled && viewState.isViewState() && viewRequiresCSRFTokenPredicate.test(context)) {
            context.getViewScope().put(CSRF_TOKEN_VIEWSCOPE_NAME, csrfTokenManager.generateCSRFToken());            
        }

    }

    /**
     * Checks the CSRF token in the HTTP request matches that stored in the request context viewScope.
     * 
     * <p>Only applies if the listener is enabled, the current state is a view-state, and the request context and 
     * event match the <code>eventRequiresCSRFTokenValidationPredicate</code> condition.</p>
     * 
     * <p>Invalid tokens - those not found or not matching - are signalled by throwing a 
     * {@link InvalidCSRFTokenException}.</p>
     * 
     * {@inheritDoc}
     */
    @Override
    public void eventSignaled(@Nonnull final RequestContext context, @Nonnull final Event event) {

        //always make sure listener is enabled and the current state is an active view-state.
        if (enabled && context.inViewState() && 
                eventRequiresCSRFTokenValidationPredicate.test(context,event)){            
           
            final String stateId = context.getCurrentState().getId();
            
            log.trace("Event '{}' signaled from view '{}' requires a CSRF token", event.getId(),stateId);

            final Object storedCsrfTokenObject = context.getViewScope().get(CSRF_TOKEN_VIEWSCOPE_NAME);
            
            if (storedCsrfTokenObject == null || (!(storedCsrfTokenObject instanceof CSRFToken))) {
                log.warn("CSRF token is required but was not found in the view-scope; for "
                        + "view-state '{}' and event '{}'.",stateId,event.getId());
                throw new InvalidCSRFTokenException(context.getActiveFlow().getId(), stateId,
                        "Invalid CSRF token");               
            }

            final CSRFToken storedCsrfToken = (CSRFToken) storedCsrfTokenObject;

            //external context and request parameter map should never be null.
            final Object csrfTokenFromRequest =
                    context.getExternalContext().getRequestParameterMap().get(storedCsrfToken.getParameterName());

            if (csrfTokenFromRequest == null || !(csrfTokenFromRequest instanceof String)) {    
                log.warn("CSRF token is required but was not found in the request; for "
                        + "view-state '{}' and event '{}'.",stateId,event.getId());
                throw new InvalidCSRFTokenException(context.getActiveFlow().getId(), stateId,
                        "Invalid CSRF token");
            }

            log.trace("Stored (viewScoped) CSRF Token '{}', CSRF Token in HTTP request '{}'", 
                    storedCsrfToken.getToken(), csrfTokenFromRequest);

            if (!csrfTokenManager.isValidCSRFToken(storedCsrfToken, (String) csrfTokenFromRequest)) {
                log.warn("CSRF token in the request did not match that stored in the view-scope; for "
                        + "view-state '{}' and event '{}'.",stateId,event.getId());
                throw new InvalidCSRFTokenException(context.getActiveFlow().getId(), stateId,
                        "Invalid CSRF token");
            }

        }
    } 


    /** {@inheritDoc} */
    public void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (csrfTokenManager == null ) {
            throw new ComponentInitializationException("CSRF token manager can not be null");
        }
        if (viewRequiresCSRFTokenPredicate==null) {
            throw new ComponentInitializationException("View requires CSRF token predicate can not be null");
        }
        if (eventRequiresCSRFTokenValidationPredicate==null) {
            throw new ComponentInitializationException("Event requires CSRF token validation predicate can "
                    + "not be null");
        }
        
    }

}
