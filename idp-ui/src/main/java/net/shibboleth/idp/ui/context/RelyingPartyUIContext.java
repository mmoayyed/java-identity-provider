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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.messaging.context.BaseContext;
import org.opensaml.saml.ext.saml2mdui.Logo;
import org.opensaml.saml.ext.saml2mdui.UIInfo;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml.saml2.metadata.ContactPerson;
import org.opensaml.saml.saml2.metadata.ContactPersonTypeEnumeration;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Organization;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.attribute.AttributesMapContainer;
import net.shibboleth.idp.saml.metadata.ACSUIInfo;
import net.shibboleth.idp.saml.metadata.IdPUIInfo;
import net.shibboleth.idp.saml.metadata.OrganizationUIInfo;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * The context which carries the user interface information.
 */
public final class RelyingPartyUIContext extends BaseContext {

    /** The log. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(RelyingPartyUIContext.class);

    /** The appropriate {@link EntityDescriptor}. */
    @Nullable private EntityDescriptor rpEntityDescriptor;

    /** The appropriate {@link SPSSODescriptor}. */
    @Nullable private SPSSODescriptor rpSPSSODescriptor;

    /** The appropriate {@link ACSUIInfo}. */
    @Nullable private ACSUIInfo rpACSUIinfo;

    /** The appropriate {@link IdPUIInfo}. */
    @Nullable private IdPUIInfo rpUIInfo;

    /** The languages that this browser wants to know about. */
    @Nullable @Deprecated @NonnullElements private List<LanguageRange> browserLanguages;
    
    /** The languages that this the Operator want to fall back to. */
    @Nonnull private List<LanguageRange> fallbackLanguages;
    
    /** Current HTTP request, if available. */
    @Nullable private HttpServletRequest httpServletRequest;
    
    /** Constructor. */
    public RelyingPartyUIContext() {
        browserLanguages = Collections.emptyList();
        fallbackLanguages = Collections.emptyList();
    }

    /**
     * Get the {@link EntityDescriptor}.
     * 
     * @return Returns the entity.
     */
    @Nullable protected EntityDescriptor getRPEntityDescriptor() {
        return rpEntityDescriptor;
    }

    /**
     * Set the {@link EntityDescriptor}.
     * 
     * @param what what to set
     * 
     * @return this context
     */
    @Nonnull public RelyingPartyUIContext setRPEntityDescriptor(@Nullable final EntityDescriptor what) {
        rpEntityDescriptor = what;
        return this;
    }

    /**
     * Get the {@link SPSSODescriptor}.
     * 
     * @return Returns the SPSSODescriptor.
     */
    @Nullable protected SPSSODescriptor getRPSPSSODescriptor() {
        return rpSPSSODescriptor;
    }

    /**
     * Set the {@link SPSSODescriptor}.
     * 
     * @param what what to set
     * 
     * @return this context
     */
    @Nonnull public RelyingPartyUIContext setRPSPSSODescriptor(@Nullable final SPSSODescriptor what) {
        rpSPSSODescriptor = what;
        return this;
    }

    /**
     * Shorthand method that returns a collapsed copy of the String values of a given
     * mapped Entity Attribute in the metadata, or an empty collection. 
     * 
     * @param id    attribute ID
     * 
     * @return unmodifiable collection of string values
     * 
     * @since 4.0.0
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Collection<String> getEntityAttributeStringValues(
            @Nonnull @NotEmpty final String id) {
        
        XMLObject object = getRPEntityDescriptor(); 
        if (object == null) {
            return Collections.emptyList();
        }
        
        final List<String> accumulator = new ArrayList<>();
        
        while (object != null) {
            object.getObjectMetadata().get(AttributesMapContainer.class).forEach(
                    c -> accumulator.addAll(c.getStringValues(id)));
            object = object.getParent();
        }

        return List.copyOf(accumulator);
    }
    
    /**
     * Get the {@link ACSUIInfo} for the request.
     * 
     * @return Returns the RP's {@link ACSUIInfo}.
     */
    @Nullable protected ACSUIInfo getRPACSUInfo() {
        return rpACSUIinfo;
    }

