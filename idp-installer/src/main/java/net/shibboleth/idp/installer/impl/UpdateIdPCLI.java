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

package net.shibboleth.idp.installer.impl;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.SystemUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.opensaml.security.httpclient.HttpClientSecurityContextHandler;
import org.slf4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import net.shibboleth.idp.Version;
import net.shibboleth.idp.admin.impl.IdPInfo;
import net.shibboleth.idp.cli.AbstractIdPHomeAwareCommandLine;
import net.shibboleth.idp.installer.InstallerSupport;
import net.shibboleth.idp.installer.impl.UpdateIdPArguments.OperationType;
import net.shibboleth.idp.installer.plugin.impl.TrustStore;
import net.shibboleth.idp.installer.plugin.impl.TrustStore.Signature;
import net.shibboleth.profile.installablecomponent.InstallableComponentInfo;
import net.shibboleth.profile.installablecomponent.InstallableComponentSupport;
import net.shibboleth.profile.installablecomponent.InstallableComponentVersion;
import net.shibboleth.profile.installablecomponent.InstallableComponentInfo.VersionInfo;
import net.shibboleth.profile.installablecomponent.InstallableComponentSupport.SupportLevel;
import net.shibboleth.shared.cli.AbstractCommandLine;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.spring.httpclient.resource.HTTPResource;

/**
 * Command line update cheker.
 */
public class UpdateIdPCLI extends AbstractIdPHomeAwareCommandLine<UpdateIdPArguments> {

    /** The place we publish our keys. */
    @Nonnull public static String SHIBBOLETH_SIGNING_KEYS = "http://shibboleth.net/downloads/PGP_KEYS";

    /** Logger. */
    @Nullable private Logger log;
    
