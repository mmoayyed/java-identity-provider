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

package net.shibboleth.idp.saml.profile.config;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * A strategy function that examines SAML metadata associated with a relying party and derives bean-based
 * configuration settings based on EntityAttribute extension tags.
 * 
 * <p>Defaults to no caching of the result to avoid bean lifecycle issues if relying party config is reloaded.</p>
 * 
 * @param <T> type of bean
 * 
 * @since 3.4.0
 */
public class BeanConfigurationLookupStrategy<T> extends AbstractMetadataDrivenConfigurationLookupStrategy<T>
        implements ApplicationContextAware {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(BeanConfigurationLookupStrategy.class);

    /** Enclosing Spring context. */
    @Nullable private ApplicationContext applicationContext;
    
    /** Type of bean to return. */
    @NonnullAfterInit private Class<T> propertyType;
    
    /** Constructor. */
    public BeanConfigurationLookupStrategy() {
        setEnableCaching(false);
    }
    
    /**
     * Set the type of bean to search for.
     * 
     * @param type bean type
     */
    public void setPropertyType(@Nonnull final Class<T> type) {
        throwSetterPreconditionExceptions();
        propertyType = Constraint.isNotNull(type, "Property type cannot be null");
    }

    /** {@inheritDoc} */
    public void setApplicationContext(final ApplicationContext context) throws BeansException {
        throwSetterPreconditionExceptions();
        applicationContext = context;
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (propertyType == null || applicationContext == null) {
            throw new ComponentInitializationException("Property type and Spring ApplicationContext cannot be null");
        }
    }
    
    /** {@inheritDoc} */
    @Override
    @Nullable protected T doTranslate(@Nonnull final IdPAttribute tag) {
        
        final List<IdPAttributeValue> values = tag.getValues();
        if (values.size() != 1) {
            log.error("Tag '{}' contained multiple values, returning none", tag.getId());
            return null;
        }

        log.debug("Converting tag '{}' to Bean property of tyoe '{}'", tag.getId(), propertyType.getSimpleName());
        
        final IdPAttributeValue value = values.get(0);
        if (value instanceof StringAttributeValue) {
            try {
                return applicationContext.getBean(((StringAttributeValue) value).getValue(), propertyType);
            } catch (final BeansException e) {
                log.error("Error locating appropriately typed bean named {}",
                        ((StringAttributeValue) value).getValue(), e);
                return null;
            }
        }
        log.error("Tag '{}' contained non-string value, returning null");
        return null;
    }
    
    /** {@inheritDoc} */
    @Override
    @Nullable protected T doTranslate(@Nonnull final Attribute tag) {
        
        final List<XMLObject> values = tag.getAttributeValues();
        if (values.size() != 1) {
            log.error("Tag '{}' contained multiple values, returning none", tag.getName());
            return null;
        }
        
        log.debug("Converting tag '{}' to Bean property of tyoe '{}'", tag.getName(), propertyType.getSimpleName());
        return xmlObjectToBean(values.get(0));
    }
    
    /**
     * Convert an XMLObject to a Spring bean reference if the type is supported.
     * 
     * @param object object to convert
     * 
     * @return the converted value, or null
     */
    @Nullable private T xmlObjectToBean(@Nonnull final XMLObject object) {
        String value = null;
        if (object instanceof XSString) {
            value = ((XSString) object).getValue();
        } else if (object instanceof XSAny) {
            final XSAny wc = (XSAny) object;
            if (wc.getUnknownAttributes().isEmpty() && wc.getUnknownXMLObjects().isEmpty()) {
                value = wc.getTextContent();
            }
        }
        
        if (value != null) {
            try {
                return applicationContext.getBean(value, propertyType);
            } catch (final BeansException e) {
                log.error("Error locating appropriately typed bean named {}", value, e);
                return null;
            }
        }
        
        log.error("Unsupported conversion to Spring bean from XMLObject type ({})", object.getClass().getName());
        return null;
    }
    
}