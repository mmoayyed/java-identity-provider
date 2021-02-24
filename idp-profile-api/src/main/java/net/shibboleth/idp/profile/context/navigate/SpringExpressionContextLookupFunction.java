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

package net.shibboleth.idp.profile.context.navigate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.ext.spring.util.SpringExpressionFunction;
import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.messaging.context.navigate.ContextDataLookupFunction;
import org.slf4j.LoggerFactory;

/**
 * A {@link java.util.function.Function} over a {@link BaseContext}
 * which calls out to a Spring Expression.
 * 
 * @param <T> the specific type of context
 * @param <U> output type
 * @since 3.3.0
 */
public class SpringExpressionContextLookupFunction<T extends BaseContext,U> extends SpringExpressionFunction<T,U>
        implements ContextDataLookupFunction<T,U> {

    /**
     * Constructor.
     * 
     * @param inClass the class we accept as input.
     * @param expression the expression to evaluate.
     */
    public SpringExpressionContextLookupFunction(@Nonnull @ParameterName(name="inClass") final Class<T> inClass,
            @Nonnull @NotEmpty @ParameterName(name="expression") final String expression) {
        super(expression);
        setInputType(Constraint.isNotNull(inClass, "Supplied inputClass cannot be null"));
        
        if(!BaseContext.class.isAssignableFrom(inClass)) {
            LoggerFactory.getLogger(SpringExpressionContextLookupFunction.class).
                warn("inClass {} is not derived from {}", inClass, BaseContext.class);
        }
    }

    /**
     * Constructor.
     * 
     * @param inClass the class we accept as input.
     * @param expression the expression to evaluate.
     * @param outputType the type to test against.
     */
    public SpringExpressionContextLookupFunction(@Nonnull @ParameterName(name="inClass") final Class<T> inClass,
            @Nonnull @NotEmpty @ParameterName(name="expression") final String expression, 
            @ParameterName(name="outputType") @Nullable final Class<U> outputType) {
        this(inClass, expression);
        setOutputType(outputType);
    }

}