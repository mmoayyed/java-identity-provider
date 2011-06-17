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

package net.shibboleth.idp.attribute.resolver;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.ComponentValidationException;
import net.shibboleth.idp.attribute.Attribute;

import org.opensaml.util.Assert;
import org.springframework.expression.Expression;

/**
 * A proxy which wraps a resolved data connector and always returns the same attributes. The goal being that once a data
 * connector is resolved this can be used in its place and calls to
 * {@link BaseDataConnector#resolve(AttributeResolutionContext)} are "free".
 * 
 * This proxy is immutable so all setter methods simply return.
 */
@ThreadSafe
public class ResolvedDataConnector extends BaseDataConnector {

    /** The data connector that was resolved to produce the attributes. */
    private final BaseDataConnector resolvedConnector;

    /** The attributes produced by the resolved data connector. */
    private final Map<String, Attribute<?>> resolvedAttributes;

    /**
     * Constructor.
     * 
     * @param connector data connector that was resolved to produce the attributes, never null
     * @param attributes attributes produced by the resolved data connector, may be null
     */
    public ResolvedDataConnector(BaseDataConnector connector, Map<String, Attribute<?>> attributes) {
        super(connector.getId());

        Assert.isNotNull(connector, "Resolved data connector can not be null");
        resolvedConnector = connector;

        resolvedAttributes = attributes;
    }

    /** {@inheritDoc} */
    protected Map<String, Attribute<?>> doDataConnectorResolve(AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        return resolvedAttributes;
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        return resolvedConnector.equals(obj);
    }

    /** {@inheritDoc} */
    public Set<ResolverPluginDependency> getDependencies() {
        return resolvedConnector.getDependencies();
    }

    /** {@inheritDoc} */
    public Expression getEvaluationCondition() {
        return null;
    }

    /** {@inheritDoc} */
    public String getFailoverDataConnectorId() {
        return resolvedConnector.getFailoverDataConnectorId();
    }

    /** {@inheritDoc} */
    public String getId() {
        return resolvedConnector.getId();
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return resolvedConnector.hashCode();
    }

    /** {@inheritDoc} */
    public boolean isApplicable(AttributeResolutionContext resolutionContext) throws AttributeResolutionException {
        return true;
    }

    /** {@inheritDoc} */
    public boolean isPropagateEvaluationConditionExceptions() {
        return resolvedConnector.isPropagateEvaluationConditionExceptions();
    }

    /** {@inheritDoc} */
    public boolean isPropagateResolutionExceptions() {
        return resolvedConnector.isPropagateResolutionExceptions();
    }

    /** {@inheritDoc} */
    public void setDependencies(List<ResolverPluginDependency> pluginDependencies) {
        return;
    }

    /** {@inheritDoc} */
    public void setEvaluationCondition(Expression condition) {
        return;
    }

    /** {@inheritDoc} */
    public void setFailoverDataConnectorId(String id) {
        return;
    }

    /** {@inheritDoc} */
    public void setPropagateEvaluationConditionExceptions(boolean propagate) {
        return;
    }

    /** {@inheritDoc} */
    public void setPropagateResolutionExceptions(boolean propagate) {
        return;
    }

    /** {@inheritDoc} */
    public String toString() {
        return resolvedConnector.toString();
    }

    /**
     * Gets the wrapped data connector that was resolved.
     * 
     * @return the resolved data connector
     */
    public BaseDataConnector unwrap() {
        return resolvedConnector;
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        return;
    }
}