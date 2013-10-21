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

package net.shibboleth.idp.attribute.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.CollectionSupport;
import net.shibboleth.utilities.java.support.collection.LazyList;
import net.shibboleth.utilities.java.support.component.AbstractDestructableIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.DestructableComponent;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

//TODO(lajoie) perf metrics

/** Service that filters out attributes and values based upon loaded policies. */
@ThreadSafe
public class AttributeFilter extends AbstractDestructableIdentifiableInitializableComponent implements
        ValidatableComponent, DestructableComponent, UnmodifiableComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeFilter.class);

    /** Filter policies used by this engine. */
    private final List<AttributeFilterPolicy> filterPolicies;

    /** Log prefix. */
    private String logPrefix;

    /**
     * Constructor.
     * 
     * @param engineId ID of this engine
     * @param policies filter policies used by this engine
     */
    public AttributeFilter(@Nonnull @NotEmpty String engineId,
            @Nullable @NullableElements final Collection<AttributeFilterPolicy> policies) {
        setId(engineId);

        ArrayList<AttributeFilterPolicy> checkedPolicies = new ArrayList<AttributeFilterPolicy>();
        CollectionSupport.addIf(checkedPolicies, policies, Predicates.notNull());
        filterPolicies = ImmutableList.copyOf(Iterables.filter(checkedPolicies, Predicates.notNull()));
    }

    /**
     * Gets the immutable collection of filter policies.
     * 
     * @return immutable collection of filter policies
     */
    @Nonnull @NonnullElements @Unmodifiable public List<AttributeFilterPolicy> getFilterPolicies() {
        return filterPolicies;
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        final LazyList<String> invalidPolicyIds = new LazyList<String>();
        final List<AttributeFilterPolicy> policies = getFilterPolicies();
        for (AttributeFilterPolicy policy : policies) {
            try {
                log.debug("Attribute filtering engine '{}': checking if policy '{}' is valid", getId(), policy.getId());
                policy.validate();
                log.debug("Attribute filtering engine '{}': policy '{}' is valid", getId(), policy.getId());
            } catch (ComponentValidationException e) {
                log.warn("Attribute filtering engine '{}': filter policy '{}' is not valid", new Object[] {
                        this.getId(), policy.getId(), e,});
                invalidPolicyIds.add(policy.getId());
            }
        }

        if (!invalidPolicyIds.isEmpty()) {
            throw new ComponentValidationException("The following attribute filter policies were invalid: "
                    + StringSupport.listToStringValue(invalidPolicyIds, ", "));
        }
    }

    /**
     * Filters attributes and values. This filtering process may remove attributes and values but must never add them.
     * 
     * @param filterContext context containing the attributes to be filtered and collecting the results of the filtering
     *            process
     * 
     * @throws AttributeFilterException thrown if there is a problem retrieving or applying the attribute filter policy
     */
    public void filterAttributes(@Nonnull final AttributeFilterContext filterContext) throws AttributeFilterException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        Constraint.isNotNull(filterContext, "Attribute filter context can not be null");
        Map<String, IdPAttribute> prefilteredAttributes = filterContext.getPrefilteredIdPAttributes();

        log.debug("{} beginning process of filtering the following {} attributes: {}", new Object[] {getLogPrefix(),
                prefilteredAttributes.size(), prefilteredAttributes.keySet(),});

        final List<AttributeFilterPolicy> policies = getFilterPolicies();
        for (AttributeFilterPolicy policy : policies) {
            policy.apply(filterContext);
        }

        IdPAttribute filteredAttribute;
        for (String attributeId : filterContext.getPrefilteredIdPAttributes().keySet()) {
            final Collection filteredAttributeValues = getFilteredValues(attributeId, filterContext);
            if (null != filteredAttributeValues && !filteredAttributeValues.isEmpty()) {
                try {
                    filteredAttribute = prefilteredAttributes.get(attributeId).clone();
                } catch (CloneNotSupportedException e) {
                    throw new AttributeFilterException(e);
                }
                filteredAttribute.setValues(filteredAttributeValues);
                filterContext.getFilteredIdPAttributes().put(filteredAttribute.getId(), filteredAttribute);
            }
        }
    }

    /**
     * Gets the permitted values for the given attribute from the
     * {@link AttributeFilterContext#getPermittedAttributeValues()} and removes all denied values given in the
     * {@link AttributeFilterContext#getDeniedAttributeValues()}.
     * 
     * @param attributeId ID of the attribute whose values are to be retrieved
     * @param filterContext current attribute filter context
     * 
     * @return null if no values were permitted to be released, an empty collection if values were permitted but then
     *         all were removed by deny policies, a collection containing permitted values
     */
    @Nullable protected Collection getFilteredValues(@Nonnull @NotEmpty final String attributeId,
            @Nonnull final AttributeFilterContext filterContext) {
        Constraint.isNotNull(attributeId, "attributeId can not be null");
        Constraint.isNotNull(filterContext, "filterContext can not be null");

        final Collection filteredAttributeValues = filterContext.getPermittedAttributeValues().get(attributeId);

        if (filteredAttributeValues == null || filteredAttributeValues.isEmpty()) {
            log.debug("Attribute filtering engine '{}': no policy permitted release of attribute {} values", getId(),
                    attributeId);
            return null;
        }

        if (filterContext.getDeniedAttributeValues().containsKey(attributeId)) {
            filteredAttributeValues.removeAll(filterContext.getDeniedAttributeValues().get(attributeId));
        }

        if (filteredAttributeValues.isEmpty()) {
            log.debug("Attribute filtering engine '{}': deny policies filtered out all values for attribute '{}'",
                    getId(), attributeId);
        } else {
            log.debug("Attribute filtering engine '{}': {} values for attribute '{}' remained after filtering",
                    new Object[] {filteredAttributeValues.size(), attributeId,});
        }

        return filteredAttributeValues;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        logPrefix = null;

        for (AttributeFilterPolicy policy : filterPolicies) {
            policy.initialize();
        }
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        final List<AttributeFilterPolicy> policies = getFilterPolicies();
        for (AttributeFilterPolicy policy : policies) {
            policy.destroy();
        }

        super.doDestroy();
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
}