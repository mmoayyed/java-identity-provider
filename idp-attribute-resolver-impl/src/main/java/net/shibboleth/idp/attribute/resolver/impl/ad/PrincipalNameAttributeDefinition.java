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

package net.shibboleth.idp.attribute.resolver.impl.ad;

import java.util.Collections;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AbstractAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.AttributeRecipientContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * An attribute definition which returns an attribute with a single value - the principal.
 */
public class PrincipalNameAttributeDefinition extends AbstractAttributeDefinition {

    @Override @Nonnull protected IdPAttribute doAttributeDefinitionResolve(
            @Nonnull final AttributeResolutionContext resolutionContext) throws ResolutionException {
        final AttributeRecipientContext attributeRecipientContext =
                resolutionContext.getSubcontext(AttributeRecipientContext.class);

        if (null == attributeRecipientContext) {
            throw new ResolutionException(getLogPrefix() + " no attribute recipient context provided ");
        }

        final String principalName = StringSupport.trimOrNull(attributeRecipientContext.getPrincipal());
        if (null == principalName) {
            throw new ResolutionException(getLogPrefix() + " provided prinicipal name was empty");
        }

        final IdPAttribute attribute = new IdPAttribute(getId());
        attribute.setValues(Collections.singleton(new StringAttributeValue(principalName)));
        return attribute;
    }
}
