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

package net.shibboleth.idp.relyingparty.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.criterion.ProfileRequestContextCriterion;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.criterion.RoleDescriptorCriterion;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.slf4j.Logger;

import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.relyingparty.CriteriaRelyingPartyConfigurationResolver;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.AbstractIdentifiedInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.component.IdentifiableComponent;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.Resolver;
import net.shibboleth.shared.resolver.ResolverException;

/**
 * Resolver which uses an instance of {@link CriteriaRelyingPartyConfigurationResolver} to
 * resolve the self entityID.
 * 
 * <p>
 * The required and allowed criteria are the same as the {@link CriteriaRelyingPartyConfigurationResolver}
 * implementation in use.
 * </p>
 */
public class CriteriaSelfEntityIDResolver extends AbstractIdentifiedInitializableComponent
    implements Resolver<String, CriteriaSet>, IdentifiableComponent {
    
    /** Logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(CriteriaSelfEntityIDResolver.class);
    
    /** The CriteriaRelyingPartyConfigurationResolver to which to delegate. */
    @NonnullAfterInit private CriteriaRelyingPartyConfigurationResolver rpcResolver;
    
    /**
     * Set the {@link CriteriaRelyingPartyConfigurationResolver} instance to which to delegate.
     * 
     * @param resolver the relying party resolver 
     */
    public void setRelyingPartyConfigurationResolver(
            @Nullable final CriteriaRelyingPartyConfigurationResolver resolver) {
        checkSetterPreconditions();
        rpcResolver = resolver;
    }

    /** {@inheritDoc} */
    @Override public void setId(@Nonnull @NotEmpty final String componentId) {
        super.setId(componentId);
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (rpcResolver == null) {
            throw new ComponentInitializationException("CriteriaRelyingPartyConfigurationResolver was null");
        }
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        rpcResolver = null;
        super.doDestroy();
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements public Iterable<String> resolve(
            @Nullable final CriteriaSet criteria) throws ResolverException {
        checkComponentActive();
        final String entityID = resolveSingle(criteria);
        if (entityID != null) {
            return CollectionSupport.singletonList(entityID);
        }
        return CollectionSupport.emptyList();
    }

    /** {@inheritDoc} */
    @Nullable public String resolveSingle(@Nullable final CriteriaSet criteria) throws ResolverException {
        checkComponentActive();
        @Nonnull final ProfileRequestContext prc = Constraint.isNotNull(buildContext(criteria), "Could not build context");
        final CriteriaSet prcSet = new CriteriaSet(new ProfileRequestContextCriterion(prc));
        
        final RelyingPartyConfiguration rpc = rpcResolver.resolveSingle(prcSet);
        if (rpc != null) {
            return rpc.getResponderId(prc);
        }
        return null;
    }

    /**
     * Build and populate the synthetic instance of {@link ProfileRequestContext} which will be passed
     * in the resolution call to the delegate.
     * 
     * @param criteria the input criteria
     * @return the synthetic context instance, or null if required data is not supplied
     */
    @Nullable private ProfileRequestContext buildContext(@Nullable final CriteriaSet criteria) {
        if (criteria == null) {
            return null;
        }
        
        final ProfileRequestContextCriterion prcCriterion = criteria.get(ProfileRequestContextCriterion.class);
        if (prcCriterion != null) {
            return prcCriterion.getProfileRequestContext();
        }

        final String entityID = resolveEntityID(criteria);
        log.debug("Resolved effective entityID from criteria: {}", entityID);

        final EntityDescriptor entityDescriptor = resolveEntityDescriptor(criteria);
        log.debug("Resolved effective entity descriptor from criteria: {}", entityDescriptor);

        final RoleDescriptor roleDescriptor = resolveRoleDescriptor(criteria);
        log.debug("Resolved effective role descriptor from criteria: {}", roleDescriptor);

        if (entityID != null || entityDescriptor != null || roleDescriptor != null) {
            final ProfileRequestContext prc = new ProfileRequestContext();
            final RelyingPartyContext rpc = prc.getOrCreateSubcontext(RelyingPartyContext.class);
            rpc.setVerified(true);

            rpc.setRelyingPartyId(entityID);

            if (entityDescriptor != null || roleDescriptor != null) {
                final SAMLPeerEntityContext peerContext = prc.getOrCreateSubcontext(SAMLPeerEntityContext.class);
                rpc.setRelyingPartyIdContextTree(peerContext);

                peerContext.setEntityId(entityID);

                if (roleDescriptor != null) {
                    peerContext.setRole(roleDescriptor.getSchemaType() != null
                            ? roleDescriptor.getSchemaType() : roleDescriptor.getElementQName());
                }

                final SAMLMetadataContext metadataContext = peerContext.getOrCreateSubcontext(SAMLMetadataContext.class);
                metadataContext.setEntityDescriptor(entityDescriptor);
                metadataContext.setRoleDescriptor(roleDescriptor);
            }
            return prc;
        }
        return null;
    }

    /**
     * Resolve the entityID from the criteria.
     * 
     * @param criteria the input criteria
     * @return the input entityID criterion or null if could not be resolved
     */
    private String resolveEntityID(@Nonnull final CriteriaSet criteria) {
        final EntityIdCriterion eic = criteria.get(EntityIdCriterion.class);
        if (eic != null) {
            return eic.getEntityId();
        }

        final EntityDescriptor ed = resolveEntityDescriptor(criteria);
        if (ed != null) {
            return ed.getEntityID();
        }

        return null;
    }

    /**
     * Resolve the EntityDescriptor from the criteria.
     *
     * @param criteria the input criteria
     * @return the input entity descriptor criterion, or null if could not be resolved
     */
    private EntityDescriptor resolveEntityDescriptor(@Nonnull final CriteriaSet criteria) {
        final RoleDescriptor rd = resolveRoleDescriptor(criteria);
        if (rd != null && rd.getParent() != null && rd.getParent() instanceof EntityDescriptor) {
            return (EntityDescriptor)rd.getParent();
        }

        return null;
    }

    /**
     * Resolve the RoleDescriptor from the criteria.
     *
     * @param criteria the input criteria
     * @return the input role descriptor criterion or null if could not be resolved
     */
    private RoleDescriptor resolveRoleDescriptor(@Nonnull final CriteriaSet criteria) {
        final RoleDescriptorCriterion rdc = criteria.get(RoleDescriptorCriterion.class);
        if (rdc != null) {
            return rdc.getRole();
        }

        return null;
    }
    
}