    /**
     * Get the RP {@link UIInfo} associated with the request.
     * 
     * @return the value or null if there is none.
     */
    @Nullable protected IdPUIInfo getRPUInfo() {
        return rpUIInfo;
    }

    /**
     * Set the RP {@link IdPUIInfo} associated with the request.
     * We normally expect to get this from the object metadata.
     * @param what the value to set
     * 
     * @return this context
     */
    @Nonnull public RelyingPartyUIContext setRPUInfo(@Nullable final UIInfo what) {
        if (what == null) {
            return this;
        }
        final List<IdPUIInfo> list = what.getObjectMetadata().get(IdPUIInfo.class);
        if (list.isEmpty()) {
            rpUIInfo = new IdPUIInfo(what);
        } else {
            rpUIInfo = list.get(0);
        }
        return this;
    }

    /**
     * Set the {@link ACSUIInfo} for the request.
     * We normally expect to get this from the object metadata.
     * @param what what to set
     * 
     * @return this context
     */
    @Nonnull public RelyingPartyUIContext setRPAttributeConsumingService(
            @Nullable final AttributeConsumingService what) {
        if (what == null) {
            return this;
        }
        final List<ACSUIInfo> list = what.getObjectMetadata().get(ACSUIInfo.class);
        if (list.isEmpty()) {
            rpACSUIinfo = new ACSUIInfo(what);
        } else {
            rpACSUIinfo = list.get(0);
        }
        return this;
    }

    /** Set the Servlet Request.
     * @param what what to set.
     * @return this context
     */
    public RelyingPartyUIContext setHttpServletRequest(@Nonnull final HttpServletRequest what) {
        httpServletRequest = what;
        return this;
    }

    /**
     * Set the browser languages.
     *
     * @param languages the languages to set
     * @deprecated use {@link #setHttpServletRequest(HttpServletRequest)}
     * @return this context
     */
    @Deprecated(since="4.0.0", forRemoval=true)
    @Nonnull public RelyingPartyUIContext setBrowserLanguages(@Nonnull @NonnullElements final List<String> languages) {
        Constraint.isNotNull(languages, "Language List cannot be null");
        // The replacement was created in V4.1.1
        DeprecationSupport.warnOnce(ObjectType.METHOD, "setBrowserLanguages", null, "setHttpServletResponse");
        browserLanguages = languages.
                stream().
                filter(e -> e != null).
                map(s -> new LanguageRange(s)).
                collect(Collectors.toUnmodifiableList());
        return this;
    }

    /**
     * Set the browser languages.
     * 
     * @param ranges the languages to set
     * @deprecated use {@link #setHttpServletRequest(HttpServletRequest)}
     * @return this context
     */
    @Deprecated(since="4.1.1", forRemoval=true)
    @Nonnull public RelyingPartyUIContext setBrowserLanguageRanges(
            @Nonnull @NonnullElements final List<LanguageRange> ranges) {
        // The replacement was created in V4.1.1
        DeprecationSupport.warnOnce(ObjectType.METHOD, "setBrowserLanguageRanges", null, "setHttpServletResponse");
        browserLanguages = Constraint.isNotNull(ranges, "Language Range cannot be null");
        return this;
    }


    /**
     * Get the browser languages.
     * 
     * @return the languages.
     */
    @Nonnull @NonnullElements protected List<LanguageRange> getBrowserLanguages() {
        if (httpServletRequest == null) {
            return browserLanguages;
        }
        return SpringSupport.getLanguageRange(httpServletRequest);
    }

    /**
     * Set the fallback languages.
     * 
     * @param languages the languages to set
     * 
     * @return this context
     */
    @Nonnull public RelyingPartyUIContext setFallbackLanguages(@Nullable final List<String> languages) {
        if (languages == null || languages.isEmpty()) {
            fallbackLanguages = Collections.emptyList();
        }
        fallbackLanguages = languages.
                stream().
                filter(s -> s != null).
                map(s -> new LanguageRange(s)).
                collect(Collectors.toUnmodifiableList());
        return this;
    }

    /**
     * Get the fallback languages.
     * 
     * @return the languages.
     */
    @Nonnull @NonnullElements protected List<LanguageRange> getFallbackLanguages() {
        return fallbackLanguages;
    }

