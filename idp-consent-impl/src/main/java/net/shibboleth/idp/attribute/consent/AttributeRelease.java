/*
 * Copyright 2009 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.attribute.consent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashSet;

import net.shibboleth.idp.attribute.Attribute;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.vt.middleware.crypt.digest.SHA256;
import edu.vt.middleware.crypt.util.HexConverter;

/**
 *
 */
public class AttributeRelease {
    
    private static final Logger logger = LoggerFactory.getLogger(AttributeRelease.class);
   
    private final String attributeId;
    private final String valuesHash;
    private final DateTime consentDate;
    
    public AttributeRelease(final String attributeId, final String valuesHash, final DateTime consentDate) {
        this.attributeId = attributeId;
        this.valuesHash = valuesHash;
    	this.consentDate = consentDate;
    }

    private AttributeRelease(final Attribute<?> attribute, final DateTime consentDate) {
        this.attributeId = attribute.getId();
        this.valuesHash = hashValues(attribute.getValues());
        this.consentDate = consentDate;
    }
    
    public String getAttributeId() {
        return attributeId;
    }

    public String getValuesHash() {
        return valuesHash;
    }

    public DateTime getDate() {
        return consentDate;
    }

    public static Collection<AttributeRelease> createAttributeReleases(Collection<Attribute<?>> attributes, DateTime date) {
        Collection<AttributeRelease> attributeReleases = new HashSet<AttributeRelease>();        
        for (Attribute attribute : attributes) {
            attributeReleases.add(new AttributeRelease(attribute, date));
        }
        return attributeReleases;
    }
    
    static String hashValues(final Collection<?> values) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream;
        try {
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(values);
            objectOutputStream.flush();
            objectOutputStream.close();
            byteArrayOutputStream.close();
        } catch (IOException e) {
            logger.error("Error while converting attribute values into a byte array", e);
            return null;
        }
        return new SHA256().digest(byteArrayOutputStream.toByteArray(), new HexConverter(true));
    }
    
    public boolean contains(Attribute<?> attribute) {
        return attributeId.equals(attribute.getId()) && 
            valuesHash.equals(hashValues(attribute.getValues()));
    }
}
