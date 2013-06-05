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

package net.shibboleth.idp.attribute.filter.impl.saml;

import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.impl.filtercontext.NavigationHelper;
import net.shibboleth.idp.attribute.resolver.AttributeRecipientContext;

import org.opensaml.saml.saml2.metadata.EntityDescriptor;

/**
 * Matcher functor that checks, via matching against a regular expression, if the attribute requester contains an entity
 * attribute with a given value.
 */
public class AttributeIssuerEntityAttributeRegexMatcher extends AbstractEntityAttributeRegexMatcher {

    /** {@inheritDoc} */
    protected EntityDescriptor getEntityMetadata(AttributeFilterContext filterContext) {
        final AttributeRecipientContext recipient =
                NavigationHelper.locateRecipientContext(NavigationHelper.locateResolverContext(filterContext));
        return recipient.getAttributeIssuerMetadata();
    }

}
