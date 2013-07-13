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

package net.shibboleth.idp.attribute.filter.impl.policyrule.filtercontext;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.impl.policyrule.AbstractStringPolicyRule;
import net.shibboleth.idp.attribute.resolver.AttributeRecipientContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compare the authentication method for this resolution with the provided string.
 */
public class AuthenticationMethodPolicyRule extends AbstractStringPolicyRule {

    /** The logger. */
    private final Logger log = LoggerFactory.getLogger(AuthenticationMethodPolicyRule.class);

    /**
     * Compare the authentication method for this resolution with the provided string.
     * 
     * @param filterContext the context
     * @return whether it matches
     * 
     *         {@inheritDoc}
     */
    public Tristate matches(@Nonnull AttributeFilterContext filterContext) {

        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        final AttributeResolutionContext resolver = NavigationHelper.locateResolverContext(filterContext);
        if (null == resolver) {
            log.warn("{} Could not locate resolver context", getLogPrefix());
            return Tristate.FAIL;
        }
        
        final AttributeRecipientContext recipient =
                NavigationHelper.locateRecipientContext(resolver);
        if (null == recipient) {
            log.warn("{} Could not locate recipient context", getLogPrefix());
            return Tristate.FAIL;
        }
        
        final String method = recipient.getPrincipalAuthenticationMethod();
        if (null == method) {
            log.warn("{} No authetication method found for comparison", getLogPrefix());
            return Tristate.FAIL;
        }
        log.debug("{} found authentication method: ", getLogPrefix(), method);

        return stringCompare(method);
    }

}
