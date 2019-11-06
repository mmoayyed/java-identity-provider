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

package net.shibboleth.idp.consent.logic.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.cryptacular.util.CodecUtil;
import org.cryptacular.util.HashUtil;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.NameIDType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.XMLObjectAttributeValue;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;

/**
 * Function to calculate the hash of the values of an IdP attribute.
 * 
 * Returns <code>null</code> for a <code>null</code> input or empty collection of IdP attribute values.
 * <code>Null</code> IdP attribute values are ignored.
 * 
 * The hash returned is the Base64 encoded representation of the SHA-256 digest.
 */
public class AttributeValuesHashFunction implements Function<Collection<IdPAttributeValue>, String> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AttributeValuesHashFunction.class);

    // CheckStyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Nullable public String apply(@Nullable @NonnullElements final Collection<IdPAttributeValue> input) {

        if (input == null) {
            return null;
        }

        final Collection<IdPAttributeValue> filteredInput = List.copyOf(input);

        if (filteredInput.isEmpty()) {
            return null;
        }

        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {

            for (final IdPAttributeValue value : filteredInput) {
                if (value instanceof ScopedStringAttributeValue) {
                    objectOutputStream.writeObject(((ScopedStringAttributeValue) value).getValue() + '@'
                            + ((ScopedStringAttributeValue) value).getScope());
                } else if (value instanceof XMLObjectAttributeValue) {
                    final XMLObject xmlObject = ((XMLObjectAttributeValue) value).getValue();
                    if (xmlObject instanceof NameIDType) {
                        objectOutputStream.writeObject(((NameIDType) xmlObject).getValue());
                    } else {
                        try {
                            objectOutputStream.writeObject(SerializeSupport.nodeToString(
                                    XMLObjectSupport.marshall(xmlObject)));
                        } catch (final MarshallingException e) {
                            log.error("Error while marshalling XMLObject value", e);
                            return null;
                        }
                    }
                } else if (value instanceof StringAttributeValue) {
                    objectOutputStream.writeObject(((StringAttributeValue)value).getValue());
                
                } else if (value.getNativeValue() != null) {
                    objectOutputStream.writeObject(value.getNativeValue());
                }
            }

            objectOutputStream.flush();
            return CodecUtil.b64(HashUtil.sha256(byteArrayOutputStream.toByteArray()));

        } catch (final IOException e) {
            log.error("Error while converting attribute values into a byte array", e);
            return null;
        }
    }
    // CheckStyle: CyclomaticComplexity ON
    
}