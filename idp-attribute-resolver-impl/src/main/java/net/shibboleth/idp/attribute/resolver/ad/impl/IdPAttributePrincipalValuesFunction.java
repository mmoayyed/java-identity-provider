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

package net.shibboleth.idp.attribute.resolver.ad.impl;

import java.security.Principal;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.authn.principal.IdPAttributePrincipal;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;

import com.google.common.base.Function;

/** Engine to mine values from {@link IdPAttributePrincipal}s. */
public class IdPAttributePrincipalValuesFunction extends AbstractInitializableComponent implements
        Function<Principal, List<IdPAttributeValue<?>>> {

    /** The Attribute Name to look for. */
    @Nonnull private String attributeName;

    /**
     * Constructor.
     * 
     * @param attrName the name to filter on.
     * @deprecated use the property setter instead
     */
    @Deprecated public IdPAttributePrincipalValuesFunction(@Nonnull final String attrName) {
        attributeName = Constraint.isNotNull(attrName, "Attribute Name should be non-null");
        DeprecationSupport.warn(ObjectType.METHOD, 
                "IdPAttributePrincipalValuesFunction(String)", null, null);
    }

    /**  Constructor.  */
    public IdPAttributePrincipalValuesFunction() {
    }

    /**
     * Set the attribute name.
     * 
     * @param attrName the name to filter on.
     */
    public void setAttributeName(@Nonnull final String attrName) {
        attributeName = Constraint.isNotNull(attrName, "Attribute Name should be non-null");
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        Constraint.isNotNull(attributeName, "Attribute Name should be non-null");
        super.doInitialize();
    }

    /** {@inheritDoc} */
    @Override @Nullable public List<IdPAttributeValue<?>> apply(@Nullable final Principal principal) {

        if (null != principal && principal instanceof IdPAttributePrincipal) {
            final IdPAttributePrincipal attributePrincipal = (IdPAttributePrincipal) principal;
            final IdPAttribute attribute = attributePrincipal.getAttribute();
            if (null != attribute && attributeName.equals(attribute.getId())) {
                return attribute.getValues();
            }
        }
        return null;
    }
}
