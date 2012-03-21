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

package net.shibboleth.idp.attribute.filtering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
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
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSortedSet;

//TODO(lajoie) perf metrics

/** Services that filters out attributes and values based upon loaded policies. */
@ThreadSafe
public class AttributeFilteringEngine extends AbstractDestructableIdentifiableInitializableComponent implements
        ValidatableComponent, DestructableComponent, UnmodifiableComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeFilteringEngine.class);

    /** Filter policies used by this engine. */
    private final SortedSet<AttributeFilterPolicy> filterPolicies;

    /**
     * Constructor.
     * 
     * @param engineId ID of this engine
     * @param policies filter policies used by this engine
     */
    public AttributeFilteringEngine(@Nonnull @NotEmpty String engineId,
            @Nullable @NullableElements final Collection<AttributeFilterPolicy> policies) {
        setId(engineId);

        ArrayList<AttributeFilterPolicy> checkedPolicies = new ArrayList<AttributeFilterPolicy>();
        CollectionSupport.addIf(checkedPolicies, policies, Predicates.notNull());
        filterPolicies = ImmutableSortedSet.copyOf(checkedPolicies);
    }

    /**
     * Gets the immutable collection of filter policies.
     * 
     * @return immutable collection of filter policies
     */
    @Nonnull @NonnullElements @Unmodifiable public Set<AttributeFilterPolicy> getFilterPolicies() {
        return filterPolicies;
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        final LazyList<String> invalidPolicyIds = new LazyList<String>();
        final Set<AttributeFilterPolicy> policies = getFilterPolicies();
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
     * @throws AttributeFilteringException thrown if there is a problem retrieving or applying the attribute filter
     *             policy
     */
    public void filterAttributes(@Nonnull final AttributeFilterContext filterContext)
            throws AttributeFilteringException {
        assert filterContext != null : "Attribute filter context can not be null";

        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        Map<String, Attribute> prefilteredAttributes = filterContext.getPrefilteredAttributes();

        log.debug("Attribute filter engine '{}': beginning process of filtering the following {} attributes: {}",
                new Object[] {getId(), prefilteredAttributes.size(), prefilteredAttributes.keySet(),});

        final Set<AttributeFilterPolicy> policies = getFilterPolicies();
        for (AttributeFilterPolicy policy : policies) {
            if (!policy.isApplicable(filterContext)) {
                log.debug("Attribute filtering engine '{}': filter policy '{}' is not applicable", getId(),
                        policy.getId());
                continue;
            }

            policy.apply(filterContext);
        }

        Optional<Collection> filteredAttributeValues;
        Attribute filteredAttribute;
        for (String attributeId : filterContext.getPermittedAttributeValues().keySet()) {
            filteredAttributeValues = getFilteredValues(attributeId, filterContext);
            if (filteredAttributeValues.isPresent() && !filteredAttributeValues.get().isEmpty()) {
                try {
                    filteredAttribute = prefilteredAttributes.get(attributeId).clone();
                } catch (CloneNotSupportedException e) {
                    throw new AttributeFilteringException(e);
                }
                filteredAttribute.setValues(filteredAttributeValues.get());
                filterContext.getFilteredAttributes().put(filteredAttribute.getId(), filteredAttribute);
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
     * @return {@link Optional#absent()} if not values were permitted to be released, {@link Optional} containing an
     *         empty collection if values were permitted but then all were removed by deny policies, or {@link Optional}
     *         with a collection containing permitted values
     */
    protected Optional<Collection> getFilteredValues(@Nonnull @NotEmpty final String attributeId,
            @Nonnull final AttributeFilterContext filterContext) {
        assert attributeId != null : "attributeId can not be null";
        assert filterContext != null : "filterContext can not be null";

        final Collection filteredAttributeValues = filterContext.getPermittedAttributeValues().get(attributeId);

        if (filteredAttributeValues == null || filteredAttributeValues.isEmpty()) {
            log.debug("Attribute filtering engine '{}': no policy permitted release of attribute {} values", getId(),
                    attributeId);
            //
            // Note that this code will not be exercised - empty attributes are stripped out in 
            // AttributeFilterPolicy#apply
            //
            return Optional.absent();
        }

        if (filterContext.getDeniedAttributeValues().containsKey(attributeId)) {
            filteredAttributeValues.removeAll(filterContext.getDeniedAttributeValues().get(attributeId));
        }

        if (filteredAttributeValues.isEmpty()) {
            log.debug("Attribute filtering engine '{}': deny policies filtered out all values for attribute '{}'",
                    getId(), attributeId);
            return Optional.absent();
        } else {
            log.debug("Attribute filtering engine '{}': {} values for attribute '{}' remained after filtering",
                    new Object[] {getId(), filteredAttributeValues.size(), attributeId,});
        }

        return Optional.of(filteredAttributeValues);
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        for (AttributeFilterPolicy policy : filterPolicies) {
            policy.initialize();
        }
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        final Set<AttributeFilterPolicy> policies = getFilterPolicies();
        for (AttributeFilterPolicy policy : policies) {
            policy.destroy();
        }

        super.doDestroy();
    }
}