    /**
     * Check to see whether a supplied URL is acceptable, returning the default if it isn't.
     * 
     * @param url the url to look at
     * @param acceptableSchemes the schemes to test against
     * @return the input or null as appropriate.
     */
    @Nullable private String policeURL(@Nullable final String url,
            @Nonnull @NotEmpty final List<String> acceptableSchemes) {
        if (null == url) {
            log.trace("Empty Value - returning null");
            return null;
        }

        try {
            final String scheme = new URI(url).getScheme();

            for (final String acceptableScheme : acceptableSchemes) {
                if (acceptableScheme.equals(scheme)) {
                    log.debug("Acceptable Scheme '{}', returning value '{}'", acceptableScheme, url);
                    return url;
                }
            }

            log.warn("The logo URL '{}' contained an invalid scheme (expected '{}'), returning null", url,
                    acceptableSchemes);
            return null;
        } catch (final URISyntaxException e) {
            log.warn("The logo URL '{}' contained was not a URL, returning null", url);
            return null;
        }
    }

    /**
     * Police a url found for a logo.
     * 
     * @param url the url to look at
     * @return the input or the default as appropriate
     */
    @Nullable protected String policeURLLogo(@Nullable final String url) {
        return policeURL(url, Arrays.asList("http", "https", "data"));
    }

    /**
     * Police a url found for non logo data.
     * 
     * @param url the url to look at
     * @return the input or the default as appropriate
     */
    @Nullable protected String policeURLNonLogo(@Nullable final String url) {
        return policeURL(url, Arrays.asList("http", "https", "mailto"));
    }

    /**
     * Look for an &lt;AttributeConsumeService&gt; and if its there look for an appropriate name.
     * 
     * @return null or an appropriate name
     */
    @Nullable protected String getNameFromAttributeConsumingService() {

        final ACSUIInfo acsInfo = getRPACSUInfo();
        if (null == acsInfo) {
            log.debug("No ACS so no ServiceName");
            return null;
        }

        final Map<Locale, String> serviceNames = acsInfo.getServiceNames();
        Locale l = Locale.lookup(getBrowserLanguages(), serviceNames.keySet());

        if (l == null) {
            l = Locale.lookup(getFallbackLanguages(), serviceNames.keySet());
        }

        if (l != null) {
            log.debug("Found ServiceName '{}' in ACS, locale '{}'", serviceNames.get(l), l);
            return serviceNames.get(l);
        }
        log.debug("No ServiceName in ACS for '{}' or '{}'", getBrowserLanguages(), getFallbackLanguages());
        return null;
    }

    /**
     * If the entityId can look like a host return that, otherwise the entityId in full.
     * 
     * @return either the host or the entityId.
     */
    @Nullable protected String getNameFromEntityId() {

        if (null == getRPEntityDescriptor()) {
            log.trace("No relying party, no Name");
            return null;
        }
        final String spName = getRPEntityDescriptor().getEntityID();

        try {
            final URI entityId = new URI(spName);
            final String scheme = entityId.getScheme();

            if ("http".equals(scheme) || "https".equals(scheme)) {
                log.debug("Found matching scheme, returning name of '{}'", entityId.getHost());
                return entityId.getHost();
            }
            log.debug("Not a usual scheme, returning name of '{}'", spName);

            return spName;
        } catch (final URISyntaxException e) {
            log.debug("Not a URI, returning name of '{}'", spName);
            return spName;
        }
    }

    /**
     * look at &lt;UIInfo&gt; if there and if so look for appropriate description.
     * 
     * @return null or an appropriate description
     */
    @Nullable protected String getDescriptionFromUIInfo() {

        final IdPUIInfo info = getRPUInfo();
        if (info == null) {
            log.warn("GetDescription: No UIInfo");
            return null;
        }

        final Map<Locale, String> descriptions = getRPUInfo().getDescriptions();
        Locale l = Locale.lookup(getBrowserLanguages(), descriptions.keySet());

        if (l == null) {
            l = Locale.lookup(getFallbackLanguages(), descriptions.keySet());
        }

        if (l != null) {
            log.debug("Found Description '{}' in UIInfo, locale '{}'", descriptions.get(l), l);
            return descriptions.get(l);
        }
        log.debug("No Description in UIINFO for '{}' or '{}'", getBrowserLanguages(), getFallbackLanguages());
        return null;
    }

