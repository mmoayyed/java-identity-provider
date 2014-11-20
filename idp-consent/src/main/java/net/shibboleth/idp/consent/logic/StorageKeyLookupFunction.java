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

package net.shibboleth.idp.consent.logic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ContextDataLookupFunction;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Joiner;

/**
 * {@link ContextDataLookupFunction} to return the storage key for a consent flow. The storage key consists of the
 * relying party ID and the value of an attribute joined with a delimiter.
 */
public class StorageKeyLookupFunction implements ContextDataLookupFunction<ProfileRequestContext, String> {

    /** The delimiter. */
    @Nonnull public static final String DELIMITER = ":";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(StorageKeyLookupFunction.class);

    /** Lookup function for relying party id. */
    @Nonnull private Function<ProfileRequestContext, String> relyingPartyIdLookupFunction;

    /** Lookup function for user id. */
    @Nonnull private Function<ProfileRequestContext, String> userIdLookupFunction;

    /**
     * Constructor.
     *
     * @param attributeId identifier of attribute whose value is the user identifier
     */
    public StorageKeyLookupFunction(@Nonnull @NotEmpty String attributeId) {
        this(new RelyingPartyIdLookupFunction(), new AttributeValueLookupFunction(attributeId));
    }

    /**
     * Constructor.
     *
     * @param relyingPartyIdLookupStrategy strategy used to locate the relying party ID
     * @param userIdLookupStrategy strategy used to locate the user ID
     */
    public StorageKeyLookupFunction(@Nonnull Function<ProfileRequestContext, String> relyingPartyIdLookupStrategy,
            @Nonnull Function<ProfileRequestContext, String> userIdLookupStrategy) {
        relyingPartyIdLookupFunction =
                Constraint.isNotNull(relyingPartyIdLookupStrategy, "Relying party ID lookup strategy cannot be null");
        userIdLookupFunction = Constraint.isNotNull(userIdLookupStrategy, "User ID lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    public String apply(@Nullable final ProfileRequestContext input) {
        if (input == null) {
            return null;
        }

        final String relyingPartyId = relyingPartyIdLookupFunction.apply(input);
        log.debug("Resolved relying party id '{}'", relyingPartyId);

        final String userId = userIdLookupFunction.apply(input);
        log.debug("Resolved user id '{}'", userId);

        final Joiner joiner = Joiner.on(DELIMITER).skipNulls();

        final String storageKey = joiner.join(userId, relyingPartyId);
        log.debug("Resolved storage key '{}'", storageKey);
        return storageKey;
    }

}
