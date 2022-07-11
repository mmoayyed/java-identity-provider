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

package net.shibboleth.idp.profile.spring.factory;

import org.springframework.binding.expression.EvaluationException;
import org.springframework.binding.expression.Expression;
import org.springframework.webflow.definition.registry.FlowDefinitionLocator;

/**
 * This code is copied verbatim from org.springframework.webflow.engine.builder.model.SubflowExpression
 */
class SubflowExpression implements Expression {

	private Expression subflowId;

	private FlowDefinitionLocator flowDefinitionLocator;

	public SubflowExpression(Expression subflowId, FlowDefinitionLocator flowDefinitionLocator) {
		this.subflowId = subflowId;
		this.flowDefinitionLocator = flowDefinitionLocator;
	}

	public Object getValue(Object context) throws EvaluationException {
		String subflowId = (String) this.subflowId.getValue(context);
		return flowDefinitionLocator.getFlowDefinition(subflowId);
	}

	public void setValue(Object context, Object value) throws EvaluationException {
		throw new UnsupportedOperationException("Cannot set a subflow expression");
	}

	public Class<?> getValueType(Object context) {
		return null;
	}

	public String getExpressionString() {
		return null;
	}
}