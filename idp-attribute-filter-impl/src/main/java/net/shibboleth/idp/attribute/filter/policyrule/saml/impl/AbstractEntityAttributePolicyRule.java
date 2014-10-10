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

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.policyrule.impl.AbstractPolicyRule;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for matchers that check whether a particular entity attribute is present and contains a given value.<br/>
 * 
 * Given the metadata for an entity, this class takes care of navigation to the attribute and extracting the values
 * 
 * Classes wishing to implement Entity Attribute matchers implement {@link #getEntityMetadata(AttributeFilterContext)}
 * to navigate to the entity (probably recipient or issuer) and {@link #entityAttributeValueMatches(String)} to
 * implement the comparison (probably string or regexp).
 */
public abstract class AbstractEntityAttributePolicyRule extends AbstractPolicyRule {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractEntityAttributePolicyRule.class);

    /** The name of the entity attribute the entity must have. */
    private String attrName;

    /** The name format of the entity attribute the entity must have. */
    private String nameFormat;

    /**
     * Gets the name of the entity attribute the entity must have.
     * 
     * @return name of the entity attribute the entity must have
     */
    @NonnullAfterInit public String getAttributeName() {
        return attrName;
    }

    /**
     * Sets the name of the entity attribute the entity must have.
     * 
     * @param attributeName name of the entity attribute the entity must have
     */
    public void setAttributeName(@Nullable final String attributeName) {
        attrName = StringSupport.trimOrNull(attributeName);
    }

    /**
     * Gets the name format of the entity attribute the entity must have.
     * 
     * @return name format of the entity attribute the entity must have
     */
    @Nullable public String getNameFormat() {
        return nameFormat;
    }

    /**
     * Sets the name format of the entity attribute the entity must have.
     * 
     * @param attributeNameFormat name format of the entity attribute the entity must have
     */
    public void setNameFormat(@Nullable final String attributeNameFormat) {
        nameFormat = StringSupport.trimOrNull(attributeNameFormat);
    }

    /**
     * Checks to see if the entity returned by {@link #getEntityMetadata(AttributeFilterContext)} contains the
     * entity attribute specified by this matcher's configuration.
     * 
     * @param filterContext current request context
     * 
     * @return whether the entity has the configured attribute {@inheritDoc}
     */
    public Tristate matches(@Nonnull AttributeFilterContext filterContext) {

        Constraint.isNotNull(filterContext, "Context must be supplied");
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        EntityDescriptor entityDescriptor = getEntityMetadata(filterContext);
        if (entityDescriptor == null) {
            log.warn("{} No metadata available for the entity, returning FAIL", getLogPrefix());
            return Tristate.FAIL;
        }

        Attribute entityAttribute = getEntityAttribute(entityDescriptor);
        if (entityAttribute == null) {
            log.debug("{} No entityAttribute for the entity, returning FALSE", getLogPrefix());
            return Tristate.FALSE;
        }

        List<XMLObject> attributeValues = entityAttribute.getAttributeValues();
        if (attributeValues == null || attributeValues.isEmpty()) {
            log.debug("{} Entity attribute {} for entity {} does not contain any values, returning FALSE",
                    getLogPrefix(), getAttributeName(), entityDescriptor.getEntityID());
            return Tristate.FALSE;
        }

        log.debug("{} Checking if entity attribute {} contains the required value.", getLogPrefix(), 
                getAttributeName());
        String valueString;
        for (XMLObject attributeValue : attributeValues) {
            if (attributeValue instanceof XSAny) {
                valueString = ((XSAny) attributeValue).getTextContent();
            } else if (attributeValue instanceof XSString) {
                valueString = ((XSString) attributeValue).getValue();
            } else {
                log.debug("{} Entity attribute {} contains the unsupported value type {}, skipping it", getLogPrefix(),
                        getAttributeName(), attributeValue.getClass().getName());
                continue;
            }

            if (valueString != null) {
                if (entityAttributeValueMatches(valueString)) {
                    log.debug("{} Entity attribute {} value {} meets matching requirements", getLogPrefix(),
                            getAttributeName(), valueString);
                    return Tristate.TRUE;
                }
                log.debug("{} Entity attribute {} value {} does not meet matching requirements", getLogPrefix(),
                        getAttributeName(), valueString);
            }
        }

        return Tristate.FALSE;
    }

    /**
     * Gets the entity attribute from the given entity metadata. If both the attribute name and name format for this
     * match functor is configured then both must match, otherwise only the attribute name must match.
     * 
     * @param entityDescriptor the metadata for the entity
     * 
     * @return the entity or null if the metadata does not contain such an entity attribute
     */
    @Nullable protected Attribute getEntityAttribute(EntityDescriptor entityDescriptor) {
        List<XMLObject> entityAttributesCollection = null;
        if (entityDescriptor.getExtensions() != null) {
            entityAttributesCollection =
                    entityDescriptor.getExtensions().getUnknownXMLObjects(EntityAttributes.DEFAULT_ELEMENT_NAME);
        }
        if (entityAttributesCollection == null || entityAttributesCollection.isEmpty()) {
            log.debug("{} Descriptor for {} does not contain any EntityAttributes", getLogPrefix(),
                    entityDescriptor.getEntityID());
            return null;
        }

        if (entityAttributesCollection.size() > 1) {
            log.debug("{} Descriptor for {} contains more than EntityAttributes extension, only using the first one",
                    getLogPrefix(), entityDescriptor.getEntityID());
        }

        List<Attribute> entityAttributes = ((EntityAttributes) entityAttributesCollection.get(0)).getAttributes();
        if (entityAttributes == null || entityAttributes.isEmpty()) {
            log.debug("{} EntityAttributes extension for {} does not contain any Attributes", getLogPrefix(),
                    entityDescriptor.getEntityID());
            return null;
        }

        //  TODO this is syntatic sugar for checkstyle.  Remove and relax the rules. 
        return compareAttributes(entityAttributes, entityDescriptor);
    }

    /**
     * Helper function for {@link #getEntityAttribute(EntityDescriptor)}. Having done all the null checking in
     * {@link #getEntityAttribute(EntityDescriptor)}, this function actually does the match as per the rules:<br/>
     * If both the attribute name and name format for this match functor is configured then both must match, otherwise
     * only the attribute name must match.
     * 
     * @param entityAttributes the list of attributes
     * @param entityDescriptor the entity in question
     * @return an attribute that matches or null
     */
    @Nullable private Attribute compareAttributes(List<Attribute> entityAttributes, EntityDescriptor entityDescriptor) {

        for (Attribute entityAttribute : entityAttributes) {
            if (!Objects.equals(getAttributeName(), entityAttribute.getName())) {
                continue;
            }

            if (getNameFormat() == null || (Objects.equals(getNameFormat(), entityAttribute.getNameFormat()))) {
                log.debug("{} Descriptor for {} contains an entity attribute with the name {} and the format {}",
                        new Object[] {getLogPrefix(), entityDescriptor.getEntityID(), getAttributeName(),
                                getNameFormat(),});
                return entityAttribute;
            }
        }

        log.debug("{} Descriptor for {} does not contain an entity attribute with the name {} and the format {}",
                new Object[] {getLogPrefix(), entityDescriptor.getEntityID(), getAttributeName(), getNameFormat()});
        return null;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (attrName == null) {
            throw new ComponentInitializationException(getLogPrefix() + " Attribute name is null");
        }
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
     * Checks whether the given entity attribute value matches the rules for particular implementation of this functor.
     * 
     * @param entityAttributeValue the entity attribute value, never null
     * 
     * @return true if the value matches, false if not
     */
    protected abstract boolean entityAttributeValueMatches(String entityAttributeValue);

}
