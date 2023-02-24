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

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import net.shibboleth.idp.ui.csrf.BaseCSRFTokenPredicate;


/**
 * <p>Default {@link BiPredicate} for determining if CSRF token validation should occur 
 * from a compatible request context and event. Guaranteed to be in a view-state when tested by the 
 * {@link CSRFTokenFlowExecutionListener}.</p>
 * 
 * 
 * <p>Returns true if the view-state and event requires CSRF token validation.  More specifically,
 * returns true iff the state definition does not contain a 
 * <code>{@value BaseCSRFTokenPredicate#CSRF_EXCLUDED_ATTRIBUTE_NAME}</code> metadata attribute with a 
 * value of <code>{@literal true}</code>.</p>
 * 
 * <p>Note, as Spring Webflow does not distinguish between HTTP request methods, checking only for 
 * POST requests would lead to a bypass using a GET request. Hence HTTP method is not checked.</p>
 * 
 */
public class DefaultEventRequiresCSRFTokenValidationPredicate 
            extends BaseCSRFTokenPredicate implements BiPredicate<RequestContext,Event>{
    
   /** {@inheritDoc} */
   public boolean test(final RequestContext context, final Event event) {
       
       assert context != null && event != null;
       final boolean excluded = safeGetBooleanStateAttribute(context.getCurrentState(),
               CSRF_EXCLUDED_ATTRIBUTE_NAME,false);
       //if NOT excluded from CSRF checks, return true, else return false.
       return !excluded;
   }

   
}
