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

package net.shibboleth.idp.attribute.filter.impl.matcher.logic;

import java.util.Set;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.MatchFunctor;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;

/**
 * {@link MatchFunctor} that returns true when evaluated as a policy requirement rule and returns all attribute values
 * when evaluated as an attribute rule.
 */
public class AnyMatcher extends AbstractIdentifiableInitializableComponent implements MatchFunctor {

    /** {@inheritDoc} */
    public synchronized void setId(@Nonnull @NotEmpty final String componentId) {
        super.setId(componentId);
    }
    
    /** {@inheritDoc} */
    public boolean evaluatePolicyRule(@Nonnull AttributeFilterContext filterContext)
             throws AttributeFilterException {
        return true;
    }

    /** {@inheritDoc} */
    @Nonnull public Set<AttributeValue> getMatchingValues(@Nonnull Attribute attribute,
            @Nonnull AttributeFilterContext filterContext) throws AttributeFilterException {
        return attribute.getValues();
    }
}
