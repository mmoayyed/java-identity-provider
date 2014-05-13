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

package net.shibboleth.idp.ui.taglib;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.tagext.BodyTagSupport;

import net.shibboleth.utilities.java.support.codec.HTMLEncoder;
import net.shibboleth.utilities.java.support.collection.LazyList;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.OutboundMessageContextLookup;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.ext.saml2mdui.DisplayName;
import org.opensaml.saml.ext.saml2mdui.UIInfo;
import org.opensaml.saml.saml2.common.Extensions;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Organization;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml.saml2.metadata.ServiceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;

/**
 * Display the serviceName.
 * 
 * This is taken in order 1) From the mdui 2) AttributeConsumeService 3) HostName from the EntityId 4) EntityId.
 */
public class ServiceTagSupport extends BodyTagSupport {

    /** Serial ID. */
    private static final long serialVersionUID = 4405207268569727209L;

    /** Class logger. */
    private static Logger log = LoggerFactory.getLogger(ServiceTagSupport.class);

    /** Strategy function for access to {@link SAMLMetadataContext} for input to resolver. */
    @Nonnull private static Function<ProfileRequestContext, SAMLMetadataContext> metadataContextLookupStrategy =
            Functions.compose(new ChildContextLookup<>(SAMLMetadataContext.class), Functions.compose(
                    new ChildContextLookup<>(SAMLPeerEntityContext.class), new OutboundMessageContextLookup()));

    /** Bean storage. class reference */
    @Nullable private String cssClass;

    /** Bean storage. id reference */
    @Nullable private String cssId;

    /** Bean storage. style reference */
    @Nullable private String cssStyle;

    /**
     * Set the Css class to use.
     * 
     * @param value what to set
     */
    public void setCssClass(@Nullable String value) {
        cssClass = value;
    }

    /**
     * Get the Css class to use.
     * 
     * @param value what to set
     */
    public void setCssId(@Nullable String value) {
        cssId = value;
    }

    /**
     * Set the Css style to use.
     * 
     * @param value what to set
     */
    public void setCssStyle(@Nullable String value) {
        cssStyle = value;
    }

    /**
     * Add the class and Id (if present) to the string under construction.
     * 
     * @param sb the {@link StringBuilder} to add to.
     */
    protected void addClassAndId(@Nonnull StringBuilder sb) {
        if (cssClass != null) {
            sb.append(" class=\"").append(cssClass).append('"');
        }
        if (cssId != null) {
            sb.append(" id=\"").append(cssId).append('"');
        }
        if (cssStyle != null) {
            sb.append(" style=\"").append(cssStyle).append('"');
        }
    }

    /**
     * Build a hyperlink from the parameters.
     * 
     * @param url the URL
     * @param text what to embed
     * @return the hyperlink.
     */
    @Nonnull protected String buildHyperLink(@Nonnull String url, @Nonnull String text) {
        final String encodedUrl;

        try {
            final URI theUrl = new URI(url);
            final String scheme = theUrl.getScheme();

            if (!"http".equals(scheme) && !"https".equals(scheme) && !"mailto".equals(scheme)) {
                log.warn("The URL " + url + " contained an invalid scheme");
                return "";
            }
            encodedUrl = HTMLEncoder.encodeForHTMLAttribute(url);
        } catch (URISyntaxException e) {
            //
            // It wasn't an URI.
            //
            log.warn("The URL " + url + " was invalid: " + e.toString());
            return "";
        }

        StringBuilder sb = new StringBuilder("<a href=\"");
        sb.append(encodedUrl).append('"');
        addClassAndId(sb);
        sb.append(">").append(HTMLEncoder.encodeForHTML(text)).append("</a>");
        return sb.toString();
    }

    /**
     * Get the {@link EntityDescriptor} of the relying party.
     * 
     * @return the SPs EntityDescriptor
     */
    @Nullable protected EntityDescriptor getSPEntityDescriptor() {

        final HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

        if (request == null) {
            return null;
        }

        final ProfileRequestContext pc = (ProfileRequestContext) request.getAttribute("profileRequestContext");
        final SAMLMetadataContext metadata = metadataContextLookupStrategy.apply(pc);
        if (null == metadata) {
            return null;
        }
        return metadata.getEntityDescriptor();
    }