    /**
     * Look for an &lt;AttributeConsumeService&gt; and if it's there look for an appropriate description.
     * 
     * @param lang - which language to look up
     * @return null or an appropriate description
     */
    @Nullable protected String getDescriptionFromAttributeConsumingService(final String lang) {
        final ACSUIInfo acsInfo = getRPACSUInfo();
        if (null == acsInfo) {
            log.debug("No ACS so no ServiceDescription");
            return null;
        }

        final Map<Locale, String> serviceDescriptions = acsInfo.getServiceDescriptions();
        Locale l = Locale.lookup(getBrowserLanguages(), serviceDescriptions.keySet());

        if (l == null) {
            l = Locale.lookup(getFallbackLanguages(), serviceDescriptions.keySet());
        }

        if (l != null) {
            log.debug("Found ServiceDescription '{}' in ACS, locale '{}'", serviceDescriptions.get(l), l);
            return serviceDescriptions.get(l);
        }
        log.debug("No ServiceDescription in ACS for '{}' or '{}'", getBrowserLanguages(), getFallbackLanguages());
        return null;
    }

    /**
     * Get the {@link Organization} from the {@link SPSSODescriptor}, failing that the {@link EntityDescriptor}.
     * 
     * @return the {@link Organization} for the relying party.
     */
    @Nullable protected OrganizationUIInfo getOrganization() {

        Organization organization = null;
        if (null != getRPSPSSODescriptor()) {
            organization = getRPSPSSODescriptor().getOrganization();
        }
        if (organization == null && getRPEntityDescriptor() != null) {
            organization = getRPEntityDescriptor().getOrganization();
        }
        if (organization == null) {
            return null;
        }

        final List<OrganizationUIInfo> infoList = organization.getObjectMetadata().get(OrganizationUIInfo.class);
        if (infoList.isEmpty()) {
           return new OrganizationUIInfo(organization);
        }
        return infoList.get(0);
    }

    /**
     * Parser for the contact type.
     * 
     * @param type in value
     * @return the enumeration type. suitably defaulted.
     */
    protected ContactPersonTypeEnumeration getContactType(@Nullable final String type) {
        final String value = StringSupport.trimOrNull(type);
        if (null == value) {
            log.warn("no parameter provided to contactType");
            return ContactPersonTypeEnumeration.SUPPORT;
        }
        if (type.equals(ContactPersonTypeEnumeration.ADMINISTRATIVE.toString())) {
            return ContactPersonTypeEnumeration.ADMINISTRATIVE;
        } else if (type.equals(ContactPersonTypeEnumeration.BILLING.toString())) {
            return ContactPersonTypeEnumeration.BILLING;
        } else if (type.equals(ContactPersonTypeEnumeration.OTHER.toString())) {
            return ContactPersonTypeEnumeration.OTHER;
        } else if (type.equals(ContactPersonTypeEnumeration.SUPPORT.toString())) {
            return ContactPersonTypeEnumeration.SUPPORT;
        } else if (type.equals(ContactPersonTypeEnumeration.TECHNICAL.toString())) {
            return ContactPersonTypeEnumeration.TECHNICAL;
        } else {
            log.warn("parameter provided to contactType: " + type + " is invalid");
            return ContactPersonTypeEnumeration.SUPPORT;
        }
    }

    /**
     * Lookup the specified type of Contact in the RP metadata.
     * 
     * @param contactType what type to look up.
     * @return the {@link ContactPerson} or null.
     */
    @Nullable public ContactPerson getContactPerson(final ContactPersonTypeEnumeration contactType) {
        if (null == getRPEntityDescriptor()) {
            return null;
        }
        final List<ContactPerson> contacts = getRPEntityDescriptor().getContactPersons();
        if (null == contacts || contacts.isEmpty()) {
            log.trace("No Contacts found at all");
            return null;
        }
        for (final ContactPerson contact : contacts) {
            if (contactType == contact.getType()) {
                return contact;
            }
        }
        log.trace("No matching Contacts found at all");
        return null;
    }

