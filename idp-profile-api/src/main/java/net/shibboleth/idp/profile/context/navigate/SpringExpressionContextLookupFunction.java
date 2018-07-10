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
 * A {@link com.google.common.base.Function} over a {@link BaseContext}
 * which calls out to a Spring Expression.
 * 
 * @param <T> the specific type of context
 * @since 3.3.0
 */
public class SpringExpressionContextLookupFunction<T extends BaseContext>
    implements ContextDataLookupFunction<T, Object> {

    /**
     * The object that does the work.
     * In Future versions this should be replaced with inheritance.
     */
    @Deprecated
    private final SpringExpressionFunction<T, Object> embeddedObject;

    /**
     * Constructor.
     * 
     * @param inClass the class we accept as input.
     * @param expression the expression to evaluate.
     */
    public SpringExpressionContextLookupFunction(@Nonnull @ParameterName(name="inClass") final Class<T> inClass,
            @Nonnull @NotEmpty @ParameterName(name="expression") final String expression) {
        embeddedObject = new SpringExpressionFunction<>(expression);
        embeddedObject.setInputType(Constraint.isNotNull(inClass, "Supplied inputClass cannot be null"));
        if(!BaseContext.class.isAssignableFrom(inClass)) {
            LoggerFactory.getLogger(SpringExpressionContextLookupFunction.class).
                warn("InClass {} is not derived from {}", inClass, BaseContext.class);
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
            @ParameterName(name="outputType") @Nullable final Class outputType) {
        this(inClass, expression);
        embeddedObject.setOutputType(outputType);
    }

    /**
     * Return the custom (externally provided) object.
     * 
     * @return the custom object
     */
    @Nullable public Object getCustomObject() {
        return embeddedObject.getCustomObject();
    }

    /**
     * Set the custom (externally provided) object.
     * 
     * @param object the custom object
     */
    @Nullable public void setCustomObject(final Object object) {
        embeddedObject.setCustomObject(object);
    }

    /**
     * Set whether to hide exceptions in expression execution (default is false).
     * 
     * @param flag flag to set
     */
    public void setHideExceptions(final boolean flag) {
        embeddedObject.setHideExceptions(flag);
    }

    /** {@inheritDoc} */
    @Override public Object apply(@Nullable final T context) {
        return embeddedObject.apply(context);
    }

}