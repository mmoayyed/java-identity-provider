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

package net.shibboleth.idp.attribute.logic;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Assert;

import org.opensaml.messaging.context.BaseContext;

import com.google.common.base.Function;

/**
 * This {@link Function} looks up an {@link Attribute}, by its ID, from a {@link BaseContext} tree. The function first
 * checks to see if the given {@link BaseContext} is a {@link AttributeContext}, if not it checks to see if any siblings
 * of the given context are {@link AttributeContext}, if not the process is repeated with the parent of the given
 * {@link BaseContext}. Once an {@link AttributeContext} is found, the attribute is looked up. If no such attribute is
 * found, the function does <strong>not</strong> look for any other {@link AttributeContext}.
 */
public class LookupAttributeFromAttributeContextFunction implements Function<BaseContext, Attribute> {

    /** ID of the attribute. */
    private final String id;

    /**
     * Constructor.
     * 
     * @param attributeId ID of the attribute
     */
    public LookupAttributeFromAttributeContextFunction(@Nonnull @NotEmpty final String attributeId) {
        id = Assert.isNotNull(attributeId, "Attribute ID can not be null");
    }

    /** {@inheritDoc} */
    public Attribute apply(@Nonnull BaseContext context) {
        assert context != null : "Context can not be null";

        AttributeContext attributeContext;
        if (context instanceof AttributeContext) {
            attributeContext = (AttributeContext) context;
            return attributeContext.getAttributes().get(id);
        }

        BaseContext parentContext = context.getParent();
        if (parentContext == null) {
            return null;
        }

        attributeContext = parentContext.getSubcontext(AttributeContext.class, false);
        if (attributeContext != null) {
            return attributeContext.getAttributes().get(id);
        }

        return apply(parentContext);
    }
}
