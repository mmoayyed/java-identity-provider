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

package net.shibboleth.idp.attribute.filter.matcher.saml.impl;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSBase64Binary;
import org.opensaml.core.xml.schema.XSBoolean;
import org.opensaml.core.xml.schema.XSDateTime;
import org.opensaml.core.xml.schema.XSInteger;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.schema.XSURI;
import org.opensaml.saml.common.messaging.context.AttributeConsumingServiceContext;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;

import net.shibboleth.idp.attribute.AttributesMapContainer;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.IdPRequestedAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.filter.Matcher;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.DOMTypeSupport;


/**
 * Matcher that checks whether an attribute is enumerated in an SP's metadata as a required or optional attribute. Also
 * supports simple value filtering.
 */
public class AttributeInMetadataMatcher extends AbstractIdentifiableInitializableComponent implements Matcher {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AttributeInMetadataMatcher.class);

    /** Whether optionally requested attributes should be matched. */
    private boolean onlyIfRequired = true;

    /** Whether to return a match if the metadata does not contain an ACS descriptor. */
    private boolean matchIfMetadataSilent;
    
    /** The SAML Attribute Name to look for in the metadata. */
    @Nullable @NotEmpty private String attributeName;

    /** The SAML Attribute NameFormat to look for in the metadata. */
    @Nullable @NotEmpty private String attributeNameFormat;
    
    /** The String used to prefix log message. */
    @Nullable @NotEmpty private String logPrefix;

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
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
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
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        matchIfMetadataSilent = flag;
    }

    /**
     * Get the SAML Attribute Name to look for in the metadata. If not used, the
     * evaluated attribute's own eventual encoded name(s) will be used to find a match.
     * 
     * @return the Name to look for
     */
    @Nullable @NotEmpty public String getAttributeName() {
        return attributeName;
    }
    
    /**
     * Set the SAML Attribute Name to look for in the metadata. If not used, the
     * evaluated attribute's own eventual encoded name(s) will be used to find a match.
     * 
     * <p>This allows a "look aside" to match a different SAML Attribute Name in the metadata.</p>
     * 
     * @param name the Name to look for
     */
    public void setAttributeName(@Nullable @NotEmpty final String name) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        attributeName = StringSupport.trimOrNull(name);
    }

    /**
     * Get the SAML Attribute NameFormat to look for in the metadata. If not used, the
     * evaluated attribute's own eventual encoded name format(s) will be used to find a match.
     * 
     * @return the Name to look for
     */
    @Nullable @NotEmpty public String getAttributeNameFormat() {
        return attributeNameFormat;
    }

    /**
     * Set the SAML Attribute NameFormat to look for in the metadata. If not used, the
     * evaluated attribute's own eventual encoded name format(s) will be used to find a match.
     * 
     * <p>This allows a "look aside" to match a different SAML Attribute NameFormat in the metadata.</p>
     * 
     * @param format the NameFormat to look for
     */
    public void setAttributeNameFormat(@Nullable @NotEmpty final String format) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        attributeNameFormat = StringSupport.trimOrNull(format);
        if (attributeNameFormat != null && Attribute.UNSPECIFIED.equals(attributeNameFormat)) {
            attributeNameFormat = null;
        }
    }

