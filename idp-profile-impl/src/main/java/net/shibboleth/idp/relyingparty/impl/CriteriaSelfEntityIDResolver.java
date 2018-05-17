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

import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.relyingparty.CriteriaRelyingPartyConfigurationResolver;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiedInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.IdentifiableComponent;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.Resolver;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

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
    private Logger log = LoggerFactory.getLogger(CriteriaSelfEntityIDResolver.class);
    
    /** The CriteriaRelyingPartyConfigurationResolver to which to delegate. */
    @NonnullAfterInit private CriteriaRelyingPartyConfigurationResolver rpcResolver;
    
    /** Constructor. */
    public CriteriaSelfEntityIDResolver() {
        super();
    }
    
    /**
     * Set the {@link CriteriaRelyingPartyConfigurationResolver} instance to which to delegate.
     * 
     * @param resolver the relying party resolver 
     */
    public void setRelyingPartyConfigurationResolver(
            @Nullable final CriteriaRelyingPartyConfigurationResolver resolver) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        rpcResolver = resolver;
    }

    /** {@inheritDoc} */
    @Override public void setId(@Nonnull final String componentId) {
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
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        
        final String entityID = resolveSingle(criteria);
        if (entityID != null) {
            return Collections.singletonList(entityID);
        } else {
            return Collections.emptyList();
        }
    }

    /** {@inheritDoc} */
    @Nullable public String resolveSingle(@Nullable final CriteriaSet criteria) throws ResolverException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        
        final RelyingPartyConfiguration rpc = rpcResolver.resolveSingle(criteria);
        if (rpc != null) {
            return rpc.getResponderId();
        } else {
            return null;
            
        }
    }

}
