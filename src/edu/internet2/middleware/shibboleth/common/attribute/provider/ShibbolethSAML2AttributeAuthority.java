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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.opensaml.Configuration;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.AttributeQuery;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.metadata.AttributeAuthorityDescriptor;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.xml.XMLObjectBuilderFactory;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.AttributeEncoder;
import edu.internet2.middleware.shibboleth.common.attribute.AttributeEncodingException;
import edu.internet2.middleware.shibboleth.common.attribute.AttributeRequestException;
import edu.internet2.middleware.shibboleth.common.attribute.SAML2AttributeAuthority;
import edu.internet2.middleware.shibboleth.common.attribute.SAML2AttributeEncoder;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.AttributeFilteringEngine;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver;

/**
 * SAML 2.0 Attribute Authority.
 */
public class ShibbolethSAML2AttributeAuthority implements SAML2AttributeAuthority {

    /** Class logger. */
    private static Logger log = Logger.getLogger(ShibbolethSAML2AttributeAuthority.class);

    /** For building attribute statements. */
    private SAMLObjectBuilder<AttributeStatement> statementBuilder;

    /** For building attributes. */
    private SAMLObjectBuilder<org.opensaml.saml2.core.Attribute> attributeBuilder;

    /** Attribute resolver. */
    private AttributeResolver<ShibbolethAttributeRequestContext> attributeResolver;

    /** To determine releasable attributes. */
    private AttributeFilteringEngine<ShibbolethAttributeRequestContext> filteringEngine;
    
    /**
     * This creates a new attribute authority.
     * 
     * @param resolver The attribute resolver to set
     */
    public ShibbolethSAML2AttributeAuthority(AttributeResolver<ShibbolethAttributeRequestContext> resolver) {
        
        XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();
        statementBuilder = (SAMLObjectBuilder<AttributeStatement>) builderFactory
                .getBuilder(AttributeStatement.DEFAULT_ELEMENT_NAME);
        attributeBuilder = (SAMLObjectBuilder<org.opensaml.saml2.core.Attribute>) builderFactory
                .getBuilder(org.opensaml.saml2.core.Attribute.DEFAULT_ELEMENT_NAME);
        
        attributeResolver = resolver;
    }

    /**
     * Gets the attribute resolver.
     * 
     * @return Returns the attributeResolver.
     */
    public AttributeResolver<ShibbolethAttributeRequestContext> getAttributeResolver() {
        return attributeResolver;
    }

    /**
     * Gets the filtering engine.
     * 
     * @return Returns the filteringEngine.
     */
    public AttributeFilteringEngine<ShibbolethAttributeRequestContext> getFilteringEngine() {
        return filteringEngine;
    }
    
    /**
     * Sets the attribute filtering engine.
     * 
     * @param engine attribute filtering engine
     */
    public void setFilteringEngine(AttributeFilteringEngine<ShibbolethAttributeRequestContext> engine){
        filteringEngine = engine;
    }

    /** {@inheritDoc} */
    public AttributeStatement performAttributeQuery(AttributeQuery query,
            ShibbolethAttributeRequestContext requestContext) throws AttributeRequestException {

        // get attributes from the message
        Set<String> queryAttributeIds = getAttributeIds(query);

        // get attributes from the metadata
        Set<String> metadataAttributeIds = getAttribtueIds(requestContext.getAttributeRequesterMetadata());

        // union the attribute id sets
        requestContext.getRequestedAttributes().addAll(queryAttributeIds);
        requestContext.getRequestedAttributes().addAll(metadataAttributeIds);

        // Resolve and filter attributes
        Map<String, Attribute> attributes = getAttributes(requestContext);

        // encode attributes
        List<org.opensaml.saml2.core.Attribute> encodedAttributes = encodeAttributes(attributes);

        // return attribute statement
        AttributeStatement statement = buildAttributeStatement(encodedAttributes);

        if(query != null){
            // TODO filter out attribute values specified in the query
        }
        
        return statement;
    }

    /** {@inheritDoc} */
    public AttributeStatement performAttributeQuery(ShibbolethAttributeRequestContext requestContext)
            throws AttributeRequestException {
        return performAttributeQuery(null, requestContext);
    }

    /** {@inheritDoc} */
    public Map<String, Attribute> getAttributes(ShibbolethAttributeRequestContext requestContext)
            throws AttributeRequestException {
        Map<String, Attribute> attributes = attributeResolver.resolveAttributes(requestContext);
        
        if(filteringEngine != null){
            attributes = filteringEngine.filterAttributes(attributes, requestContext);
        }
        
        return attributes;
    }

