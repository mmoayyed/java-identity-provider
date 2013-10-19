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
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeRecipientContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An attribute definition which returns an attribute with a single value - the AuthenticationMethod.
 */
public class PrincipalAuthenticationMethodAttributeDefinition extends BaseAttributeDefinition {

    /** Log. */
    private final Logger log = LoggerFactory.getLogger(PrincipalAuthenticationMethodAttributeDefinition.class);

    /** {@inheritDoc} */
    @Nullable protected IdPAttribute doAttributeDefinitionResolve(
            @Nonnull AttributeResolutionContext resolutionContext) throws ResolutionException {
        
        final AttributeRecipientContext attributeRecipientContext =
                resolutionContext.getSubcontext(AttributeRecipientContext.class);

        if (null == attributeRecipientContext) {
            throw new ResolutionException(getLogPrefix() + " no attribute recipient context provided ");
        }

        
        final String method = StringSupport.trimOrNull(attributeRecipientContext.getPrincipalAuthenticationMethod());

        if (null == method) {
            log.info("{} null or empty method was returned", getLogPrefix());
            return null;
        }

        final IdPAttribute attribute = new IdPAttribute(getId());
        attribute.setValues(Collections.singleton((AttributeValue) new StringAttributeValue(method)));
        return attribute;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
    }
}
