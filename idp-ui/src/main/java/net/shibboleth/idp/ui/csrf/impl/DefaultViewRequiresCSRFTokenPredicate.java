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

import java.util.function.Predicate;
import javax.annotation.Nonnull;

import org.springframework.webflow.execution.RequestContext;

import net.shibboleth.idp.ui.csrf.BaseCSRFTokenPredicate;

/**
 * <p>Default {@link Predicate} for determining if a CSRF token is required for the given
 * request context. Guaranteed to be in a view-state when tested by the 
 * {@link CSRFTokenFlowExecutionListener}.</p>
 * 
 * <p>Returns true if the view-state requires a CSRF token. More specifically,
 * returns true iff the state definition does not contain a 
 * <code>{@value BaseCSRFTokenPredicate#CSRF_EXCLUDED_ATTRIBUTE_NAME}</code> metadata attribute with a 
 * value of <code>{@literal true}</code>.</p>
 */
public class DefaultViewRequiresCSRFTokenPredicate
            extends BaseCSRFTokenPredicate implements Predicate<RequestContext>{
    
    /** {@inheritDoc} */
    public boolean test(@Nonnull final RequestContext context) {
        
        final boolean excluded = safeGetBooleanStateAttribute(context.getCurrentState(),
                CSRF_EXCLUDED_ATTRIBUTE_NAME,false);
        //if NOT excluded from CSRF checks, return true, else return false.
        return !excluded;
    }

}
