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
import java.util.regex.Pattern;

import javax.annotation.concurrent.NotThreadSafe;

import net.shibboleth.idp.saml.xmlobject.Scope;

import org.opensaml.core.xml.AbstractXMLObject;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSBooleanValue;

/** Implementation of {@link Scope}. */
@NotThreadSafe
public class ScopeImpl extends AbstractXMLObject implements Scope {

    /** The regexp attribute value. */
    private XSBooleanValue regexp;

    /** The string content value. */
    private String scopeValue;

    /** Pattern used to match scopes against criteria. */
    private Pattern matchPattern;

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
    public Boolean getRegexp() {
        if (regexp == null) {
            return Boolean.FALSE;
        }
        return regexp.getValue();
    }

    /** {@inheritDoc} */
    public void setRegexp(final Boolean newRegexp) {
        if (newRegexp != null) {
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
    public void setRegexp(final XSBooleanValue newRegexp) {
        regexp = prepareForAssignment(regexp, newRegexp);
    }

    /** {@inheritDoc} */
    public String getValue() {
        return scopeValue;
    }

    /** {@inheritDoc} */
    public void setValue(final String newScopeValue) {
        scopeValue = prepareForAssignment(scopeValue, newScopeValue);
        matchPattern = null;
    }

    /** {@inheritDoc} */
    public Pattern getMatchPattern() {
        if (getRegexp() != Boolean.TRUE) {
            throw new IllegalStateException("Scope value is not indicated to be a regex");
        }
        if (matchPattern == null) {
            matchPattern = Pattern.compile(scopeValue);
        }
        return matchPattern;
    }
    
    /** {@inheritDoc} */
    public List<XMLObject> getOrderedChildren() {
        return null;
    }
}