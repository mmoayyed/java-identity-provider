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

import org.opensaml.Configuration;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.saml1.core.Attribute;

import edu.internet2.middleware.shibboleth.common.attribute.encoding.SAML1AttributeEncoder;

/**
 * Base for encoders that produce {@link Attribute}s.
 */
public abstract class AbstractSAML1AttributeEncoder extends AbstractAttributeEncoder<Attribute> implements
        SAML1AttributeEncoder {

    /** Attribute factory. */
    protected final SAMLObjectBuilder<Attribute> attributeBuilder;

    /** Namespace of attribute. */
    private String namespace;

    /** Constructor. */
    protected AbstractSAML1AttributeEncoder() {
        attributeBuilder = (SAMLObjectBuilder<Attribute>) Configuration.getBuilderFactory().getBuilder(
                Attribute.DEFAULT_ELEMENT_NAME);
    }

    /** {@inheritDoc} */
    public String getNamespace() {
        return namespace;
    }

    /** {@inheritDoc} */
    public void setNamespace(String newNamespace) {
        namespace = newNamespace;
    }

    /**
     * Populates the attribute with attribute name and namespace.
     * 
     * @param attribute to populate
     */
    protected void populateAttribute(Attribute attribute) {
        attribute.setAttributeName(getAttributeName());
        attribute.setAttributeNamespace(getNamespace());
    }
}