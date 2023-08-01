/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.cli.impl;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.beust.jcommander.Parameter;

import net.shibboleth.idp.cli.AbstractIdPHomeAwareCommandLineArguments;
import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Arguments for {@link IdPModule} management CLI.
 */
public class ModuleManagerArguments extends AbstractIdPHomeAwareCommandLineArguments {

    /** Logger. */
    @Nullable private Logger log;

    /** Brief info about installed modules. */
    @Parameter(names= {"-l", "--list"})
    private boolean list;

    /** Detailed info about installed module(s). */
    @Parameter(names= {"-i", "--info"})
    @Nullable private List<String> infoModuleIds = new ArrayList<>();

    /** Test status of installed module(s). */
    @Parameter(names= {"-t", "--test"})
    @Nullable private List<String> testModuleIds = new ArrayList<>();

    /** ID of module(s) to enable. */
    @Parameter(names= {"-e", "--enable"})
    @Nullable private List<String> enableModuleIds = new ArrayList<>();

    /** ID of module(s) to enable. */
    @Parameter(names= {"-d", "--disable"})
    @Nullable private List<String> disableModuleIds = new ArrayList<>();

    /** Clean when disabling. */
    @Parameter(names= {"-f", "--clean"})
    private boolean clean;

    /** {@inheritDoc} */
    @Nonnull public Logger getLog() {
        if (log == null) {
            log = LoggerFactory.getLogger(ModuleManagerArguments.class);
        }
        assert log!=null;
        return log;
    }

    /**
     * Are we doing a list?
     * 
     * @return whether this is a list operation
     */
    public boolean getList() {
        return list;
    }

    /**
     * Gets the module ID(s) to report on.
     * 
     * @return module ID(s) to report on
     */
    @Nonnull @NotLive @Unmodifiable public Collection<String> getInfoModuleIds() {
        return CollectionSupport.copyToList(StringSupport.normalizeStringCollection(infoModuleIds));
    }

    /**
     * Gets the module ID(s) to test.
     * 
     * @return module ID(s) to test
     */
    @Nonnull @NotLive @Unmodifiable public Collection<String> getTestModuleIds() {
        return CollectionSupport.copyToList(StringSupport.normalizeStringCollection(testModuleIds));
    }

    /**
     * Gets the module ID(s) to enable.
     * 
     * @return module ID(s) to enable
     */
    @Nonnull @NotLive @Unmodifiable public Collection<String> getEnableModuleIds() {
        return CollectionSupport.copyToList(StringSupport.normalizeStringCollection(enableModuleIds));
    }
    
    /**
     * Gets the module ID(s) to disable.
     * 
     * @return module ID(s) to disable
     */
    @Nonnull @NotLive @Unmodifiable public Collection<String> getDisableModuleIds() {
        return CollectionSupport.copyToList(StringSupport.normalizeStringCollection(disableModuleIds));
    }

    /**
     * Are we disabling with the clean option?
     * 
     * @return clean option
     */
    public boolean getClean() {
        return clean;
    }
    
    /** {@inheritDoc} */
    @Override
    public void validate() throws IllegalArgumentException {
        super.validate();

        if (getEnableModuleIds().isEmpty() && getDisableModuleIds().isEmpty()) {
            if (getInfoModuleIds().isEmpty() && getTestModuleIds().isEmpty()) {
                list = true;
            }
        } else if (list || !getInfoModuleIds().isEmpty() || !getTestModuleIds().isEmpty()) {
            getLog().error("Cannot query and enable/disable in the same operation");
            throw new IllegalArgumentException("Cannot query and enable/disable in the same operation.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void printHelp(@Nonnull final PrintStream out) {
        out.println("ModuleManager");
        out.println("Provides a command line interface for IdP Module management operations.");
        out.println();
        out.println("   module [options] [springConfiguration]");
        out.println();
        out.println("      springConfiguration      name of optional Spring configuration resource to use");
        super.printHelp(out);
        out.println();
        out.println(String.format("  %-22s %s", "-l, --list",
                "Brief Information on all installed modules"));
        out.println(String.format("  %-22s %s", "-i, --info <id>[,<id>]",
                "Full details on specific module(s)"));
        out.println(String.format("  %-22s %s", "-t, --test <id>[,<id>]",
                "Test specific module(s) for enablement"));
        out.println(String.format("  %-22s %s", "-e, --enable <id>[,<id>]",
                "Enable module(s)"));
        out.println(String.format("  %-22s %s", "-d, --disable <id>[,<id>]",
                "Disable module(s)"));
        out.println(String.format("  %-22s %s", "-f, --clean",
                "Clean disabled files instead of preserving them"));
        out.println();
    }

}