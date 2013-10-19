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

package net.shibboleth.idp.attribute.resolver.impl.ad;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An encapsulated Attribute suitable for handing to scripts. This handles some of the cumbersome issues associated with
 * {@link IdPAttribute} and also a lot of the V2 backwards compatibility stuff. <br/>
 * NOTE, the java signature for this class may and will change on minor version changes. However the Scripting interface
 * will remain the same (methods will never be removed).
 */
public class ScriptedAttribute {

    /** The {@link IdPAttribute} we are encapsulating. */
    private final IdPAttribute encapsulatedAttribute;

    /** Logger. */
    private final Logger log = LoggerFactory.getLogger(ScriptedAttribute.class);

    /** has method {@link #getNativeAttribute()} be called. */
    private boolean calledGetNativeAttribute;

    /**
     * All the {@link StringAttributeValue}, but as strings.<br/>
     * All other attributes as their native representation. If null then the {@link #getValues()} method has not been
     * called.
     */
    private Collection<Object> attributeValues;

    /** The prefix for logging. Derived from the definition's logPrefix and the attribute ID. */
    private final String logPrefix;

    /**
     * Constructor.
     * 
     * @param attribute the attribute we are encapsulating.
     * @param prefix the log path from the definition.
     */
    public ScriptedAttribute(@Nonnull IdPAttribute attribute, String prefix) {
        encapsulatedAttribute = attribute;

        logPrefix =
                new StringBuilder(prefix).append(" scripted attribute '").append(attribute.getId()).append("':")
                        .toString();
    }

    /**
     * Return all the values, but with {@link StringAttributeValue} values returned as strings.<br/>
     * This method is a helper method for V2 compatibility.
     * 
     * @return a modifiable collection of the string attributes (not the String
     * @throws ResolutionException if the script has called {@link #getNativeAttribute()}
     */
    @Nullable @NonnullElements public Collection<Object> getValues() throws ResolutionException {
        if (calledGetNativeAttribute) {
            throw new ResolutionException(getLogPrefix()
                    + " cannot call getNativeAttribute() and getValues() on the same attribute()");
        }
        if (null != attributeValues) {
            return attributeValues;
        }

        log.debug("{} values being prepared", getLogPrefix());

        // NOTE. This has to be a List - the examples use get(0)
        ArrayList<Object> newValues = new ArrayList<Object>(encapsulatedAttribute.getValues().size());
        for (AttributeValue value : encapsulatedAttribute.getValues()) {
            if ((value instanceof StringAttributeValue) && !(value instanceof ScopedStringAttributeValue)) {
                newValues.add(((StringAttributeValue) value).getValue());
            } else {
                newValues.add(value);
            }
        }
        attributeValues = newValues;
        log.debug("{} values are : {}", getLogPrefix(), newValues);
        return newValues;
    }

    /**
     * return the underlying attribute.
     * 
     * @return the attribute
     * @throws ResolutionException if the script has called getValues.
     */
    @Nonnull public IdPAttribute getNativeAttribute() throws ResolutionException {
        if (null != attributeValues) {
            throw new ResolutionException(getLogPrefix()
                    + "': cannot call getNativeAttribute() and getValues() on the same attribute()");
        }
        calledGetNativeAttribute = true;
        return encapsulatedAttribute;
    }

    /**
     * Get the encapsulated attributeId.
     * 
     * @return the id
     */
    @Nonnull @NotEmpty public String getId() {
        return encapsulatedAttribute.getId();
    }

    /**
     * Add the provided value to the provided set, converting {@link String} to {@link StringAttributeValue}.
     * 
     * @param values the set to add to.
     * @param value the value to add. Known to be a {@link String} or an {@link AttributeValue}
     */
    private void addValue(@Nonnull Set<AttributeValue> values, @Nonnull Object value) {
        if (value instanceof String) {
            values.add(new StringAttributeValue((String) value));
        } else {
            values.add((AttributeValue) value);
        }
    }

    /**
     * Check that provided object is of type {@link String} or {@link AttributeValue}.
     * 
     * @param what value to check
     * @throws ResolutionException if there is a type conflict.
     */
    private void policeValueType(@Nullable Object what) throws ResolutionException {
        if (null == what) {
            throw new ResolutionException(getLogPrefix() + " added element was null");

        } else if (!(what instanceof String) && !(what instanceof AttributeValue)) {
            throw new ResolutionException(getLogPrefix()
                    + " added element must be a String or AttributeValue, provided = " + what.getClass().toString());
        }
    }

    /**
     * Add the provided object to the attribute values, policing for type.
     * 
     * @param what a {@link String} or a {@link AttributeValue} to add.
     * @throws ResolutionException if the provided value is of the wrong type
     */
    public void addValue(@Nullable Object what) throws ResolutionException {
        policeValueType(what);

        if (null != attributeValues) {
            // We have called getValues - this is what we will keep up to date
            attributeValues.add(what);
        } else {
            addValue(encapsulatedAttribute.getValues(), what);
        }
    }

    /**
     * Function to reconstruct the attribute after the scripting. If {@link #getValues()} has been called then this is
     * taken as the content and the attribute updated, otherwise the Attribute is returned.
     * 
     * @return a suitable modified attribute.
     * @throws ResolutionException if we find the wrong type.
     */
    @Nonnull protected IdPAttribute getResultingAttribute() throws ResolutionException {

        if (null == attributeValues) {
            log.debug("{} return initial attribute unchanged", getLogPrefix());
            return encapsulatedAttribute;
        }

        // Otherwise re-marshall the {@link #attributeValues}
        Set<AttributeValue> values = new HashSet<AttributeValue>(attributeValues.size());

        log.debug("{} recreating attribute contents from {}", getLogPrefix(), attributeValues);
        for (Object object : attributeValues) {
            policeValueType(object);
            addValue(values, object);
        }
        encapsulatedAttribute.setValues(values);
        log.debug("{} recreated attribute contents are {}", getLogPrefix(), values);
        return encapsulatedAttribute;
    }

    /**
     * The prefix for the logs.
     * 
     * @return the prefix
     */
    @Nonnull protected String getLogPrefix() {
        return logPrefix;
    }
}
