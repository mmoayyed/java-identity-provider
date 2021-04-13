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

package net.shibboleth.idp.attribute.filter.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.RootContextLookup;
import org.opensaml.profile.context.MetricContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.ext.spring.service.AbstractServiceableComponent;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.filter.AttributeFilter;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.AttributeFilterPolicy;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterWorkContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/** Service that filters out attributes and values based upon loaded policies. */
@ThreadSafe
public class AttributeFilterImpl extends AbstractServiceableComponent<AttributeFilter> implements AttributeFilter {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AttributeFilterImpl.class);

    /** Filter policies used by this engine. */
    @Nonnull @NonnullElements private final List<AttributeFilterPolicy> filterPolicies;

    /** Log prefix. */
    @Nullable private String logPrefix;

    /** Strategy to get the {@link MetricContext} for timing. */
    @Nonnull private Function<AttributeFilterContext,MetricContext> metricContextLookupStrategy;

    /**
     * Constructor.
     * 
     * @param engineId ID of this engine
     * @param policies filter policies used by this engine
     */
    public AttributeFilterImpl(@Nonnull @NotEmpty final String engineId,
            @Nullable @NonnullElements final Collection<AttributeFilterPolicy> policies) {
        setId(engineId);

        filterPolicies = List.copyOf(policies);
        
        metricContextLookupStrategy = new ChildContextLookup<>(MetricContext.class).compose(new RootContextLookup<>());
    }

    /**
     * Gets the immutable collection of filter policies.
     * 
     * @return immutable collection of filter policies
     */
    @Override @Nonnull @NonnullElements @Unmodifiable public List<AttributeFilterPolicy> getFilterPolicies() {
        return filterPolicies;
    }

    /**
     * Filters attributes and values. This filtering process may remove attributes and values but must never add them.
     * 
     * @param filterContext context containing the attributes to be filtered and collecting the results of the filtering
     *            process
     * 
     * @throws AttributeFilterException thrown if there is a problem retrieving or applying the attribute filter policy
     */
    @Override public void filterAttributes(@Nonnull final AttributeFilterContext filterContext)
            throws AttributeFilterException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        Constraint.isNotNull(filterContext, "Attribute filter context can not be null");
        
        final boolean timerStarted = startTimer(filterContext);
        try {        
            final Map<String, IdPAttribute> prefilteredAttributes = filterContext.getPrefilteredIdPAttributes();
    
            // Create work context to hold intermediate results.
            filterContext.getSubcontext(AttributeFilterWorkContext.class, true);
    
            log.debug("{} Beginning process of filtering the following {} attributes: {}", new Object[] {getLogPrefix(),
                    prefilteredAttributes.size(), prefilteredAttributes.keySet(),});
    
            final List<AttributeFilterPolicy> policies = getFilterPolicies();
            for (final AttributeFilterPolicy policy : policies) {
                policy.apply(filterContext);
            }
    
            IdPAttribute filteredAttribute;
            for (final String attributeId : filterContext.getPrefilteredIdPAttributes().keySet()) {
                final Collection<IdPAttributeValue> filteredAttributeValues =
                        getFilteredValues(attributeId, filterContext);
                if (null != filteredAttributeValues && !filteredAttributeValues.isEmpty()) {
                    try {
                        filteredAttribute = prefilteredAttributes.get(attributeId).clone();
                    } catch (final CloneNotSupportedException e) {
                        throw new AttributeFilterException(e);
                    }
                    filteredAttribute.setValues(List.copyOf(filteredAttributeValues));
                    filterContext.getFilteredIdPAttributes().put(filteredAttribute.getId(), filteredAttribute);
                }
            }
        } finally {
            if (timerStarted) {
                stopTimer(filterContext);
            }
        }
    }

    /**
     * Gets the permitted values for the given attribute from the
     * {@link AttributeFilterWorkContext#getPermittedIdPAttributeValues()} and removes all denied values given in the
     * {@link AttributeFilterWorkContext#getDeniedAttributeValues()}.
     * 
     * @param attributeId ID of the attribute whose values are to be retrieved
     * @param filterContext current attribute filter context
     * 
     * @return null if no values were permitted to be released, an empty collection if values were permitted but then
     *         all were removed by deny policies, a collection containing permitted values
     */
    @Nullable protected Collection<IdPAttributeValue> getFilteredValues(@Nonnull @NotEmpty final String attributeId,
            @Nonnull final AttributeFilterContext filterContext) {
        Constraint.isNotNull(attributeId, "attributeId can not be null");
        Constraint.isNotNull(filterContext, "filterContext can not be null");

        final AttributeFilterWorkContext filterWorkContext =
                filterContext.getSubcontext(AttributeFilterWorkContext.class, false);
        Constraint.isNotNull(filterWorkContext, "Attribute filter work context can not be null");

        final Collection<IdPAttributeValue> filteredAttributeValues = filterWorkContext.
                getPermittedIdPAttributeValues().get(attributeId);

        if (filteredAttributeValues == null || filteredAttributeValues.isEmpty()) {
            log.debug("Attribute filtering engine '{}': no policy permitted release of attribute {} values", getId(),
                    attributeId);
            return null;
        }

        if (filterWorkContext.getDeniedAttributeValues().containsKey(attributeId)) {
            filteredAttributeValues.removeAll(filterWorkContext.getDeniedAttributeValues().get(attributeId));
        }

        if (filteredAttributeValues.isEmpty()) {
            log.debug("Attribute filtering engine '{}': deny policies filtered out all values for attribute '{}'",
                    getId(), attributeId);
        } else {
            log.debug("Attribute filtering engine '{}': {} values for attribute '{}' remained after filtering",
                    new Object[] {getId(), filteredAttributeValues.size(), attributeId,});
        }

        return filteredAttributeValues;
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        logPrefix = null;
    }

    /**
     * Get the prefix for logging.
     * 
     * @return Returns the logPrefix.
     */
    protected String getLogPrefix() {
        String result;

        result = logPrefix;
        if (null == result) {
            result = new StringBuffer("Attribute filtering engine '").append(getId()).append("' ").toString();
            logPrefix = result;
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override @Nonnull public AttributeFilter getComponent() {
        return this;
    }

    /**
     * Conditionally start a timer at the beginning of the filtering process.
     * 
     * @param filterContext attribute filtering context
     * 
     * @return true iff the {@link #stopTimer(AttributeFilterContext)} method needs to be called
     */
    private boolean startTimer(@Nonnull final AttributeFilterContext filterContext) {
        final MetricContext timerCtx = metricContextLookupStrategy.apply(filterContext);
        if (timerCtx != null) {
            timerCtx.start(getId());
            return true;
        }
        return false;
    }
    
    /**
     * Conditionally stop a timer at the end of the filtering process.
     * 
     * @param filterContext attribute filtering context
     */
    private void stopTimer(@Nonnull final AttributeFilterContext filterContext) {
        final MetricContext timerCtx = metricContextLookupStrategy.apply(filterContext);
        if (timerCtx != null) {
            timerCtx.stop(getId());
        }
    }

}