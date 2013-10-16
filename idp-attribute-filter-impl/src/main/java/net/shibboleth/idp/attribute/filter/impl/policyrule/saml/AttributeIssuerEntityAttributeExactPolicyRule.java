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

package net.shibboleth.idp.attribute.filter.impl.policyrule.saml;

import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.impl.policyrule.filtercontext.NavigationSupport;
import net.shibboleth.idp.attribute.resolver.AttributeRecipientContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;

import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Matcher that checks, via an exact match, if the attribute issuer contains an entity attribute with a given
 * value.<br/>
 * 
 * Most of the work is done in parent classes.
 */
public class AttributeIssuerEntityAttributeExactPolicyRule extends AbstractEntityAttributeExactPolicyRule {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeIssuerEntityAttributeExactPolicyRule.class);

    /** {@inheritDoc} */
    @Nullable protected EntityDescriptor getEntityMetadata(final AttributeFilterContext filterContext) {
        final AttributeResolutionContext resolver = NavigationSupport.locateResolverContext(filterContext);
        if (null == resolver) {
            log.warn("{} Could not locate resolver context", getLogPrefix());
            return null;
        }
        
        final AttributeRecipientContext recipient =
                NavigationSupport.locateRecipientContext(resolver);

        if (null == recipient) {
            log.warn("{} Could not locate recipient context", getLogPrefix());
            return null;
        }
        return recipient.getAttributeIssuerMetadata();
    }
}
