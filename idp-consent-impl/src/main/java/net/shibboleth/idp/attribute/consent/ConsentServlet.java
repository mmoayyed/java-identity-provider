/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.attribute.consent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.consent.ConsentContext.Consent;
import net.shibboleth.idp.attribute.consent.storage.Storage;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Consent servlet which control the consent UI. */
public class ConsentServlet extends HttpServlet {

    /** Key for the current user object. */
    public static final String USER_KEY = "consent.key.user";

    /** Key for the current relying party id. */
    public static final String RELYINGPARTYID_KEY = "consent.key.relyingPartyId";

    /** Key for the current attribute release. */
    public static final String ATTRIBUTES_KEY = "consent.key.attributes";

    /** Serial version UID. */
    private static final long serialVersionUID = 2763387866916451439L;

    /** Class logger. */
    private final Logger logger = LoggerFactory.getLogger(ConsentServlet.class);

    /** The {@see Storage} instance which is used. */
    @Resource(name = "consent.storage")
    private Storage storage;

    /** The {@see LocalizationHelper} which is used. */
    @Resource(name = "consent.config.localizationHelper")
    private LocalizationHelper localizationHelper;

    /** Whether global consent is enabled or not. */
    @Resource(name = "consent.config.globalConsentEnabled")
    private boolean globalConsentEnabled;

    /** The specified attribute sort order. */
    @Resource(name = "consent.config.attributeSortOrder")
    private List<String> attributeSortOrder;

    /** The velocity engine. */
    @Resource(name = "velocityEngine")
    private VelocityEngine velocityEngine;

    /** {@inheritDoc} */
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
            IOException {
        final String relyingPartyId = (String) request.getAttribute(RELYINGPARTYID_KEY);
        final Collection<Attribute<?>> attributes = (Collection<Attribute<?>>) request.getAttribute(ATTRIBUTES_KEY);

        final String resourceName = localizationHelper.getRelyingPartyName(relyingPartyId, request.getLocale());
        final String resourceDescription =
                localizationHelper.getRelyingPartyDescription(relyingPartyId, request.getLocale());

        final SortedSet<Attribute<?>> sortedAttributes = ConsentHelper.sortAttributes(attributeSortOrder, attributes);

        final List<DisplayAttribute> displayAttributes = createDisplayAttributes(sortedAttributes, request.getLocale());

        final Context context = new VelocityContext();
        context.put("resourceName", resourceName);
        context.put("resourceDescription", resourceDescription);
        context.put("displayAttributes", displayAttributes);
        context.put("globalConsentEnabled", globalConsentEnabled);

        try {
            velocityEngine.mergeTemplate("attribute-release.vm", "UTF-8", context, response.getWriter());
        } catch (Exception e) {
            throw new ServletException("Unable to call velocity engine", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        final ConsentContext consentContext = ConsentHelper.getConsentContext(request);
        // Probably we need to go via session or other storage service
        final User user = (User) request.getAttribute(USER_KEY);
        final String relyingPartyId = (String) request.getAttribute(RELYINGPARTYID_KEY);
        final Collection<Attribute<?>> attributes = (Collection<Attribute<?>>) request.getAttribute(ATTRIBUTES_KEY);

        final boolean accepted =
                request.getParameter("consent.accept") != null && request.getParameter("consent.accept").equals("yes");

        if (accepted) {
            logger.debug("User {} accepted attribute release for relying party {}", user, relyingPartyId);
            final DateTime consentDate = new DateTime();
            user.setAttributeReleases(relyingPartyId, AttributeRelease.createAttributeReleases(attributes, consentDate));

            final boolean globalConsent =
                    request.getParameter("consent.global") != null
                            && request.getParameter("consent.global").equals("yes");
            if (globalConsentEnabled && globalConsent) {
                logger.debug("User {} has given global consent", user);
                user.setGlobalConsent(true);
            }

            if (storage.containsUser(user.getId())) {
                storage.updateUser(user);
            } else {
                storage.createUser(user);
            }

            for (AttributeRelease attributeRelease : user.getAttributeReleases(relyingPartyId)) {
                if (storage.containsAttributeRelease(user.getId(), relyingPartyId, attributeRelease.getAttributeId())) {
                    storage.updateAttributeRelease(user.getId(), relyingPartyId, attributeRelease);
                } else {
                    storage.createAttributeRelease(user.getId(), relyingPartyId, attributeRelease);
                }
            }

            consentContext.setConsentDecision(Consent.OBTAINED);

        } else {
            logger.debug("User {} declined attribute release for relying party {}", user, relyingPartyId);
            consentContext.setConsentDecision(Consent.DENIED);
        }
    }

    /**
     * Creates a list of display attributes using a specific locale.
     * 
     * @param attributes The attributes.
     * @param locale The locale.
     * @return Returns a list of display attributes.
     */
    private List<DisplayAttribute> createDisplayAttributes(final Collection<Attribute<?>> attributes,
            final Locale locale) {
        final List<DisplayAttribute> displayAttributes = new ArrayList<DisplayAttribute>();
        for (Attribute attribute : attributes) {
            displayAttributes.add(new DisplayAttribute(attribute, locale));
        }
        return displayAttributes;
    }

    /**
     * Aggregation class for an attribute.
     * 
     * It used for displaying it in the UI. Name and description is set in the right language.
     */
    public class DisplayAttribute {
        /** The name of the attribute. */
        private final String name;

        /** The description of the attribute. */
        private final String description;

        /** The values of the attribute. */
        private final Collection<?> values;

        /**
         * Constructs a display attribute from an attribute and defined locale.
         * 
         * @param attribute The attribute.
         * @param locale The locale.
         */
        public DisplayAttribute(final Attribute<?> attribute, final Locale locale) {
            name = localizationHelper.getAttributeName(attribute, locale);
            description = localizationHelper.getAttributeDescription(attribute, locale);
            values = attribute.getValues();
        }

        /**
         * Gets the name.
         * 
         * @return Returns the name.
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the description.
         * 
         * @return Returns the description.
         */
        public String getDescription() {
            return description;
        }

        /**
         * Gets the values.
         * 
         * @return Returns the values.
         */
        public Collection<?> getValues() {
            return values;
        }

    }
}