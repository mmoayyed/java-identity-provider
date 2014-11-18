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

package net.shibboleth.idp.consent.flow.ar;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

/**
 * Attribute consent action which constrains the attributes released to those consented to.
 * 
 * For every IdP attribute in the attribute context, this action will release the attribute iff consent for the
 * attribute has been approved.
 * 
 * Consent is obtained from the consent context. If there are no current consents then the previous consents are used to
 * determine the attributes to be released. The current consents will be present if user input has been obtained during
 * the attribute release flow. The previous consents will be used when there is no user interaction, for example if
 * there are no new attributes to consent to.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @post See above.
 */
public class ReleaseAttributes extends AbstractAttributeReleaseAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ReleaseAttributes.class);

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        final Map<String, Consent> consents =
                getConsentContext().getCurrentConsents().isEmpty() ? getConsentContext().getPreviousConsents()
                        : getConsentContext().getCurrentConsents();
        log.debug("{} Consents '{}'", getLogPrefix(), consents);

        final Map<String, IdPAttribute> attributes = getAttributeContext().getIdPAttributes();
        log.debug("{} Attributes before release '{}'", getLogPrefix(), attributes);

        final Map<String, IdPAttribute> releasedAttributes = new HashMap<String, IdPAttribute>();

        for (final IdPAttribute attribute : attributes.values()) {
            if (!consents.containsKey(attribute.getId())) {
                log.debug("{} Unknown attribute '{}' will not be released", getLogPrefix(), attribute);
                continue;
            }
            final Consent consent = consents.get(attribute.getId());
            if (consent.isApproved()) {
                log.debug("{} Release of attribute '{}' is consented to", getLogPrefix(), attribute);
                releasedAttributes.put(attribute.getId(), attribute);
            } else {
                log.debug("{} Release of attribute '{}' is not consented to", getLogPrefix(), attribute);
            }
        }

        log.debug("{} Releasing attributes '{}'", getLogPrefix(), releasedAttributes);

        if (log.isDebugEnabled()) {
            final MapDifference<String, IdPAttribute> diff = Maps.difference(attributes, releasedAttributes);
            log.debug("{} Not releasing attributes '{}'", getLogPrefix(), diff.entriesOnlyOnLeft());
        }

        getAttributeContext().setIdPAttributes(releasedAttributes.values());
    }

}
