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

package net.shibboleth.idp.attribute.filter.policyrule.saml.impl;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.AttributesMapContainer;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.policyrule.impl.AbstractPolicyRule;
import net.shibboleth.idp.saml.xmlobject.ScopedValue;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.DOMTypeSupport;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSBase64Binary;
import org.opensaml.core.xml.schema.XSBoolean;
import org.opensaml.core.xml.schema.XSDateTime;
import org.opensaml.core.xml.schema.XSInteger;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.schema.XSURI;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;

/**
 * Base class for matchers that check whether a particular entity attribute is present and contains a given value.
 * 
 * <p>
 * Given the metadata for an entity, this class takes care of navigation to the attribute and extracting the values,
 * including optimized handling of mapped attributes.
 * </p>
 * 
 * <p>
 * Classes wishing to implement Entity Attribute matchers implement {@link #getEntityMetadata(AttributeFilterContext)}
 * to navigate to the entity (probably recipient or issuer) and {@link #entityAttributeValueMatches(Set)} to
 * implement the comparison (probably string or regexp).
 * </p>
 */
public abstract class AbstractEntityAttributePolicyRule extends AbstractPolicyRule {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractEntityAttributePolicyRule.class);

    /** The name of the entity attribute the entity must have. */
    @NonnullAfterInit @NotEmpty private String attrName;

    /** The name format of the entity attribute the entity must have. */
    @Nullable @NotEmpty private String nameFormat;
    
    /** Whether to ignore unmapped attributes as an optimization. */
    private boolean ignoreUnmappedEntityAttributes;

    /**
     * Gets the name of the entity attribute the entity must have.
     * 
     * @return name of the entity attribute the entity must have
     */
    @NonnullAfterInit @NotEmpty public String getAttributeName() {
        return attrName;
    }

    /**
     * Sets the name of the entity attribute the entity must have.
     * 
     * @param attributeName name of the entity attribute the entity must have
     */
    public void setAttributeName(@Nullable @NotEmpty final String attributeName) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        attrName = StringSupport.trimOrNull(attributeName);
    }

    /**
     * Gets the name format of the entity attribute the entity must have.
     * 
     * @return name format of the entity attribute the entity must have
     */
    @Nullable @NotEmpty public String getNameFormat() {
        return nameFormat;
    }

    /**
     * Sets the name format of the entity attribute the entity must have.
     * 
     * @param attributeNameFormat name format of the entity attribute the entity must have
     */
    public void setNameFormat(@Nullable @NotEmpty final String attributeNameFormat) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        nameFormat = StringSupport.trimOrNull(attributeNameFormat);
    }
    
    /**
     * Gets whether to ignore unmapped/decoded EntityAttribute extensions as an optimization.
     * 
     * @return whether to ignore unmapped/decoded EntityAttribute extensions as an optimization
     */
    public boolean getIgnoreUnmappedEntityAttributes() {
        return ignoreUnmappedEntityAttributes;
    }

    /**
     * Sets whether to ignore unmapped/decoded EntityAttribute extensions as an optimization.
     * 
     * <p>Defaults to false. Only applies if {@link #getNameFormat()} property is non-null.</p>
     * 
     * @param flag flag to set
     */
    public void setIgnoreUnmappedEntityAttributes(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ignoreUnmappedEntityAttributes = flag;
    }
    
    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (attrName == null) {
            throw new ComponentInitializationException(getLogPrefix() + " Attribute name is null");
        } else if (nameFormat != null) {
            ignoreUnmappedEntityAttributes = false;
        }
    }

    /**
     * Checks to see if the entity returned by {@link #getEntityMetadata(AttributeFilterContext)} contains the entity
     * attribute specified by this matcher's configuration.
     * 
     * @param filterContext current request context
     * 
     * @return whether the entity has the configured attribute {@inheritDoc}
     */
    @Override public Tristate matches(@Nonnull final AttributeFilterContext filterContext) {

        Constraint.isNotNull(filterContext, "Context must be supplied");
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        final EntityDescriptor entityDescriptor = getEntityMetadata(filterContext);
        if (entityDescriptor == null) {
            log.debug("{} No metadata available for entity, returning FALSE", getLogPrefix());
            return Tristate.FALSE;
        }

        final Set<String> attributeValues = new LinkedHashSet<>();
        
        getEntityAttributeValues(entityDescriptor, entityDescriptor.getEntityID(), attributeValues);

        XMLObject parent = entityDescriptor.getParent();
        while (parent instanceof EntitiesDescriptor) {
            getEntityAttributeValues(parent, ((EntitiesDescriptor) parent).getName(), attributeValues);
            parent = parent.getParent();
        }
        
        if (attributeValues.isEmpty()) {
            log.debug("{} No values found for entity attribute {} for entity {}, returning FALSE",
                    getLogPrefix(), getAttributeName(), entityDescriptor.getEntityID());
            return Tristate.FALSE;
        }
        
        if (entityAttributeValueMatches(attributeValues)) {
            log.debug("{} Entity attribute values for {} match requirements", getLogPrefix(), getAttributeName());
            return Tristate.TRUE;
        }
        
        log.debug("{} Entity attribute values for {} do not match requirements", getLogPrefix(), getAttributeName());
        return Tristate.FALSE;
    }

    /**
     * Gets the entity descriptor for the entity to check.
     * 
     * @param filterContext current filter request context
     * 
     * @return entity descriptor for the entity to check or null if not found
     */
    @Nullable protected abstract EntityDescriptor getEntityMetadata(AttributeFilterContext filterContext);

    /**
     * Checks whether the given entity attribute's values match for particular implementations of this functor.
     * 
     * @param entityAttributeValues the entity attribute values
     * 
     * @return true if the value matches, false if not
     */
    protected abstract boolean entityAttributeValueMatches(
            @Nonnull @NotEmpty @NonnullElements final Set<String> entityAttributeValues);

    /**
     * Gets the entity attribute values from the given metadata.
     * 
     * <p>If both the attribute name and name format for this match functor is configured then both must match,
     * otherwise only the attribute name must match.</p>
     * 
     * @param metadataObject the metadata object
     * @param name name of metadata object
     * @param valueAccumulator stores values of the designated attribute
     */
