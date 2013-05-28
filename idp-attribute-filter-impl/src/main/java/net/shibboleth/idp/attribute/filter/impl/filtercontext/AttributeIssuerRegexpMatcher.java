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

package net.shibboleth.idp.attribute.filter.impl.filtercontext;

import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.impl.matcher.AbstractRegexpStringMatcher;
import net.shibboleth.idp.attribute.resolver.AttributeRecipientContext;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;

/**
 * Compare the attribute issuer's entity ID for this resolution with the provided name.
 */
public class AttributeIssuerRegexpMatcher extends AbstractRegexpStringMatcher {

    /** The logger. */
    private Logger log = LoggerFactory.getLogger(AttributeIssuerRegexpMatcher.class);

    /** Constructor. */
    public AttributeIssuerRegexpMatcher() {
        setPolicyPredicate(new Predicate<AttributeFilterContext>() {

            public boolean apply(@Nullable AttributeFilterContext input) {
                return doCompare(input);
            }});        
    }

    /** Compare the issuer from the provided context with the string.
     * @param filterContext the context
     * @return whether it matches
     */
    protected boolean doCompare(@Nullable AttributeFilterContext filterContext) {

        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        final AttributeRecipientContext recipient =
                NavigationHelper.locateRecipientContext(NavigationHelper.locateResolverContext(filterContext));
        final String issuer = recipient.getAttributeIssuerID();

        if (null == issuer) {
            log.warn("{} No attribute issuer found for comparison", getLogPrefix());
            return false;
        }
        log.debug("{} found attribute issuer: ", getLogPrefix(), issuer);

        return regexpCompare(issuer);
    }
}
