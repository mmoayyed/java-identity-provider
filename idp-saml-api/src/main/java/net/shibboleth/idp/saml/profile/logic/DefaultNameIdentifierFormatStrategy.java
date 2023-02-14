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

package net.shibboleth.idp.saml.profile.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.profile.logic.MetadataNameIdentifierFormatStrategy;
import org.opensaml.saml.saml2.core.NameID;
import org.slf4j.Logger;

/**
 * Function to filter a set of candidate NameIdentifier/NameID Format values derived from an entity's SAML metadata
 * against configuration preferences.
 */
public class DefaultNameIdentifierFormatStrategy extends MetadataNameIdentifierFormatStrategy {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(DefaultNameIdentifierFormatStrategy.class);

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** Override the {@link ProfileConfiguration} to look for rather than whatever's populated. */
    @Nullable private String profileId;
    
    /** Default format to use if nothing else is known. */
    @Nonnull @NotEmpty private String defaultFormat;

    /** Constructor. */
    public DefaultNameIdentifierFormatStrategy() {
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
        defaultFormat = NameID.UNSPECIFIED;
    }

    /**
     * Set the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link RelyingPartyContext} associated with a given
     *            {@link ProfileRequestContext}
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {

        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy cannot be null");
    }
    
    /**
     * Set the profile configuration ID to locate in the {@link RelyingPartyConfiguration} for the purposes of
     * establishing format precedence rules.
     * 
     * <p>By default/without one set, the strategy is to use the configuration object populated in the
     * {@link RelyingPartyContext}.</p>
     * 
     * @param id profile ID to look for
     */
    public void setProfileId(@Nullable final String id) {
        profileId = StringSupport.trimOrNull(id);
    }

    /**
     * Set the default format to return.
     * 
     * @param format default format
     */
    public void setDefaultFormat(@Nonnull @NotEmpty final String format) {
        defaultFormat =
                Constraint.isNotNull(StringSupport.trimOrNull(format), "Default format cannot be null or empty");
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NonnullElements public List<String> apply(@Nullable final ProfileRequestContext input) {
        final List<String> fromConfig = new ArrayList<>();
        final List<String> fromMetadata = super.apply(input);

        final RelyingPartyContext relyingPartyCtx = relyingPartyContextLookupStrategy.apply(input);
        final RelyingPartyConfiguration rpConfig = relyingPartyCtx != null ? relyingPartyCtx.getConfiguration() : null;
        if (rpConfig != null) {
            final ProfileConfiguration profileConfig;
            
            if (profileId != null) {
                log.debug("Using overridden profile configuration ID: {}", profileId);
                profileConfig = rpConfig.getProfileConfiguration(input, profileId);
            } else {
                assert relyingPartyCtx != null;
                profileConfig = relyingPartyCtx.getProfileConfig();
            }
            
            if (profileConfig
                    instanceof net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration) {
                fromConfig.addAll(
                        ((net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration) profileConfig)
                        .getNameIDFormatPrecedence(input));
                log.debug("Configuration specifies the following formats: {}", fromConfig);
            } else if (profileConfig instanceof
                    net.shibboleth.idp.saml.saml1.profile.config.BrowserSSOProfileConfiguration) {
                fromConfig.addAll(
                        ((net.shibboleth.idp.saml.saml1.profile.config.BrowserSSOProfileConfiguration) profileConfig)
                        .getNameIDFormatPrecedence(input));
                log.debug("Configuration specifies the following formats: {}", fromConfig);
            } else {
                log.debug("No ProfileConfiguraton available (or not a BrowserSSOProfileConfiguration)");
            }
        } else {
            log.debug("No RelyingPartyContext or RelyingPartyConfiguration available");
        }

        if (fromConfig.isEmpty()) {
            if (fromMetadata.isEmpty()) {
                log.debug("No formats specified in configuration or in metadata, returning default");
                return CollectionSupport.singletonList(defaultFormat);
            }
            log.debug("Configuration did not specify any formats, relying on metadata alone");
            return fromMetadata;
        } else if (fromMetadata.isEmpty()) {
            log.debug("Metadata did not specify any formats, relying on configuration alone");
            return fromConfig;
        } else {
            fromConfig.retainAll(fromMetadata);
            log.debug("Filtered non-metadata-supported formats from configured formats, leaving: {}", fromConfig);
            return fromConfig;
        }
    }
    
}