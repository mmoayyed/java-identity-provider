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

package net.shibboleth.idp.attribute.resolver.context;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;

/** A context which carries Information about the recipient of the attribute.  This is the
 *  principal, the "self" entityID and the RecipientEntityID.
 *  <br/>TODO
 *  This context is here mostly as a temporary scaffolding to allow development of the 
 *  attribute filter and resolver.  We expect it to change.
 *   
 *   */
@NotThreadSafe
public class AttributeRecipientContext extends BaseContext {

    /** The issuer's metadata. */
    private EntityDescriptor attributeIssuerMetadata;

    /** The recipient's metadata. */
    private EntityDescriptor attributeRecipientMetadata;
    
    /** The issuer's Role Descriptor.  */
    private RoleDescriptor attributeIssuerRoleDescriptor;
    
    /** The requester's Role Descriptor.  */
    private RoleDescriptor attributeRequesterRoleDescriptor;
    

    /**
     * Return the metadata for the entity issuing the attributes.
     * @return Returns the issuerMetadata.
     */
    @Nullable public EntityDescriptor getAttributeIssuerMetadata() {
        return attributeIssuerMetadata;
    }

    /**
     * Sets the metadata for the entity issuing the attributes.
     * @param metadata The issuerMetadata to set.
     */
    public void setAttributeIssuerMetadata(@Nullable EntityDescriptor metadata) {
        attributeIssuerMetadata = metadata;
    }

    /**
     * Return the metadata for the entity requesting the attributes.
     * @return Returns the requesterMetadata.
     */
    @Nullable public EntityDescriptor getAttributeRecipientMetadata() {
        return attributeRecipientMetadata;
    }

    /**
     * Sets the metadata for the entity requesting the attributes.
     * @param metadata The requesterMetadata to set.
     */
    public void setAttributeRecipientMetadata(@Nullable EntityDescriptor metadata) {
        attributeRecipientMetadata = metadata;
    }

    /**
     * Get the attribute issuer's RoleDescriptor. 
     * @return Returns the attributeIssuerRoleDescriptor.
     */
    @Nullable public RoleDescriptor getAttributeIssuerRoleDescriptor() {
        return attributeIssuerRoleDescriptor;
    }

    /**
     * Set the attribute issuer's Role Descriptor. 
     * @param roleDescriptor The attributeIssuerRoleDescriptor to set.
     */
    public void setAttributeIssuerRoleDescriptor(@Nullable RoleDescriptor roleDescriptor) {
        attributeIssuerRoleDescriptor = roleDescriptor;
    }

    /**
     * Get the attribute requester's Role Descriptor. 
     * @return Returns the attributeRequesterRoleDescriptor.
     */
    @Nullable public RoleDescriptor getAttributeRequesterRoleDescriptor() {
        return attributeRequesterRoleDescriptor;
    }

    /**
     * Set the attribute requester's RoleDescriptor. 
     * @param roleDescriptor The attributeRequesterRoleDescriptor to set.
     */
    public void setAttributeRequesterRoleDescriptor(@Nullable RoleDescriptor roleDescriptor) {
        attributeRequesterRoleDescriptor = roleDescriptor;
    }

}