    /**
     * Get the service name.
     * 
     * @return the name or null if there wasn't one 
     */
    @Nullable public String getServiceName() {
        final IdPUIInfo uiinfo = getRPUInfo();
        final ACSUIInfo acsinfo = getRPACSUInfo();
        Locale l = null;
        Map<Locale, String> names = null;
        log.trace("GetServiceName - looking browser Locales '{}', Falllback locales '{}'");
        if (uiinfo != null) {
            log.trace("Looking in UI info for Browser Locales");
            names = uiinfo.getDisplayNames();
            l = Locale.lookup(getBrowserLanguages(), names.keySet());
        }
        if (l == null && acsinfo != null) {
            log.trace("Looking in ACS for Browser Locales");
            names = acsinfo.getServiceNames();
            l = Locale.lookup(getBrowserLanguages(), names.keySet());
        }
        if (l == null && uiinfo != null) {
            log.trace("Looking in UI info for Fallback Locales");
            names = uiinfo.getDisplayNames();
            l = Locale.lookup(getFallbackLanguages(), names.keySet());
        }
        if (l == null && acsinfo != null) {
            log.trace("Looking in ACS for Fallback Locales");
            names = acsinfo.getServiceNames();
            l = Locale.lookup(getFallbackLanguages(), names.keySet());
        }
        if (l != null) {
            final String result = names.get(l);
            log.debug("Found Name '{}' for Locale '{}'", result, l);
            return result;
        }

        log.debug("Nothing found returning name from entity");
        return getNameFromEntityId();
    }

    /**
     * Get the service Description.
     * 
     * @return the description or null if there wasn't one 
     */
    @Nullable public String getServiceDescription() {
        final IdPUIInfo uiinfo = getRPUInfo();
        final ACSUIInfo acsinfo = getRPACSUInfo();
        Locale l = null;
        Map<Locale, String> names = null;
        log.trace("GetServiceDescription - looking browser Locales '{}', Falllback locales '{}'");
        if (uiinfo != null) {
            log.trace("Looking in UI info for Browser Locales");
            names = uiinfo.getDescriptions();
            l = Locale.lookup(getBrowserLanguages(), names.keySet());
        }
        if (l == null && acsinfo != null) {
            log.trace("Looking in ACS for Browser Locales");
            names = acsinfo.getServiceDescriptions();
            l = Locale.lookup(getBrowserLanguages(), names.keySet());
        }
        if (l == null && uiinfo != null) {
            log.trace("Looking in UI info for Fallback Locales");
            names = uiinfo.getDescriptions();
            l = Locale.lookup(getFallbackLanguages(), names.keySet());
        }
        if (l == null && acsinfo != null) {
            log.trace("Looking in ACS for Fallback Locales");
            names = acsinfo.getServiceDescriptions();
            l = Locale.lookup(getFallbackLanguages(), names.keySet());
        }
        if (l != null) {
            final String result = names.get(l);
            log.debug("Found Name '{}' for Locale '{}'", result, l);
            return result;
        }

        log.debug("Nothing found");
        return null;
    }

    /**
     * Look for the &lt;OrganizationDisplayName&gt;.
     * 
     * @return An appropriate string or null
     */
    @Nullable public String getOrganizationDisplayName() {
        final OrganizationUIInfo org = getOrganization();
        if (null == org) {
            log.debug("No Organization, returning null");
            return null;
        }
        return getLocalizeString(org.getOrganizationDisplayNames(), "OrganizationDisplayName");
    }

    /**
     * Look for the &lt;OrganizationName&gt;.
     * 
     * @return An appropriate string or null
     */
    @Nullable public String getOrganizationName() {
        final OrganizationUIInfo org = getOrganization();
        if (null == org) {
            log.debug("No Organization, returning null");
            return null;
        }
        return getLocalizeString(org.getOrganizationNames(), "OrganizationName");
    }

    /**
     * * Look for the &lt;OrganizationURL&gt;.
     * 
     * @return An appropriate string or the null
     */
    public String getOrganizationURL() {
        final OrganizationUIInfo org = getOrganization();
        if (null == org) {
            log.debug("No Organization, returning null");
            return null;
        }
        return policeURLNonLogo(getLocalizeString(org.getOrganizationUrls(), "OrganizationURLs"));
    }

