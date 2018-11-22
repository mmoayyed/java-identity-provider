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

package net.shibboleth.idp.saml.nameid.impl;

import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.authn.AbstractSubjectCanonicalizationAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.saml.nameid.NameIDCanonicalizationFlowDescriptor;
import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.utilities.java.support.service.ReloadableService;

/**
 * Action to fail if asked to perform C14N ..
 */
public class LegacyCanonicalization extends AbstractSubjectCanonicalizationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(LegacyCanonicalization.class);
    
    /**
     * Constructor.
     * 
     * @param resolverService the service which will implement {@link LegacyPrincipalDecoder}.
     */
    public LegacyCanonicalization(@Nonnull @ParameterName(name="resolverService") 
                        final ReloadableService<AttributeResolver> resolverService) {
        Constraint.isNotNull(resolverService, "AttributeResolver cannot be null");
    }
    
//CheckStyle: ReturnCount OFF
    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final SubjectCanonicalizationContext c14nContext) {

        log.error("legacy C14N no supported");
        ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT_C14N_CTX);
    }
//CheckStyle: ReturnCount ON
    
    /** Factory used to generate a specific Connector. 
     * @param activationCondition - the activationCondition
     * @return an appropriate FlowDescriptor 
     */
    public static NameIDCanonicalizationFlowDescriptor c14LegacyPrincipalConnectorFactory(
                final @ParameterName(name="activationCondition") Predicate<ProfileRequestContext> activationCondition) {
        DeprecationSupport.warn(ObjectType.BEAN, "c14n/LegacyPrincipalConnector", "c14n/subject-c14n.xml", "<remove>");
        final NameIDCanonicalizationFlowDescriptor result = new NameIDCanonicalizationFlowDescriptor();
        result.setActivationCondition(activationCondition);
        return result;
    }

    /**
     * A predicate that determines if this action can run or not.  This can never run.
     */
    public static class ActivationCondition implements Predicate<ProfileRequestContext> {

        /** Service used to get the resolver used to fetch attributes. */
        @Nullable private final ReloadableService<AttributeResolver> attributeResolverService;

        /**
         * Constructor.
         * 
         * @param service the service we need to interrogate.
         */
        public ActivationCondition(final @ParameterName(name="service") ReloadableService<AttributeResolver> service) {
            attributeResolverService = service;
        }

        /**
         * {@inheritDoc}
         * 
         * <p>Never run this</p>
         */
        public boolean test(@Nullable final ProfileRequestContext input) {
            return false;
        }
    }
}