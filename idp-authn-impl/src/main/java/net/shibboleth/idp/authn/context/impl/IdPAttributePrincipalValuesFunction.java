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

package net.shibboleth.idp.authn.context.impl;

import java.security.Principal;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.authn.principal.IdPAttributePrincipal;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.AbstractInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.primitive.StringSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/** Engine to mine values from {@link IdPAttributePrincipal}s. */
public class IdPAttributePrincipalValuesFunction extends AbstractInitializableComponent implements
        Function<Principal, List<IdPAttributeValue>> {

    /** The Attribute Name to look for. */
    @NonnullAfterInit @NotEmpty private String attributeName;

    /**
     * Set the attribute name.
     * 
     * @param attrName the attribute name to read values from
     */
    public void setAttributeName(@Nonnull @NotEmpty final String attrName) {
        attributeName = Constraint.isNotNull(StringSupport.trimOrNull(attrName),
                "Attribute Name cannot be null or empty");
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (attributeName == null) {
            throw new ComponentInitializationException("Attribute name cannot be null or empty");
        }
    }

    /** {@inheritDoc} */
    @Nullable public List<IdPAttributeValue> apply(@Nullable final Principal principal) {

        if (null != principal && principal instanceof IdPAttributePrincipal) {
            final IdPAttributePrincipal attributePrincipal = (IdPAttributePrincipal) principal;
            final IdPAttribute attribute = attributePrincipal.getAttribute();
            if (attributeName.equals(attribute.getId())) {
                return attribute.getValues();
            }
        }
        return null;
    }

}