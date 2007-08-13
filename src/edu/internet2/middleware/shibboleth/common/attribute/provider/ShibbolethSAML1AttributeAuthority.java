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

import org.apache.log4j.Logger;
import org.opensaml.Configuration;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.saml1.core.Attribute;
import org.opensaml.saml1.core.AttributeDesignator;
import org.opensaml.saml1.core.AttributeQuery;
import org.opensaml.saml1.core.AttributeStatement;
import org.opensaml.saml1.core.NameIdentifier;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.xml.XMLObjectBuilderFactory;

import edu.internet2.middleware.shibboleth.common.attribute.AttributeRequestException;
import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.encoding.AttributeEncoder;
import edu.internet2.middleware.shibboleth.common.attribute.encoding.AttributeEncodingException;
import edu.internet2.middleware.shibboleth.common.attribute.encoding.SAML1AttributeEncoder;
import edu.internet2.middleware.shibboleth.common.attribute.encoding.provider.SAML1StringAttributeEncoder;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.ShibbolethAttributeFilteringEngine;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethAttributeResolver;
import edu.internet2.middleware.shibboleth.common.relyingparty.ProfileConfiguration;

/**
 * SAML 1 Attribute Authority.
 */
public class ShibbolethSAML1AttributeAuthority implements SAML1AttributeAuthority {
    /** Class logger. */
    private static Logger log = Logger.getLogger(ShibbolethSAML1AttributeAuthority.class);

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
    public ShibbolethSAML1AttributeAuthority(ShibbolethAttributeResolver resolver) {

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

        AttributeStatement statement = statementBuilder.buildObject();
        statement.getAttributes().addAll(encodedAttributes);
        return statement;
    }

    /** {@inheritDoc} */
    public String getAttributeIDBySAMLAttribute(AttributeDesignator attribute) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public String getPrincipal(ShibbolethSAMLAttributeRequestContext<NameIdentifier, SAMLObject, SAMLObject, ProfileConfiguration> requestContext)
            throws AttributeRequestException {
        if (requestContext.getRelyingPartyEntityId() == null || requestContext.getSubjectNameIdentifier() == null) {
            throw new AttributeRequestException(
                    "Unable to resolve principal, attribute request ID and subject name identifier may not be null");
        }

        return attributeResolver.resolvePrincipalName(requestContext);
    }

    /** {@inheritDoc} */
    public AttributeDesignator getSAMLAttributeByAttributeID(String id) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public Map<String, BaseAttribute> getAttributes(
            ShibbolethSAMLAttributeRequestContext<NameIdentifier, SAMLObject, SAMLObject, ProfileConfiguration> requestContext)
            throws AttributeRequestException {

        AttributeQuery query = requestContext.getInboundSAMLMessage();

        HashSet<String> requestedAttributes = new HashSet<String>();

        // get attributes from the message
        Set<String> queryAttributeIds = getAttributeIds(query);
        requestedAttributes.addAll(queryAttributeIds);

        // get attributes from metadata
        Set<String> metadataAttributeIds = getAttribtueIds(requestContext.getRelyingPartyMetadata());
        requestedAttributes.addAll(metadataAttributeIds);

        requestContext.setRequestedAttributes(requestedAttributes);

        Map<String, BaseAttribute> attributes = attributeResolver.resolveAttributes(requestContext);

        if (filteringEngine != null) {
            attributes = filteringEngine.filterAttributes(attributes, requestContext);
        }

        return attributes;
    }

    /**
     * Gets the attribute IDs for those attributes requested in the attribute query.
     * 
     * @param query the attribute query
     * 
     * @return attribute IDs for those attributes requested in the attribute query
     */
    protected Set<String> getAttributeIds(AttributeQuery query) {
        Set<String> queryAttributeIds = new HashSet<String>();
        if (query != null) {
            List<AttributeDesignator> queryAttributes = query.getAttributeDesignators();
            queryAttributeIds = getAttributeIds(queryAttributes);
            if (log.isDebugEnabled()) {
                log.debug("query message contains the following attributes: " + queryAttributeIds);
            }
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
        // TODO
        return metadataAttributeIds;
    }

    /**
     * This parses the attribute ids from the supplied list of attributes.
     * 
     * @param attributes <code>List</code>
     * @return <code>Set</code> of attribute ids
     */
    protected Set<String> getAttributeIds(List<AttributeDesignator> attributes) {
        final Set<String> attributeIds = new HashSet<String>();
        for (AttributeDesignator a : attributes) {
            String attrId = getAttributeIDBySAMLAttribute(a);
            attributeIds.add(attrId);
        }
        return attributeIds;
    }

    /**
     * This encodes the supplied attributes with that attribute's SAML1 encoder.
     * 
     * @param attributes shibboleth attributes to be encoded into SAML attributes
     * 
     * @return collection of encoded SAML attributes
     */
    @SuppressWarnings("unchecked")
    protected Collection<Attribute> encodeAttributes(Collection<BaseAttribute> attributes) {
        Collection<Attribute> encodedAttributes = new ArrayList<Attribute>();

        boolean attributeEncoded = false;
        AttributeDesignator samlAttribute;
        SAML1StringAttributeEncoder defaultEncoder;

        for (BaseAttribute<?> shibbolethAttribute : attributes) {
            if (shibbolethAttribute.getValues() == null || shibbolethAttribute.getValues().size() == 0) {
                continue;
            }

            // first try to encode with an SAML 1 attribute encoders
            for (AttributeEncoder encoder : shibbolethAttribute.getEncoders()) {
                if (encoder instanceof SAML1AttributeEncoder) {
                    try {
                        encodedAttributes.add((Attribute) encoder.encode(shibbolethAttribute));
                        attributeEncoded = true;
                        if (log.isDebugEnabled()) {
                            log.debug("Encoded attribute " + shibbolethAttribute.getId() + " with encoder of type "
                                    + encoder.getClass().getName());
                        }
                    } catch (AttributeEncodingException e) {
                        log.warn("unable to encode attribute (" + shibbolethAttribute.getId() + "): "
                                + e.getMessage());
                    }
                }
            }


            // if it couldn't be encoded try using the default encoder
            if (!attributeEncoded) {
                defaultEncoder = new SAML1StringAttributeEncoder();
                samlAttribute = getSAMLAttributeByAttributeID(shibbolethAttribute.getId());
                if (samlAttribute != null) {
                    defaultEncoder.setAttributeName(samlAttribute.getAttributeName());
                    defaultEncoder.setNamespace(samlAttribute.getAttributeNamespace());
                } else {
                    defaultEncoder.setAttributeName(shibbolethAttribute.getId());
                    defaultEncoder.setNamespace("urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified");
                }

                encodedAttributes.add(defaultEncoder.encode(shibbolethAttribute));
                if (log.isDebugEnabled()) {
                    log.debug("Encoded attribute " + shibbolethAttribute.getId() + " with encoder of type "
                            + defaultEncoder.getClass().getName());
                }
            }
        }

        return encodedAttributes;
    }
}