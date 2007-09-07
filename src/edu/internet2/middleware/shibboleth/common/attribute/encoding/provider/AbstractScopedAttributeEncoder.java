/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.attribute.encoding.provider;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.opensaml.Configuration;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.schema.impl.XSStringBuilder;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ScopedAttributeValue;
import edu.internet2.middleware.shibboleth.common.xmlobject.ShibbolethScopedValue;

/**
 * Base class for scoped attribute encoders.
 * 
 * @param <EncodedType> the type of object created by encoding the attribute
 */
public abstract class AbstractScopedAttributeEncoder<EncodedType> extends AbstractAttributeEncoder<EncodedType> {

    /** Type of scoping to use. */
    private String scopeType;

    /** Delimeter used for "inline" scopeType. */
    private String scopeDelimiter;

    /** Attribute name used for "attribute" scopeType. */
    private String scopeAttribute;

    /**
     * Get the scope attribute.
     * 
     * @return Returns the scopeAttribute.
     */
    public String getScopeAttribute() {
        return scopeAttribute;
    }

    /**
     * Get the scope delimiter.
     * 
     * @return Returns the scopeDelimiter.
     */
    public String getScopeDelimiter() {
        return scopeDelimiter;
    }

    /**
     * Get the scope type.
     * 
     * @return Returns the scopeType.
     */
    public String getScopeType() {
        return scopeType;
    }

    /**
     * Set the scope attribute.
     * 
     * @param newScopeAttribute The scopeAttribute to set.
     */
    public void setScopeAttribute(String newScopeAttribute) {
        scopeAttribute = newScopeAttribute;
    }

    /**
     * Set the scope delimiter.
     * 
     * @param newScopeDelimiter The scopeDelimiter to set.
     */
    public void setScopeDelimiter(String newScopeDelimiter) {
        scopeDelimiter = newScopeDelimiter;
    }

    /**
     * Set the scope type.
     * 
     * @param newScopeType The scopeType to set.
     */
    public void setScopeType(String newScopeType) {
        scopeType = newScopeType;
    }

    /**
     * Encodes attributes whose values are scoped.
     * 
     * @param objectName name of the attribute value element to create for each value
     * @param attribute the attribute whose values will be encoded
     * 
     * @return the list of encoded attribute values
     */
    @SuppressWarnings("unchecked")
    protected List<XMLObject> encodeAttributeValues(QName objectName, BaseAttribute<ScopedAttributeValue> attribute) {
        ArrayList<XMLObject> encodedValues = new ArrayList<XMLObject>();

        if ("attribute".equals(getScopeType())) {
            XMLObjectBuilder<ShibbolethScopedValue> valueBuilder = Configuration.getBuilderFactory().getBuilder(
                    ShibbolethScopedValue.TYPE_NAME);
            ShibbolethScopedValue scopedValue;
            
            for (ScopedAttributeValue attributeValue : attribute.getValues()) {
                if (attributeValue == null) {
                    continue;
                }

                scopedValue = valueBuilder.buildObject(objectName);
                scopedValue.setScopeAttributeName(getScopeAttribute());
                scopedValue.setScope(attributeValue.getScope());
                scopedValue.setValue(attributeValue.getValue());

                encodedValues.add(scopedValue);
            }

        } else if ("inline".equals(getScopeType())) {
            XSStringBuilder valueBuilder = new XSStringBuilder();
            XSString scopedValue;
            
            for (ScopedAttributeValue attributeValue : attribute.getValues()) {
                if (attributeValue == null) {
                    continue;
                }

                scopedValue = valueBuilder.buildObject(objectName, XSString.TYPE_NAME);
                scopedValue.setValue(attributeValue.getValue() + getScopeDelimiter() + attributeValue.getScope());

                encodedValues.add(scopedValue);
            }

        }

        return encodedValues;
    }
}