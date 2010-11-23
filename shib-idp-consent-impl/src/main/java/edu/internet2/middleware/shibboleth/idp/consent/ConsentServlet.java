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

package edu.internet2.middleware.shibboleth.idp.consent;

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

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.idp.attribute.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.storage.Storage;



/**
 *
 */

public class ConsentServlet extends HttpServlet {

    /**
     * 
     */
    private static final long serialVersionUID = 2763387866916451439L;

    private final Logger logger = LoggerFactory.getLogger(ConsentServlet.class);
    
    public static final String USER_KEY = "consent.key.user";
    public static final String RELYINGPARTYID_KEY = "consent.key.relyingPartyId";
    public static final String ATTRIBUTES_KEY = "consent.key.attributes";
    
    @Resource(name="consent.storage")
    private Storage storage;
    
    @Resource(name="consent.config.localizationHelper")
    private LocalizationHelper localizationHelper;
    
    @Resource(name="consent.config.globalConsentEnabled")
    private boolean globalConsentEnabled;
    
    @Resource(name="consent.config.attributeSortOrder")
    private List<String> attributeSortOrder;
    
    
    /** {@inheritDoc} */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String relyingPartyId = (String) request.getAttribute(RELYINGPARTYID_KEY);
        Collection<Attribute<?>> attributes = (Collection<Attribute<?>>) request.getAttribute(ATTRIBUTES_KEY);
        
        String resourceName = localizationHelper.getRelyingPartyName(relyingPartyId, request.getLocale());
        String resourceDescription = localizationHelper.getRelyingPartyDescription(relyingPartyId, request.getLocale());
   
        SortedSet<Attribute<?>> sortedAttributes = ConsentHelper.sortAttributes(attributeSortOrder, attributes);
        
        List<DisplayAttribute> displayAttributes = createDisplayAttributes(sortedAttributes, request.getLocale());
        
        // TODO show velocity view including model for
        // resourceName
        // resourceDescription
        // displayAttributes
        // globalConsentEnabled
    }

    /** {@inheritDoc} */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = (User) request.getAttribute(USER_KEY); // Probably we need to go via session or other storage service
        String relyingPartyId = (String) request.getAttribute(RELYINGPARTYID_KEY);
        Collection<Attribute<?>> attributes = (Collection<Attribute<?>>) request.getAttribute(ATTRIBUTES_KEY);
        
        
        logger.debug("User {} accepted attribute release for relying party {}", user, relyingPartyId);
        DateTime consentDate = new DateTime();
        user.setAttributeReleases(relyingPartyId, AttributeRelease.createAttributeReleases(attributes, consentDate));
        
        boolean globalConsent = request.getParameter("consent.global") != null && request.getParameter("consent.global").equals("yes");
        if (globalConsentEnabled && globalConsent) {
            logger.debug("User {} has given global consent", user);
            user.setGlobalConsent(true);
        }
        
        if (storage.containsUser(user.getId())) {
            storage.updateUser(user);
        } else {
            storage.createUser(user);
        }
        
        for (AttributeRelease attributeRelease: user.getAttributeReleases(relyingPartyId)) {
            if (storage.containsAttributeRelease(user.getId(), relyingPartyId, attributeRelease.getAttributeId())) {
                storage.updateAttributeRelease(user.getId(), relyingPartyId, attributeRelease);
            } else {
                storage.createAttributeRelease(user.getId(), relyingPartyId, attributeRelease);
            }
        }
    }
    
    private  List<DisplayAttribute> createDisplayAttributes(Collection<Attribute<?>> attributes, Locale locale) {
        List<DisplayAttribute> displayAttributes = new ArrayList<DisplayAttribute>();
        for (Attribute attribute: attributes) {
            displayAttributes.add(new DisplayAttribute(attribute, locale));
        }
        return displayAttributes;
    }
    
    private class DisplayAttribute {
        public final String name;
        public final String description;
        public final String values;
        
        public DisplayAttribute(Attribute<?> attribute, Locale locale) {
            name = localizationHelper.getAttributeName(attribute, locale);
            description = localizationHelper.getAttributeDescription(attribute, locale);
            StringBuilder valuesBuilder = new StringBuilder();
            for (Object value : attribute.getValues()) {
                valuesBuilder.append(value).append("<br />");
            }
            values = valuesBuilder.toString();
        }
    }
}