// Checkstyle: CyclomaticComplexity|ReturnCount|MethodLength OFF
    /** {@inheritDoc} */
    @Override @Nonnull public Set<IdPAttributeValue> getMatchingValues(@Nonnull final IdPAttribute attribute,
            @Nonnull final AttributeFilterContext filterContext) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        final AttributeConsumingService service = getAttributeConsumingService(filterContext);
    
        if (null == service) {
            if (matchIfMetadataSilent) {
                log.debug("{} The peer's metadata did not contain requested attribute information"
                        + ", returning all the input values", getLogPrefix());
                return Set.copyOf(attribute.getValues());
            }
            log.debug("{} The peer's metadata did not contain requested attribute information"
                    + ", returning no values", getLogPrefix());
            return Collections.emptySet();
        }
    
        if (attributeName != null) {
            // Look for a RequestedAttribute explicitly identified by config.
            log.debug("Looking for RequestedAttribute {} (NameFormat {}) in metadata", attributeName,
                    attributeNameFormat);
            final RequestedAttribute requestedAttribute = findInMetadata(service, attributeName, attributeNameFormat);
            final String attributeToLog = attributeName != null ? attributeName : attribute.getId();

            if (null == requestedAttribute) {
                log.debug("{} Attribute {} not found in metadata", getLogPrefix(), attributeToLog);
                return Collections.emptySet();
            }
        
            if (onlyIfRequired && !requestedAttribute.isRequired()) {
                log.debug("{} Attribute {} found in metadata, but was not required, values not matched",
                        getLogPrefix(), attributeToLog);
                return Collections.emptySet();
            }
            
            final Set<IdPAttributeValue> values = new LinkedHashSet<>();
            values.addAll(filterValues(attributeToLog, attribute, requestedAttribute.getAttributeValues()));
            return values;
        }
        
        // Utilize pre-mapped approach.
        final List<AttributesMapContainer> containerList =
                service.getObjectMetadata().get(AttributesMapContainer.class);
        if (null == containerList || containerList.isEmpty() || containerList.get(0).get() == null ||
                containerList.get(0).get().isEmpty()) {
            log.debug("{} No decoded attributes found when filtering", getLogPrefix());
            if (matchIfMetadataSilent) {
                // TODO: not sure what the right answer is here
                log.debug("{} The peer's metadata did not contain requested attribute information"
                        + ", returning all the input values", getLogPrefix());
                return Set.copyOf(attribute.getValues());
            }
            log.debug("{} The peer's metadata did not contain requested attribute information"
                    + ", returning no values", getLogPrefix());
            return Collections.emptySet();
        }
        if (containerList.size() > 1) {
            log.error("{} More than one set of mapped attributes found when filtering, this shouldn't ever happen",
                    getLogPrefix());
        }
        
        final Multimap<String,IdPAttribute> requestedAttributes = containerList.get(0).get();

        final Collection<? extends IdPAttribute> requestedAttributeList =
                requestedAttributes.get(attribute.getId());
        if (null == requestedAttributeList) {
            log.debug("{} Decoded attribute {} not found in metadata", getLogPrefix(), attribute.getId());
            return Collections.emptySet();
        }

        final Set<IdPAttributeValue> values = new LinkedHashSet<>();

        for (final IdPAttribute requestedAttribute : List.copyOf(requestedAttributeList)) {

            if (requestedAttribute instanceof IdPRequestedAttribute
                    && !((IdPRequestedAttribute) requestedAttribute).isRequired() && onlyIfRequired) {
                log.debug("{} Decoded attribute {} found in metadata, but not required, values not matched",
                        getLogPrefix(), attribute.getId());
                continue;
            }

            values.addAll(filterValues(attribute, requestedAttribute.getValues()));
        }
        
        return values;
    
    }
