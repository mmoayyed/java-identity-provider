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

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.messaging.context.navigate.ContextDataLookupFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * A {@link Function} over a {@link BaseContext} which calls out to a Spring Expression.
 * 
 * @param <T> the specific type of context
 */
public class SpringExpressionContextLookupFunction<T extends BaseContext>
    implements ContextDataLookupFunction<T, Object> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SpringExpressionContextLookupFunction.class);

    /** SpEL expression to evaluate. */
    @Nullable private String springExpression;

    /** What class we want the output to test against. */
    @Nullable private Class outputClass;

    /** What class we want the input to test against. */
    @Nonnull private final Class<T> inputClass;

    /** A custom object that can be injected into the expression. */
    @Nullable private Object customObject;
        
    /** Whether to raise runtime exceptions if an expression fails. */
    private boolean hideExceptions;


    /**
     * Constructor.
     * 
     * @param inClass the class we accept as input.
     * @param expression the expression to evaluate.
     */
    public SpringExpressionContextLookupFunction(@Nonnull final Class<T> inClass,
            @Nonnull @NotEmpty final String expression) {
        inputClass = Constraint.isNotNull(inClass, "Supplied inputClass cannot be null");
        springExpression = Constraint.isNotNull(expression, "Supplied expression cannot be null");
    }

    /**
     * Constructor.
     * 
     * @param inClass the class we accept as input.
     * @param expression the expression to evaluate.
     * @param outputType the type to test against.
     */
    public SpringExpressionContextLookupFunction(@Nonnull final Class<T> inClass,
            @Nonnull @NotEmpty final String expression, @Nullable final Class outputType) {
        this(inClass, expression);
        outputClass = outputType;
    }

    /**
     * Return the custom (externally provided) object.
     * 
     * @return the custom object
     */
    @Nullable public Object getCustomObject() {
        return customObject;
    }

    /**
     * Set the custom (externally provided) object.
     * 
     * @param object the custom object
     */
    @Nullable public void setCustomObject(final Object object) {
        customObject = object;
    }

    /**
     * Set whether to hide exceptions in expression execution (default is false).
     * 
     * @param flag flag to set
     */
    public void setHideExceptions(final boolean flag) {
        hideExceptions = flag;
    }

    /** {@inheritDoc} */
    @Override public Object apply(@Nullable final T context) {

        if (null != context && !inputClass.isInstance(context)) {
            throw new ClassCastException("Input was type " + context.getClass() + " which is not an instance of "
                    + inputClass);
        }

        try {
            final ExpressionParser parser = new SpelExpressionParser();
            final StandardEvaluationContext eval = new StandardEvaluationContext();
            eval.setVariable("custom", customObject);
            eval.setVariable("input", context);
            
            final Object output = parser.parseExpression(springExpression).getValue(eval);
            if (null != outputClass && null != output && !outputClass.isInstance(output)) {
                log.error("Output of type {} was not of type {}", output.getClass(), outputClass);
                return null;
            }
            return output;
            
        } catch (final ParseException|EvaluationException e) {
            log.error("Error evaluating Spring expression", e);
            if (hideExceptions) {
                return null;
            }
            throw e;
        }
    }

}