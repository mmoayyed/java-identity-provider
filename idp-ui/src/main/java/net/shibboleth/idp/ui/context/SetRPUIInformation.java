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

package net.shibboleth.idp.ui.context;

import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.collection.LazyList;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.OutboundMessageContextLookup;
import org.opensaml.saml.common.messaging.context.AttributeConsumingServiceContext;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.ext.saml2mdui.UIInfo;
import org.opensaml.saml.saml2.common.Extensions;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;

import com.google.common.base.Function;
import com.google.common.base.Functions;

/**
 * Action to populate the {@link ProfileRequestContext} with a {@link RelyingPartyUIContext}. The contents are populated
 * by applying the XXX
 */
public class SetRPUIInformation extends AbstractProfileAction {

    /** Strategy function for access to {@link SAMLMetadataContext}. */
    private Function<ProfileRequestContext, SAMLMetadataContext> metadataContextLookupStrategy = Functions.compose(
            new ChildContextLookup<>(SAMLMetadataContext.class), Functions.compose(new ChildContextLookup<>(
                    SAMLPeerEntityContext.class), new OutboundMessageContextLookup()));

    /** Strategy function to create the {@link RelyingPartyUIContext}. */
    private Function<ProfileRequestContext, RelyingPartyUIContext> rpUIContextCreateStrategy = Functions.compose(
            new ChildContextLookup<AuthenticationContext, RelyingPartyUIContext>(RelyingPartyUIContext.class, true),
            new ChildContextLookup<ProfileRequestContext, AuthenticationContext>(AuthenticationContext.class, true));

    /**
     * The {@link EntityDescriptor}. If we cannot find this we short cut the {@link #doExecute(ProfileRequestContext)}
     * stage.
     */
    private EntityDescriptor entityDescriptor;

    /** The {@link SPSSODescriptor}. Not finding this is not fatal */
    private SPSSODescriptor spSSODescriptor;

    /** The RPUI context - we always create this in {@link #doPreExecute(ProfileRequestContext)}. */
    private RelyingPartyUIContext rpUIContext;

    /** The ACS context. */
    private AttributeConsumingService acsDesriptor;

    /**
     * Get the mechanism to go from the {@link ProfileRequestContext} to the {@link SAMLMetadataContext}.
     * 
     * @return lookup strategy
     */
    public Function<ProfileRequestContext, SAMLMetadataContext> getMetadataContextLookupStrategy() {
        return metadataContextLookupStrategy;
    }

    /**
     * Set the mechanism to go from the {@link ProfileRequestContext} to the {@link SAMLMetadataContext}.
     * 
     * @param strgy what to set.
     */
    public void setMetadataContextLookupStrategy(@Nonnull Function<ProfileRequestContext, SAMLMetadataContext> strgy) {
        metadataContextLookupStrategy = Constraint.isNotNull(strgy, "Injected Metadata Strategy cannot be null");
    }

    /**
     * Get the mechanism to create/get the {@link RelyingPartyUIContext} from the {@link ProfileRequestContext}.
     * 
     * @return lookup/creation strategy
     */
    public Function<ProfileRequestContext, RelyingPartyUIContext> getRPUIContextCreateStrategy() {
        return rpUIContextCreateStrategy;
    }

    /**
     * Set the mechanism to create/get the {@link RelyingPartyUIContext} from the {@link ProfileRequestContext}.
     * 
     * @param strategy what to set.
     */
    public void setRPUIContextCreateStrategy(@Nonnull Function<ProfileRequestContext, RelyingPartyUIContext> strategy) {
        rpUIContextCreateStrategy = Constraint.isNotNull(strategy, "Injected RPUI Strategy cannot be null");
    }
    
    /**
     * Get the RP {@link UIInfo}, caching the value and consulting if needed.
     * 
     * @return the value or null if there is none.
     */
    protected UIInfo getRPUInfo() {
        final Extensions exts = spSSODescriptor.getExtensions();
        if (exts != null) {
            for (XMLObject object : exts.getOrderedChildren()) {
                if (object instanceof UIInfo) {
                    return (UIInfo) object;
                }
            }
        }
        return null;
    }

    /**
     * Pluck the languages from the browser.
     * 
     * @return the two letter language
     */
    @Nonnull @NonnullElements protected List<String> getBrowserLanguages() {

        final Enumeration<Locale> locales = getHttpServletRequest().getLocales();

        final List<String> languages = new LazyList<>();

        while (locales.hasMoreElements()) {
            final Locale locale = locales.nextElement();
            if (null != locale.getLanguage()) {
                languages.add(locale.getLanguage());
            }
        }
        return languages;
    }

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(ProfileRequestContext profileRequestContext) {
        rpUIContext = rpUIContextCreateStrategy.apply(profileRequestContext);
        final SAMLMetadataContext metadataContext = metadataContextLookupStrategy.apply(profileRequestContext);
        if (null == metadataContext) {
            return false;
        }
        entityDescriptor = metadataContext.getEntityDescriptor();
        if (null == entityDescriptor) {
            return false;
        }
        final RoleDescriptor roleDescriptor = metadataContext.getRoleDescriptor();
        if (roleDescriptor instanceof SPSSODescriptor) {
            spSSODescriptor = (SPSSODescriptor) roleDescriptor;
        }
        final AttributeConsumingServiceContext acsCtx =
                metadataContext.getSubcontext(AttributeConsumingServiceContext.class);
        if (null != acsCtx) {
            acsDesriptor = acsCtx.getAttributeConsumingService();
        }

        return super.doPreExecute(profileRequestContext);
    }    
    
    /** {@inheritDoc} */
    @Override protected void doExecute(ProfileRequestContext profileRequestContext) {
        super.doExecute(profileRequestContext);
        
        rpUIContext.setRPEntityDescriptor(entityDescriptor);
        rpUIContext.setRPSPSSODescriptor(spSSODescriptor);
        rpUIContext.setRPAttributeConsumingService(acsDesriptor);
        rpUIContext.setRPUInfo(getRPUInfo());
        rpUIContext.setBrowserLanguages(getBrowserLanguages());
    }
}
