/*
 * Copyright 2011 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.attribute.resolver;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;

/** Mock implementation of {@link Expression} that always returns the same value. */
public class MockExpression implements Expression {

    /** Value returned by the expression. */
    private Object expressionValue;

    /**
     * Constructor.
     * 
     * @param value value returned by the expression
     */
    public MockExpression(Object value) {
        expressionValue = value;
    }

    /** {@inheritDoc} */
    public Object getValue() {
        return expressionValue;
    }

    /** {@inheritDoc} */
    public Object getValue(Object rootObject) {
        return expressionValue;
    }

    /** {@inheritDoc} */
    public <T> T getValue(Class<T> desiredResultType) {
        return (T) expressionValue;
    }

    /** {@inheritDoc} */
    public <T> T getValue(Object rootObject, Class<T> desiredResultType) {
        return (T) expressionValue;
    }

    /** {@inheritDoc} */
    public Object getValue(EvaluationContext context) {
        return expressionValue;
    }

    /** {@inheritDoc} */
    public Object getValue(EvaluationContext context, Object rootObject) {
        return expressionValue;
    }

    /** {@inheritDoc} */
    public <T> T getValue(EvaluationContext context, Class<T> desiredResultType) {
        return (T) expressionValue;
    }

    /** {@inheritDoc} */
    public <T> T getValue(EvaluationContext context, Object rootObject, Class<T> desiredResultType) {
        return (T) expressionValue;
    }

    /** {@inheritDoc} */
    public Class getValueType() {
        return expressionValue.getClass();
    }

    /** {@inheritDoc} */
    public Class getValueType(Object rootObject) {
        return expressionValue.getClass();
    }

    /** {@inheritDoc} */
    public Class getValueType(EvaluationContext context) {
        return expressionValue.getClass();
    }

    /** {@inheritDoc} */
    public Class getValueType(EvaluationContext context, Object rootObject) {
        return expressionValue.getClass();
    }

    /** {@inheritDoc} */
    public TypeDescriptor getValueTypeDescriptor() {
        return TypeDescriptor.forObject(expressionValue);
    }

    /** {@inheritDoc} */
    public TypeDescriptor getValueTypeDescriptor(Object rootObject) {
        return TypeDescriptor.forObject(expressionValue);
    }

    /** {@inheritDoc} */
    public TypeDescriptor getValueTypeDescriptor(EvaluationContext context) {
        return TypeDescriptor.forObject(expressionValue);
    }

    /** {@inheritDoc} */
    public TypeDescriptor getValueTypeDescriptor(EvaluationContext context, Object rootObject) {
        return TypeDescriptor.forObject(expressionValue);
    }

    /** {@inheritDoc} */
    public boolean isWritable(EvaluationContext context) {
        return false;
    }

    /** {@inheritDoc} */
    public boolean isWritable(EvaluationContext context, Object rootObject) {
        return false;
    }

    /** {@inheritDoc} */
    public boolean isWritable(Object rootObject) {
        return false;
    }

    /** {@inheritDoc} */
    public void setValue(EvaluationContext context, Object value) {

    }

    /** {@inheritDoc} */
    public void setValue(Object rootObject, Object value) {

    }

    /** {@inheritDoc} */
    public void setValue(EvaluationContext context, Object rootObject, Object value) {

    }

    /** {@inheritDoc} */
    public String getExpressionString() {
        return expressionValue.toString();
    }
}