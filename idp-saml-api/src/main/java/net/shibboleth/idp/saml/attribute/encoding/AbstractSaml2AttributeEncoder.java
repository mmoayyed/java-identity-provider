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

package net.shibboleth.idp.saml.attribute.encoding;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Attribute;

/**
 * Base class for encoders that produce a SAML 2 {@link Attribute}.
 * 
 * @param <EncodedType> the type of data that can be encoded by the encoder
 */
public abstract class AbstractSaml2AttributeEncoder<EncodedType extends AttributeValue> extends
        AbstractSamlAttributeEncoder<Attribute, EncodedType> {

    /** Builder used to construct {@link Attribute} objects. */
    private final SAMLObjectBuilder<Attribute> attributeBuilder;

    /** A friendly, human readable, name for the attribute. */
    private String friendlyName;

    /** Constructor. */
    public AbstractSaml2AttributeEncoder() {
        super();

        attributeBuilder =
                (SAMLObjectBuilder<Attribute>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                        Attribute.TYPE_NAME);
    }

    /** {@inheritDoc} */
    @Nonnull public final String getProtocol() {
        return SAMLConstants.SAML20P_NS;
    }

    /**
     * Gets the friendly, human readable, name for the attribute.
     * 
     * @return friendly, human readable, name for the attribute
     */
    @Nullable public final String getFriendlyName() {
        return friendlyName;
    }

    /**
     * Sets the friendly, human readable, name for the attribute.
     * 
     * @param attributeFriendlyName friendly, human readable, name for the attribute
     */
    public final synchronized void setFriendlyName(@Nullable final String attributeFriendlyName) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        friendlyName = StringSupport.trimOrNull(attributeFriendlyName);
    }
    
    /**
     * Ensures that the friendly is not null.
     * 
     * {@inheritDoc}
     */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (friendlyName == null) {
            throw new ComponentInitializationException("Friendly name can not be null or empty");
        }
    }


    /** {@inheritDoc} */
    @Nonnull protected Attribute buildAttribute(final IdPAttribute attribute,
            final List<XMLObject> attributeValues) throws AttributeEncodingException {

        final Attribute samlAttribute = attributeBuilder.buildObject();
        samlAttribute.setName(getName());
        samlAttribute.setFriendlyName(getFriendlyName());
        samlAttribute.setNameFormat(getNamespace());
        samlAttribute.getAttributeValues().addAll(attributeValues);

        return samlAttribute;
    }
}