    /**
     * look for the &lt;ContactPerson&gt; and within that the SurName.
     * 
     * @param contactType the type of contact to look for
     * @return An appropriate string or null
     */
    @Nullable public String getContactSurName(@Nullable final String contactType) {

        final ContactPerson contact = getContactPerson(getContactType(contactType));
        if (null == contact || null == contact.getSurName()) {
            return null;
        }
        return contact.getSurName().getValue();
    }

    /**
     * look for the &lt;ContactPerson&gt; and within that the GivenName.
     * 
     * @param contactType the type of contact to look for
     * @return An appropriate string or null
     */
    @Nullable public String getContactGivenName(@Nullable final String contactType) {

        final ContactPerson contact = getContactPerson(getContactType(contactType));
        if (null == contact || null == contact.getGivenName()) {
            return null;
        }
        return contact.getGivenName().getValue();
    }

    /**
     * look for the &lt;ContactPerson&gt; and within that the Email.
     * 
     * @param contactType the type of contact to look for
     * @return An appropriate string or null
     */
    @Nullable public String getContactEmail(@Nullable final String contactType) {

        final ContactPerson contact = getContactPerson(getContactType(contactType));
        if (null == contact || null == contact.getEmailAddresses() || contact.getEmailAddresses().isEmpty()) {
            return null;
        }
        return policeURLNonLogo(contact.getEmailAddresses().get(0).getURI());
    }

    /** Helper function for methods which need localized strings.
     * @param map the map to lookup
     * @param type the name we are looking up (for logging)
     * @return the suitable value, or null
     */
    @Nullable private String getLocalizeString(@Nonnull final Map<Locale, String> map, @Nonnull final String type) {
        if (null == map || map.isEmpty()) {
            log.debug("No {}s returning null", type);
            return null;
        }
        Locale l = Locale.lookup(getBrowserLanguages(), map.keySet());
        if (l == null) {
            log.trace("No {} found from Brower langages '{}' in '{}'",
                    type, getBrowserLanguages(), map.keySet());
           l = Locale.lookup(getFallbackLanguages(), map.keySet());
        }
        if (l == null) {
            log.debug("No relevant {} with language match, returning null", type);
            return null;
        }
        final String result = map.get(l);
        log.debug("Found {} '{}' for '{}'", type, result, l);
        return result;
    }

    /**
     * Get the &lt;mdui:InformationURL&gt;.
     * 
     * @return the value or the default value
     */
    @Nullable public String getInformationURL() {

        if (null == getRPUInfo()) {
            log.debug("No UIInfo returning null");
            return null;
        }
        return policeURLNonLogo(getLocalizeString(getRPUInfo().getInformationURLs(), "InformationURL"));
    }

    /**
     * Get the &lt;mdui:PrivacyStatementURL&gt;.
     * 
     * @return the value or null
     */
    @Nullable public String getPrivacyStatementURL() {
        if (null == getRPUInfo()) {
            log.debug("No UIInfo returning null");
            return null;
        }
        return policeURLNonLogo(getLocalizeString(getRPUInfo().getPrivacyStatementURLs(), "PrivacyStatementURL"));
    }

    /**
     * Does the logo fit the supplied parameters?
     * 
     * @param logo the logo
     * @param minWidth min Width
     * @param minHeight min Height
     * @param maxWidth max Width
     * @param maxHeight max Height
     * @return whether it fits
     */
    private boolean logoFits(final Logo logo, final int minWidth, final int minHeight, final int maxWidth,
            final int maxHeight) {
        final int height;
        if (null == logo.getHeight()) {
            log.warn("No height available for {} assuming a fit", logo.getURI());
            height = maxHeight -1;
        } else {
            height = logo.getHeight();
        }
        final int width;
        if (null == logo.getWidth()) {
            log.warn("No width available for {} assuming a fit", logo.getURI());
            width = maxWidth - 1;
        } else {
            width = logo.getWidth();
        }
        return height <= maxHeight && height >= minHeight && width <= maxWidth && width >= minWidth;
    }


