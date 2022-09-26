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

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSBase64Binary;
import org.opensaml.core.xml.schema.XSBoolean;
import org.opensaml.core.xml.schema.XSBooleanValue;
import org.opensaml.core.xml.schema.XSDateTime;
import org.opensaml.core.xml.schema.XSInteger;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.schema.XSURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;

/**
 * A strategy function that examines SAML metadata associated with a relying party and derives List&lt;String&gt;-valued
 * configuration settings based on EntityAttribute extension tags.
 * 
 * @param <T1> type of collection member
 * @param <T2> type of collection itself
 * 
 * @since 3.4.0
 */
public abstract class AbstractCollectionConfigurationLookupStrategy<T1,T2>
    extends AbstractMetadataDrivenConfigurationLookupStrategy<T2> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractCollectionConfigurationLookupStrategy.class);

    /** Type of bean in collection. */
    @NonnullAfterInit private Class<T1> propertyType;
    
    /**
     * Get the type of object to coerce collection elements into.
     * 
     * @return object type
     */
    @NonnullAfterInit public Class<T1> getPropertyType() {
        return propertyType;
    }
    
    /**
     * Set the type of object to coerce collection elements into.
     * 
     * @param type object type
     */
    public void setPropertyType(@Nonnull final Class<T1> type) {
        checkSetterPreconditions();
        propertyType = Constraint.isNotNull(type, "Property type cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (propertyType == null) {
            throw new ComponentInitializationException("Property type cannot be null");
        }
    }

    /**
     * Helper method to manufacture instance of object using a string constructor or a cast.
     * 
     * @param input the input string
     * 
     * @return the new object or the existing object if casting is possible
     * 
     * @throws ReflectiveOperationException if the attempt fails
     */
    protected T1 createInstanceFromString(@Nonnull @NotEmpty final String input) throws ReflectiveOperationException {
        if (propertyType.isAssignableFrom(input.getClass())) {
            return propertyType.cast(input);
        }
        
        return propertyType.getConstructor(String.class).newInstance(input);
    }
    
 // Checkstyle: CyclomaticComplexity OFF
    /**
     * Convert an XMLObject to a String if the type is supported.
     * 
     * @param object object to convert
     * 
     * @return the converted value, or null
     */
    @Nullable protected String xmlObjectToString(@Nonnull final XMLObject object) {
        if (object instanceof XSString) {
            return ((XSString) object).getValue();
        } else if (object instanceof XSURI) {
            return ((XSURI) object).getURI();
        } else if (object instanceof XSBoolean) {
            final XSBooleanValue value = ((XSBoolean) object).getValue();
            return value != null ? (value.getValue() ? "1" : "0") : null;
        } else if (object instanceof XSInteger) {
            final Integer value = ((XSInteger) object).getValue();
            return value != null ? value.toString() : null;
        } else if (object instanceof XSDateTime) {
            final Instant dt = ((XSDateTime) object).getValue();
            return dt != null ? Long.toString(dt.toEpochMilli()) : null;
        } else if (object instanceof XSBase64Binary) {
            return ((XSBase64Binary) object).getValue();
        } else if (object instanceof XSAny) {
            final XSAny wc = (XSAny) object;
            if (wc.getUnknownAttributes().isEmpty() && wc.getUnknownXMLObjects().isEmpty()) {
                return wc.getTextContent();
            }
        }
        
        log.error("Unsupported conversion to String from XMLObject type ({})", object.getClass().getName());
        return null;
    }
// Checkstyle: CyclomaticComplexity ON
}