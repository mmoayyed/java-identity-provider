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

package net.shibboleth.idp.attribute.filter.impl.policyrule.saml;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.IdPRequestedAttribute;
import net.shibboleth.idp.attribute.filter.Matcher;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;

/**
 * Matcher that checks whether an attribute is enumerated in an SP's metadata as a required or optional attribute. Also
 * supports simple value filtering.
 */
public class AttributeInMetadataPolicyRule extends AbstractIdentifiableInitializableComponent implements Matcher {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeInMetadataPolicyRule.class);

    /** Whether optionally requested attributes should be matched. */
    private boolean onlyIfRequired;

    /** Whether to return a match if the metadata does not contain an ACS descriptor. */
    private boolean matchIfMetadataSilent;

    /** The String used to prefix log message. */
    private String logPrefix;

    /**
     * Gets whether optionally requested attributes should be matched.
     * 
     * @return Whether optionally requested attributes should be matched.
     */
    public boolean getOnlyIfRequired() {
        return onlyIfRequired;
    }

    /**
     * Sets whether optionally requested attributes should be matched.
     * 
     * @param flag whether optionally requested attributes should be matched
     */
    public void setOnlyIfRequired(final boolean flag) {
        onlyIfRequired = flag;
    }

    /**
     * Gets whether to matched if the metadata contains no AttributeConsumingService.
     * 
     * @return whether to match if the metadata contains no AttributeConsumingService
     */
    public boolean getMatchIfMetadataSilent() {
        return matchIfMetadataSilent;
    }

    /**
     * Sets whether to match if the metadata contains no AttributeConsumingService.
     * 
     * @param flag whether to match if the metadata contains no AttributeConsumingService
     */
    public void setMatchIfMetadataSilent(final boolean flag) {
        matchIfMetadataSilent = flag;
    }

    /** {@inheritDoc} */
    @Override @Nonnull public Set<IdPAttributeValue<?>> getMatchingValues(@Nonnull final IdPAttribute attribute,
            @Nonnull final AttributeFilterContext filterContext) {

        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        final Multimap<String, IdPRequestedAttribute> requestedAttributes = filterContext.getRequestedIdPAttributes();

        if (null == requestedAttributes || requestedAttributes.isEmpty()) {
            if (matchIfMetadataSilent) {
                log.debug("{} The peer's metadata did not have appropriate requested attributes available"
                        + ", returning all the input values", getLogPrefix());
                return Collections.unmodifiableSet(attribute.getValues());
            } else {
                log.debug("{} The peer's metadata did not have appropriate requested attributes available"
                        + ", returning no values", getLogPrefix());
                return Collections.EMPTY_SET;
            }
        }

        final Collection<IdPRequestedAttribute> requestedAttributeList = requestedAttributes.get(attribute.getId());

        if (null == requestedAttributeList) {
            log.debug("{} Attribute {} not found in metadata", getLogPrefix(), attribute.getId());
            return Collections.EMPTY_SET;
        }

        final Set<IdPAttributeValue<?>> values = new HashSet<>();

        for (final IdPRequestedAttribute requestedAttribute : requestedAttributeList) {

            if (null == requestedAttribute) {
                log.info("{} Attribute {} found in metadata but with no values that"
                        + " could be decoded, values not matched", getLogPrefix(), attribute.getId());
                continue;
            }

            if (!requestedAttribute.getIsRequired() && onlyIfRequired) {
                log.debug("{} Attribute {} found in metadata, but was not required" + ", values not matched",
                        getLogPrefix(), attribute.getId());
                continue;
            }

            values.addAll(filterValues(attribute, requestedAttribute.getValues()));
        }
        return values;
    }

    /**
     * Given an attribute and the requested values do the filtering.
     * 
     * @param attribute the attribute
     * @param requestedValues the values
     * @return the result of the filter
     */
    @Nonnull private Set<IdPAttributeValue<?>> filterValues(@Nullable final IdPAttribute attribute,
            @Nonnull @NonnullElements final Set<? extends IdPAttributeValue> requestedValues) {

        if (null == requestedValues || requestedValues.isEmpty()) {
            log.debug("{} Attribute {} found in metadata and no values specified", getLogPrefix(), attribute.getId());
            return attribute.getValues();
        }

        final Set<IdPAttributeValue<?>> result = new HashSet<>(attribute.getValues().size());

        for (final IdPAttributeValue attributeValue : attribute.getValues()) {
            if (requestedValues.contains(attributeValue)) {
                result.add(attributeValue);
            }
        }
        log.debug("{} Values matched with metadata for Attribute {} : {}", getLogPrefix(), attribute.getId(), result);
        return result;
    }

    /**
     * return a string which is to be prepended to all log messages.
     * 
     * @return "Attribute Filter '<filterID>' :"
     */
    @Nonnull protected String getLogPrefix() {
        // local cache of cached entry to allow unsynchronised clearing.
        String prefix = logPrefix;
        if (null == prefix) {
            final StringBuilder builder = new StringBuilder("Attribute Filter '").append(getId()).append("':");
            prefix = builder.toString();
            if (null == logPrefix) {
                logPrefix = prefix;
            }
        }
        return prefix;
    }

}
