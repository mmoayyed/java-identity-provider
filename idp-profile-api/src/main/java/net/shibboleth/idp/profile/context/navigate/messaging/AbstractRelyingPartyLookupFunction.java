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

package net.shibboleth.idp.profile.context.navigate.messaging;

import java.util.function.Function;

import javax.annotation.Nonnull;

import org.opensaml.messaging.context.InOutOperationContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.ContextDataLookupFunction;
import org.opensaml.messaging.context.navigate.RecursiveTypedParentContextLookup;

import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Abstract base class for a function that requires a {@link RelyingPartyContext}
 * obtained via a lookup function, by default a child of the {@link InOutOperationContext} 
 * the parent of the specified {@link MessageContext}.
 * 
 * @param <ResultType> return type of function
 */
public abstract class AbstractRelyingPartyLookupFunction<ResultType>
        implements ContextDataLookupFunction<MessageContext,ResultType> {

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link MessageContext}.
     */
    @Nonnull private Function<MessageContext,RelyingPartyContext> relyingPartyContextLookupStrategy;
    
    /** Constructor. */
    public AbstractRelyingPartyLookupFunction() {
        relyingPartyContextLookupStrategy =
                new ChildContextLookup<>(RelyingPartyContext.class).compose(
                        new RecursiveTypedParentContextLookup<>(InOutOperationContext.class));
    }

    /**
     * Set the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link MessageContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<MessageContext,RelyingPartyContext> strategy) {
        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy cannot be null");
    }
    
    /**
     * Get the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link MessageContext}.
     * 
     * @return lookup strategy
     */
    @Nonnull public Function<MessageContext,RelyingPartyContext> getRelyingPartyContextLookupStrategy() {
        return relyingPartyContextLookupStrategy;
    }

}