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

package net.shibboleth.idp.attribute.filtering.impl.matcher.attributevalue;

import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filtering.impl.matcher.AbstractStringMatchFunctor;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import com.google.common.base.Predicate;

/**
 * Basic Implementation of a {@link net.shibboleth.idp.attribute.filtering.impl.matcher.MatchFunctor} based on string
 * comparison. The missing parts
 */
public abstract class AbstractAttributeTargetedStringMatchFunctor extends AbstractStringMatchFunctor implements
        TargetedMatchFunctor {

    /** ID of the attribute whose values will be evaluated. */
    private String attributeId;

    /** {@inheritDoc} */
    public String getAttributeId() {
        return attributeId;
    }

    /** {@inheritDoc} */
    public void setAttributeId(String id) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        attributeId = StringSupport.trimOrNull(id);
    }
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        if (null == attributeId) {
            // This is a UNTARGETTED filter, so we expect to be called to compare
            // attribute values
            setValuePredicate(new Predicate<AttributeValue>() {
    
                public boolean apply(@Nullable AttributeValue input) {
                    return compareAttributeValue(input);
                }
            });
            setPolicyPredicate(AttributeValueHelper.filterContextPredicate(this));
        } else {
            setPolicyPredicate(AttributeValueHelper.filterContextPredicate(this, attributeId));
        }
        super.doInitialize();
    }
}
