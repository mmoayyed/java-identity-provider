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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

        try {
            final ModuleContext moduleContext =
                    new ModuleContext(getApplicationContext().getEnvironment().getProperty("idp.home"));
            moduleContext.setHttpClient(getHttpClient());
            moduleContext.setHttpClientSecurityParameters(getHttpClientSecurityParameters());
            moduleContext.setLanguageRanges(args.getLanguageRanges());
            
            if (args.getList() || !args.getInfoModuleIds().isEmpty()) {
                return doList(moduleContext, args);
            }
            
            return doManage(moduleContext, args);
        } catch (final ModuleException e) {
            System.out.println(e.getMessage());
            System.out.println(ANSIColors.ANSI_RED + "[FAILED]" + ANSIColors.ANSI_RESET);
            System.out.println();
            return RC_INIT;
        }
    }

    /**
     * List/report on modules.
     * 
     * @param moduleContext context
     * @param args arguments
     * 
     * @return return code
     */
    private int doList(@Nonnull final ModuleContext moduleContext, @Nonnull final ModuleManagerArguments args) {
        
        int ret = RC_OK;
        
        final Iterator<IdPModule> modules = ServiceLoader.load(IdPModule.class).iterator();
        
        while (modules.hasNext()) {
            try {
                final IdPModule module = modules.next();
                
                if (args.getTestModuleIds().contains(module.getId())) {
                    if (!module.isEnabled(moduleContext)) {
                        ret = RC_UNKNOWN;
                    }
                }
                
                if (args.getInfoModuleIds().contains(module.getId())) {
                    System.out.println();
                    System.out.println("Module: " + module.getId());
                    System.out.println("\tName: " + module.getName(moduleContext));
                    System.out.println("\tDesc: " + module.getDescription(moduleContext));
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
                    System.out.println();
                } else if (args.getInfoModuleIds().isEmpty()) {
                    if (module.isEnabled(moduleContext)) {
                        System.out.println("Module: " + module.getId() +
                                ANSIColors.ANSI_GREEN + " [ENABLED]" + ANSIColors.ANSI_RESET);
                    } else {
                        System.out.println("Module: " + module.getId() +
                                ANSIColors.ANSI_RED + " [DISABLED]" + ANSIColors.ANSI_RESET);
                    }
                }
            } catch (final ServiceConfigurationError e) {
                System.out.println("ServiceConfigurationError: " + e.getMessage());
            }
        }
        
        return ret;
    }

    /**
     * Manage modules as directed.
     * 
     * @param moduleContext context
     * @param args arguments
     * 
     * @return return code
     * 
     * @throws ModuleException to report module errors
     */
    private int doManage(@Nonnull final ModuleContext moduleContext, @Nonnull final ModuleManagerArguments args)
            throws ModuleException {
        
        int ret = RC_OK;
        final Iterator<IdPModule> modules = ServiceLoader.load(IdPModule.class).iterator();
        
        while (modules.hasNext()) {
            try {
                final IdPModule module = modules.next();
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
                    ret = RC_IO;
                }
            } catch (final ServiceConfigurationError e) {
                System.out.println("ServiceConfigurationError: " + e.getMessage());
                ret = RC_UNKNOWN;
            }
        }
        
        return ret;
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