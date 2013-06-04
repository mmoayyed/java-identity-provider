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

package net.shibboleth.idp.attribute.filter.impl.saml;

import java.util.List;

import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.MatcherException;
import net.shibboleth.idp.attribute.filter.impl.matcher.AbstractComparisonMatcher;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;

/**
 * Base class for match functions that check whether a particular entity attribute is present and contains a given
 * value.
 */
public abstract class AbstractEntityAttributeMatcher extends AbstractComparisonMatcher {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractEntityAttributeMatcher.class);

    /** The name of the entity attribute the entity must have. */
    private String name;

    /** The name format of the entity attribute the entity must have. */
    private String nameFormat;

    /** Constructor. */
    public AbstractEntityAttributeMatcher() {
        setPolicyPredicate(new Predicate<AttributeFilterContext>() {

            public boolean apply(@Nullable AttributeFilterContext input) {
                return hasEntityAttribute(input);
            }
        });
    }

    /**
     * Gets the name of the entity attribute the entity must have.
     * 
     * @return name of the entity attribute the entity must have
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the entity attribute the entity must have.
     * 
     * @param attributeName name of the entity attribute the entity must have
     */
    public void setAttributeName(String attributeName) {
        name = attributeName;
    }

    /**
     * Gets the name format of the entity attribute the entity must have.
     * 
     * @return name format of the entity attribute the entity must have
     */
    public String getNameFormat() {
        return nameFormat;
    }

    /**
     * Sets the name format of the entity attribute the entity must have.
     * 
     * @param attributeNameFormat name format of the entity attribute the entity must have
     */
    public void setNameFormat(String attributeNameFormat) {
        nameFormat = StringSupport.trimOrNull(attributeNameFormat);
    }

    /**
     * Checks to see if the entity returned by {@link #getEntityMetadata(ShibbolethFilteringContext)} contains the
     * entity attribute specified by this functor's configuration.
     * 
     * @param filterContext current request context
     * 
     * @return true if the entity has the configured attribute, false otherwise
     */
    protected boolean hasEntityAttribute(AttributeFilterContext filterContext) {
        EntityDescriptor entityDescriptor = getEntityMetadata(filterContext);
        if (entityDescriptor == null) {
            throw new MatcherException(getLogPrefix() + " No metadata available for the entity");
        }

        Attribute entityAttribute = getEntityAttribute(entityDescriptor);
        if (entityAttribute == null) {
            return false;
        }

        List<XMLObject> attributeValues = entityAttribute.getAttributeValues();
        if (attributeValues == null || attributeValues.isEmpty()) {
            log.debug("{} Entity attribute {} for entity {} does not contain any values", getLogPrefix(), getName(),
                    entityDescriptor.getEntityID());
            return false;
        }

        log.debug("{} Checking if entity attribute {} contains the required value.", getLogPrefix(), getName());
        String valueString;
        for (XMLObject attributeValue : attributeValues) {
            if (attributeValue instanceof XSAny) {
                valueString = ((XSAny) attributeValue).getTextContent();
            } else if (attributeValue instanceof XSString) {
                valueString = ((XSString) attributeValue).getValue();
            } else {
                log.debug("{} Entity attribute {} contains the unsupported value type {}, skipping it", getLogPrefix(),
                        getName(), attributeValue.getClass().getName());
                continue;
            }

            if (valueString != null) {
                if (entityAttributeValueMatches(valueString)) {
                    log.debug("{} Entity attribute {} value {} meets matching requirements", getLogPrefix(), getName(),
                            valueString);
                    return true;
                }
                log.debug("{} Entity attribute {} value {} does not meet matching requirements", getLogPrefix(),
                        getName(), valueString);
            }
        }

        return false;
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

        return compareAttributes(entityAttributes, entityDescriptor);
    }

    /**
     * Helper function for {@link #getEntityAttribute(EntityDescriptor)}. return an attribute that matches.
     * 
     * @param entityAttributes the list of attributes
     * @param entityDescriptor the entity in question
     * @return an attribute that matches or null
     */
    @Nullable private Attribute compareAttributes(List<Attribute> entityAttributes, EntityDescriptor entityDescriptor) {

        for (Attribute entityAttribute : entityAttributes) {
            if (!Objects.equal(getName(), entityAttribute.getName())) {
                continue;
            }

            if (getNameFormat() == null || (Objects.equal(getNameFormat(), entityAttribute.getNameFormat()))) {
                log.debug("{} Descriptor for {} contains an entity attribute with the name {} and the format {}",
                        new Object[] {getLogPrefix(), entityDescriptor.getEntityID(), getName(), getNameFormat()});
                return entityAttribute;
            }
        }

        log.debug("{} Descriptor for {} does not contain an entity attribute with the name {} and the format {}",
                new Object[] {getLogPrefix(), entityDescriptor.getEntityID(), getName(), getNameFormat()});
        return null;
    }

    /**
     * Gets the entity descriptor for the entity to check.
     * 
     * @param filterContext current filter request context
     * 
     * @return entity descriptor for the entity to check
     */
    protected abstract EntityDescriptor getEntityMetadata(AttributeFilterContext filterContext);

    /**
     * Checks whether the given entity attribute value matches the rules for particular implementation of this functor.
     * 
     * @param entityAttributeValue the entity attribute value, never null
     * 
     * @return true if the value matches, false if not
     */
    protected abstract boolean entityAttributeValueMatches(String entityAttributeValue);

}
