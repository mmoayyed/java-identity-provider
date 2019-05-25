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

package net.shibboleth.idp.saml.xmlobject.impl;

import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import org.opensaml.core.xml.AbstractXMLObject;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSBooleanValue;

import net.shibboleth.idp.saml.xmlobject.Scope;

/** Implementation of {@link Scope}. */
@NotThreadSafe
public class ScopeImpl extends AbstractXMLObject implements Scope {

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
    protected ScopeImpl(final String namespaceURI, final String elementLocalName, final String namespacePrefix) {
        super(namespaceURI, elementLocalName, namespacePrefix);
        regexp = null;
    }

    /** {@inheritDoc} */
    @Override public Boolean getRegexp() {
        if (regexp == null) {
            return Boolean.FALSE;
        }
        return regexp.getValue();
    }

    /** {@inheritDoc} */
    @Override public void setRegexp(final Boolean newRegexp) {
        if (newRegexp != null) {
            regexp = prepareForAssignment(regexp, new XSBooleanValue(newRegexp, false));
        } else {
            regexp = prepareForAssignment(regexp, null);
        }
    }

    /** {@inheritDoc} */
    @Override public XSBooleanValue getRegexpXSBoolean() {
        return regexp;
    }

    /** {@inheritDoc} */
    @Override public void setRegexp(final XSBooleanValue newRegexp) {
        regexp = prepareForAssignment(regexp, newRegexp);
    }

    /** {@inheritDoc} */
    @Override public String getValue() {
        return scopeValue;
    }

    /** {@inheritDoc} */
    @Override public void setValue(final String newScopeValue) {
        scopeValue = prepareForAssignment(scopeValue, newScopeValue);
    }
    
    /** {@inheritDoc} */
    @Override public List<XMLObject> getOrderedChildren() {
        return null;
    }
}