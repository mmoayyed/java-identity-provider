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

package net.shibboleth.idp.authn.spnego.impl;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.AbstractInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.net.CookieManager;
import net.shibboleth.shared.primitive.StringSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Component managing the auto-login state via cookie.
 */
public class SPNEGOAutoLoginManager extends AbstractInitializableComponent {

    /** Name of the SPNEGO auto-login signaling parameter. */
    @Nonnull @NotEmpty public static final String AUTOLOGIN_PARAMETER_NAME = "_shib_idp_SPNEGO_enable_autologin";

    /** Default name of the SPNEGO auto-login cookie. */
    @Nonnull @NotEmpty public static final String AUTOLOGIN_COOKIE_NAME = "_idp_spnego_autologin";

    /** SPNEGO auto-login cookie value representing true. */
    @Nonnull @NotEmpty public static final String AUTOLOGIN_COOKIE_VALUE_TRUE = "1";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SPNEGOAutoLoginManager.class);

    /** Manages creation of cookies. */
    @NonnullAfterInit private CookieManager cookieManager;
    
    /** Auto-login cookie name. */
    @Nonnull @NotEmpty private String cookieName;

    /** Constructor. */
    public SPNEGOAutoLoginManager() {
        cookieName = AUTOLOGIN_COOKIE_NAME;
    }
    
    /**
     * Set the {@link CookieManager} to use.
     * 
     * @param manager the CookieManager to use.
     */
    public void setCookieManager(@Nonnull final CookieManager manager) {
        checkSetterPreconditions();
        cookieManager = Constraint.isNotNull(manager, "CookieManager cannot be null");
    }

    /**
     * Get the {@link CookieManager}.
     * 
     * @return the CookieManager.
     */
    @NonnullAfterInit public CookieManager getCookieManager() {
        return cookieManager;
    }

    /**
     * Set the auto-login cookie name.
     * 
     * @param name cookie name
     * 
     * @since 4.2.0
     */
    public void setCookieName(@Nonnull @NotEmpty final String name) {
        checkSetterPreconditions();
        cookieName = Constraint.isNotEmpty(StringSupport.trimOrNull(name), "Cookie name cannot be null or empty");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
    
        if (cookieManager == null) {
            throw new ComponentInitializationException("CookieManager cannot be null");
        }
    }
    

    /**
     * Enable auto-login, i.e. set cookie to 'true'.
     */
    public void enable() {
        cookieManager.addCookie(cookieName, AUTOLOGIN_COOKIE_VALUE_TRUE);
        log.debug("Auto-login has been enabled.");
    }

    /**
     * Disable auto-login. i.e. unset cookie.
     */
    public void disable() {
        cookieManager.unsetCookie(cookieName);
        log.debug("Auto-login has been disabled.");
    }

    /**
     * Checks whether auto-login is enabled.
     * 
     * @return true if auto-login is enabled.
     */
    public boolean isEnabled() {
        return cookieManager.cookieHasValue(cookieName, AUTOLOGIN_COOKIE_VALUE_TRUE);
    }

    /**
     * Checks whether auto-login is disabled.
     * 
     * @return true if auto-login is disabled.
     */
    public boolean isDisabled() {
        /*
         * auto-login is considered disabled if cookie is absent or value is anything except "true".
         */
        final String value = getCookieManager().getCookieValue(cookieName, null);
        return value == null || !value.equals(AUTOLOGIN_COOKIE_VALUE_TRUE);
    }

}