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

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.profile.logic.RelyingPartyIdPredicate;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Factory function for manufacturing activation conditions based on
 * {@link AttributeTranscoderRegistry#PROP_RELYINGPARTIES}.
 * 
 * @since 5.0.0
 */
public class RelyingPartiesActivationConditionFactory
        implements Function<Map<String,Object>,Predicate<ProfileRequestContext>> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(RelyingPartiesActivationConditionFactory.class);

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Nullable public Predicate<ProfileRequestContext> apply(@Nullable final Map<String,Object> rule) {
        final Object relyingParties = rule.get(AttributeTranscoderRegistry.PROP_RELYINGPARTIES);
        
        if (relyingParties instanceof Collection) {
            return new RelyingPartyIdPredicate((Collection<String>) relyingParties);
        } else if (relyingParties instanceof String) {
            final Collection<String> parsed = StringSupport.normalizeStringCollection(
                    StringSupport.stringToList((String) relyingParties, " "));
            return new RelyingPartyIdPredicate(parsed);
        } else if (relyingParties != null) {
            log.error("{} property did not contain a Collection or String, ignored",
                    AttributeTranscoderRegistry.PROP_RELYINGPARTIES);
        }
        
        return null;
    }

}