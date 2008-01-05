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
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.impl.AttributeBuilder;

import edu.internet2.middleware.shibboleth.common.attribute.encoding.SAML2AttributeEncoder;

/**
 * Base for encoders that produce {@link Attribute}s.
 */
public abstract class AbstractSAML2AttributeEncoder extends AbstractAttributeEncoder<Attribute> implements
        SAML2AttributeEncoder {

    /** Builder for SAML 2 attribute XMLObjects. */
    protected final SAMLObjectBuilder<Attribute> attributeBuilder;

    /** Format of attribute. */
    private String format;

    /** Friendly name of attribute. */
    private String friendlyName;

    /** Constructor. */
    protected AbstractSAML2AttributeEncoder() {
        attributeBuilder = (AttributeBuilder) Configuration.getBuilderFactory().getBuilder(
                Attribute.DEFAULT_ELEMENT_NAME);
    }

    /** {@inheritDoc} */
    public String getNameFormat() {
        return format;
    }

    /** {@inheritDoc} */
    public String getFriendlyName() {
        return friendlyName;
    }

    /** {@inheritDoc} */
    public void setNameFormat(String newFormat) {
        format = newFormat;
    }

    /** {@inheritDoc} */
    public void setFriendlyName(String name) {
        friendlyName = name;
    }

    /**
     * Populates the attribute with attribute name, name format, and friendly name information.
     * 
     * @param attribute to populate
     */
    protected void populateAttribute(Attribute attribute) {
        attribute.setName(getAttributeName());
        attribute.setNameFormat(getNameFormat());
        attribute.setFriendlyName(getFriendlyName());
    }
}
