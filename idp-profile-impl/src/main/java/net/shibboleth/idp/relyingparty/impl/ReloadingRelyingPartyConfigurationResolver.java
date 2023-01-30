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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.security.config.SecurityConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyConfigurationResolver;
import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.resolver.ResolverException;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.shared.service.ServiceException;
import net.shibboleth.shared.service.ServiceableComponent;

/**
 * Retrieves a per-relying party configuration for a given profile request based on the request context. The
 * configuration is loaded via the supplied service.
 * 
 * <p>
 * Note that this resolver requires that none of the returned structures do any operations on receipt of
 * {@link #destroy()} since the returned value is not covered by the
 * </p>
 */
public class ReloadingRelyingPartyConfigurationResolver extends AbstractIdentifiableInitializableComponent implements
        RelyingPartyConfigurationResolver {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ReloadingRelyingPartyConfigurationResolver.class);

    /** The service which managed the reloading. */
    private final ReloadableService<RelyingPartyConfigurationResolver> service;

    /**
     * Constructor.
     * 
     * @param resolverService the service which will manage the loading.
     */
    public ReloadingRelyingPartyConfigurationResolver(@Nonnull @ParameterName(name="resolverService")
    final ReloadableService<RelyingPartyConfigurationResolver> resolverService) {
        service = Constraint.isNotNull(resolverService, "RelyingParty Service cannot be null");
    }

    /** {@inheritDoc} */
    @Override @Nonnull @NonnullElements public Iterable<RelyingPartyConfiguration> resolve(
            @Nullable final ProfileRequestContext context) throws ResolverException {
        checkComponentActive();
        try (final ServiceableComponent<RelyingPartyConfigurationResolver> component = service.getServiceableComponent()) {
            final RelyingPartyConfigurationResolver resolver = component.getComponent();
            final List<RelyingPartyConfiguration> results = new ArrayList<>();
            for (final RelyingPartyConfiguration result : resolver.resolve(context)) {
                results.add(result);
            }
            return results;
        } catch (final ResolverException e) {
            log.error("RelyingPartyResolver '{}': error in resolution", getId(), e);
        } catch (final ServiceException e) {
            log.error("RelyingPartyResolver '{}': Invalid RelyingPartyResolver configuration", getId(), e);
        }
        return Collections.emptySet();
    }

    /** {@inheritDoc} */
    @Override @Nullable public RelyingPartyConfiguration resolveSingle(@Nullable final ProfileRequestContext context)
            throws ResolverException {
        checkComponentActive();
        try (final ServiceableComponent<RelyingPartyConfigurationResolver> component = service.getServiceableComponent()){
            return component.getComponent().resolveSingle(context);
        } catch (final ResolverException e) {
            log.error("RelyingPartyResolver '{}': error in resolution", getId(), e);
        } catch (final ServiceException e) {
            log.error("RelyingPartyResolver '{}': Invalid RelyingPartyResolver configuration", getId(), e);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override public SecurityConfiguration getDefaultSecurityConfiguration(final String profileId) {
        checkComponentActive();
        try (final ServiceableComponent<RelyingPartyConfigurationResolver> component = service.getServiceableComponent()){
            return component.getComponent().getDefaultSecurityConfiguration(profileId);
        } catch (final ServiceException e) {
            log.error("RelyingPartyResolver '{}': Invalid RelyingPartyResolver configuration", getId(), e);
        }
        return null;
    }
    
}