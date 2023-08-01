/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.profile.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.criterion.ProfileRequestContextCriterion;
import org.slf4j.Logger;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.profile.relyingparty.RelyingPartyConfigurationResolver;
import net.shibboleth.profile.relyingparty.VerifiedProfileCriterion;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.ResolverException;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.shared.service.ServiceException;
import net.shibboleth.shared.service.ServiceableComponent;

/**
 * This action attempts to resolve a {@link RelyingPartyConfiguration} and adds it to the {@link RelyingPartyContext}
 * that was looked up.
 * 
 * <p>Both the original and the later-added criteria-driven resolvers are supported.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link IdPEventIds#INVALID_RELYING_PARTY_CTX}
 * @event {@link IdPEventIds#INVALID_RELYING_PARTY_CONFIG}
 * 
 * @post If a {@link RelyingPartyContext} is located, it will be populated with a non-null result of applying
 * the supplied {@link RelyingPartyConfigurationResolver} to the {@link ProfileRequestContext}.
 */
public final class SelectRelyingPartyConfiguration extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SelectRelyingPartyConfiguration.class);

    /** Resolver used to look up relying party configurations. */
    @NonnullAfterInit private ReloadableService<RelyingPartyConfigurationResolver> rpConfigResolver;

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** The {@link RelyingPartyContext} to manipulate. */
    @NonnullBeforeExec private RelyingPartyContext relyingPartyCtx;
    
    /** Constructor. */
    public SelectRelyingPartyConfiguration() {
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
    }

    /**
     * Set the relying party config resolver to use.
     * 
     * @param resolver  the resolver to use
     */
    public void setRelyingPartyConfigurationResolver(
            @Nonnull final ReloadableService<RelyingPartyConfigurationResolver> resolver) {
        checkSetterPreconditions();
        
        rpConfigResolver = Constraint.isNotNull(resolver, "Relying party configuration resolver cannot be null");
    }
    
    /**
     * Set the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link RelyingPartyContext} associated with a given
     *            {@link ProfileRequestContext}
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        checkSetterPreconditions();
        
        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy cannot be null");
    }
    
    /** Null safe getter.
     * @return Returns the relyingPartyCtx.
     */
    @SuppressWarnings("null")
    @Nonnull private RelyingPartyContext getRelyingPartyCtx() {
        assert isPreExecuteCalled();
        return relyingPartyCtx;
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (rpConfigResolver == null) {
            throw new ComponentInitializationException("RelyingPartyConfigurationResolver cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        relyingPartyCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (relyingPartyCtx == null) {
            log.debug("{} No relying party context available", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    public void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        try (final ServiceableComponent<RelyingPartyConfigurationResolver> resolver =
                rpConfigResolver.getServiceableComponent()) {
            
            final RelyingPartyConfiguration config;
            final CriteriaSet criteria = new CriteriaSet();
            if (getRelyingPartyCtx().isVerified()) {
                criteria.add(new VerifiedProfileCriterion(true));
            }
            if (getRelyingPartyCtx().getParent() == profileRequestContext) {
                // Works as is.
                criteria.add(new ProfileRequestContextCriterion(profileRequestContext));
                config = resolver.getComponent().resolveSingle(criteria);
            } else {
                // Temporarily re-root for compatibility.
                // TODO: I think this *may* be moot now with the addition of the
                // explicit VerifiedProfileCriterion.
                final ProfileRequestContext newPRC = new ProfileRequestContext();
                final BaseContext originalParent = getRelyingPartyCtx().getParent();
                newPRC.addSubcontext(getRelyingPartyCtx());
                criteria.add(new ProfileRequestContextCriterion(newPRC));
                config = resolver.getComponent().resolveSingle(criteria);
                if (originalParent != null) {
                    originalParent.addSubcontext(getRelyingPartyCtx());
                }
            }
            
            if (config == null) {
                log.debug("{} No relying party configuration applies to this request", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CONFIG);
                return;
            }

            log.debug("{} Found relying party configuration {} for request", getLogPrefix(), config.getId());
            getRelyingPartyCtx().setConfiguration(config);
        } catch (final ResolverException e) {
            log.error("{} Error trying to resolve relying party configuration", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CONFIG);
        } catch (final ServiceException e) {
            log.error("{} Invalid relying party configuration", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CONFIG);
        }
    }
    
}