// Checkstyle: CyclomaticComplexity|ReturnCount|MethodLength ON
    
    /**
     * Get the appropriate {@link AttributeConsumingService}.
     * 
     * @param filterContext the context for the operation
     * 
     * @return the service, or null
     */
    @Nullable private AttributeConsumingService getAttributeConsumingService(
            @Nonnull final AttributeFilterContext filterContext) {
        
        final SAMLMetadataContext metadataContext = filterContext.getRequesterMetadataContext();
        if (null == metadataContext) {
            log.warn("{} No metadata context when filtering", getLogPrefix());
            return null;
        }
        
        final AttributeConsumingServiceContext acsContext =
                metadataContext.getSubcontext(AttributeConsumingServiceContext.class);
        return acsContext != null ? acsContext.getAttributeConsumingService() : null;
    }
    
    /**
     * Locates a RequestedAttribute object in metadata that matches a specific Attribute Name
     * and NameFormat.
     * 
     * @param service the metadata descriptor to search
     * @param name Attribute Name to match
     * @param nameFormat Attribute NameFormat to match
     * @return a matching RequestedAttribute, or null
     */
    @Nullable private RequestedAttribute findInMetadata(@Nonnull final AttributeConsumingService service,
            @Nonnull final String name, @Nullable final String nameFormat) {
        
        final List<RequestedAttribute> requested = service.getRequestedAttributes();
        for (final RequestedAttribute attr : requested) {
            if (attr.getName().equals(name)) {
                final String format = attr.getNameFormat();
                if (nameFormat == null || format == null || format.equals(Attribute.UNSPECIFIED)
                        || nameFormat.equals(format)) {
                    return attr;
                }
            }
        }
        return null;
    }
    
    /**
     * Given an attribute and the requested values do the filtering.
     * 
     * @param attribute the attribute
     * @param requestedValues the values
     * @return the result of the filter
     */
    @Nonnull private Set<IdPAttributeValue> filterValues(@Nullable final IdPAttribute attribute,
            @Nonnull @NonnullElements final List<IdPAttributeValue> requestedValues) {

        if (null == requestedValues || requestedValues.isEmpty()) {
            log.debug("{} Attribute {} found in metadata and no values specified", getLogPrefix(), attribute.getId());
            return Set.copyOf(attribute.getValues());
        }
        
        final Set<IdPAttributeValue> result = attribute.getValues().stream().
                filter(v -> requestedValues.contains(v)).collect(Collectors.toUnmodifiableSet());

        log.debug("{} Values matched with metadata for Attribute {} : {}", getLogPrefix(), attribute.getId(), result);
        return result;
    }
    
    /**
     * Given an attribute and the requested values do the filtering.
     * 
     * @param attributeToLog name of attribute to log
     * @param attribute the attribute
     * @param requestedValues the values
     * 
     * @return the result of the filter
     */
    @Nonnull @Unmodifiable @NonnullElements private Set<IdPAttributeValue> filterValues(
            @Nonnull final String attributeToLog,
            @Nullable final IdPAttribute attribute, 
            @Nonnull @NonnullElements final List<XMLObject> requestedValues) {

        if (requestedValues.isEmpty()) {
            log.debug("{} Attribute {} found in metadata and no values specified", getLogPrefix(), attributeToLog);
            return Set.copyOf(attribute.getValues());
        }

        final Set<IdPAttributeValue> result = new LinkedHashSet<>(attribute.getValues().size());
                
        for (final IdPAttributeValue attributeValue : attribute.getValues()) {
            if (attributeValue instanceof StringAttributeValue) {
                for (final XMLObject xmlObj : requestedValues) {
                    if (match(xmlObj, ((StringAttributeValue) attributeValue).getValue())) {
                        result.add(attributeValue);
                    }
                }
            } else {
                log.warn("{} Attribute {} value not a simple string, can't match against values in metadata",
                        getLogPrefix(), attribute.getId());
            }
        }
        
        log.debug("{} Values matched with metadata for Attribute {} : {}", getLogPrefix(), attributeToLog, result);
        return Set.copyOf(result);
    }

// Checkstyle: CyclomaticComplexity OFF
    /**
     * Checks whether an XMLObject's "value" matches a candidate value.
     * 
     * @param xmlObj the XMLObject to match
     * @param attributeValue the candidate value to match against
     * @return true iff the two parameters are non-null and match
     */
    private boolean match(final XMLObject xmlObj, final String attributeValue) {
        // This is a substitute for a decoder layer that can generate
        // internal comparable value objects out of AttributeValue elements.
        // Short of that, some kind of pluggable comparison object with
        // knowledge of the XML syntax and the internal attribute objects
        // would be needed.
        String toMatch = null;
        if (xmlObj instanceof XSString) {
            toMatch = ((XSString) xmlObj).getValue();
        } else if (xmlObj instanceof XSURI) {
            toMatch = ((XSURI) xmlObj).getURI();
        } else if (xmlObj instanceof XSBoolean) {
            toMatch = ((XSBoolean) xmlObj).getValue().getValue() ? "1" : "0";
        } else if (xmlObj instanceof XSInteger) {
            toMatch = ((XSInteger) xmlObj).getValue().toString();
        } else if (xmlObj instanceof XSDateTime) {
            final Instant dt = ((XSDateTime) xmlObj).getValue();
            if (dt != null) {
                toMatch = DOMTypeSupport.instantToString(dt);
            }
        } else if (xmlObj instanceof XSBase64Binary) {
            toMatch = ((XSBase64Binary) xmlObj).getValue();
        } else if (xmlObj instanceof XSAny) {
            final XSAny wc = (XSAny) xmlObj;
            if (wc.getUnknownAttributes().isEmpty() && wc.getUnknownXMLObjects().isEmpty()) {
                toMatch = wc.getTextContent();
            }
        }
        if (toMatch != null) {
            return toMatch.equals(attributeValue);
        }
        log.warn("Unrecognized XMLObject type, unable to match as a string to candidate value");
        return false;
    }
// Checkstyle: CyclomaticComplexity ON
    
    /**
     * return a string which is to be prepended to all log messages.
     * 
     * @return "Attribute Filter '&lt;filterID&gt;' :"
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