    /**
     * Traverse the SP's EntityDescriptor and pick out the {@link UIInfo}.
     * 
     * @return the first UIInfo for the SP.
     */
    @Nullable protected UIInfo getSPUIInfo() {
        final EntityDescriptor spEntity = getSPEntityDescriptor();

        if (null == spEntity) {
            //
            // all done
            //
            return null;
        }

        for (final RoleDescriptor role : spEntity.getRoleDescriptors(SPSSODescriptor.DEFAULT_ELEMENT_NAME)) {
            final Extensions exts = role.getExtensions();
            if (exts != null) {
                for (XMLObject object : exts.getOrderedChildren()) {
                    if (object instanceof UIInfo) {
                        return (UIInfo) object;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Traverse the SP's EntityDescriptor and pick out the {@link Organization}.
     * 
     * @return the {@link Organization} for the SP
     */
    @Nullable protected Organization getSPOrganization() {
        final EntityDescriptor spEntity = getSPEntityDescriptor();
        if (null == spEntity) {
            //
            // all done
            //
            return null;
        }

        for (final RoleDescriptor role : spEntity.getRoleDescriptors(SPSSODescriptor.DEFAULT_ELEMENT_NAME)) {
            if (role.getOrganization() != null) {
                return role.getOrganization();
            }
        }

        return spEntity.getOrganization();
    }

    /**
     * Pluck the languages from the browser.
     * 
     * @return the two letter language
     */
    @Nonnull protected List<String> getBrowserLanguages() {
        final HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

        final Enumeration<Locale> locales = request.getLocales();

        final List<String> languages = new LazyList<>();

        while (locales.hasMoreElements()) {
            final Locale locale = locales.nextElement();
            languages.add(locale.getLanguage());
        }
        return languages;
    }

    /**
     * If the entityId can look like a host return that, otherwise the entityId in full.
     * 
     * @return either the host or the entityId.
     */
    @Nullable private String getNameFromEntityId() {
        final EntityDescriptor sp = getSPEntityDescriptor();

        if (null == sp) {
            log.debug("No relying party, nothing to display");
            return null;
        }

        try {
            final URI entityId = new URI(sp.getEntityID());
            final String scheme = entityId.getScheme();

            if ("http".equals(scheme) || "https".equals(scheme)) {
                return entityId.getHost();
            }
        } catch (URISyntaxException e) {
            //
            // It wasn't an URI. return full entityId.
            //
            return sp.getEntityID();
        }
        //
        // not a URL return full entityID
        //
        return sp.getEntityID();
    }

    /**
     * look at &lt;UIinfo&gt;; if there and if so look for appropriate name.
     * 
     * @param lang - which language to look up
     * @return null or an appropriate name
     */
    @Nullable private String getNameFromUIInfo(String lang) {

        if (getSPUIInfo() != null) {
            for (final DisplayName name : getSPUIInfo().getDisplayNames()) {
                if (log.isDebugEnabled()) {
                    log.debug("Found name in UIInfo, language=" + name.getXMLLang());
                }
                if (name.getXMLLang().equals(lang)) {
                    //
                    // Found it
                    //
                    if (log.isDebugEnabled()) {
                        log.debug("returning name from UIInfo " + name.getValue());
                    }
                    return name.getValue();
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("No name in MDUI for " + lang);
            }
        }
        return null;
    }

    /**
     * look for an &ltAttributeConsumeService&gt and if its there look for an appropriate name.
     * 
     * @param lang - which language to look up
     * @return null or an appropriate name
     */
    @Nullable private String getNameFromAttributeConsumingService(final String lang) {
        final EntityDescriptor sp = getSPEntityDescriptor();
        if (null == sp) {
            log.warn("No relying party, nothing to display");
            return null;
        }

        AttributeConsumingService acs = null;

        final List<RoleDescriptor> roles = sp.getRoleDescriptors(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        if (!roles.isEmpty()) {
            SPSSODescriptor spssod = (SPSSODescriptor) roles.get(0);
            acs = spssod.getDefaultAttributeConsumingService();
        }
        if (acs != null) {
            for (ServiceName name : acs.getNames()) {
                if (log.isDebugEnabled()) {
                    log.debug("Found name in AttributeConsumingService, language=" + name.getXMLLang());
                }
                if (name.getXMLLang().equals(lang)) {
                    if (log.isDebugEnabled()) {
                        log.debug("returning name from AttributeConsumingService " + name.getValue());
                    }
                    return name.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Get the identifier for the service name as per the rules above.
     * 
     * @return something sensible for display.
     */
    @Nullable protected String getServiceName() {
        String result = null;
        //
        // First look for MDUI
        //
        if (getSPEntityDescriptor() == null) {
            log.debug("No relying party, nothing to display");
            return null;
        }

        //
        // For each Language
        //
        final List<String> languages = getBrowserLanguages();
        for (final String lang : languages) {
            //
            // Look at <UIInfo>
            //
            result = getNameFromUIInfo(lang);
            if (result != null) {
                return result;
            }

            //
            // Otherwise <AttributeConsumingService>
            //
            result = getNameFromAttributeConsumingService(lang);
            if (result != null) {
                return result;
            }
        }
        //
        // Or look at the entityName
        //
        return getNameFromEntityId();
    }

}