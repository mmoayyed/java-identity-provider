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

package edu.internet2.middleware.shibboleth.common.attribute;

import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeQuery;
import org.opensaml.saml2.core.AttributeStatement;

import edu.internet2.middleware.shibboleth.common.attribute.provider.ShibbolethAttributeRequestContext;

/**
 * An attribute authority that can take an attribute query and produce a resultant attribute statement.
 */
public interface SAML2AttributeAuthority extends AttributeAuthority<ShibbolethAttributeRequestContext> {

    /**
     * Performs a SAML 2 attribute query based on the given query. This includes fetching of the requested attributes,
     * filtering the attributes and values, and finally creating an attribute statement in respone to the query.
     * 
     * @param query the SAML 2 attribute query
     * @param requestContext contextual information for the attribute request
     * 
     * @return the attribute statement in response to the attribute query
     * 
     * @throws AttributeRequestException thrown if there is a problem retrieving the attributes
     * 
     */
    public AttributeStatement performAttributeQuery(AttributeQuery query,
            ShibbolethAttributeRequestContext requestContext) throws AttributeRequestException;

    /**
     * Performs a query for attributes to be released to the given entity. This includes fetching of the requested
     * attributes, filtering the attributes and values, and finally creating an attribute statement in respone to the
     * query.
     * 
     * @param requestContext contextual information for the attribute request
     * 
     * @return the attribute statement for the entity
     * 
     * @throws AttributeRequestException thrown if there is a problem retrieving the attributes
     * 
     */
    public AttributeStatement performAttributeQuery(ShibbolethAttributeRequestContext requestContext)
            throws AttributeRequestException;

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