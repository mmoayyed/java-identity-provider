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

package net.shibboleth.idp.authn.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.security.auth.Subject;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.AbstractRelyingPartyLookupFunction;
import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * An implementation of the <code>loginConfigStrategy</code> for {@link JAASCredentialValidator}
 * which uses a supplied map to resolve the JAAS config to use.
 *
 * <p>
 * The map key is the relying party ID, the value is the JAAS config name.
 * </p>
 */
public class RelyingPartyMapJAASLoginConfigStrategy
        extends AbstractRelyingPartyLookupFunction<Collection<Pair<String,Subject>>> {

    /** Logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(RelyingPartyMapJAASLoginConfigStrategy.class);

    /** Map holding the relying party to JAAS config name mappings. */
    @Nonnull private Map<String, String> relyingPartyMap;

    /** The default JAAS config name to use when no specific mapping configured. */
    @Nonnull private String defaultConfigName;

    /**
     * Constructor.
     *
     * @param map the map of relying party ID to JAAS config name
     */
    public RelyingPartyMapJAASLoginConfigStrategy(final @Nonnull @ParameterName(name="map") Map<String,String> map) {
        relyingPartyMap = Constraint.isNotNull(map, "Relying party map was null");

        defaultConfigName = "ShibUserPassAuth";
    }

    /**
     * Set the default JAAS config name returned when no specific one is configured for a particular
     * relying party.
     *
     * <p>
     * The default value is: ShibUserPassAuth.
     * </p>
     *
     * @param name the default JAAS config name
     */
    public void setDefaultConfigName(final @Nonnull @NotEmpty String name) {
        defaultConfigName = Constraint.isNotNull(StringSupport.trimOrNull(name),
                "Default config name was null or empty");
    }

    /** {@inheritDoc} */
    @Nonnull public Collection<Pair<String, Subject>> apply(
            @Nonnull final ProfileRequestContext profileRequestContext) {

        final RelyingPartyContext relyingPartyContext =
                getRelyingPartyContextLookupStrategy().apply(profileRequestContext);
        if (relyingPartyContext == null) {
            log.warn("No RelyingPartyContext was available, using default config name");
            return Collections.singleton(new Pair<>(defaultConfigName, (Subject)null));
        }

        final String relyingPartyId = relyingPartyContext.getRelyingPartyId();
        if (relyingPartyId == null) {
            log.warn("No relying party ID was available, using default config name");
            return Collections.singleton(new Pair<>(defaultConfigName, (Subject)null));
        }

        final String config = StringSupport.trimOrNull(relyingPartyMap.get(relyingPartyId));
        if (config != null) {
            log.debug("For relying party ID '{}' resolved JAAS config name '{}'", relyingPartyId, config);
            return Collections.singleton(new Pair<>(config, (Subject)null));
        }
        log.debug("For relying party ID '{}' resolved no JAAS config name, returning default", relyingPartyId);
        return Collections.singleton(new Pair<>(defaultConfigName, (Subject)null));
    }

}
