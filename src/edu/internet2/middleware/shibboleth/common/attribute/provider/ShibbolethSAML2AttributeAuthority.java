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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opensaml.Configuration;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeQuery;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.RequestAbstractType;
import org.opensaml.saml2.core.StatusResponseType;
import org.opensaml.saml2.metadata.AttributeAuthorityDescriptor;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.AttributeRequestException;
import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.encoding.AttributeEncoder;
import edu.internet2.middleware.shibboleth.common.attribute.encoding.AttributeEncodingException;
import edu.internet2.middleware.shibboleth.common.attribute.encoding.SAML2AttributeEncoder;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.ShibbolethAttributeFilteringEngine;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethAttributeResolver;
import edu.internet2.middleware.shibboleth.common.profile.provider.SAMLProfileRequestContext;
import edu.internet2.middleware.shibboleth.common.relyingparty.provider.saml2.AbstractSAML2ProfileConfiguration;

/**
 * SAML 2.0 Attribute Authority.
 */
public class ShibbolethSAML2AttributeAuthority implements SAML2AttributeAuthority {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ShibbolethSAML2AttributeAuthority.class);

    /** For building attribute statements. */
    private SAMLObjectBuilder<AttributeStatement> statementBuilder;

    /** Attribute resolver. */
    private ShibbolethAttributeResolver attributeResolver;

    /** To determine releasable attributes. */
    private ShibbolethAttributeFilteringEngine filteringEngine;

    /**
     * This creates a new attribute authority.
     * 
     * @param resolver The attribute resolver to set
     */
    @SuppressWarnings("unchecked")
    public ShibbolethSAML2AttributeAuthority(ShibbolethAttributeResolver resolver) {

        XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();
        statementBuilder = (SAMLObjectBuilder<AttributeStatement>) builderFactory
                .getBuilder(AttributeStatement.DEFAULT_ELEMENT_NAME);

        attributeResolver = resolver;
    }

    /**
     * Gets the attribute resolver.
     * 
     * @return Returns the attributeResolver.
     */
    public ShibbolethAttributeResolver getAttributeResolver() {
        return attributeResolver;
    }

    /**
     * Gets the filtering engine.
     * 
     * @return Returns the filteringEngine.
     */
    public ShibbolethAttributeFilteringEngine getFilteringEngine() {
        return filteringEngine;
    }

    /**
     * Sets the attribute filtering engine.
     * 
     * @param engine attribute filtering engine
     */
    public void setFilteringEngine(ShibbolethAttributeFilteringEngine engine) {
        filteringEngine = engine;
    }

    /** {@inheritDoc} */
    public AttributeStatement buildAttributeStatement(AttributeQuery query, Collection<BaseAttribute> attributes)
            throws AttributeEncodingException {

        Collection<Attribute> encodedAttributes = encodeAttributes(attributes);

        filterAttributesByValue(query, encodedAttributes);

        if(!encodedAttributes.isEmpty()){
            AttributeStatement statement = statementBuilder.buildObject();
            List<org.opensaml.saml2.core.Attribute> samlAttributes = statement.getAttributes();
            samlAttributes.addAll(encodedAttributes);
            return statement;
        }else{
            log.debug("No attributes remained after encoding and filtering by value, no attribute statement built");
            return null;
        }
    }

    /** {@inheritDoc} */
    public String getAttributeIDBySAMLAttribute(Attribute attribute) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public Attribute getSAMLAttributeByAttributeID(String id) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public String getPrincipal(
            SAMLProfileRequestContext<? extends RequestAbstractType, ? extends StatusResponseType, NameID, ? extends AbstractSAML2ProfileConfiguration> requestContext)
            throws AttributeRequestException {
        if (requestContext.getInboundMessageIssuer() == null || requestContext.getSubjectNameIdentifier() == null) {
            throw new AttributeRequestException(
                    "Unable to resolve principal, attribute request ID and subject name identifier may not be null");
        }

        return attributeResolver.resolvePrincipalName(requestContext);
    }

    /** {@inheritDoc} */
    public Map<String, BaseAttribute> getAttributes(
            SAMLProfileRequestContext<? extends RequestAbstractType, ? extends StatusResponseType, NameID, ? extends AbstractSAML2ProfileConfiguration> requestContext)
            throws AttributeRequestException {
        HashSet<String> requestedAttributes = new HashSet<String>();

        // get attributes from the message
        Set<String> queryAttributeIds = getAttributeIds(requestContext.getInboundSAMLMessage());
        requestedAttributes.addAll(queryAttributeIds);

        // get attributes from metadata
        Set<String> metadataAttributeIds = getAttribtueIds(requestContext.getPeerEntityMetadata());
        requestedAttributes.addAll(metadataAttributeIds);

        requestContext.setRequestedAttributes(requestedAttributes);

        // Resolve attributes
        Map<String, BaseAttribute> attributes = attributeResolver.resolveAttributes(requestContext);

        // Filter resulting attributes
        if (filteringEngine != null) {
            attributes = filteringEngine.filterAttributes(attributes, requestContext);
        }

        return attributes;
    }

    /**
     * This encodes the supplied attributes with that attribute's SAML2 encoder.
     * 
     * @param attributes the attributes to encode
     * 
     * @return the encoded attributes
     * 
     * @throws AttributeEncodingException thrown if an attribute could not be encoded
     */
    @SuppressWarnings("unchecked")
    protected Collection<Attribute> encodeAttributes(Collection<BaseAttribute> attributes)
            throws AttributeEncodingException {
        Collection<Attribute> encodedAttributes = new ArrayList<Attribute>();

        boolean attributeEncoded;

        for (BaseAttribute<?> shibbolethAttribute : attributes) {
            attributeEncoded = false;

            if (shibbolethAttribute.getValues() == null || shibbolethAttribute.getValues().size() == 0) {
                continue;
            }

            // first try to encode with an SAML 2 attribute encoders
            for (AttributeEncoder encoder : shibbolethAttribute.getEncoders()) {
                if (encoder instanceof SAML2AttributeEncoder) {
                    try {
                        encodedAttributes.add((Attribute) encoder.encode(shibbolethAttribute));
                        attributeEncoded = true;
                        log.debug("Encoded attribute {} with encoder of type {}", shibbolethAttribute.getId(), encoder
                                .getClass().getName());
                    } catch (AttributeEncodingException e) {
                        log.warn("unable to encode attribute: " + shibbolethAttribute.getId(), e);
                    }
                }
            }

            // if it couldn't be encoded log it
            if (!attributeEncoded) {
                log.info("Attribute {} was not encoded because no SAML2AttributeEncoder was attached to it.",
                        shibbolethAttribute.getId());
            }
        }

        return encodedAttributes;
    }

    /**
     * Filters out all but the values, for an attribute, provided in the query, if and only if, the query specifies at
     * least one value for the attribute. That is to say, if the attribute query does not specify any attribute values
     * then all values for that attribute are accepted and remain. Because this comparison acts on the marshalled form
     * the provided attributes will be encoded prior to filtering.
     * 
     * @param query the attribute query
     * @param attributes the attributes to filter
     */
    protected void filterAttributesByValue(AttributeQuery query, Collection<Attribute> attributes) {
        if (query == null) {
            return;
        }

        // TODO not implemented yet
    }

    /**
     * Gets the attribute IDs for those attributes requested in the attribute query.
     * 
     * @param samlRequest incomming SAML request
     * 
     * @return attribute IDs for those attributes requested in the attribute query
     */
    protected Set<String> getAttributeIds(RequestAbstractType samlRequest) {
        Set<String> queryAttributeIds = new HashSet<String>();
        if (!(samlRequest instanceof AttributeQuery)) {
            return queryAttributeIds;
        }

        AttributeQuery query = (AttributeQuery) samlRequest;
        if (query != null) {
            List<org.opensaml.saml2.core.Attribute> queryAttributes = query.getAttributes();
            queryAttributeIds = getAttributeIds(queryAttributes);
            log.debug("query message contains the following attributes: {}", queryAttributeIds);
        }

        return queryAttributeIds;
    }

    /**
     * Gets the attribute IDs for those attributes requested in the entity metadata.
     * 
     * @param metadata the entity metadata
     * 
     * @return attribute IDs for those attributes requested in the entity metadata
     */
    protected Set<String> getAttribtueIds(EntityDescriptor metadata) {
        Set<String> metadataAttributeIds = new HashSet<String>();
        AttributeAuthorityDescriptor aaDescriptor;
        if (metadata != null) {
            aaDescriptor = metadata.getAttributeAuthorityDescriptor(SAMLConstants.SAML20P_NS);
            if (aaDescriptor != null) {
                List<org.opensaml.saml2.core.Attribute> metadataAttributes = aaDescriptor.getAttributes();
                metadataAttributeIds = getAttributeIds(metadataAttributes);
                log.debug("metadata contains the following attributes: {}", metadataAttributeIds);
            }
        }

        return metadataAttributeIds;
    }

    /**
     * This parses the attribute ids from the supplied list of attributes.
     * 
     * @param attributes <code>List</code>
     * @return <code>Set</code> of attribute ids
     */
    protected Set<String> getAttributeIds(List<org.opensaml.saml2.core.Attribute> attributes) {
        final Set<String> attributeIds = new HashSet<String>();
        for (org.opensaml.saml2.core.Attribute a : attributes) {
            String attrId = getAttributeIDBySAMLAttribute(a);
            if (attrId != null) {
                attributeIds.add(attrId);
            }
        }
        return attributeIds;
    }
}