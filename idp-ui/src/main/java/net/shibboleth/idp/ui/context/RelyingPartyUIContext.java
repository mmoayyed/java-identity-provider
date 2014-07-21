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
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.saml.ext.saml2mdui.Description;
import org.opensaml.saml.ext.saml2mdui.DisplayName;
import org.opensaml.saml.ext.saml2mdui.UIInfo;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml.saml2.metadata.ServiceDescription;
import org.opensaml.saml.saml2.metadata.ServiceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The context which carries the {@link RelyingPartyUIInformation}.
 */
public class RelyingPartyUIContext extends BaseContext {

    /** The log. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(RelyingPartyUIContext.class);

    /** The appropriate {@link EntityDescriptor}. */
    @Nullable private EntityDescriptor rpEntityDescriptor;

    /** The appropriate {@link SPSSODescriptor}. */
    @Nullable private SPSSODescriptor rpSPSSODescriptor;

    /** The appropriate {@link AttributeConsumingService}. */
    @Nullable private AttributeConsumingService rpAttributeConsumingService;

    /** The appropriate {@link UIInfo}. */
    @Nullable private UIInfo rpUIInfo;

    /** The languages that this browser wants to know about. */
    @Nonnull private List<String> browserLanguages;

    /**
     * Get the {@link EntityDescriptor}.
     * 
     * @return Returns the entity.
     */
    @Nullable public EntityDescriptor getRPEntityDescriptor() {
        return rpEntityDescriptor;
    }

    /**
     * Set the {@link EntityDescriptor}.
     * 
     * @param what what to set.
     */
    public void setRPEntityDescriptor(@Nonnull EntityDescriptor what) {
        rpEntityDescriptor = Constraint.isNotNull(what, "Injected RP EntityDescriptor cannot be null");
    }

    /**
     * Get the {@link SPSSODescriptor}.
     * 
     * @return Returns the SPSSODescriptor.
     */
    @Nullable public SPSSODescriptor getRPSPSSODescriptor() {
        return rpSPSSODescriptor;
    }

    /**
     * Set the {@link SPSSODescriptor}.
     * 
     * @param what what to set.
     */
    public void setRPSPSSODescriptor(@Nonnull SPSSODescriptor what) {
        rpSPSSODescriptor = Constraint.isNotNull(what, "Injected RP EntityDescriptor cannot be null");
    }

    /**
     * Get the {@link AttributeConsumingService} for the request.
     * 
     * @return Returns the SPSSODescriptor.
     */
    @Nullable public AttributeConsumingService getRPAttributeConsumingService() {
        return rpAttributeConsumingService;
    }

    /**
     * Get the RP {@link UIInfo} associated with the request.
     * 
     * @return the value or null if there is none.
     */
    @Nullable public UIInfo getRPUInfo() {
        return rpUIInfo;
    }

    /**
     * Set the RP {@link UIInfo} associated with the request.
     * 
     * @param what the value to set.
     */
    public void getRPUInfo(@Nullable UIInfo what) {
        rpUIInfo = what;
    }

    /**
     * Set the {@link AttributeConsumingService} for the request.
     * 
     * @param what what to set.
     */
    public void setRPAttributeConsumingService(@Nonnull AttributeConsumingService what) {
        rpAttributeConsumingService =
                Constraint.isNotNull(what, "Injected RP AttributeConsumingService cannot be null");
    }

    /**
     * Set the browser languages.
     * 
     * @param languages the languages to set.
     */
    public void setBrowserLanguages(@Nonnull List<String> languages) {
        browserLanguages = Constraint.isNotNull(languages, "Language List mustr be non null");
    }

    /**
     * Get the browser languages.
     * 
     * @return the languages.
     */
    @Nonnull public List<String> getBrowserLanguages() {
        return browserLanguages;
    }

