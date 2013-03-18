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

package net.shibboleth.idp.attribute.filtering.impl.filtercontext;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.impl.matcher.AbstractStringMatchFunctor;
import net.shibboleth.idp.attribute.resolver.AttributeRecipientContext;

/**
 * Compare the principal name for this resolution with the provided name.
 */
public class PrincipalNameMatcher extends AbstractStringMatchFunctor {

    /** The logger. */
    private Logger log = LoggerFactory.getLogger(PrincipalNameMatcher.class);

    /** {@inheritDoc} */
    public boolean apply(@Nullable AttributeFilterContext filterContext) {
        final AttributeRecipientContext recipient =
                NavigationHelper.locateRecipientContext(NavigationHelper.locateResolverContext(filterContext));
        final String principal = recipient.getPrincipal();

        if (null == principal) {
            log.warn("No principal found for comparison");
            return false;
        }

        return stringCompare(principal);
    }
}
