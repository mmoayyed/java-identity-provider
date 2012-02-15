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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.LazyList;
import net.shibboleth.utilities.java.support.collection.TransformedInputCollectionBuilder;
import net.shibboleth.utilities.java.support.component.AbstractDestrucableIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.DestructableComponent;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

//TODO(lajoie) perf metrics

/** Services that filters out attributes and values based upon loaded policies. */
@ThreadSafe
public class AttributeFilteringEngine extends AbstractDestrucableIdentifiableInitializableComponent implements
        ValidatableComponent, DestructableComponent, UnmodifiableComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeFilteringEngine.class);
    
    /** Whether the Id has been set*/
    private boolean idSet;

    /** Filter policies used by this engine. */
    private Set<AttributeFilterPolicy> filterPolicies;

    /** Constructor. */
    public AttributeFilteringEngine() {
        filterPolicies = new TransformedInputCollectionBuilder().buildImmutableSet();
        super.setId("<unidentified Attribute Filtering Engine>");
        idSet = false;
    }

    /** {@inheritDoc} */
    public synchronized void setId(@Nonnull @NotEmpty final String componentId) {
        super.setId(componentId);
        idSet = true;
    }

    /**
     * Gets the immutable collection of filter policies.
     * 
     * @return immutable collection of filter policies
     */
    @Nonnull @NonnullElements @Unmodifiable public Set<AttributeFilterPolicy> getFilterPolicies() {
        return filterPolicies;
    }

    /**
     * Sets the new policies for the filtering engine.
     * 
     * @param policies new policies for the filtering engine
     */
    public synchronized void setFilterPolicies(
            @Nullable @NullableElements final Collection<AttributeFilterPolicy> policies) {
        ifInitializedThrowUnmodifiabledComponentException(getId());
        ifDestroyedThrowDestroyedComponentException(getId());

        filterPolicies = new TransformedInputCollectionBuilder().addAll(policies).buildImmutableSet();
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        ifNotInitializedThrowUninitializedComponentException(getId());
        ifDestroyedThrowDestroyedComponentException(getId());
        
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

        ifNotInitializedThrowUninitializedComponentException(getId());
        ifDestroyedThrowDestroyedComponentException(getId());

        Map<String, Attribute> prefilteredAttributes = filterContext.getPrefilteredAttributes();

        log.debug("Attribute filter engine '{}': beginning process of filter the following {} attributes: {}",
                new Object[] {getId(), prefilteredAttributes.size(), prefilteredAttributes.keySet(),});

        final Set<AttributeFilterPolicy> policies = getFilterPolicies();
        for (AttributeFilterPolicy policy : policies) {
            if (!policy.isApplicable(filterContext)) {
                log.debug("Attribute filtering engine '{}': filter policy '{}' is not applicable", getId(),
                        policy.getId());
            }

            policy.apply(filterContext);
        }

        Optional<Collection> filteredAttributeValues;
        Attribute filteredAttribute;
        for (String attributeId : filterContext.getPermittedAttributeValues().keySet()) {
            filteredAttributeValues = getFilteredValues(attributeId, filterContext);
            if (filteredAttributeValues.isPresent() && !filteredAttributeValues.get().isEmpty()) {
                filteredAttribute = prefilteredAttributes.get(attributeId).clone();
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
            return Optional.absent();
        }

        if (filterContext.getDeniedAttributeValues().containsKey(attributeId)) {
            filteredAttributeValues.removeAll(filterContext.getDeniedAttributeValues().get(attributeId));
        }

        if (filteredAttributeValues.isEmpty()) {
            log.debug("Attribute filtering engine '{}': deny policies filtered out all values for attribute '{}'",
                    getId(), attributeId);
        } else {
            log.debug("Attribute filtering engine '{}': {} values for attribute '{}' remained after filtering",
                    new Object[] {getId(), filteredAttributeValues.size(), attributeId,});
        }

        return Optional.of(filteredAttributeValues);
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        if (!idSet) {
            throw new ComponentInitializationException("Identifier for filtering engine not set");
        }
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

        filterPolicies = Collections.emptySet();

        super.doDestroy();
    }
}