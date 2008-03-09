/*
 * Copyright 2008 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition;

import java.util.Collection;

import org.opensaml.Configuration;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.saml1.core.NameIdentifier;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;

/**
 * An attribute definition the creates attributes whose values are {@link NameIdentifier}.
 * 
 * When building the NameIdentifier the textual content of the NameIdentifier is the value of the source attribute. If a
 * {@link #nameIdQualifier} is provided that value is used as the NameIdentifier's name qualifier otherwise the
 * attribute issuer's entity ID is used. The attribute requester's entity ID is always used as the NameIdentifier's SP
 * name qualifier.
 */
public class SAML1NameIdentifierAttributeDefinition extends BaseAttributeDefinition {

    /** Builder of NameIdentifier XMLObjects. */
    private final SAMLObjectBuilder<NameIdentifier> nameIdBuilder;

    /** Format of the NameIdentifier. */
    private String nameIdFormat;

    /** Name qualifier for the NameIdentifier. */
    private String nameIdQualifier;

    /** Constructor. */
    public SAML1NameIdentifierAttributeDefinition() {
        super();
        nameIdBuilder = (SAMLObjectBuilder<NameIdentifier>) Configuration.getBuilderFactory().getBuilder(
                NameIdentifier.DEFAULT_ELEMENT_NAME);
    }

    /**
     * Gets the format for the NameIdentifier used as an attribute value.
     * 
     * @return format for the NameIdentifier used as an attribute value
     */
    public String getNameIdFormat() {
        return nameIdFormat;
    }

    /**
     * Sets the format for the NameIdentifier used as an attribute value.
     * 
     * @param format format for the NameIdentifier used as an attribute value
     */
    public void setNameIdFormat(String format) {
        nameIdFormat = format;
    }

    /**
     * Gets the NameIdentifier qualifier for the NameIdentifier used as an attribute value.
     * 
     * @return NameIdentifier qualifier for the NameIdentifier used as an attribute value
     */
    public String getNameIdQualifier() {
        return nameIdQualifier;
    }

    /**
     * Sets the NameIdentifier qualifier for the NameIdentifier used as an attribute value.
     * 
     * @param qualifier NameIdentifier qualifier for the NameIdentifier used as an attribute value
     */
    public void setNameIdQualifier(String qualifier) {
        nameIdQualifier = qualifier;
    }

    /** {@inheritDoc} */
    protected BaseAttribute<?> doResolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        BasicAttribute<NameIdentifier> attribute = new BasicAttribute<NameIdentifier>();
        attribute.setId(getId());

        Collection<?> values = getValuesFromAllDependencies(resolutionContext);
        if (values != null && !values.isEmpty()) {
            for (Object value : values) {
                attribute.getValues().add(buildNameId(value.toString(), resolutionContext));
            }
        }

        return attribute;
    }

    /**
     * Builds a name ID. The provided value is the textual content of the NameIdentifier. If a {@link #nameIdQualifier}
     * is not null it is used as the NameIdentifier's name qualifier, otherwise the attribute issuer's entity id is
     * used.
     * 
     * @param nameIdValue value of the NameIdentifier
     * @param resolutionContext current resolution context
     * 
     * @return the constructed NameIdentifier
     */
    protected NameIdentifier buildNameId(String nameIdValue, ShibbolethResolutionContext resolutionContext) {
        NameIdentifier nameId = nameIdBuilder.buildObject();
        nameId.setNameIdentifier(nameIdValue);

        if (nameIdFormat != null) {
            nameId.setFormat(nameIdFormat);
        }

        if (nameIdQualifier != null) {
            nameId.setNameQualifier(nameIdQualifier);
        } else {
            nameId.setNameQualifier(resolutionContext.getAttributeRequestContext().getLocalEntityId());
        }

        return nameId;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        // do nothing
    }
}