// Checkstyle: CyclomaticComplexity OFF
    private void getEntityAttributeValues(@Nonnull final XMLObject metadataObject,
            @Nullable @NotEmpty final String name, @Nonnull @NonnullElements final Set<String> valueAccumulator) {
        
        if (nameFormat == null) {
            getMappedEntityAttributeValues(metadataObject, valueAccumulator);
            if (ignoreUnmappedEntityAttributes) {
                return;
            }
        }
        
        List<XMLObject> entityAttributesCollection = null;
        
        Extensions extensions = null;
        if (metadataObject instanceof EntityDescriptor) {
            extensions = ((EntityDescriptor) metadataObject).getExtensions();
        } else if (metadataObject instanceof EntitiesDescriptor) {
            extensions = ((EntitiesDescriptor) metadataObject).getExtensions();
        }
        
        if (extensions != null) {
            entityAttributesCollection = extensions.getUnknownXMLObjects(EntityAttributes.DEFAULT_ELEMENT_NAME);
        }
        
        if (entityAttributesCollection == null || entityAttributesCollection.isEmpty()) {
            log.debug("{} Metadata for {} does not contain EntityAttributes extension", getLogPrefix(), name);
            return;
        }
    
        if (entityAttributesCollection.size() > 1) {
            log.debug("{} Metadata for {} contains more than one EntityAttributes extension,"
                    + " only using the first one", getLogPrefix(), name);
        }
    
        final List<Attribute> entityAttributes =
                ((EntityAttributes) entityAttributesCollection.get(0)).getAttributes();
        if (entityAttributes == null || entityAttributes.isEmpty()) {
            log.debug("{} EntityAttributes extension for {} does not contain Attributes", getLogPrefix(), name);
            return;
        }
    
        for (final Attribute entityAttribute : entityAttributes) {
            if (!Objects.equals(getAttributeName(), entityAttribute.getName())) {
                continue;
            }
    
            if (getNameFormat() == null || (Objects.equals(getNameFormat(), entityAttribute.getNameFormat()))) {
                log.debug("{} Metadata for {} contains Attribute matching name {} and format {}",
                        new Object[] {getLogPrefix(), name, getAttributeName(), getNameFormat(),});
                
                valueAccumulator.addAll(
                        entityAttribute.getAttributeValues().stream().filter(v -> v != null).map(
                                this::getStringValue).collect(Collectors.toList()));
            }
        }
    }
