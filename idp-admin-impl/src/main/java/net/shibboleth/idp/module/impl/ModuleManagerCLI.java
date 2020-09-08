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

package net.shibboleth.idp.module.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.client.HttpClient;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.google.common.base.Strings;

import net.shibboleth.idp.Version;
import net.shibboleth.idp.cli.AbstractIdPHomeAwareCommandLine;
import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.idp.module.IdPModule.ModuleResource;
import net.shibboleth.idp.module.IdPModule.ResourceResult;
import net.shibboleth.idp.module.ModuleContext;
import net.shibboleth.idp.module.ModuleException;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;

/**
 * Command line for {@link IdPModule} management.
 */
public final class ModuleManagerCLI extends AbstractIdPHomeAwareCommandLine<ModuleManagerArguments> {

    /** Class logger. */
    @Nullable private Logger log;
    
    /** The injected HttpClient. */
    @Nullable private HttpClient httpClient;
    
    /** Injected security parameters. */
    @Nullable private HttpClientSecurityParameters httpClientSecurityParameters;

    /** {@inheritDoc} */
    @Override
    @Nonnull protected Logger getLogger() {
        if (log == null) {
            log = LoggerFactory.getLogger(ModuleManagerCLI.class);
        }
        return log;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected Class<ModuleManagerArguments> getArgumentClass() {
        return ModuleManagerArguments.class;
    }

    /** {@inheritDoc} */
    @Override
    protected String getVersion() {
        return Version.getVersion();
    }
    
    /** {@inheritDoc} */
    @Override
    @Nonnull @NonnullElements @NotLive @Unmodifiable protected List<Resource> getAdditionalSpringResources() {
        return List.of(new ClassPathResource("net/shibboleth/idp/conf/http-client.xml"));
    }
    
    /** {@inheritDoc} */
    @Override
    protected int doRun(@Nonnull final ModuleManagerArguments args) {
        final int ret = super.doRun(args);
        if (ret != RC_OK) {
            return ret;
        }

        final String clientName = args.getHttpClientName() != null ? args.getHttpClientName() :
            "shibboleth.InternalHttpClient";
        try {
            httpClient = getApplicationContext().getBean(clientName, HttpClient.class);
        } catch (final NoSuchBeanDefinitionException e) {
            log.error("Could not locate HttpClient '{}'", clientName);
            return RC_IO;
        }
        
        if (args.getHttpClientSecurityParameterstName() != null) {
            try {
                httpClientSecurityParameters =
                        getApplicationContext().getBean(args.getHttpClientSecurityParameterstName(),
                                HttpClientSecurityParameters.class);
            } catch (final NoSuchBeanDefinitionException e) {
                log.error("Could not locate HttpClientSecurityParameters '{}'",
                        args.getHttpClientSecurityParameterstName());
                return RC_IO;
            }
        }

        try {
            final ModuleContext moduleContext =
                    new ModuleContext(getApplicationContext().getEnvironment().getProperty("idp.home"));
            moduleContext.setHttpClient(httpClient);
            moduleContext.setHttpClientSecurityParameters(httpClientSecurityParameters);
            
            if (args.getList() || args.getFullList()) {
                doList(moduleContext, args.getFullList());
            } else {
                doManage(moduleContext, args);
            }
        } catch (final ModuleException e) {
            System.out.println(ANSIColors.ANSI_RED + "[FAILED]" + ANSIColors.ANSI_RESET);
            System.out.println();
            return RC_INIT;
        }
        return ret;
    }

    /**
     * List all modules.
     * 
     * @param moduleContext context
     * @param full whether to do a long list
     */
    private void doList(@Nonnull final ModuleContext moduleContext, final boolean full) {
        
        for (final IdPModule module : ServiceLoader.load(IdPModule.class)) {
            if (full) {
                System.out.println();
                System.out.println("Module: " + module.getId());
                System.out.println("\tName: " + module.getName());
                System.out.println("\tDesc: " + module.getDescription());
                System.out.println("\tHelp: " + module.getURL());
                if (module.isEnabled(moduleContext)) {
                    System.out.println("\tStatus: " + ANSIColors.ANSI_GREEN + "ENABLED" + ANSIColors.ANSI_RESET);
                } else {
                    System.out.println("\tStatus: " + ANSIColors.ANSI_RED + "DISABLED" + ANSIColors.ANSI_RESET);
                }
                final Collection<ModuleResource> resources = module.getResources();
                resources.forEach(r -> {
                    System.out.println("\tResource: (" + (r.isReplace() ? "  replace" : "noreplace") + ") " +
                            r.getDestination());
                });
            } else {
                if (module.isEnabled(moduleContext)) {
                    System.out.println("Module: " + module.getId() +
                            ANSIColors.ANSI_GREEN + " [ENABLED]" + ANSIColors.ANSI_RESET);
                } else {
                    System.out.println("Module: " + module.getId() +
                            ANSIColors.ANSI_RED + " [DISABLED]" + ANSIColors.ANSI_RESET);
                }
            }
            System.out.println();
        }
    }

    /**
     * Manage modules as directed.
     * 
     * @param moduleContext context
     * @param args arguments
     * 
     * @throws ModuleException to report module errors
     */
    private void doManage(@Nonnull final ModuleContext moduleContext, @Nonnull final ModuleManagerArguments args)
            throws ModuleException {
        for (final IdPModule module : ServiceLoader.load(IdPModule.class)) {
            
            final boolean enable;
            if (args.getEnableModuleIds().contains(module.getId())) {
                enable = true;
            } else if (args.getDisableModuleIds().contains(module.getId())) {
                enable = false;
            } else {
                continue;
            }
            
            try (final ByteArrayOutputStream sink = new ByteArrayOutputStream()) {
                System.out.println((enable ? "Enabling " : "Disabling ") + module.getId() + "...");
                moduleContext.setMessageStream(new PrintStream(sink));
                
                final Map<ModuleResource,ResourceResult> results = enable ? module.enable(moduleContext) :
                    module.disable(moduleContext, args.getClean());
                results.forEach(this::doReportOperation);
                
                System.out.println(ANSIColors.ANSI_GREEN + "[OK]" + ANSIColors.ANSI_RESET);
                System.out.println();
                
                final String msg = sink.toString(Charset.forName("UTF-8"));
                moduleContext.setMessageStream(null);
                if (!Strings.isNullOrEmpty(msg)) {
                    System.out.println(msg);
                    System.out.println();
                }
                
            } catch (final IOException e) {
                getLogger().error("I/O Error", e);
            }
        }
    }
    
    /**
     * Report on a resource result.
     * 
     * @param resource resource
     * @param result result of operation
     */
    private void doReportOperation(@Nonnull final ModuleResource resource, @Nonnull final ResourceResult result) {
        System.out.print("\t" + resource.getDestination());
        switch (result) {
            case CREATED:
                System.out.println(" created");
                break;
                
            case REPLACED:
                System.out.println(" replaced, " + resource.getDestination() + ".idpsave created");
                break;
                
            case ADDED:
                System.out.println(".idpnew created");
                break;
                
            case REMOVED:
                System.out.println(" removed");
                break;
                
            case SAVED:
                System.out.println(" renamed to " + resource.getDestination() + ".idpsave");
                break;
                
            case MISSING:
                System.out.println(" missing, nothing to do");
                break;
                
            default:
        }
    }
    

    /**
     * CLI entry point.
     * 
     * @param args arguments
     */
    public static void main(@Nonnull final String[] args) {
        System.exit(new ModuleManagerCLI().run(args));
    }

}