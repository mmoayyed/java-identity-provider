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

package net.shibboleth.idp.saml.impl.xmlobject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.jcip.annotations.NotThreadSafe;
import net.shibboleth.idp.saml.xmlobject.KeyAuthority;

import org.opensaml.xml.XMLObject;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.util.AttributeMap;
import org.opensaml.xml.util.XMLObjectChildrenList;
import org.opensaml.xml.validation.AbstractValidatingXMLObject;

/** Implementation of {@link KeyAuthority}. */
@NotThreadSafe
public class KeyAuthorityImpl extends AbstractValidatingXMLObject implements KeyAuthority {

    /** The list of KeyInfo child elements. */
    private final List<KeyInfo> keyInfos;

    /** The VerifyDepth attribute. */
    private Integer verifyDepth;

    /** Wildcard, unknown 'anyAttribute' attributes. */
    private AttributeMap unknownAttributes;

    /**
     * Constructor.
     * 
     * @param namespaceURI the namespace the element is in
     * @param elementLocalName the local name of the XML element this Object represents
     * @param namespacePrefix the prefix for the given namespace
     */
    protected KeyAuthorityImpl(String namespaceURI, String elementLocalName, String namespacePrefix) {
        super(namespaceURI, elementLocalName, namespacePrefix);
        keyInfos = new XMLObjectChildrenList<KeyInfo>(this);
        unknownAttributes = new AttributeMap(this);
    }

    /** {@inheritDoc} */
    public List<KeyInfo> getKeyInfos() {
        return keyInfos;
    }

    /** {@inheritDoc} */
    public Integer getVerifyDepth() {
        return verifyDepth;
    }

    /** {@inheritDoc} */
    public void setVerifyDepth(Integer newVerifyDepth) {
        verifyDepth = prepareForAssignment(verifyDepth, newVerifyDepth);
    }

    /** {@inheritDoc} */
    public AttributeMap getUnknownAttributes() {
        return unknownAttributes;
    }

    /** {@inheritDoc} */
    public List<XMLObject> getOrderedChildren() {
        if (keyInfos.isEmpty()) {
            return Collections.emptyList();
        }
        
        ArrayList<XMLObject> children = new ArrayList<XMLObject>();
        children.addAll(keyInfos);
        return Collections.unmodifiableList(children);
    }
}