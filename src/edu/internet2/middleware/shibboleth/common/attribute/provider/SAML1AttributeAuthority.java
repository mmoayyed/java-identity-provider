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

package edu.internet2.middleware.shibboleth.common.attribute.provider;

import java.util.Collection;

import org.opensaml.saml1.core.AttributeDesignator;
import org.opensaml.saml1.core.AttributeQuery;
import org.opensaml.saml1.core.AttributeStatement;
import org.opensaml.saml1.core.NameIdentifier;
import org.opensaml.saml1.core.RequestAbstractType;
import org.opensaml.saml1.core.ResponseAbstractType;

import edu.internet2.middleware.shibboleth.common.attribute.AttributeAuthority;
import edu.internet2.middleware.shibboleth.common.attribute.AttributeRequestException;
import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.encoding.AttributeEncodingException;
import edu.internet2.middleware.shibboleth.common.profile.provider.BaseSAMLProfileRequestContext;
import edu.internet2.middleware.shibboleth.common.relyingparty.provider.saml1.AbstractSAML1ProfileConfiguration;

/**
 * An attribute authority that can take an attribute query and produce a resultant attribute statement.
 */
public interface SAML1AttributeAuthority
        extends
        AttributeAuthority<BaseSAMLProfileRequestContext<? extends RequestAbstractType, ? extends ResponseAbstractType, NameIdentifier, ? extends AbstractSAML1ProfileConfiguration>> {

    /**
     * Resolves a {@link NameIdentifier} into the internal principal name used Shibboleth.
     * 
     * @param requestContext The request context within which to retrieve the principal. At a mimium, a
     *            {@link NameIdentifier} and relying party ID must be included.
     * 
     * @return {@link NameIdentifier} into the internal principal name used Shibboleth
     * 
     * @throws AttributeRequestException thrown if the principal get not be resolved
     */
    public String getPrincipal(
            BaseSAMLProfileRequestContext<? extends RequestAbstractType, ? extends ResponseAbstractType, NameIdentifier, ? extends AbstractSAML1ProfileConfiguration> requestContext)
            throws AttributeRequestException;

    /**
     * Creates a SAML 1 attribute statment from a collection of {@link BaseAttribute}.
     * 
     * @param query the attribute query the statement is in respone to, may be null
     * @param attributes the attributes to create the attribute statement form
     * 
     * @return the generated attribute statement
     * 
     * @throws AttributeEncodingException thrown if an {@link BaseAttribute} can not be encoded
     */
    public AttributeStatement buildAttributeStatement(AttributeQuery query, Collection<BaseAttribute> attributes)
            throws AttributeEncodingException;

    /**
     * Translates SAML 1 attribute naming information into the internal attribute ID used by the resolver and filtering
     * engine.
     * 
     * @param attribute the SAML 1 attribute to translate
     * 
     * @return the attribute ID used by the resolver and filtering engine
     */
    public String getAttributeIDBySAMLAttribute(AttributeDesignator attribute);

    /**
     * Translates the internal attribute ID, used by the resolver and filtering engine, into its representative SAML 1
     * attribute name.
     * 
     * @param id internal attribute ID
     * 
     * @return SAML 1 attribute name
     */
    public AttributeDesignator getSAMLAttributeByAttributeID(String id);
}