    /** {@inheritDoc} */
    @Override
    @Nonnull protected Class<UpdateIdPArguments> getArgumentClass() {
        return UpdateIdPArguments.class;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected String getVersion() {
        final String result = Version.getVersion();
        assert result != null;
        return result;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected Logger getLogger() {
        Logger localLog = log;
        if (localLog == null) {
            localLog = log = LoggerFactory.getLogger(UpdateIdPCLI.class);
        }
        return localLog;
    }

    /** {@inheritDoc} */
    @Nonnull protected List<Resource> getAdditionalSpringResources() {
        return CollectionSupport.singletonList(
               new ClassPathResource("net/shibboleth/idp/conf/http-client.xml"));
    }

    /** {@inheritDoc} */
    protected int doRun(@Nonnull final UpdateIdPArguments args) {
        
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        if (args.getHttpClientName() == null) {
            args.setHttpClientName("shibboleth.InternalHttpClient");
        }
        super.doRun(args);
        if (getHttpClient() == null) {
            getLogger().error("Could not not locate http client {}", args.getHttpClientName());
            return RC_INIT;
        }

        final List<String> urlStrings = args.getUpdateURLs().isEmpty() ?
                CollectionSupport.listOf(
                        "https://shibboleth.net/downloads/identity-provider/plugins/idp-versions.properties",
                        "http://plugins.shibboleth.net/idp-versions.properties") :
                args.getUpdateURLs();
        final List<URL> urls = new ArrayList<>(urlStrings.size());
        for (final String s:urlStrings) {
            try {
                urls.add(new URL(s));
            } catch (MalformedURLException e) {
                getLogger().error("Could not convert {} to a URL", s);
                return RC_IO;
            }
        }
        final HttpClient client = getHttpClient();
        assert client != null;
        final Properties properties = InstallableComponentSupport.loadInfo(urls, client, getHttpClientSecurityParameters());
        if (properties == null) {
            return RC_IO;
        }
        final InstallableComponentInfo info = new IdPInfo(properties);
        
        final OperationType operation = args.getOperation();
        if (operation == OperationType.LIST) {
            return list(args, info);
        } else {
            return checkUpdate(args, info, operation == OperationType.DOWLOAD);
        }
    }
    
    /** Check for a potential upgrade, then download if that was requested
     * @param args The command line
     * @param info information about the IdP update states, digested from "plugin.properties"
     * @param doDownload whether to download the distribution 
     * @return a "return status"
     */
    private int checkUpdate(@Nonnull UpdateIdPArguments args, @Nonnull final InstallableComponentInfo info, boolean doDownload) {
        
        final InstallableComponentVersion from = args.getUpdateFromVersion();
        final VersionInfo currInfo = info.getAvailableVersions().get(from);
        if (currInfo == null) {
            getLogger().warn("Could not locate version info for version {}", from);
        } else {
           final SupportLevel sl = currInfo.getSupportLevel();
           switch (sl) {
               case Current:
                   getLogger().info("Version {} is current");
                   break;
               case Secadv:
                   getLogger().error("Version {} has known security vulnerabilities", from);
                   break;
               default:
                   getLogger().warn("Support level for {} is {}", from, sl);
                   break;
           }
        }

        InstallableComponentVersion newIdPVersion = args.getUpdateToVersion();
        boolean versionSpecified = newIdPVersion != null;
        if (!versionSpecified) {
            newIdPVersion = InstallableComponentSupport.getBestVersion(from, from, info);
        }
        if (newIdPVersion == null) {
            getLogger().info("No Upgrade available from {}", from);
            return RC_OK;
        }
        if (versionSpecified) {
            getLogger().info("Download version {}", newIdPVersion);
        } else {
            getLogger().info("Version {} can be upgraded to {}", from, newIdPVersion);
        }
        if (!doDownload) {
            return RC_OK;
        }
        final InstallableComponentInfo.VersionInfo verInfo = info.getAvailableVersions().get(newIdPVersion);
        assert verInfo != null;
        return download(args, newIdPVersion, info);
    }
    
    /** List all available versions.
     * @param args The command line
     * @param info information about the IdP update states, digested from "plugin.properties"
     * @return a "return status"
     */
    private int list(@Nonnull final UpdateIdPArguments args, @Nonnull final InstallableComponentInfo info) {
        
        final Map<InstallableComponentVersion, InstallableComponentInfo.VersionInfo> versionMap = info.getAvailableVersions();
        final List<InstallableComponentVersion> versionList = new ArrayList<>(versionMap.keySet());
        versionList.sort(null);
        final InstallableComponentVersion us = args.getUpdateFromVersion();
        for (final InstallableComponentVersion ver:versionList) {
            final InstallableComponentInfo.VersionInfo inf = versionMap.get(ver);
            getLogger().info("Version {}{} Supported Status: {}, Upgrade Candidate: {}", ver, ver.equals(us) ? " (current);" : ";",
                    inf.getSupportLevel(),
                    info.isSupportedWithIdPVersion(ver, us)?"yes": "no"); 
        }
        
        return RC_OK;
    }

    /** Download the provided or inferred version
     * @param args the command line
     * @param version the idp version to download
     * @param info version about all IdP release
     * @return a "return status"
     */
    private int download(@Nonnull final UpdateIdPArguments args,
            @Nonnull final InstallableComponentVersion version, @Nonnull final InstallableComponentInfo info) {

        final String baseName = info.getUpdateBaseName(version);
        if (baseName == null) {
            getLogger().error("Could not get file name for idp update version {}", version);
            return RC_IO;
        }            
        final String fileName = baseName + (SystemUtils.IS_OS_WINDOWS? ".zip" : ".tgz");

        final URL baseUrl = info.getUpdateURL(version);
        if (baseUrl == null) {
            getLogger().error("Could not get base URL for idp update version {}", version);
            return RC_IO;
        }
        
        getLogger().info("Downloading version {} to {}  from {}/{}", version, args.getDownloadLocation(), baseUrl, fileName);
        try {
            final HttpClient client = getHttpClient();
            assert client != null;
            final HTTPResource baseResource = new HTTPResource(client, baseUrl); 

            final HttpClientSecurityContextHandler handler = new HttpClientSecurityContextHandler();
            handler.setHttpClientSecurityParameters(getHttpClientSecurityParameters());
            handler.initialize();
            baseResource.setHttpClientContextHandler(handler);

            InstallerSupport.download(baseResource, handler, args.getDownloadLocation(), fileName + ".asc");
            InstallerSupport.download(baseResource, handler, args.getDownloadLocation(), fileName);

        } catch (final IOException | ComponentInitializationException e) {
            getLogger().error("Could not download idp version {} from {}", version, baseUrl, e);
            return RC_IO;
        }
        getLogger().debug("Checking signature");
        int result = checkSignature(args, fileName);
        if (result != RC_OK) {
            getLogger().info("Deleting downloaded files");
            try {
                Files.delete(args.getDownloadLocation().resolve(fileName));
                Files.delete(args.getDownloadLocation().resolve(fileName + ".asc"));
            } catch (IOException e) {
                getLogger().error("Could not delete {}[.asc]", fileName, e);
                args.getDownloadLocation().resolve(fileName).toFile().deleteOnExit();
                args.getDownloadLocation().resolve(fileName + ".asc").toFile().deleteOnExit();
            }
        } else {
            getLogger().info("Signature checked OK");
        }
        return result;
    }

    /** Check the signature of the downloaded distribution.
     * @param args the command line
     * @param fileName the name.
     * @return "status" from the operation
     */
    private int checkSignature(@Nonnull final UpdateIdPArguments args, @Nonnull final String fileName) {
        try (final InputStream sigStream = new BufferedInputStream(
                new FileInputStream(args.getDownloadLocation().resolve(fileName + ".asc").toFile()))) {
            final TrustStore trust = new TrustStore();
            final Path idpHome = Path.of(args.getIdPHome());
            assert idpHome != null;
            trust.setIdpHome(idpHome);
            trust.setTrustStore(args.getTruststore());
            trust.setPluginId(IdPInfo.IDP_PLUGIN_ID);
            trust.initialize();
            final Signature sig = TrustStore.signatureOf(sigStream);
            if (!trust.contains(sig)) {
                getLogger().info("TrustStore does not contain signature {}", sig);
                getLogger().info("Downloading {}", SHIBBOLETH_SIGNING_KEYS);
                
                final HttpClient client = getHttpClient();
                assert client != null;
                final HTTPResource baseResource = new HTTPResource(client, SHIBBOLETH_SIGNING_KEYS); 

                final HttpClientSecurityContextHandler handler = new HttpClientSecurityContextHandler();
                handler.setHttpClientSecurityParameters(getHttpClientSecurityParameters());
                handler.initialize();
                baseResource.setHttpClientContextHandler(handler);
                
                try (final InputStream keysStream = new BufferedInputStream(baseResource.getInputStream())) {
                    trust.importKeyFromStream(sig, keysStream, new InstallerSupport.InstallerQuery("Accept this key"));
                }
                if (!trust.contains(sig)) {
                    getLogger().info("Key not added to Trust Store");
                    return RC_IO;
                }
            }

            try (final InputStream distroStream = new BufferedInputStream(
                new FileInputStream(args.getDownloadLocation().resolve(fileName).toFile()))) {
                if (!trust.checkSignature(distroStream, sig)) {
                    getLogger().info("Signature checked for {} failed", fileName);
                    return RC_IO;                }
            }

        } catch (final ComponentInitializationException | IOException e) {
            getLogger().error("Could not manage truststore for [{}, {}] ", args.getIdPHome(), IdPInfo.IDP_PLUGIN_ID, e);
            return RC_IO;
        }
        return RC_OK;
    }


    /** Shim for CLI entry point: Allows the code to be run from a test.
    *
    * @return one of the predefines {@link AbstractCommandLine#RC_INIT},
    * {@link AbstractCommandLine#RC_IO}, {@link AbstractCommandLine#RC_OK}
    * or {@link AbstractCommandLine#RC_UNKNOWN}
    *
    * @param args arguments
    */
   public static int runMain(@Nonnull final String[] args) {
       final UpdateIdPCLI cli = new UpdateIdPCLI();

       return cli.run(args);
   }

   /**
    * CLI entry point.
    * @param args arguments
    */
   public static void main(@Nonnull final String[] args) {
       System.exit(runMain(args));
   }

}