    /** {@inheritDoc} */
    public String getAttributeIDBySAMLAttribute(org.opensaml.saml2.core.Attribute attribute) {
        // TODO not implemented yet
        return null;
    }

    /** {@inheritDoc} */
    public org.opensaml.saml2.core.Attribute getSAMLAttributeByAttributeID(String id) {
        // TODO not implemented yet
        return null;
    }

    /**
     * Gets the attribute IDs for those attributes requested in the attribute query.
     * 
     * @param query the attribute query
     * 
     * @return attribute IDs for those attributes requested in the attribute query
     */
    protected Set<String> getAttributeIds(AttributeQuery query){
        Set<String> queryAttributeIds = new HashSet<String>();
        if(query != null){
            List<org.opensaml.saml2.core.Attribute> queryAttributes = query.getAttributes();
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
    protected Set<String> getAttribtueIds(EntityDescriptor metadata){
        Set<String> metadataAttributeIds = new HashSet<String>();
        AttributeAuthorityDescriptor aaDescriptor;
        if (metadata != null) {
            aaDescriptor = metadata.getAttributeAuthorityDescriptor(SAMLConstants.SAML20P_NS);
            if(aaDescriptor != null){
                List<org.opensaml.saml2.core.Attribute> metadataAttributes = aaDescriptor.getAttributes();
                metadataAttributeIds = getAttributeIds(metadataAttributes);
                if (log.isDebugEnabled()) {
                    log.debug("metadata contains the following attributes: " + metadataAttributeIds);
                }
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
            attributeIds.add(attrId);
        }
        return attributeIds;
    }

    /**
     * This encodes the supplied attributes with that attribute's SAML2 encoder.
     * 
     * @param resolvedAttributes <code>Map</code>
     * @return <code>List</code> of core attributes
     */
    protected List<org.opensaml.saml2.core.Attribute> encodeAttributes(Map<String, Attribute> resolvedAttributes) {
        List<org.opensaml.saml2.core.Attribute> encodedAttributes = new ArrayList<org.opensaml.saml2.core.Attribute>();

        Attribute shibbolethAttribute;
        AttributeEncoder<org.opensaml.saml2.core.Attribute> enc;
        org.opensaml.saml2.core.Attribute samlAttribute;
        SAML2StringAttributeEncoder defaultAttributeEncoder;
        for (Map.Entry<String, Attribute> entry : resolvedAttributes.entrySet()) {
            shibbolethAttribute = entry.getValue();
            if(shibbolethAttribute.getValues() == null || shibbolethAttribute.getValues().size() ==0){
                continue;
            }
            
            enc = shibbolethAttribute.getEncoderByCategory(SAML2AttributeEncoder.CATEGORY);
            if(enc == null){
                defaultAttributeEncoder = new SAML2StringAttributeEncoder();
                samlAttribute = getSAMLAttributeByAttributeID(shibbolethAttribute.getId());
                if(samlAttribute != null){
                    
                    defaultAttributeEncoder.setAttributeName(samlAttribute.getName());
                    defaultAttributeEncoder.setNameFormat(samlAttribute.getNameFormat());
                    defaultAttributeEncoder.setFriendlyName(samlAttribute.getFriendlyName());
                }else{
                    defaultAttributeEncoder.setAttributeName(shibbolethAttribute.getId());
                    defaultAttributeEncoder.setNameFormat("urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified");
                }
                
                enc = defaultAttributeEncoder;
            }
            
            try {
                encodedAttributes.add(enc.encode(entry.getValue()));
            } catch (AttributeEncodingException e) {
                log.warn("unable to encode attribute (" + entry.getKey() + "): " + e.getMessage());
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("attribute encoder encoded the following attributes: " + encodedAttributes);
        }
        return encodedAttributes;
    }

    /**
     * This builds the attribute statement for this SAML request.
     * 
     * @param encodedAttributes <code>List</code> of attributes
     * @return <code>AttributeStatement</code>
     */
    protected AttributeStatement buildAttributeStatement(List<org.opensaml.saml2.core.Attribute> encodedAttributes) {
        AttributeStatement statement = statementBuilder.buildObject();
        List<org.opensaml.saml2.core.Attribute> attributes = statement.getAttributes();
        attributes.addAll(encodedAttributes);
        return statement;
    }
}
