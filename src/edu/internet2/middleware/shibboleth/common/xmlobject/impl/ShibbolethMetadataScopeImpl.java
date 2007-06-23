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

package edu.internet2.middleware.shibboleth.common.xmlobject.impl;

import java.util.List;

import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSBooleanValue;
import org.opensaml.xml.validation.AbstractValidatingXMLObject;

import edu.internet2.middleware.shibboleth.common.xmlobject.ShibbolethMetadataScope;

/**
 * Implementation of {@link ShibbolethMetadataScope}.
 */
public class ShibbolethMetadataScopeImpl extends AbstractValidatingXMLObject implements ShibbolethMetadataScope {
    
    /** The regexp attribute value. */
    private XSBooleanValue regexp;
    
    /** The string content value. */
    private String scopeValue;

    /**
     * Constructor.
     *
     * @param namespaceURI the namespace the element is in
     * @param elementLocalName the local name of the XML element this Object represents
     * @param namespacePrefix the prefix for the given namespace
     */
    protected ShibbolethMetadataScopeImpl(String namespaceURI, String elementLocalName, String namespacePrefix) {
        super(namespaceURI, elementLocalName, namespacePrefix);
        regexp = null;
    }

    /** {@inheritDoc} */
    public Boolean getRegexp() {
        if (regexp == null){
            return Boolean.FALSE;
        }
        return regexp.getValue();
    }

    /** {@inheritDoc} */
    public void setRegexp(Boolean newRegexp) {
        if (newRegexp != null){
            regexp = prepareForAssignment(regexp, new XSBooleanValue(newRegexp, false));
        } else {
            regexp = prepareForAssignment(regexp, null);
        }
    }

    /** {@inheritDoc} */
    public XSBooleanValue getRegexpXSBoolean() {
        return regexp;
    }

    /** {@inheritDoc} */
    public void setRegexp(XSBooleanValue newRegexp) {
        regexp = prepareForAssignment(regexp, newRegexp);
    }
    
    /** {@inheritDoc} */
    public String getValue() {
        return scopeValue;
    }

    /** {@inheritDoc} */
    public void setValue(String newScopeValue) {
        scopeValue = prepareForAssignment(scopeValue, newScopeValue);
    }
    
    /** {@inheritDoc} */
    public List<XMLObject> getOrderedChildren() {
        return null;
    }



}
