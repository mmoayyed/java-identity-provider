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

package net.shibboleth.idp.cas.flow.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.service.Service;
import net.shibboleth.idp.cas.service.impl.ServiceEntityDescriptor;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.profile.context.RelyingPartyContext;

/**
 * Builds a {@link SAMLMetadataContext} child of {@link RelyingPartyContext} to facilitate relying party selection
 * by group name. Possible outcomes:
 * <ul>
 *     <li><code>null</code> on success</li>
 *     <li>{@link ProtocolError#IllegalState IllegalState}</li>
 * </ul>
 *
 * @param <RequestType> request
 * @param <ResponseType> response
 *
 * @author Marvin S. Addison
 */
public class BuildSAMLMetadataContextAction<RequestType,ResponseType>
        extends AbstractCASProtocolAction<RequestType,ResponseType> {

    /** Whether to overwrite the relying party ID based on metadata. */
    private boolean relyingPartyIdFromMetadata;
    
    /** CAS service. */
    @Nullable private Service service;
    
    /** RelyingPartyContext. */
    @Nullable private RelyingPartyContext rpCtx;
    
    /**
     * Sets whether the {@link RelyingPartyContext#getRelyingPartyId()} method should return an entityID
     * established from SAML metadata instead of the service URL.
     * 
     * <p>Defaults to false for compatibility.</p>
     * 
     * @param flag flag to set
     */
    public void setRelyingPartyIdFromMetadata(final boolean flag) {
        checkSetterPreconditions();
        relyingPartyIdFromMetadata = flag;
    }
    
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }

        try {
            service = getCASService(profileRequestContext);
        } catch (final EventException e) {
            ActionSupport.buildEvent(profileRequestContext, e.getEventID());
            return false;
        }
        
        rpCtx = profileRequestContext.getSubcontext(RelyingPartyContext.class);
        if (rpCtx == null) {
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return false;
        }

        return true;
    }    
    
    @Override
    protected void doExecute(final @Nonnull ProfileRequestContext profileRequestContext) {
        
        final SAMLMetadataContext mdCtx = new SAMLMetadataContext();
        final Service svc = service;
        assert svc != null;
        final EntityDescriptor entity = svc.getEntityDescriptor() != null
                ? svc.getEntityDescriptor()
                : new ServiceEntityDescriptor(svc);
        mdCtx.setEntityDescriptor(entity);
        mdCtx.setRoleDescriptor(svc.getRoleDescriptor());
        
        if (relyingPartyIdFromMetadata) {
            assert rpCtx != null;
            rpCtx.setRelyingPartyId(entity.getEntityID());
        }
        
        assert rpCtx != null;
        rpCtx.setRelyingPartyIdContextTree(mdCtx);
    }

}