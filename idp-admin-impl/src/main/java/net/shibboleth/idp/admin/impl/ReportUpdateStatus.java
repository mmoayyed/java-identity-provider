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

package net.shibboleth.idp.admin.impl;

import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hc.client5.http.classic.HttpClient;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.slf4j.Logger;

import net.shibboleth.idp.Version;
import net.shibboleth.profile.installablecomponent.InstallableComponentInfo;
import net.shibboleth.profile.installablecomponent.InstallableComponentInfo.VersionInfo;
import net.shibboleth.profile.installablecomponent.InstallableComponentSupport;
import net.shibboleth.profile.installablecomponent.InstallableComponentSupport.SupportLevel;
import net.shibboleth.profile.installablecomponent.InstallableComponentVersion;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * A class to reach out and find out whether we are up to date.
 */
public class ReportUpdateStatus extends AbstractIdentifiableInitializableComponent implements Runnable {

    /** Logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ReportUpdateStatus.class);

    /** Where to look for update information. */
    @Nonnull private List<URL> updateUrls = CollectionSupport.emptyList(); 

    /** Are we to run? */
    private boolean enabled;

    /** How to reach out. */
    @NonnullAfterInit private HttpClient httpClient;

    /** any security parameters needed. */
    @Nullable private HttpClientSecurityParameters securityParams;

    /** Set where to look.
     * @param urls what to set.
     */
    public void setUpdateUrls(@Nullable final List<URL> urls) {
        if (urls == null || urls.isEmpty()) {
            log.error("Empty URL update list specified");
        } else {
            updateUrls = urls;
        }
    }

    /** Set any {@link HttpClientSecurityParameters}.
     * @param params what to set.
     */
    public void setSecurityParams(@Nullable final HttpClientSecurityParameters params) {
        securityParams = params;
    }

    /** Set the {@link HttpClient} to use.
     * @param client what to set.
     */
    public void setHttpClient(@Nonnull final HttpClient client) {
        httpClient = Constraint.isNotNull(client, "HttpClient cannot be null");
    }

    /** Are we going to do anything?
     * @param on are we enabled
     */
    public void setEnabled(final boolean on) {
        enabled = on;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        if (!enabled) {
            return;
        }
        if (httpClient == null) {
            log.error("Http Client was not set");
            throw new ComponentInitializationException("Http Client was not set");
        }
        if (updateUrls.isEmpty()) {
            log.error("No Update Urls set");
            throw new ComponentInitializationException("No Update Urls set");
        }
        
        final ExecutorService svc =  Executors.newSingleThreadExecutor();
        svc.execute(this);
        svc.shutdown();
    }

    /** {@inheritDoc} 
     * Do the lookup, but in a different thread so as to not slow down startup.
     */
    @Override
    public void run() {
        try {
            final String versionStr = Version.getVersion();
            if (versionStr == null) {
                log.error("Could not find Current IdP Version");
                return;
            }
            @Nonnull final InstallableComponentVersion version = new InstallableComponentVersion(versionStr); 
            assert httpClient!=null;
            final Properties properties = InstallableComponentSupport.loadInfo(updateUrls, httpClient, securityParams);
            if (properties == null) {
                log.error("Could not locate IdP update information");
                return;
            }
            final InstallableComponentInfo info = new IdPInfo(properties);
        
            final InstallableComponentVersion newIdPVersion =
                    InstallableComponentSupport.getBestVersion(version, version, info);
            if (newIdPVersion == null) {
                log.info("No Upgrade available from {}", version);
            } else {
                log.warn("Version {} can be upgraded to {}", version, newIdPVersion);
            }
            final VersionInfo verInfo = info.getAvailableVersions().get(version);
            if (verInfo == null) {
                log.warn("Could not locate version info for version {}", version);
            } else {
               final SupportLevel sl = verInfo.getSupportLevel();
               switch (sl) {
                   case Current:
                       log.debug("Version {} is current");
                       break;
                   case Secadv:
                       log.error("Version {} has secuorty alerts again it.", version);
                       break;
                   default:
                       log.warn("Support level for {} is {}", version, sl);
                       break;
               }
            }
        } catch (final Throwable t) {
            log.error("Check for update status failed unexpectedly", t);
        }
    }
    
}
