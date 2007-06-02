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

import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeQuery;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.NameID;

import edu.internet2.middleware.shibboleth.common.attribute.AttributeAuthority;
import edu.internet2.middleware.shibboleth.common.attribute.AttributeRequestException;
import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.encoding.AttributeEncodingException;

/**
 * An attribute authority that can take an attribute query and produce a resultant attribute statement.
 */
public interface SAML2AttributeAuthority extends
        AttributeAuthority<ShibbolethSAMLAttributeRequestContext<NameID, AttributeQuery>> {

    /**
     * Resolves a {@link NameID} into the internal principal name used Shibboleth.
     * 
     * @param requestContext The request context within which to retrieve the principal. At a mimium, a {@link NameID}
     *            and relying party ID must be included.
     * 
     * @return {@link NameID} into the internal principal name used Shibboleth
     * 
     * @throws AttributeRequestException thrown if the principal get not be resolved
     */
    public String getPrincipal(ShibbolethSAMLAttributeRequestContext<NameID, AttributeQuery> requestContext)
            throws AttributeRequestException;

    /**
     * Creates a SAML 2 attribute statment from a collection of {@link BaseAttribute}.
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
     * Translates SAML 2 attribute naming information into the internal attribute ID used by the resolver and filtering
     * engine.
     * 
     * @param attribute the SAML 2 attribute to translate
     * 
     * @return the attribute ID used by the resolver and filtering engine
     */
    public String getAttributeIDBySAMLAttribute(Attribute attribute);

    /**
     * Translates the internal attribute ID, used by the resolver and filtering engine, into its representative SAML 2
     * attribute name.
     * 
     * @param id internal attribute ID
     * 
     * @return SAML 2 attribute name
     */
    public Attribute getSAMLAttributeByAttributeID(String id);
}