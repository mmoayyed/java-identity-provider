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
import javax.annotation.Nonnull;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import net.shibboleth.idp.ui.csrf.BaseCSRFTokenPredicate;


/**
 * <p>Default {@link BiPredicate} for determining if CSRF token validation should occur 
 * from a compatible request context and event. Guaranteed to be in a view-state when tested by the 
 * {@link CSRFTokenFlowExecutionListener}.</p>
 * 
 * 
 * <p>Returns true if the view-state and event requires CSRF token validation - as determined by checking
 * the request context against the {@link BaseCSRFTokenPredicate#isStateIncluded()} method.</p>
 * 
 * <p>Note, as Spring Webflow does not distinguish between HTTP request methods, checking only for POST requests would
 * lead to a bypass using a GET request. Hence HTTP method is not checked.</p>
 * 
 */
public class DefaultEventRequiresCSRFTokenValidationPredicate 
            extends BaseCSRFTokenPredicate implements BiPredicate<RequestContext,Event>{

   /** {@inheritDoc} */
   public boolean test(@Nonnull final RequestContext context, @Nonnull final Event event) {
       
       //if state is not included by configuration, return false.
       if (!isStateIncluded(context)) {
           return false;
       }
       
       return true;
   }

   
}