// Checkstyle: CyclomaticComplexity ON

    /**
     * Gets the mapped entity attribute values from the given metadata.
     * 
     * @param metadataObject the metadata object
     * @param valueAccumulator stores values of the designated attribute
     */
    private void getMappedEntityAttributeValues(@Nonnull final XMLObject metadataObject,
            @Nonnull @NonnullElements final Set<String> valueAccumulator) {
        
        final List<AttributesMapContainer> containerList =
                metadataObject.getObjectMetadata().get(AttributesMapContainer.class);
        if (null == containerList || containerList.isEmpty() || containerList.get(0).get() == null ||
                containerList.get(0).get().isEmpty()) {
            log.debug("{} No mapped entity attributes found for {}", getLogPrefix(), attrName);
            return;
        }
        
        log.debug("{} Checking for mapped entity attributes named {}", getLogPrefix(), attrName);
        
        int count = 0;
        
        final Multimap<String, IdPAttribute> mappedAttributes = containerList.get(0).get();
        for (final IdPAttribute attribute : mappedAttributes.get(attrName)) {
            for (final IdPAttributeValue attributeValue : attribute.getValues()) {
                if (attributeValue instanceof StringAttributeValue) {
                    valueAccumulator.add(((StringAttributeValue) attributeValue).getValue());
                    count++;
                } else {
                    log.error("{} Ignoring non-string value in mapped entity attribute {}", getLogPrefix(), attrName);
                }
            }
        }
        
        log.debug("{} Added {} values of mapped entity attribute {} for evaluation", getLogPrefix(), count, attrName);
    }

    /**
     * Function to return an XMLObject in string form.
     * 
     * @param object object to decode
     * 
     * @return decoded string, or null
     */
// Checkstyle: CyclomaticComplexity OFF
    @Nullable private String getStringValue(@Nonnull final XMLObject object) {
        String retVal = null;

        if (object instanceof XSString) {

            retVal = ((XSString) object).getValue();

        } else if (object instanceof XSURI) {

            retVal = ((XSURI) object).getURI();

        } else if (object instanceof XSBoolean) {

            retVal = ((XSBoolean) object).getValue().getValue() ? "1" : "0";

        } else if (object instanceof XSInteger) {

            retVal = ((XSInteger) object).getValue().toString();

        } else if (object instanceof XSDateTime) {

            final Instant dt = ((XSDateTime) object).getValue();
            if (dt != null) {
                retVal = DOMTypeSupport.instantToString(dt);
            } else {
                retVal = null;
            }

        } else if (object instanceof XSBase64Binary) {

            retVal = ((XSBase64Binary) object).getValue();
            
        } else if (object instanceof ScopedValue) {
            
            retVal = ((ScopedValue) object).getValue();

        } else if (object instanceof XSAny) {

            final XSAny wc = (XSAny) object;
            if (wc.getUnknownAttributes().isEmpty() && wc.getUnknownXMLObjects().isEmpty()) {
                retVal = wc.getTextContent();
            } else {
                retVal = null;
            }
        }

        if (null == retVal) {
            log.info("Value of type {} could not be converted", object.getClass().getSimpleName());
        }
        return retVal;
    }
// Checkstyle: CyclomaticComplexity ON
    
}