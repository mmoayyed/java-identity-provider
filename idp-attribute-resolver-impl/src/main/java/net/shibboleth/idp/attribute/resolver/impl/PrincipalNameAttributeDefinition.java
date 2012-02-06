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

import java.util.Collections;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.session.AuthenticationEvent;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Attribute definition that exposes the user's <em>current</em> principal name as an attribute. <br/>
 * See {@link AbstractPrincipalAttributeDefinition} for a detailed description of the context relationships expected.
 */
@ThreadSafe
public class PrincipalNameAttributeDefinition extends AbstractPrincipalAttributeDefinition {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(PrincipalNameAttributeDefinition.class);

    /** {@inheritDoc} */
    protected Attribute<?> doAttributeDefinitionResolve(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {

        final AuthenticationEvent event = getAuthenticationEvent(resolutionContext);

        if (null == event) {
            return null;
        }
        final String principalName = StringSupport.trimOrNull(event.getPrincipal().getName());

        if (null == principalName) {
            log.debug("Attribute Definition {}: principal name was emtpy", getId());
            return null;
        }

        final Attribute<String> result = new Attribute<String>(getId());
        result.setValues(Collections.singleton(principalName));
        return result;
    }
}
