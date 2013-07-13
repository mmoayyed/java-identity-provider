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

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.Matcher;
import net.shibboleth.idp.attribute.filter.RequestedAttribute;
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

    /** {@inheritDoc} */
    @Nonnull public Set<AttributeValue> getMatchingValues(@Nonnull final Attribute attribute,
            @Nonnull final AttributeFilterContext filterContext) {

        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        final Multimap<String, RequestedAttribute> requestedAttributes = filterContext.getRequestedAttributes();

        if (null == requestedAttributes || requestedAttributes.isEmpty()) {
            log.debug("{} The peer's metadata did not have appropriate requested attributes available",
                    getLogPrefix());
            if (matchIfMetadataSilent) {
                return Collections.unmodifiableSet(attribute.getValues());
            } else {
                return Collections.EMPTY_SET;
            }
        }

        final Collection<RequestedAttribute> requestedAttributeList = requestedAttributes.get(attribute.getId());

        if (null == requestedAttributeList) {
            log.debug("{} Attribute {} not found in metadata", getLogPrefix(),
                    attribute.getId());
            return Collections.EMPTY_SET;
        }
        
        final Set<AttributeValue> values = new HashSet<AttributeValue>();
        
        for (RequestedAttribute requestedAttribute: requestedAttributeList) {
            
            if (null == requestedAttribute) {
                log.info("{} Attribute {} found in metadata but with no values that could be decoded");
                continue;
            }

            if (!requestedAttribute.getIsRequired() && onlyIfRequired) {
                log.debug("{} Attribute {} found in metadata, but was not required", getLogPrefix(), attribute.getId());
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
    @Nonnull private Set<AttributeValue> filterValues(@Nullable final Attribute attribute,
            final Set<AttributeValue> requestedValues) {

        if (null == requestedValues || requestedValues.isEmpty()) {
            log.debug("{} Attribute {} found in metadata and no values specified", getLogPrefix(), attribute.getId());
            return attribute.getValues();
        }

        final Set<AttributeValue> result = new HashSet<AttributeValue>(attribute.getValues().size());

        for (AttributeValue attributeValue : attribute.getValues()) {
            if (requestedValues.contains(attributeValue)) {
                result.add(attributeValue);
            }
        }
        log.debug("{} Values matched with metadata for Attribute {} : {}", getLogPrefix(), attribute.getId(), result);
        return result;
    }

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
    public void setId(@Nullable String id) {
        super.setId(id);
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
            StringBuilder builder = new StringBuilder("Attribute Filter '").append(getId()).append("':");
            prefix = builder.toString();
            if (null == logPrefix) {
                logPrefix = prefix;
            }
        }
        return prefix;
    }

}
