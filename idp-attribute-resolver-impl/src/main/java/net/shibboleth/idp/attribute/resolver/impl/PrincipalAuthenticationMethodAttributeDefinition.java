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

package net.shibboleth.idp.attribute.resolver.impl;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;

import org.opensaml.util.StringSupport;
import org.opensaml.util.collections.CollectionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Attribute that exposes the user's <em>current</em> principal authentication method as an attribute.<bt/> See
 * {@link AbstractPrincipalAttributeDefinition} for a detailed description of the context relationships expected.
 */
@ThreadSafe
public class PrincipalAuthenticationMethodAttributeDefinition extends AbstractPrincipalAttributeDefinition {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(PrincipalAuthenticationMethodAttributeDefinition.class);

    /** {@inheritDoc} */
    protected Attribute<?> doAttributeResolution(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {

        final String authMethod =
                StringSupport.trimOrNull(getAuthenticationEvent(resolutionContext).getAuthenticationWorkflow());

        if (null == authMethod) {
            log.debug("Principal Authentication Method Attribute Definition " + getId() + ": method was empty");
            return null;
        }

        final Attribute<String> result = new Attribute<String>(getId());
        result.setValues(CollectionSupport.toList(authMethod));
        return result;
    }

}