    /**
     * look at &lt;UIinfo&gt;; if there and if so look for appropriate name.
     * 
     * @param lang - which language to look up
     * @return null or an appropriate name
     */
    @Nullable protected String getNameFromUIInfo(String lang) {

        if (getRPUInfo() != null) {
            for (final DisplayName name : getRPUInfo().getDisplayNames()) {
                if (log.isDebugEnabled()) {
                    log.trace("Found name in UIInfo, language=" + name.getXMLLang());
                }
                if (name.getXMLLang().equals(lang)) {
                    if (log.isDebugEnabled()) {
                        log.trace("returning name from UIInfo " + name.getValue());
                    }
                    return name.getValue();
                }
            }
            if (log.isDebugEnabled()) {
                log.trace("No name in MDUI for " + lang);
            }
        }
        return null;
    }

    /**
     * Look for an &lt;AttributeConsumeService&gt; and if its there look for an appropriate name.
     * 
     * @param lang - which language to look up
     * @return null or an appropriate name
     */
    @Nullable protected String getNameFromAttributeConsumingService(final String lang) {

        if (null == getRPAttributeConsumingService()) {
            return null;
        }

        for (ServiceName name : getRPAttributeConsumingService().getNames()) {
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
        return null;
    }

    /**
     * If the entityId can look like a host return that, otherwise the entityId in full.
     * 
     * @return either the host or the entityId.
     */
    @Nullable protected String getNameFromEntityId() {

        if (null == getRPEntityDescriptor()) {
            log.debug("No relying party, nothing to display");
            return null;
        }
        final String spName = getRPEntityDescriptor().getEntityID();

        try {
            final URI entityId = new URI(spName);
            final String scheme = entityId.getScheme();

            if ("http".equals(scheme) || "https".equals(scheme)) {
                log.trace("found matching schema, returning name of {}", entityId.getHost());
                return entityId.getHost();
            }
            log.trace("Not a standard schema returning name of {}", spName);

            return spName;
        } catch (URISyntaxException e) {
            log.trace("Not a URI returning name of {}", spName);
            return spName;
        }
    }

    /**
     * look at &lt;UIInfo&gt; if there and if so look for appropriate description.
     * 
     * @param lang - which language to look up
     * @return null or an appropriate description
     */
    @Nullable protected String getDescriptionFromUIInfo(String lang) {
        if (getRPUInfo() == null || getRPUInfo().getDescriptions() == null) {
            log.trace("No UIInfo");
            return null;
        }
        for (final Description desc : getRPUInfo().getDescriptions()) {
            if (log.isDebugEnabled()) {
                log.debug("Found description in UIInfo, language=" + desc.getXMLLang());
            }
            if (desc.getXMLLang().equals(lang)) {
                //
                // Found it
                //
                if (log.isDebugEnabled()) {
                    log.debug("returning description from UIInfo " + desc.getValue());
                }
                return desc.getValue();
            }
        }
        log.trace("No matching description in UIInfo");
        return null;
    }

    /**
     * look for an &ltAttributeConsumeService&gt and if its there look for an appropriate description.
     * 
     * @param lang - which language to look up
     * @return null or an appropriate description
     */
    @Nullable private String getDescriptionFromAttributeConsumingService(String lang) {
        if (getRPAttributeConsumingService() == null) {
            log.trace("No ACS found");
            return null;
        }
        for (final ServiceDescription desc : getRPAttributeConsumingService().getDescriptions()) {
            log.trace("Found name in AttributeConsumingService, language=" + desc.getXMLLang());
            if (desc.getXMLLang().equals(lang)) {
                log.trace("returning name from AttributeConsumingService " + desc.getValue());
                return desc.getValue();
            }
        }
        log.trace("No description in AttributeConsumingService");

        return null;
    }

    /**
     * Get the service name.
     * 
     * @param defaultValue what to provide if this is an anonymous lookup
     * @return the name
     */
    public String getServiceName(String defaultValue) {
        String result;

        if (getRPEntityDescriptor() == null) {
            log.debug("No relying party, nothing to display");
            return defaultValue;
        }

        for (final String lang : getBrowserLanguages()) {
            result = getNameFromUIInfo(lang);
            if (result != null) {
                return result;
            }

            result = getNameFromAttributeConsumingService(lang);
            if (result != null) {
                return result;
            }
        }
        // failing that just look at the entity name
        return getNameFromEntityId();
    }

}
