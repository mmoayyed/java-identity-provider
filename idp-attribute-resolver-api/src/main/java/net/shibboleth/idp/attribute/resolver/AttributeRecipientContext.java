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

package net.shibboleth.idp.attribute.resolver;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

/** A context which carries Information about the Recipient of the attribute.  This is the
 *  principal, the "self" entityID and the RecipientEntityID. */
@NotThreadSafe
public class AttributeRecipientContext extends BaseContext {

    /** The unique principal. This is required by some resolvers and injected during setup. */
    private String principal;
    
    /** The local entityID. This is required by some resolvers and injected during setup. */
    private String attributeIssuerID;
    
    /** The other entityID. This is required by some resolvers and injected during setup. */
    private String attributeRecipientID;
    
    /** How was the principal Authenticated? */
    private String principalAuthenticationMethod;
    
    /** The issuer's metadata. */
    private EntityDescriptor issuerMetadata;

    /** The issuer's metadata. */
    private EntityDescriptor requesterMetadata;

    /** Gets the principal associated with this resolution. 
     * 
     * @return the principal associated with this resolution. 
     */
    @Nullable public String getPrincipal() {
        return principal;
    }
    
    /** Sets the principal associated with this resolution. 
     * 
     * @param value the principal associated with this resolution. 
     */
    @Nullable public void setPrincipal(@Nullable String value) {
        principal = value;
    }
    
    /** Gets the attribute issuer (me) associated with this resolution. 
     * 
     * @return the attribute issuer associated with this resolution. 
     */
    @Nullable public String getAttributeIssuerID() {
        return attributeIssuerID;
    }
    
    /** Sets the attribute issuer (me)  associated with this resolution. 
     * 
     * @param value the attribute issuer associated with this resolution. 
     */
    @Nullable public void setAttributeIssuerID(@Nullable String value) {
        attributeIssuerID = value;
    }

    /** Gets the attribute recipient (her) associated with this resolution. 
     * 
     * @return the attribute recipient associated with this resolution. 
     */
    @Nullable public String getAttributeRecipientID() {
        return attributeRecipientID;
    }
    
    /** Sets the attribute recipient (her)  associated with this resolution. 
     * 
     * @param value the attribute recipient associated with this resolution. 
     */
    @Nullable public void setAttributeRecipientID(@Nullable String value) {
        attributeRecipientID = value;
    }
    /** Sets how the principal was authenticated. 
     * @return Returns the principalAuthenticationMethod.
     */
    @Nullable public String getPrincipalAuthenticationMethod() {
        return principalAuthenticationMethod;
    }

    /** Gets how the principal was authenticated.
     * @param method The principalAuthenticationMethod to set.
     */
    public void setPrincipalAuthenticationMethod(String method) {
        this.principalAuthenticationMethod = method;
    }

    /**
     * Return the metadata for the entity issuing the attributes.
     * @return Returns the issuerMetadata.
     */
    public EntityDescriptor getIssuerMetadata() {
        return issuerMetadata;
    }

    /**
     * Sets the metadata for the entity issuing the attributes.
     * @param metadata The issuerMetadata to set.
     */
    public void setIssuerMetadata(EntityDescriptor metadata) {
        this.issuerMetadata = metadata;
    }

    /**
     * Return the metadata for the entity requesting the attributes.
     * @return Returns the requesterMetadata.
     */
    public EntityDescriptor getRequesterMetadata() {
        return requesterMetadata;
    }

    /**
     * Sets the metadata for the entity requesting the attributes.
     * @param metadata The requesterMetadata to set.
     */
    public void setRequesterMetadata(EntityDescriptor metadata) {
        this.requesterMetadata = metadata;
    }

}