    /**
     * Get the Logo (or null). We apply the languages and the supplied lengths.
     * 
     * @param minWidth the minimum width to allow.
     * @param minHeight the minimum height to allow.
     * @param maxWidth the maximum width to allow.
     * @param maxHeight the maximum height to allow.
     * @return an appropriate logo URL or null.
     */
    // CheckStyle: CyclomaticComplexity OFF
    @Nullable public String getLogo(final int minWidth, final int minHeight, final int maxWidth, final int maxHeight) {

        if (null == getRPUInfo()) {
            log.debug("No UIInfo or logos returning null");
            return null;
        }

        final Map<Locale, List<Logo>> logos = getRPUInfo().getLocaleLogos();

        if (logos != null && !logos.isEmpty()) {
            for (final Locale l: Locale.filter(getBrowserLanguages(), logos.keySet())) {
                for (final Logo logo : logos.get(l)) {
                    log.trace("Found logo in UIInfo, ({} x {}) - {}", logo.getWidth(), logo.getHeight(), l);
                    if (logoFits(logo, minWidth, minHeight, maxWidth, maxHeight)) {
                        final String result = policeURLLogo(logo.getURI());
                        if (result != null) {
                            log.debug("Found locale logo from UIInfo, ({} x {}) : {}",
                                    logo.getWidth(), logo.getHeight(), result);
                            return result;
                        }
                    }
                }
            }
            for (final Locale l: Locale.filter(getFallbackLanguages(), logos.keySet())) {
                for (final Logo logo : logos.get(l)) {
                    log.trace("Found logo in UIInfo, ({} x {}) - {}", logo.getWidth(), logo.getHeight(), l);
                    if (logoFits(logo, minWidth, minHeight, maxWidth, maxHeight)) {
                        final String result = policeURLLogo(logo.getURI());
                        if (result != null) {
                            log.debug("Found locale logo from UIInfo, ({} x {}) : {}",
                                    logo.getWidth(), logo.getHeight(), result);
                            return result;
                        }
                    }
                }
            }
        }
        for (final Logo logo : rpUIInfo.getNonLocaleLogos()) {
            log.trace("Found logo in UIInfo, ({} x {})", logo.getWidth(), logo.getHeight());
            if (!logoFits(logo, minWidth, minHeight, maxWidth, maxHeight)) {
                log.trace("Size Mismatch");
                continue;
            }
            final String result = policeURLLogo(logo.getURI());
            if (result != null) {
                log.debug("Found nonlocale logo from UIInfo, ({} x {}) : {}",
                        logo.getWidth(), logo.getHeight(), logo.getURI());
                return result;
            }
        }
        log.debug("No valid logos which fit found");
        return null;
    }
    // CheckStyle: CyclomaticComplexity ON

    /**
     * Get the Logo (or null). We apply the languages and the supplied lengths.
     * 
     * @param minWidth the minimum width to allow.
     * @param minHeight the minimum height to allow.
     * @param maxWidth the maximum width to allow.
     * @param maxHeight the maximum height to allow.
     * @return an appropriate logo URL or null.
     */
    @Nullable public String getLogo(final String minWidth, final String minHeight, final String maxWidth,
            final String maxHeight) {

        int minW;
        try {
            minW = Integer.parseInt(minWidth);
        } catch (final NumberFormatException ex) {
            minW = Integer.MIN_VALUE;
        }

        int minH;
        try {
            minH = Integer.parseInt(minHeight);
        } catch (final NumberFormatException ex) {
            minH = Integer.MIN_VALUE;
        }

        int maxW;
        try {
            maxW = Integer.parseInt(maxWidth);
        } catch (final NumberFormatException ex) {
            maxW = Integer.MAX_VALUE;
        }

        int maxH;
        try {
            maxH = Integer.parseInt(maxHeight);
        } catch (final NumberFormatException ex) {
            maxH = Integer.MAX_VALUE;
        }

        return getLogo(minW, minH, maxW, maxH);
    }

    /**
     * Get the Logo (or null). We apply the languages. Any size works.
     * 
     * @return an appropriate logo URL or null.
     */
    @Nullable public String getLogo() {
        return getLogo(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }
}
